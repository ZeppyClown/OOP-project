package restaurantrush.domain;

import java.util.List;

public abstract class Customer {
    private final int id;
    private Restaurant restaurant;
    private int satisfaction = 100;
    private CustomerStatus status = CustomerStatus.WAITING;
    private Order order;
    private int servedTurn = -1;
    private int readyToPayTurn = -1;
    private int satisfactionAtPayment = -1;

    protected Customer(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("Customer ID must be positive: " + id);
        }
        this.id = id;
    }

    public final int getId(){
        return id;
    }

    public final int getSatisfaction(){
        return satisfaction;
    }

    public final CustomerStatus getStatus(){
        return status;
    }

    public final Order getOrder(){
        return order;
    }

    public final int getServedTurn(){
        return servedTurn;
    }

    public final int getReadyToPayTurn(){
        return readyToPayTurn;
    }

    public final int getSatisfactionAtPayment(){
        return satisfactionAtPayment;
    }

    public abstract String getTypeLabel();

    protected abstract int waitingLossPerTurn();

    /** Deterministic choice by ID so replays are repeatable. */
    public MenuItem chooseItem(Menu menu) {
        if (menu == null) { throw new IllegalArgumentException("Menu cannot be null"); }
        List<MenuItem> choices = menu.getItems();
        return choices.get((id - 1) % 3);
    }

    /** Registration is permanent, including after payment or departure. */
    void bindTo(Restaurant restaurant) {
        if (restaurant == null) {
            throw new IllegalArgumentException("Owning restaurant cannot be null");
        }
        if (this.restaurant != null && this.restaurant != restaurant) {
            throw new IllegalStateException("Customer " + id + " is already registered in another restaurant");
        }
        this.restaurant = restaurant;
    }

    String seat(){
        if (status != CustomerStatus.WAITING){
            return "Customer " + id + " cannot be seated as status is " + status;
        }
        this.status = CustomerStatus.SEATED;
        return null;
    }

    String placeOrder(MenuItem item, int billCents, int turn) {
        if (status != CustomerStatus.SEATED) {
            return "Customer " + id + " must be SEATED to order; currently " + status;
        }
        if (this.order != null) {
            return "Customer " + id + " already has an order";
        }
        if (item == null) {
            return "Selected menu item cannot be null";
        }
        if (turn <= 0 || billCents < 0) { return "Order needs a positive turn and nonnegative bill"; }
        this.order = new Order(this.id, this, item, billCents, turn);
        this.status = CustomerStatus.WAITING_FOR_FOOD;
        return null;
    }
    String serve(int turn) {
        if (status != CustomerStatus.WAITING_FOR_FOOD) {
            return "Customer " + id + " is not waiting for food; status is " + status;
        }
        if (this.order == null) {
            return "Customer " + id + " has no order to serve";
        }
        if (!this.order.canBeServedAt(turn)) {
            return "Order " + order.getId() + " is not ready to serve on turn " + turn;
        }
        String orderErr = this.order.markServed(turn);
        if (orderErr != null) {
            return orderErr;
        }
        this.status = CustomerStatus.SERVED;
        this.servedTurn = turn;
        return null;
    }

    String finishEating(int turn) {
        if (status != CustomerStatus.SERVED) {
            return "Customer " + id + " is not SERVED; status is " + status;
        }
        if (this.servedTurn >= turn) {
            return "Customer " + id + " cannot finish eating on their serving turn";
        }
        this.status = CustomerStatus.READY_TO_PAY;
        this.readyToPayTurn = turn;
        return null;
    }

    String pay(int turn) {
        if (status != CustomerStatus.READY_TO_PAY) {
            return "Customer " + id + " is not READY_TO_PAY; status is " + status;
        }
        if (this.readyToPayTurn >= turn) {
            return "Customer " + id + " cannot pay in the turn they became ready to pay";
        }
        if (this.order == null) {
            return "Customer " + id + " has no order to pay for";
        }
        String orderErr = this.order.markPaid(turn);
        if (orderErr != null) {
            return orderErr;
        }
        this.status = CustomerStatus.PAID;
        this.satisfactionAtPayment = this.satisfaction;
        return null;
    }

    public boolean isWaitingUnserved() {
        return status == CustomerStatus.WAITING
                || status == CustomerStatus.SEATED
                || status == CustomerStatus.WAITING_FOR_FOOD;
    }

    void loseWaitingSatisfaction() {
        if (!isWaitingUnserved()) {
            return;
        }
        this.satisfaction = Math.max(0, this.satisfaction - waitingLossPerTurn());
        if (this.satisfaction == 0) {
            this.status = CustomerStatus.LEFT;
            if (this.order != null && this.order.getStatus() != OrderStatus.PAID
                    && this.order.getStatus() != OrderStatus.SERVED) {
                String refusal = this.order.cancel();
                if (refusal != null) {
                    throw new IllegalStateException("Order cancel refused: " + refusal);
                }
            }
        }
    }

}
