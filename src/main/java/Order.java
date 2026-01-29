import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Order {
    private Integer id;
    private String reference;
    private Instant creationDatetime;
    private OrderTypeEnum orderType;
    private OrderStatusEnum orderStatus;
    private List<DishOrder> dishOrderList = new ArrayList<>();

    public Order() {
        this.orderStatus = OrderStatusEnum.CREATED;
    }

    public Order(Integer id, String reference, Instant creationDatetime) {
        this.id = id;
        this.reference = reference;
        this.creationDatetime = creationDatetime;
    }

    // Getters
    public Integer getId() {
        return id;
    }

    public String getReference() {
        return reference;
    }

    public Instant getCreationDatetime() {
        return creationDatetime;
    }

    public DishOrder[] getDishOrderList() {
        if (this.dishOrderList == null) {
            this.dishOrderList = new ArrayList<>();
        }
        return this.dishOrderList.toArray(new DishOrder[0]);
    }

    public OrderTypeEnum getOrderType() {
        return orderType;
    }

    public OrderStatusEnum getOrderStatus() {
        return orderStatus;
    }


    public Order setId(Integer id) {
        this.id = id;
        return this; // ← IMPORTANT: retourne this
    }

    public Order setReference(String reference) {
        this.reference = reference;
        return this; // ← IMPORTANT: retourne this
    }

    public Order setCreationDatetime(Instant creationDatetime) {
        this.creationDatetime = creationDatetime;
        return this; // ← IMPORTANT: retourne this
    }

    public Order setDishOrderList(List<DishOrder> dishOrderList) {
        if (dishOrderList == null) {
            this.dishOrderList = new ArrayList<>();
        } else {
            this.dishOrderList = new ArrayList<>(dishOrderList);
        }
        return this; // ← retourne this
    }

    public Order setOrderStatus(OrderStatusEnum orderStatus) {
        this.orderStatus = orderStatus;
        return this;
    }

    public Order setOrderType(OrderTypeEnum orderType) {
        this.orderType = orderType;
        return this;
    }

    public Order setDishOrderArray(DishOrder[] dishOrderArray) {
        if (dishOrderArray == null || dishOrderArray.length == 0) {
            this.dishOrderList = new ArrayList<>();
        } else {
            this.dishOrderList = new ArrayList<>(Arrays.asList(dishOrderArray));
        }
        return this; // ← retourne this
    }

    public Order addDishOrder(DishOrder dishOrder) {
        if (dishOrder != null) {
            if (this.dishOrderList == null) {
                this.dishOrderList = new ArrayList<>();
            }
            this.dishOrderList.add(dishOrder);
        }
        return this; // ← retourne this
    }
    public boolean isDelivered(){
        return OrderStatusEnum.DELIVERED.equals(this.orderStatus);
    }
    @Override
    public String toString() {
        return "Order{" +
                "id=" + id +
                ", reference='" + reference + '\'' +
                ", creationDatetime=" + creationDatetime +
                ", dishOrderCount=" + (dishOrderList != null ? dishOrderList.size() : 0) +
                '}';
    }
}