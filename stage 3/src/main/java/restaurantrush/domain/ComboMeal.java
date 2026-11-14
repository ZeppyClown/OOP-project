package restaurantrush.domain;

import java.util.List;

public final class ComboMeal extends MenuItem{
    private final MenuItem first;
    private final MenuItem second;
    private final List<MenuItem> components;

    public ComboMeal(MenuItem first,MenuItem second){
        super(nameFor(first,second),combinedPrice(first,second),combinedPreparation(first,second));
        this.first=first;
        this.second=second;
        this.components=List.of(first,second);
    }

    public MenuItem getFirst(){return first;}
    public MenuItem getSecond(){return second;}
    public List<MenuItem> getComponents(){return components;}

    private static String nameFor(MenuItem first,MenuItem second){
        if(first==null||second==null)throw new IllegalArgumentException("A combo requires two menu items");
        return first.getName()+"+"+second.getName()+" Combo";
    }

    private static int combinedPrice(MenuItem first,MenuItem second){
        try{return Money.applyPercent(Math.addExact(first.getPriceCents(),second.getPriceCents()),90);}
        catch(ArithmeticException overflow){throw new IllegalArgumentException("Combined component price exceeds integer cents",overflow);}
    }

    private static int combinedPreparation(MenuItem first,MenuItem second){
        try{return Math.addExact(first.getPreparationUnits(),second.getPreparationUnits());}
        catch(ArithmeticException overflow){throw new IllegalArgumentException("Combined preparation units exceed integer range",overflow);}
    }
}
