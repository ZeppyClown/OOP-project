package restaurantrush.domain;

public final class HappyHourPricingPolicy implements PricingPolicy{
    @Override
    public int priceFor(Customer customer,MenuItem item){
        if (customer==null || item==null)throw new IllegalArgumentException("Customer and menu item are required for pricing");
        return Money.applyPercent(customer.applyDiscount(item.getPriceCents()),80);
    }
}
