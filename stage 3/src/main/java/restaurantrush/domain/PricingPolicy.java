package restaurantrush.domain;

/** Quotes a final bill at order placement; Order stores that quote unchanged. */
public interface PricingPolicy {
    int priceFor(Customer customer, MenuItem item);
}
