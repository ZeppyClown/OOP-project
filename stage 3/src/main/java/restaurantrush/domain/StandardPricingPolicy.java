package restaurantrush.domain;

public final class StandardPricingPolicy implements PricingPolicy{
    @Override
    public int priceFor(Customer customer,MenuItem item){
        if(customer==null || item==null)throw new IllegalArgumentException("Customer and menu item are required for pricing");
        return customer.applyDiscount(item.getPriceCents());
    }
}
