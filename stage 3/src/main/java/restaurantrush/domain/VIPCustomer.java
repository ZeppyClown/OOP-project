package restaurantrush.domain;

/** VIPs wait more patiently and pay 90 percent of the selected item's price. */
public final class VIPCustomer extends Customer {
    public VIPCustomer(int id) {
        super(id);
    }

    @Override
    public String getTypeLabel() {
        return "VIP";
    }

    @Override
    protected int waitingLossPerTurn() {
        return 5;
    }

    @Override
    public int applyDiscount(int cents) {
        return Money.applyPercent(cents, 90);
    }
}
