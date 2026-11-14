package restaurantrush.domain;

public final class Chef extends Staff{
    public Chef(){super("Chef");}
    public String prepare(Order order,int turn){
        String reason=availabilityReason(turn);
        if(reason!=null)return reason;
        if(order==null)return "Order is required";
        reason=order.advancePreparation(turn);
        if(reason==null)markTaskUsed(turn);
        return reason;
    }
}
