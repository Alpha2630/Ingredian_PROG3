import java.util.List;
import java.util.Objects;

public class Dish {
    private Integer id;
    private Double price;
    private String name;
    private DishTypeEnum dishType;
    private List<DishIngredient> ingredients;

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Double getDishCost() {
        if (ingredients == null || ingredients.isEmpty()) {
            return 0.0;
        }

        double totalCost = 0;

        for (DishIngredient ingredient : ingredients) {
            if (ingredient.getRequiredQuantity() == null) {
                throw new IllegalStateException(
                        "Impossible de calculer le coût : quantité inconnue pour l’ingrédient "
                                + ingredient.getName());
            }

            totalCost += ingredient.getPrice() * ingredient.getRequiredQuantity();
        }

        return totalCost;
    }

    public Dish() {
    }

    public Dish(Integer id, String name, DishTypeEnum dishType, List<Ingredient> ingredients) {
        this.id = id;
        this.name = name;
        this.dishType = dishType;
        this.ingredients = ingredients;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DishTypeEnum getDishType() {
        return dishType;
    }

    public void setDishType(DishTypeEnum dishType) {
        this.dishType = dishType;
    }

    public List<Ingredient> getIngredients() {
        return ingredients;
    }

    public void setIngredients(List<DishIngredient> dishIngredients) {
        if (dishIngredients == null) {
            this.ingredients = null;
            return;
        }
        for (int i = 0; i < dishIngredients.size(); i++) {
            dishIngredients.get(i).setDish(this);
        }
        this.ingredients = dishIngredients;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        Dish dish = (Dish) o;
        return Objects.equals(id, dish.id) && Objects.equals(name, dish.name) && dishType == dish.dishType
                && Objects.equals(ingredients, dish.ingredients);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, dishType, ingredients);
    }

    @Override
    public String toString() {
        return "Dish{" +
                "id=" + id +
                ", price=" + price +
                ", name='" + name + '\'' +
                ", dishType=" + dishType +
                ", ingredients=" + ingredients +
                '}';
    }

    public Double getGrossMargin() {
        if (price == null) {
            throw new RuntimeException("Price is null");
        }
        return price - getDishCost();
    }
}
