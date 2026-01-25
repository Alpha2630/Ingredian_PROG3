import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DataRetriever {
    
    public Ingredient saveIngredient(Ingredient toSave) {
        Connection conn = null;
        try {
            conn = new DBConnection().getConnection();
            conn.setAutoCommit(false);
            
            Integer ingredientId = saveOrUpdateIngredient(conn, toSave);
            
            if (toSave.getStockMovementList() != null && !toSave.getStockMovementList().isEmpty()) {
                saveStockMovements(conn, ingredientId, toSave.getStockMovementList());
            }
            
            conn.commit();
            return findIngredientById(ingredientId);
            
        } catch (SQLException e) {
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException ex) {
                throw new RuntimeException("Error rolling back transaction", ex);
            }
            throw new RuntimeException("Error saving ingredient", e);
        } finally {
            if (conn != null) {
                try { conn.close(); } catch (SQLException e) 
            }
        }
    }
    
    private Integer saveOrUpdateIngredient(Connection conn, Ingredient ingredient) throws SQLException {
        String sql = """
            INSERT INTO ingredient (id, name, category, price)
            VALUES (?, ?, ?::ingredient_category, ?)
            ON CONFLICT (id) DO UPDATE
            SET name = EXCLUDED.name,
                category = EXCLUDED.category,
                price = EXCLUDED.price
            RETURNING id
        """;
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (ingredient.getId() != null) {
                ps.setInt(1, ingredient.getId());
            } else {
                ps.setNull(1, Types.INTEGER);
            }
            ps.setString(2, ingredient.getName());
            ps.setString(3, ingredient.getCategory().name());
            ps.setDouble(4, ingredient.getPrice());
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                } else {
                    throw new SQLException("Failed to save ingredient");
                }
            }
        }
    }
    
    private void saveStockMovements(Connection conn, Integer ingredientId, 
                                    List<StockMovement> movements) throws SQLException {
        String sql = """
            INSERT INTO stock_movement (id, ingredient_id, quantity, unit, movement_date)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT (id) DO NOTHING
        """;
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (StockMovement movement : movements) {
                if (movement.getId() != null) {
                    ps.setInt(1, movement.getId());
                } else {
                    ps.setNull(1, Types.INTEGER);
                }
                ps.setInt(2, ingredientId);
                ps.setDouble(3, movement.getQuantity());
                ps.setString(4, movement.getUnit() != null ? movement.getUnit().name() : "KG");
                ps.setTimestamp(5, Timestamp.valueOf(movement.getMovementDate()));
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }
    
    public Ingredient findIngredientById(Integer id) {
        String sql = """
            SELECT i.id, i.name, i.category, i.price,
                   sm.id AS movement_id, sm.quantity, sm.unit, sm.movement_date
            FROM ingredient i
            LEFT JOIN stock_movement sm ON i.id = sm.ingredient_id
            WHERE i.id = ?
            ORDER BY sm.movement_date DESC
        """;
        
        try (Connection conn = new DBConnection().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            
            Ingredient ingredient = null;
            List<StockMovement> movements = new ArrayList<>();
            
            while (rs.next()) {
                if (ingredient == null) {
                    ingredient = new Ingredient();
                    ingredient.setId(rs.getInt("id"));
                    ingredient.setName(rs.getString("name"));
                    ingredient.setCategory(CategoryEnum.valueOf(rs.getString("category")));
                    ingredient.setPrice(rs.getDouble("price"));
                }
                
                if (rs.getObject("movement_id") != null) {
                    StockMovement movement = new StockMovement();
                    movement.setId(rs.getInt("movement_id"));
                    movement.setIngredient(ingredient);
                    movement.setQuantity(rs.getDouble("quantity"));
                    
                    String unitStr = rs.getString("unit");
                    if (unitStr != null) {
                        movement.setUnit(UnitEnum.valueOf(unitStr));
                    }
                    
                    Timestamp timestamp = rs.getTimestamp("movement_date");
                    if (timestamp != null) {
                        movement.setMovementDate(timestamp.toLocalDateTime());
                    }
                    
                    movements.add(movement);
                }
            }
            
            if (ingredient == null) {
                throw new RuntimeException("Ingredient not found with id: " + id);
            }
            
            ingredient.setStockMovementList(movements);
            return ingredient;
            
        } catch (SQLException e) {
            throw new RuntimeException("Error finding ingredient", e);
        }
    }
    
    public Dish findDishById(Integer id) {
        String sql = """
            SELECT d.id, d.name, d.dish_type, d.price,
                   i.id AS ingredient_id, i.name AS ingredient_name,
                   i.category, i.price AS ingredient_price,
                   di.required_quantity, di.unit
            FROM dish d
            LEFT JOIN dish_ingredient di ON d.id = di.dish_id
            LEFT JOIN ingredient i ON di.ingredient_id = i.id
            WHERE d.id = ?
        """;
        
        try (Connection conn = new DBConnection().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            
            Dish dish = null;
            List<DishIngredient> dishIngredients = new ArrayList<>();
            
            while (rs.next()) {
                if (dish == null) {
                    dish = new Dish();
                    dish.setId(rs.getInt("id"));
                    dish.setName(rs.getString("name"));
                    dish.setDishType(DishTypeEnum.valueOf(rs.getString("dish_type")));
                    dish.setPrice(rs.getObject("price") == null ? null : rs.getDouble("price"));
                }
                
                if (rs.getObject("ingredient_id") != null) {
                    Ingredient ingredient = new Ingredient();
                    ingredient.setId(rs.getInt("ingredient_id"));
                    ingredient.setName(rs.getString("ingredient_name"));
                    ingredient.setCategory(CategoryEnum.valueOf(rs.getString("category")));
                    ingredient.setPrice(rs.getDouble("ingredient_price"));
                    
                    ingredient = findIngredientById(ingredient.getId());
                    
                    DishIngredient di = new DishIngredient();
                    di.setIngredient(ingredient);
                    di.setRequiredQuantity(rs.getObject("required_quantity") == null ? 
                        null : rs.getDouble("required_quantity"));
                    
                    String unitStr = rs.getString("unit");
                    if (unitStr != null) {
                        di.setUnit(UnitEnum.valueOf(unitStr));
                    }
                    
                    dishIngredients.add(di);
                }
            }
            
            if (dish == null) {
                throw new RuntimeException("Dish not found with id: " + id);
            }
            
            dish.setIngredients(dishIngredients);
            return dish;
            
        } catch (SQLException e) {
            throw new RuntimeException("Error finding dish", e);
        }
    }
    
    public Dish saveDish(Dish toSave) {
        Connection conn = null;
        try {
            conn = new DBConnection().getConnection();
            conn.setAutoCommit(false);
            
            Integer dishId = saveOrUpdateDish(conn, toSave);
            
            updateDishIngredients(conn, dishId, toSave.getIngredients());
            
            conn.commit();
            return findDishById(dishId);
            
        } catch (SQLException e) {
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException ex) {
                throw new RuntimeException("Error rolling back transaction", ex);
            }
            throw new RuntimeException("Error saving dish", e);
        } finally {
            if (conn != null) {
                try { conn.close(); } catch (SQLException e) 
            }
        }
    }
    
    private Integer saveOrUpdateDish(Connection conn, Dish dish) throws SQLException {
        String sql = """
            INSERT INTO dish (id, name, dish_type, price)
            VALUES (?, ?, ?::dish_type, ?)
            ON CONFLICT (id) DO UPDATE
            SET name = EXCLUDED.name,
                dish_type = EXCLUDED.dish_type,
                price = EXCLUDED.price
            RETURNING id
        """;
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (dish.getId() != null) {
                ps.setInt(1, dish.getId());
            } else {
                ps.setNull(1, Types.INTEGER);
            }
            ps.setString(2, dish.getName());
            ps.setString(3, dish.getDishType().name());
            ps.setDouble(4, dish.getPrice());
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                } else {
                    throw new SQLException("Failed to save dish");
                }
            }
        }
    }
    
    private void updateDishIngredients(Connection conn, Integer dishId, 
                                       List<DishIngredient> ingredients) throws SQLException {
        String deleteSql = "DELETE FROM dish_ingredient WHERE dish_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(deleteSql)) {
            ps.setInt(1, dishId);
            ps.executeUpdate();
        }
        
        if (ingredients != null && !ingredients.isEmpty()) {
            String insertSql = """
                INSERT INTO dish_ingredient (dish_id, ingredient_id, required_quantity, unit)
                VALUES (?, ?, ?, ?)
            """;
            
            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                for (DishIngredient di : ingredients) {
                    if (di.getIngredient() != null && di.getIngredient().getId() != null) {
                        ps.setInt(1, dishId);
                        ps.setInt(2, di.getIngredient().getId());
                        ps.setDouble(3, di.getRequiredQuantity());
                        ps.setString(4, di.getUnit() != null ? di.getUnit().name() : "KG");
                        ps.addBatch();
                    }
                }
                ps.executeBatch();
            }
        }
    }
    
    public StockMovement createStockMovement(Integer ingredientId, Double quantity, 
                                            UnitEnum unit, LocalDateTime date) {
        String sql = """
            INSERT INTO stock_movement (ingredient_id, quantity, unit, movement_date)
            VALUES (?, ?, ?, ?)
            RETURNING id
        """;
        
        try (Connection conn = new DBConnection().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, ingredientId);
            ps.setDouble(2, quantity);
            ps.setString(3, unit != null ? unit.name() : "KG");
            ps.setTimestamp(4, Timestamp.valueOf(date != null ? date : LocalDateTime.now()));
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    StockMovement movement = new StockMovement();
                    movement.setId(rs.getInt("id"));
                    movement.setQuantity(quantity);
                    movement.setUnit(unit);
                    movement.setMovementDate(date != null ? date : LocalDateTime.now());
                    return movement;
                } else {
                    throw new RuntimeException("Failed to create stock movement");
                }
            }
            
        } catch (SQLException e) {
            throw new RuntimeException("Error creating stock movement", e);
        }
    }
    
    public Double getCurrentStock(Integer ingredientId) {
        String sql = """
            SELECT COALESCE(SUM(quantity), 0) AS total_stock
            FROM stock_movement
            WHERE ingredient_id = ?
        """;
        
        try (Connection conn = new DBConnection().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, ingredientId);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                return rs.getDouble("total_stock");
            }
            return 0.0;
            
        } catch (SQLException e) {
            throw new RuntimeException("Error calculating stock", e);
        }
    }
    
    public void updateStockAfterSale(Integer dishId, Integer quantitySold) {
        try {
            Dish dish = findDishById(dishId);
            
            if (!dish.canBePrepared()) {
                throw new RuntimeException("Insufficient stock to prepare dish");
            }
            
            for (DishIngredient di : dish.getIngredients()) {
                if (di.getRequiredQuantity() != null && di.getIngredient() != null) {
                    Double quantityNeeded = di.getRequiredQuantity() * quantitySold;
                    createStockMovement(
                        di.getIngredient().getId(),
                        -quantityNeeded,
                        di.getUnit(),
                        LocalDateTime.now()
                    );
                }
            }
            
        } catch (Exception e) {
            throw new RuntimeException("Error updating stock after sale", e);
        }
    }
}