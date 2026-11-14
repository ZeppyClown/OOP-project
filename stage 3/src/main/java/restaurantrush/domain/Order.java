package restaurantrush.domain;

public final class Order{
    private final int id;
    private final Customer customer;
    private final MenuItem item;
    private final int billCents;
    private final int placedTurn;
    private OrderStatus status=OrderStatus.PLACED;
    private int completedPreparationUnits;
    private int lastPreparationTurn=-1;
    private int readyTurn=-1;
    private int servedTurn=-1;

    Order(int id,Customer customer,MenuItem item,int billCents,int placedTurn){
        if (id<=0 || customer==null || item==null || billCents<0 || placedTurn<=0)throw new IllegalArgumentException("Order needs a positive ID/turn,customer,item and nonnegative bill");
        this.id=id;
        this.customer=customer;
        this.item=item;
        this.billCents=billCents;
        this.placedTurn=placedTurn;
    }

    public int getId(){return id;}
    public Customer getCustomer(){return customer;}
    public MenuItem getItem(){return item;}
    public OrderStatus getStatus(){return status;}
    public int getBillCents(){return billCents;}
    public int getPlacedTurn(){return placedTurn;}
    public int getReadyTurn(){return readyTurn;}
    public int getServedTurn(){return servedTurn;}
    public int getCompletedPreparationUnits(){return completedPreparationUnits;}
    public boolean canBeServedAt(int turn){return turn>0 && status==OrderStatus.READY && readyTurn<turn;}
    public boolean canBePaidAt(int turn){return turn>0 && status==OrderStatus.SERVED && servedTurn<turn && customer.getStatus()==CustomerStatus.READY_TO_PAY && customer.getReadyToPayTurn()>servedTurn && customer.getReadyToPayTurn()<turn;}

    String advancePreparation(int turn){
        if (turn<=0 || turn<placedTurn)return "Preparation turn must be positive and not before order placement";
        if (status !=OrderStatus.PLACED && status !=OrderStatus.PREPARING)return "Order "+id+" is not awaiting preparation: "+status;
        if (turn<=lastPreparationTurn)return "Order "+id+" already received preparation on this or a later turn";
        completedPreparationUnits++;
        lastPreparationTurn=turn;
        if (completedPreparationUnits==item.getPreparationUnits()){
            status=OrderStatus.READY;
            readyTurn=turn;
        } 
        else status=OrderStatus.PREPARING;
        return null;
    }

    String markServed(int turn){
        if (!canBeServedAt(turn))return "Order "+id+" must be READY on an earlier turn before serving";
        status=OrderStatus.SERVED;
        servedTurn=turn;
        return null;
    }

    String markPaid(int turn){
        if (!canBePaidAt(turn))return "Order "+id+" needs a customer READY_TO_PAY on an earlier turn";
        status=OrderStatus.PAID;
        return null;
    }

    String cancel(){
        if (status !=OrderStatus.PLACED && status !=OrderStatus.PREPARING && status !=OrderStatus.READY)return "Order "+id+" cannot be cancelled while "+status;
        status=OrderStatus.CANCELLED;
        return null;
    }
}
