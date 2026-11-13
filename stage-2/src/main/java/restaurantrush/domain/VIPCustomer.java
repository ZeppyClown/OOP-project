package restaurantrush.domain;

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
