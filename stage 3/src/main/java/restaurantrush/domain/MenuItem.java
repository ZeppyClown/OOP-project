package restaurantrush.domain;

public class MenuItem{
    private final String name;
    private final int priceCents;
    private final int preparationUnits;

    public MenuItem(String name,int priceCents,int preparationUnits){
        if (name==null || name.isBlank())throw new IllegalArgumentException("Menu item name must not be blank");
        if (priceCents<0 || preparationUnits<=0)throw new IllegalArgumentException("Price must be nonnegative and preparation units positive");
        this.name=name;
        this.priceCents=priceCents;
        this.preparationUnits=preparationUnits;
    }

    public String getName(){return name;}
    public int getPriceCents(){return priceCents;}
    public int getPreparationUnits(){return preparationUnits;}
}
