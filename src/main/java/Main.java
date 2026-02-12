import java.time.Instant;
import java.util.Arrays;
import java.util.List;
public class Main {
    public static void main(String[] args) {
        DataRetriever dr = new DataRetriever();

        try {
            // 1. Créer une commande avec type et statut
            Order order = new Order()
                    .setReference("TEST-STATUS-001")
                    .setCreationDatetime(Instant.now())
                    .setOrderType(OrderTypeEnum.TAKE_AWAY)
                    .setOrderStatus(OrderStatusEnum.CREATED);

            // Ajouter un plat
            Dish dish = dr.findDishById(1);
            DishOrder dishOrder = new DishOrder();
                    dishOrder.setDish(dish);
                    dishOrder.setQuantity(1);
            order.setDishOrderList(Arrays.asList(dishOrder));

            // 2. Sauvegarder
            Order saved = dr.saveOrder(order);
            System.out.println("✅ Commande créée: " + saved.getId());
            System.out.println("   Type: " + saved.getOrderType());
            System.out.println("   Statut: " + saved.getOrderStatus());

            // 3. Mettre à jour le statut
            saved.setOrderStatus(OrderStatusEnum.READY);
            Order updated = dr.saveOrder(saved);
            System.out.println("\n✅ Statut mis à jour: " + updated.getOrderStatus());

            // 4. Marquer comme livré
            updated.setOrderStatus(OrderStatusEnum.DELIVERED);
            Order delivered = dr.saveOrder(updated);
            System.out.println("\n✅ Commande livrée: " + delivered.getOrderStatus());

            // 5. Essayer de modifier une commande livrée (DOIT ÉCHOUER)
            try {
                delivered.setOrderStatus(OrderStatusEnum.READY);
                dr.saveOrder(delivered);
                System.out.println("\n❌ DEVRAIT ÉCHOUER !");
            } catch (RuntimeException e) {
                System.out.println("\n✅ Exception correcte: " + e.getMessage());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}