package restaurantrush.domain;
import java.util.List;

public final class Menu{
    private final List<MenuItem> choices;

    public Menu(){this(true);}

    public Menu(boolean includeCombo){
        MenuItem burger=new MenuItem("Burger",1500,1);
        MenuItem salad=new MenuItem("Salad",900,1);
        MenuItem pasta=new MenuItem("Pasta",1800,2);
        choices=includeCombo
                ? List.of(burger,salad,pasta,new ComboMeal(salad,burger))
                : List.of(burger,salad,pasta);
    }
    
    public List<MenuItem> getItems(){return choices;}
}
