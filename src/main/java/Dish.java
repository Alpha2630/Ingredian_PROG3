import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Dish {
    private Integer id;
    private Double price;
    private String name;
    private DishTypeEnum dishType;
    private List<DishIngredient> ingredients = new ArrayList<>();

    public Dish() {}

    public Dish(Integer id, String name, DishTypeEnum dishType, Double price) {
        this.id = id;
        this.name = name;
        this.dishType = dishType;
        this.price = price;
    }

    // Calcul du coût du plat
    public Double getDishCost() {
        if (ingredients == null || ingredients.isEmpty()) {
            return 0.0;
        }

        double totalCost = 0.0;
        for (DishIngredient dishIngredient : ingredients) {
            if (dishIngredient.getRequiredQuantity() == null || 
                dishIngredient.getIngredient() == null ||
                dishIngredient.getIngredient().getPrice() == null) {
                continue;
            }
            totalCost += dishIngredient.getRequiredQuantity() * 
                        dishIngredient.getIngredient().getPrice();
        }
        return totalCost;
    }

    // Calcul de la marge brute
    public Double getGrossMargin() {
        if (price == null) {
            throw new RuntimeException("Price is null for dish: " + name);
        }
        return price - getDishCost();
    }

    // Vérifie si le plat peut être préparé (stock suffisant)
    public boolean canBePrepared() {
        if (ingredients == null || ingredients.isEmpty()) {
            return false;
        }

        for (DishIngredient dishIngredient : ingredients) {
            if (dishIngredient.getIngredient() == null || 
                dishIngredient.getRequiredQuantity() == null) {
                return false;
            }
            
            Double currentStock = dishIngredient.getIngredient().getCurrentStock();
            if (currentStock == null || currentStock < dishIngredient.getRequiredQuantity()) {
                return false;
            }
        }
        return true;
    }

    // Getters et Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public DishTypeEnum getDishType() { return dishType; }
    public void setDishType(DishTypeEnum dishType) { this.dishType = dishType; }
    
    public List<DishIngredient> getIngredients() { return ingredients; }
    public void setIngredients(List<DishIngredient> ingredients) { 
        this.ingredients = ingredients;
        if (ingredients != null) {
            for (DishIngredient di : ingredients) {
                di.setDish(this);
            }
        }
    }

    public void addIngredient(DishIngredient dishIngredient) {
        if (ingredients == null) {
            ingredients = new ArrayList<>();
        }
        dishIngredient.setDish(this);
        ingredients.add(dishIngredient);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Dish dish = (Dish) o;
        return Objects.equals(id, dish.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Dish{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", dishType=" + dishType +
                ", price=" + price +
                ", dishCost=" + getDishCost() +
                ", grossMargin=" + getGrossMargin() +
                ", canBePrepared=" + canBePrepared() +
                ", ingredients=" + ingredients +
                '}';
    }
}