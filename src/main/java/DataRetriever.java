import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.time.Instant;

public class DataRetriever {

    Order findOrderByReference(String reference) {
        DBConnection dbConnection = new DBConnection();
        try (Connection connection = dbConnection.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement("""
                    select id, reference, creation_datetime, order_type, order_status from "order" where reference like ?""");
            preparedStatement.setString(1, reference);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                Order order = new Order();
                Integer idOrder = resultSet.getInt("id");
                order.setId(idOrder);
                order.setReference(resultSet.getString("reference"));
                order.setCreationDatetime(resultSet.getTimestamp("creation_datetime").toInstant());
                order.setDishOrderList(findDishOrderByIdOrder(idOrder));
                order.setOrderType(OrderTypeEnum.valueOf(resultSet.getString("order_type"))).setOrderStatus(OrderStatusEnum.valueOf(resultSet.getString("order_status")));
                return order;
            }
            throw new RuntimeException("Order not found with reference " + reference);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private List<DishOrder> findDishOrderByIdOrder(Integer idOrder) {
        DBConnection dbConnection = new DBConnection();
        Connection connection = dbConnection.getConnection();
        List<DishOrder> dishOrders = new ArrayList<>();
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(
                    """
                            select id, id_dish, quantity from dish_order where dish_order.id_order = ?
                            """);
            preparedStatement.setInt(1, idOrder);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                Dish dish = findDishById(resultSet.getInt("id_dish"));
                DishOrder dishOrder = new DishOrder();
                dishOrder.setId(resultSet.getInt("id"));
                dishOrder.setQuantity(resultSet.getInt("quantity"));
                dishOrder.setDish(dish);
                dishOrders.add(dishOrder);
            }
            dbConnection.closeConnection(connection);
            return dishOrders;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    Dish findDishById(Integer id) {
        DBConnection dbConnection = new DBConnection();
        Connection connection = dbConnection.getConnection();
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(
                    """
                            select dish.id as dish_id, dish.name as dish_name, dish_type, dish.selling_price as dish_price
                            from dish
                            where dish.id = ?;
                            """);
            preparedStatement.setInt(1, id);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                Dish dish = new Dish();
                dish.setId(resultSet.getInt("dish_id"));
                dish.setName(resultSet.getString("dish_name"));
                dish.setDishType(DishTypeEnum.valueOf(resultSet.getString("dish_type")));
                dish.setPrice(resultSet.getObject("dish_price") == null
                        ? null : resultSet.getDouble("dish_price"));
                dish.setDishIngredients(findIngredientByDishId(id));
                return dish;
            }
            dbConnection.closeConnection(connection);
            throw new RuntimeException("Dish not found " + id);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    Ingredient saveIngredient(Ingredient toSave) {
        String upsertIngredientSql = """
                    INSERT INTO ingredient (id, name, price, category)
                    VALUES (?, ?, ?, ?::dish_type)
                    ON CONFLICT (id) DO UPDATE
                    SET name = EXCLUDED.name,
                        category = EXCLUDED.category,
                        price = EXCLUDED.price
                    RETURNING id
                """;

        try (Connection conn = new DBConnection().getConnection()) {
            conn.setAutoCommit(false);
            Integer ingredientId;
            try (PreparedStatement ps = conn.prepareStatement(upsertIngredientSql)) {
                if (toSave.getId() != null) {
                    ps.setInt(1, toSave.getId());
                } else {
                    ps.setInt(1, getNextSerialValue(conn, "ingredient", "id"));
                }
                if (toSave.getPrice() != null) {
                    ps.setDouble(2, toSave.getPrice());
                } else {
                    ps.setNull(2, Types.DOUBLE);
                }
                ps.setString(3, toSave.getName());
                ps.setString(4, toSave.getCategory().name());
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    ingredientId = rs.getInt(1);
                }
            }

            insertIngredientStockMovements(conn, toSave);

            conn.commit();
            return findIngredientById(ingredientId);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    public Order saveOrder(Order orderToSave) {
        // Vérifier si la commande existe déjà (pour mise à jour)
        if (orderToSave.getId() != null) {
            Order existingOrder = findOrderById(orderToSave.getId());
            if (existingOrder.isDelivered()) {
                throw new RuntimeException("Impossible de modifier une commande déjà livrée (ID: " + orderToSave.getId() + ")");
            }
        }

        DBConnection dbConnection = new DBConnection();
        Connection connection = null;

        try {
            connection = dbConnection.getConnection();
            connection.setAutoCommit(false);

            // Vérifier les stocks (uniquement pour nouvelles commandes ou statut CREATED)
            if (orderToSave.getId() == null ||
                    orderToSave.getOrderStatus() == null ||
                    orderToSave.getOrderStatus() == OrderStatusEnum.CREATED) {
                checkStockAvailability(orderToSave, connection);
            }

            // Déterminer si c'est un insert ou update
            if (orderToSave.getId() == null) {
                // INSERT
                String insertOrderSQL = """
                INSERT INTO "order" (reference, creation_datetime, order_type, order_status) 
                VALUES (?, ?, ?::order_type_enum, ?::order_status_enum) 
                RETURNING id
                """;

                PreparedStatement orderStmt = connection.prepareStatement(insertOrderSQL);
                orderStmt.setString(1, orderToSave.getReference());
                orderStmt.setTimestamp(2, Timestamp.from(orderToSave.getCreationDatetime()));
                orderStmt.setString(3, orderToSave.getOrderType().name());
                orderStmt.setString(4,
                        orderToSave.getOrderStatus() != null ?
                                orderToSave.getOrderStatus().name() :
                                OrderStatusEnum.CREATED.name());

                ResultSet rs = orderStmt.executeQuery();
                if (!rs.next()) {
                    throw new RuntimeException("Échec de l'insertion de la commande");
                }

                int orderId = rs.getInt("id");
                orderToSave.setId(orderId);

                // Insérer les DishOrder
                insertDishOrders(connection, orderId, orderToSave.getDishOrderList());

                // Mettre à jour les stocks
                updateStockAfterOrder(orderToSave, connection);

            } else {
                // UPDATE
                String updateOrderSQL = """
                UPDATE "order" 
                SET order_type = ?::order_type_enum, 
                    order_status = ?::order_status_enum,
                    reference = ?
                WHERE id = ?
                """;

                PreparedStatement orderStmt = connection.prepareStatement(updateOrderSQL);
                orderStmt.setString(1, orderToSave.getOrderType().name());
                orderStmt.setString(2, orderToSave.getOrderStatus().name());
                orderStmt.setString(3, orderToSave.getReference());
                orderStmt.setInt(4, orderToSave.getId());

                int rowsUpdated = orderStmt.executeUpdate();
                if (rowsUpdated == 0) {
                    throw new RuntimeException("Échec de la mise à jour de la commande");
                }
            }

            connection.commit();

            // Retourner la commande mise à jour
            return findOrderById(orderToSave.getId());

        } catch (SQLException e) {
            try {
                if (connection != null) connection.rollback();
            } catch (SQLException ex) {
                // Ignorer
            }
            throw new RuntimeException("Erreur lors de la sauvegarde: " + e.getMessage(), e);
        } finally {
            try {
                if (connection != null) {
                    connection.setAutoCommit(true);
                    connection.close();
                }
            } catch (SQLException e) {
                // Ignorer
            }
        }
    }

    private void insertDishOrders(Connection connection, int orderId, DishOrder[] dishOrders) throws SQLException {
        if (dishOrders == null || dishOrders.length == 0) {
            return;
        }

        String sql = "INSERT INTO dish_order (id_order, id_dish, quantity) VALUES (?, ?, ?)";
        PreparedStatement stmt = connection.prepareStatement(sql);

        for (DishOrder dishOrder : dishOrders) {
            stmt.setInt(1, orderId);
            stmt.setInt(2, dishOrder.getDish().getId());
            stmt.setInt(3, dishOrder.getQuantity());
            stmt.addBatch();
        }

        stmt.executeBatch();
    }
    public Order updateOrderStatus(Integer orderId, OrderStatusEnum newStatus) {
        // Récupérer la commande
        Order order = findOrderById(orderId);

        // Vérifier si déjà livrée
        if (order.isDelivered()) {
            throw new RuntimeException("Impossible de modifier une commande déjà livrée");
        }

        // Mettre à jour le statut
        order.setOrderStatus(newStatus);

        // Sauvegarder (la méthode saveOrder vérifiera aussi isDelivered())
        return saveOrder(order);
    }

    public Order updateOrderType(Integer orderId, OrderTypeEnum newType) {
        // Récupérer la commande
        Order order = findOrderById(orderId);

        // Vérifier si déjà livrée
        if (order.isDelivered()) {
            throw new RuntimeException("Impossible de modifier une commande déjà livrée");
        }

        // Mettre à jour le type
        order.setOrderType(newType);

        // Sauvegarder
        return saveOrder(order);
    }
    private Order findOrderById(int orderId) {
        DBConnection dbConnection = new DBConnection();
        try (Connection connection = dbConnection.getConnection()) {
            String sql = """
            SELECT id, reference, creation_datetime, order_type, order_status 
            FROM "order" 
            WHERE id = ?
            """;

            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setInt(1, orderId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Order order = new Order();
                order.setId(rs.getInt("id"))
                        .setReference(rs.getString("reference"))
                        .setCreationDatetime(rs.getTimestamp("creation_datetime").toInstant())
                        .setOrderType(OrderTypeEnum.valueOf(rs.getString("order_type")))
                        .setOrderStatus(OrderStatusEnum.valueOf(rs.getString("order_status")))
                        .setDishOrderList(findDishOrderByIdOrder(orderId));

                return order;
            }
            throw new RuntimeException("Commande non trouvée avec ID: " + orderId);

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la recherche: " + e.getMessage(), e);
        }
    }

    private Order checkStockAvailability(Order order, Connection connection) {
        try {
            for (DishOrder dishOrder : order.getDishOrderList()) {
                Dish dish = dishOrder.getDish();
                int quantityNeeded = dishOrder.getQuantity();

                String ingredientsSQL = """
                SELECT di.id_ingredient, di.required_quantity, i.name as ingredient_name, di.unit
                FROM dish_ingredient di
                JOIN ingredient i ON i.id = di.id_ingredient
                WHERE di.id_dish = ?
                """;

                PreparedStatement ingredientsStmt = connection.prepareStatement(ingredientsSQL);
                ingredientsStmt.setInt(1, dish.getId());
                ResultSet ingredientsRs = ingredientsStmt.executeQuery();

                while (ingredientsRs.next()) {
                    int ingredientId = ingredientsRs.getInt("id_ingredient");
                    double requiredQuantityPerDish = ingredientsRs.getDouble("required_quantity");
                    String ingredientName = ingredientsRs.getString("ingredient_name");
                    String unit = ingredientsRs.getString("unit");

                    double totalQuantityNeeded = requiredQuantityPerDish * quantityNeeded;

                    String stockSQL = """
                    SELECT COALESCE(SUM(
                        CASE 
                            WHEN type = 'IN' THEN quantity
                            WHEN type = 'OUT' THEN -quantity
                        END
                    ), 0) as available_quantity
                    FROM stock_movement
                    WHERE id_ingredient = ?
                    """;

                    PreparedStatement stockStmt = connection.prepareStatement(stockSQL);
                    stockStmt.setInt(1, ingredientId);
                    ResultSet stockRs = stockStmt.executeQuery();

                    if (stockRs.next()) {
                        double availableQuantity = stockRs.getDouble("available_quantity");

                        if (availableQuantity < totalQuantityNeeded) {
                            throw new RuntimeException("Stock insuffisant pour: " + ingredientName
                                    + ". Disponible: " + availableQuantity + " " + unit
                                    + ", Nécessaire: " + totalQuantityNeeded + " " + unit);
                        }
                    }
                }
            }
            return order;

        } catch (SQLException e) {
            throw new RuntimeException("Erreur SQL lors de la vérification des stocks: " + e.getMessage(), e);
        }
    }

    private void updateStockAfterOrder(Order order, Connection connection) throws SQLException {
        for (DishOrder dishOrder : order.getDishOrderList()) {
            Dish dish = dishOrder.getDish();
            int quantityNeeded = dishOrder.getQuantity();

            String ingredientsSQL = """
            SELECT di.id_ingredient, di.required_quantity, di.unit
            FROM dish_ingredient di
            WHERE di.id_dish = ?
            """;

            PreparedStatement ingredientsStmt = connection.prepareStatement(ingredientsSQL);
            ingredientsStmt.setInt(1, dish.getId());
            ResultSet ingredientsRs = ingredientsStmt.executeQuery();

            while (ingredientsRs.next()) {
                int ingredientId = ingredientsRs.getInt("id_ingredient");
                double requiredQuantityPerDish = ingredientsRs.getDouble("required_quantity");
                String unit = ingredientsRs.getString("unit");
                double totalQuantityToDeduct = requiredQuantityPerDish * quantityNeeded;

                // Générer un nouvel ID
                int nextId = getNextSerialValue(connection, "stock_movement", "id");

                String insertStockSQL = """
                INSERT INTO stock_movement (id, id_ingredient, quantity, type, unit, creation_datetime)
                VALUES (?, ?, ?, 'OUT'::movement_type, ?::unit, ?)
                """;

                PreparedStatement stockStmt = connection.prepareStatement(insertStockSQL);
                stockStmt.setInt(1, nextId);
                stockStmt.setInt(2, ingredientId);
                stockStmt.setDouble(3, totalQuantityToDeduct);
                stockStmt.setString(4, unit);
                stockStmt.setTimestamp(5, new Timestamp(System.currentTimeMillis()));
                stockStmt.executeUpdate();
            }
        }
    }
    private void insertIngredientStockMovements(Connection conn, Ingredient ingredient) {
        List<StockMovement> stockMovementList = ingredient.getStockMovementList();
        String sql = """
                insert into stock_movement(id, id_ingredient, quantity, type, unit, creation_datetime)
                values (?, ?, ?, ?::movement_type, ?::unit, ?)
                on conflict (id) do nothing
                """;
        try {
            PreparedStatement preparedStatement = conn.prepareStatement(sql);
            for (StockMovement stockMovement : stockMovementList) {
                if (ingredient.getId() != null) {
                    preparedStatement.setInt(1, ingredient.getId());
                } else {
                    preparedStatement.setInt(1, getNextSerialValue(conn, "stock_movement", "id"));
                }
                preparedStatement.setInt(2, ingredient.getId());
                preparedStatement.setDouble(3, stockMovement.getValue().getQuantity());
                preparedStatement.setObject(4, stockMovement.getType());
                preparedStatement.setObject(5, stockMovement.getValue().getUnit());
                preparedStatement.setTimestamp(6, Timestamp.from(stockMovement.getCreationDatetime()));
                preparedStatement.addBatch();
            }
            preparedStatement.executeBatch();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    Ingredient findIngredientById(Integer id) {
        DBConnection dbConnection = new DBConnection();
        try (Connection connection = dbConnection.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement("select id, name, price, category from ingredient where id = ?;");
            preparedStatement.setInt(1, id);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                int idIngredient = resultSet.getInt("id");
                String name = resultSet.getString("name");
                CategoryEnum category = CategoryEnum.valueOf(resultSet.getString("category"));
                Double price = resultSet.getDouble("price");
                return new Ingredient(idIngredient, name, category, price, findStockMovementsByIngredientId(idIngredient));
            }
            throw new RuntimeException("Ingredient not found " + id);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    List<StockMovement> findStockMovementsByIngredientId(Integer id) {

        DBConnection dbConnection = new DBConnection();
        Connection connection = dbConnection.getConnection();
        List<StockMovement> stockMovementList = new ArrayList<>();
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(
                    """
                            select id, quantity, unit, type, creation_datetime
                            from stock_movement
                            where stock_movement.id_ingredient = ?;
                            """);
            preparedStatement.setInt(1, id);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                StockMovement stockMovement = new StockMovement();
                stockMovement.setId(resultSet.getInt("id"));
                stockMovement.setType(MovementTypeEnum.valueOf(resultSet.getString("type")));
                stockMovement.setCreationDatetime(resultSet.getTimestamp("creation_datetime").toInstant());

                StockValue stockValue = new StockValue();
                stockValue.setQuantity(resultSet.getDouble("quantity"));
                stockValue.setUnit(Unit.valueOf(resultSet.getString("unit")));
                stockMovement.setValue(stockValue);

                stockMovementList.add(stockMovement);
            }
            dbConnection.closeConnection(connection);
            return stockMovementList;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    Dish saveDish(Dish toSave) {
        String upsertDishSql = """
                    INSERT INTO dish (id, selling_price, name, dish_type)
                    VALUES (?, ?, ?, ?::dish_type)
                    ON CONFLICT (id) DO UPDATE
                    SET name = EXCLUDED.name,
                        dish_type = EXCLUDED.dish_type,
                        selling_price = EXCLUDED.selling_price
                    RETURNING id
                """;

        try (Connection conn = new DBConnection().getConnection()) {
            conn.setAutoCommit(false);
            Integer dishId;
            try (PreparedStatement ps = conn.prepareStatement(upsertDishSql)) {
                if (toSave.getId() != null) {
                    ps.setInt(1, toSave.getId());
                } else {
                    ps.setInt(1, getNextSerialValue(conn, "dish", "id"));
                }
                if (toSave.getPrice() != null) {
                    ps.setDouble(2, toSave.getPrice());
                } else {
                    ps.setNull(2, Types.DOUBLE);
                }
                ps.setString(3, toSave.getName());
                ps.setString(4, toSave.getDishType().name());
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    dishId = rs.getInt(1);
                }
            }

            List<DishIngredient> newDishIngredients = toSave.getDishIngredients();
            detachIngredients(conn, newDishIngredients);
            attachIngredients(conn, newDishIngredients);

            conn.commit();
            return findDishById(dishId);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Ingredient> createIngredients(List<Ingredient> newIngredients) {
        if (newIngredients == null || newIngredients.isEmpty()) {
            return List.of();
        }
        List<Ingredient> savedIngredients = new ArrayList<>();
        DBConnection dbConnection = new DBConnection();
        Connection conn = dbConnection.getConnection();
        try {
            conn.setAutoCommit(false);
            String insertSql = """
                        INSERT INTO ingredient (id, name, category, price)
                        VALUES (?, ?, ?::ingredient_category, ?)
                        RETURNING id
                    """;
            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                for (Ingredient ingredient : newIngredients) {
                    if (ingredient.getId() != null) {
                        ps.setInt(1, ingredient.getId());
                    } else {
                        ps.setInt(1, getNextSerialValue(conn, "ingredient", "id"));
                    }
                    ps.setString(2, ingredient.getName());
                    ps.setString(3, ingredient.getCategory().name());
                    ps.setDouble(4, ingredient.getPrice());

                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        int generatedId = rs.getInt(1);
                        ingredient.setId(generatedId);
                        savedIngredients.add(ingredient);
                    }
                }
                conn.commit();
                return savedIngredients;
            } catch (SQLException e) {
                conn.rollback();
                throw new RuntimeException(e);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dbConnection.closeConnection(conn);
        }
    }


    private void detachIngredients(Connection conn, List<DishIngredient> dishIngredients) {
        Map<Integer, List<DishIngredient>> dishIngredientsGroupByDishId = dishIngredients.stream()
                .collect(Collectors.groupingBy(dishIngredient -> dishIngredient.getDish().getId()));
        dishIngredientsGroupByDishId.forEach((dishId, dishIngredientList) -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM dish_ingredient where id_dish = ?")) {
                ps.setInt(1, dishId);
                ps.executeUpdate(); // TODO: must be a grouped by batch
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void attachIngredients(Connection conn, List<DishIngredient> ingredients)
            throws SQLException {

        if (ingredients == null || ingredients.isEmpty()) {
            return;
        }
        String attachSql = """
                    insert into dish_ingredient (id, id_ingredient, id_dish, required_quantity, unit)
                    values (?, ?, ?, ?, ?::unit)
                """;

        try (PreparedStatement ps = conn.prepareStatement(attachSql)) {
            for (DishIngredient dishIngredient : ingredients) {
                ps.setInt(1, getNextSerialValue(conn, "dish_ingredient", "id"));
                ps.setInt(2, dishIngredient.getIngredient().getId());
                ps.setInt(3, dishIngredient.getDish().getId());
                ps.setDouble(4, dishIngredient.getQuantity());
                ps.setObject(5, dishIngredient.getUnit());
                ps.addBatch(); // Can be substitute ps.executeUpdate() but bad performance
            }
            ps.executeBatch();
        }
    }

    private List<DishIngredient> findIngredientByDishId(Integer idDish) {
        DBConnection dbConnection = new DBConnection();
        Connection connection = dbConnection.getConnection();
        List<DishIngredient> dishIngredients = new ArrayList<>();
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(
                    """
                            select ingredient.id, ingredient.name, ingredient.price, ingredient.category, di.required_quantity, di.unit
                            from ingredient join dish_ingredient di on di.id_ingredient = ingredient.id where id_dish = ?;
                            """);
            preparedStatement.setInt(1, idDish);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                Ingredient ingredient = new Ingredient();
                ingredient.setId(resultSet.getInt("id"));
                ingredient.setName(resultSet.getString("name"));
                ingredient.setPrice(resultSet.getDouble("price"));
                ingredient.setCategory(CategoryEnum.valueOf(resultSet.getString("category")));

                DishIngredient dishIngredient = new DishIngredient();
                dishIngredient.setIngredient(ingredient);
                dishIngredient.setQuantity(resultSet.getObject("required_quantity") == null ? null : resultSet.getDouble("required_quantity"));
                dishIngredient.setUnit(Unit.valueOf(resultSet.getString("unit")));

                dishIngredients.add(dishIngredient);
            }
            dbConnection.closeConnection(connection);
            return dishIngredients;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    private String getSerialSequenceName(Connection conn, String tableName, String columnName)
            throws SQLException {

        String sql = "SELECT pg_get_serial_sequence(?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tableName);
            ps.setString(2, columnName);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(1);
                }
            }
        }
        return null;
    }

    private int getNextSerialValue(Connection conn, String tableName, String columnName)
            throws SQLException {

        String sequenceName = getSerialSequenceName(conn, tableName, columnName);
        if (sequenceName == null) {
            throw new IllegalArgumentException(
                    "Any sequence found for " + tableName + "." + columnName
            );
        }
        updateSequenceNextValue(conn, tableName, columnName, sequenceName);

        String nextValSql = "SELECT nextval(?)";

        try (PreparedStatement ps = conn.prepareStatement(nextValSql)) {
            ps.setString(1, sequenceName);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private void updateSequenceNextValue(Connection conn, String tableName, String columnName, String sequenceName) throws SQLException {
        String setValSql = String.format(
                "SELECT setval('%s', (SELECT COALESCE(MAX(%s), 0) FROM %s))",
                sequenceName, columnName, tableName
        );

        try (PreparedStatement ps = conn.prepareStatement(setValSql)) {
            ps.executeQuery();
        }
    }
    public StockValue getStockValueAt(Instant t, Integer ingredientIdentifier) {
        DBConnection dbConnection = new DBConnection();

        try (Connection connection = dbConnection.getConnection()) {
            String sql = """
            SELECT 
                unit,
                SUM(CASE 
                    WHEN type = 'IN' THEN quantity
                    WHEN type = 'OUT' THEN -quantity
                    ELSE 0
                END) as actual_quantity
            FROM stock_movement
            WHERE id_ingredient = ?
                AND creation_datetime <= ?
            GROUP BY id_ingredient, unit
            """;

            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setInt(1, ingredientIdentifier);
            stmt.setTimestamp(2, Timestamp.from(t));

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                StockValue stockValue = new StockValue();
                stockValue.setQuantity(rs.getDouble("actual_quantity"));
                stockValue.setUnit(Unit.valueOf(rs.getString("unit")));
                return stockValue;
            }


            StockValue stockValue = new StockValue();
            stockValue.setQuantity(0.0);

            String unitSQL = "SELECT unit FROM dish_ingredient WHERE id_ingredient = ? LIMIT 1";
            PreparedStatement unitStmt = connection.prepareStatement(unitSQL);
            unitStmt.setInt(1, ingredientIdentifier);
            ResultSet unitRs = unitStmt.executeQuery();
            if (unitRs.next()) {
                stockValue.setUnit(Unit.valueOf(unitRs.getString("unit")));
            } else {
                stockValue.setUnit(Unit.KG); // Unité par défaut
            }
            return stockValue;

        } catch (SQLException e) {
            throw new RuntimeException("Erreur calcul stock: " + e.getMessage(), e);
        }
    }
    public Double getDishCost(Integer dishId) {
        DBConnection dbConnection = new DBConnection();

        try (Connection connection = dbConnection.getConnection()) {
            String sql = """
            SELECT 
                SUM(di.required_quantity * i.price) as total_cost
            FROM dish_ingredient di
            JOIN ingredient i ON i.id = di.id_ingredient
            WHERE di.id_dish = ?
            GROUP BY di.id_dish
            """;

            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setInt(1, dishId);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getDouble("total_cost");
            }
            return 0.0;

        } catch (SQLException e) {
            throw new RuntimeException("Erreur calcul coût plat: " + e.getMessage(), e);
        }
    }
}
