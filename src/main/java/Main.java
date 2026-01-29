import java.time.Instant;
import java.util.Arrays;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== Test de saveOrder() ===");

        try {
            DataRetriever dataRetriever = new DataRetriever();

            System.out.println("\n1. Vérification des plats en base...");
            Dish testDish = dataRetriever.findDishById(1);

            if (testDish == null) {
                System.out.println("❌ Aucun plat avec ID=1 trouvé en base");
                System.out.println("   Veuillez d'abord insérer des plats dans la table dish");
                return;
            }

            System.out.println("   Plat trouvé: " + testDish.getName());


            System.out.println("\n2. Vérification des stocks...");
            System.out.println("   (Assurez-vous d'avoir des ingrédients en stock)");

            System.out.println("\n3. Création de la commande...");
            Order order = new Order();
            order.setReference("TEST-" + System.currentTimeMillis()); // Référence unique
            order.setCreationDatetime(Instant.now());


            DishOrder dishOrder = new DishOrder();
            dishOrder.setDish(testDish);
            dishOrder.setQuantity(1);

            order.setDishOrderList(List.of(new DishOrder[]{dishOrder}));

            System.out.println("   Référence: " + order.getReference());
            System.out.println("   Plat: " + testDish.getName());
            System.out.println("   Quantité: " + dishOrder.getQuantity());


            System.out.println("\n4. Sauvegarde de la commande...");
            Order savedOrder = dataRetriever.saveOrder(order);

            System.out.println("\n✅ COMMANDE SAUVEGARDÉE AVEC SUCCÈS !");
            System.out.println("   ID généré: " + savedOrder.getId());
            System.out.println("   Référence: " + savedOrder.getReference());
            System.out.println("   Date: " + savedOrder.getCreationDatetime());
            System.out.println("   Nombre de plats: " + savedOrder.getDishOrderList().length);

            System.out.println("\n5. Vérification par recherche...");
            Order retrievedOrder = dataRetriever.findOrderByReference(savedOrder.getReference());
            System.out.println("   Commande retrouvée: " + (retrievedOrder != null ? "✅" : "❌"));

            if (retrievedOrder != null) {
                System.out.println("   ID: " + retrievedOrder.getId());
                System.out.println("   Nombre de DishOrder: " + retrievedOrder.getDishOrderList().length);
            }

        } catch (RuntimeException e) {
            System.out.println("\n❌ ERREUR lors de saveOrder():");
            System.out.println("   Message: " + e.getMessage());

            if (e.getCause() != null) {
                System.out.println("   Cause: " + e.getCause().getMessage());
            }

            if (e.getMessage() != null && e.getMessage().contains("Stock insuffisant")) {
                System.out.println("\n💡 SOLUTION: Ajoutez des ingrédients en stock:");
                System.out.println("   INSERT INTO stock_movement (id_ingredient, quantity, type, unit)");
                System.out.println("   VALUES (1, 10.0, 'IN', 'KG'); -- Par exemple");
            }
        } catch (Exception e) {
            System.out.println("\n❌ Exception inattendue:");
            e.printStackTrace();
        }
    }
}