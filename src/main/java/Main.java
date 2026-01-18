public class Main {
    public static void main(String[] args) {
        DataRetriever dr = new DataRetriever();

        Dish dish = dr.findDishById(1);

        System.out.println("Coût du plat : " + dish.getDishCost());
        System.out.println("Marge brute : " + dish.getGrossMargin());
    }
}
