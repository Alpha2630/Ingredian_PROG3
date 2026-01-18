public class DishIngredient {
    private Ingredient ingredient;
    private Double requiredQuantity;
    private UnitEnum unit;

    public Ingredient getIngredient() { return ingredient; }
    public void setIngredient(Ingredient ingredient) { this.ingredient = ingredient; }

    public Double getRequiredQuantity() { return requiredQuantity; }
    public void setRequiredQuantity(Double requiredQuantity) {
        this.requiredQuantity = requiredQuantity;
    }

    public UnitEnum getUnit() { return unit; }
    public void setUnit(UnitEnum unit) { this.unit = unit; }
}
