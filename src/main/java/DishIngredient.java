import java.util.Objects;

public class DishIngredient {
    private Dish dish;
    private Ingredient ingredient;
    private Double requiredQuantity;
    private UnitEnum unit;

    public DishIngredient() {}

    public DishIngredient(Ingredient ingredient, Double requiredQuantity, UnitEnum unit) {
        this.ingredient = ingredient;
        this.requiredQuantity = requiredQuantity;
        this.unit = unit;
    }

    public Dish getDish() { return dish; }
    public void setDish(Dish dish) { this.dish = dish; }
    
    public Ingredient getIngredient() { return ingredient; }
    public void setIngredient(Ingredient ingredient) { this.ingredient = ingredient; }
    
    public Double getRequiredQuantity() { return requiredQuantity; }
    public void setRequiredQuantity(Double requiredQuantity) { 
        this.requiredQuantity = requiredQuantity; 
    }
    
    public UnitEnum getUnit() { return unit; }
    public void setUnit(UnitEnum unit) { this.unit = unit; }

    public String getName() {
        return ingredient != null ? ingredient.getName() : null;
    }

    public Double getPrice() {
        return ingredient != null ? ingredient.getPrice() : null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DishIngredient that = (DishIngredient) o;
        return Objects.equals(dish, that.dish) && 
               Objects.equals(ingredient, that.ingredient);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dish, ingredient);
    }

    @Override
    public String toString() {
        return "DishIngredient{" +
                "ingredient=" + (ingredient != null ? ingredient.getName() : "null") +
                ", requiredQuantity=" + requiredQuantity +
                ", unit=" + unit +
                '}';
    }
}