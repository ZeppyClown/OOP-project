package restaurantrush.domain;

public final class Cashier extends Staff {
    public Cashier() { super("Cashier"); }
    String collect(Customer customer,int turn){
        String reason=availabilityReason(turn);
        if (reason!=null)return reason;
        if (customer==null)return "Customer is required";
        reason=customer.pay(turn);
        if (reason==null)markTaskUsed(turn);
        return reason;
    }
}
