package restaurantrush.domain;

public class RegularCustomer extends Customer {

    public RegularCustomer(int id) {
        super(id);
    }

    @Override
    public String getTypeLabel() {
        return "Regular";
    }

    @Override
    protected int waitingLossPerTurn() {
        return 8;
    }
}