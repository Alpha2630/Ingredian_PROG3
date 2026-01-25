import java.time.LocalDateTime;

public class StockMovement {
    private Integer id;
    private Ingredient ingredient;
    private Double quantity;
    private UnitEnum unit;
    private LocalDateTime movementDate;

    public StockMovement() {}

    public StockMovement(Integer id, Ingredient ingredient, Double quantity, UnitEnum unit, LocalDateTime movementDate) {
        this.id = id;
        this.ingredient = ingredient;
        this.quantity = quantity;
        this.unit = unit;
        this.movementDate = movementDate;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    
    public Ingredient getIngredient() { return ingredient; }
    public void setIngredient(Ingredient ingredient) { this.ingredient = ingredient; }
    
    public Double getQuantity() { return quantity; }
    public void setQuantity(Double quantity) { this.quantity = quantity; }
    
    public UnitEnum getUnit() { return unit; }
    public void setUnit(UnitEnum unit) { this.unit = unit; }
    
    public LocalDateTime getMovementDate() { return movementDate; }
    public void setMovementDate(LocalDateTime movementDate) { this.movementDate = movementDate; }

    @Override
    public String toString() {
        return "StockMovement{" +
                "id=" + id +
                ", ingredient=" + (ingredient != null ? ingredient.getName() : "null") +
                ", quantity=" + quantity +
                ", unit=" + unit +
                ", movementDate=" + movementDate +
                '}';
    }
}