import java.time.Instant;
import java.util.Arrays;
import java.util.List;
public class Main {
    public static void main(String[] args) {
        DataRetriever dr = new DataRetriever();


        StockValue stock1 = dr.getStockValueAt(Instant.now(), 1);
        System.out.println("Stock ingrédient 1: " + stock1.getQuantity() + " " + stock1.getUnit());


        Double cost = dr.getDishCost(1);
        System.out.println("Coût plat 1: " + cost);


        Double margin = dr.getGrossMargin(1);
        System.out.println("Marge brute plat 1: " + margin);


        Instant start = Instant.parse("2026-01-01T00:00:00Z");
        Instant end = Instant.parse("2026-01-31T23:59:59Z");
        dr.printStockStatistics("DAY", start, end);
    }
}