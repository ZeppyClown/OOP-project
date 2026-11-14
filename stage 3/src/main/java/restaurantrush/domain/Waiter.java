package restaurantrush.domain;

public final class Waiter extends Staff{
    public Waiter(){super("Waiter");}

    String seat(Customer customer,Table table,int turn){
        String reason=availabilityReason(turn);
        if(reason!=null){return reason;}
        if(customer==null || table==null){return "Customer and table are required";}
        reason=table.seat(customer);
        if(reason==null){markTaskUsed(turn);}
        return reason;
    }

    public String takeOrder(Customer customer,MenuItem item,int billCents,int turn){
        String reason=availabilityReason(turn);
        if(reason!=null){return reason;}
        if(customer==null){return "Customer is required";}
        reason=customer.placeOrder(item,billCents,turn);
        if(reason==null){markTaskUsed(turn);}
        return reason;
    }

    public String serve(Customer customer,int turn){
        String reason=availabilityReason(turn);
        if(reason!=null){return reason;}
        if(customer==null){return "Customer is required";}
        reason=customer.serve(turn);
        if(reason==null){markTaskUsed(turn);}
        return reason;
    }
}
