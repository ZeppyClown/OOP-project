package restaurantrush.domain;
import java.util.List;

public final class Menu{
    private final List<MenuItem> choices=List.of(
        new MenuItem("Burger", 1500, 1),
        new MenuItem("Salad", 900, 1),
        new MenuItem("Pasta", 1800, 2)
    );
    public List<MenuItem> getItems(){return choices;}
}
