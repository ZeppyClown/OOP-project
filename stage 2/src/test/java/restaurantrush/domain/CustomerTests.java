package restaurantrush.domain;

import java.util.Objects;

/**
 * Customer and Table: lifecycle guards, satisfaction loss, departure and
 * the waitingLossPerTurn() polymorphic hook.
 */
public final class CustomerTests {
    private CustomerTests() {
    }

    public static void main(String[] args) {
        runAll();
        System.out.println("CustomerTests passed");
    }

    public static void runAll() {
        constructionAndDefaults();
        menuChoiceRotatesById();
        fullLifecycleHappyPath();
        seatingGuards();
        orderingGuards();
        servingGuards();
        eatingAndPaymentTiming();
        satisfactionLossAndDeparture();
        departureCancelsUnfinishedOrder();
        servedCustomersDoNotLoseSatisfaction();
        subclassControlsLossRate();
        bindingToOneRestaurant();
        tableSeatAndRelease();
    }

    private static final Menu menu = new Menu();

    private static void constructionAndDefaults() {
        illegal(() -> new RegularCustomer(0));
        illegal(() -> new RegularCustomer(-3));
        Customer c = new RegularCustomer(7);
        equal(7, c.getId(), "id");
        equal(100, c.getSatisfaction(), "starts at 100");
        equal(CustomerStatus.WAITING, c.getStatus(), "starts WAITING");
        check(c.getOrder() == null, "no order yet");
        equal(-1, c.getServedTurn(), "not served");
        equal(-1, c.getReadyToPayTurn(), "not ready");
        equal(-1, c.getSatisfactionAtPayment(), "not paid");
        equal("Regular", c.getTypeLabel(), "type label");
        equal(8, c.waitingLossPerTurn(), "regular loss rate");
        check(c.isWaitingUnserved(), "WAITING counts as unserved");
    }

    private static void menuChoiceRotatesById() {
        String[] expected = {"Burger", "Salad", "Pasta", "Burger", "Salad", "Pasta"};
        for (int id = 1; id <= expected.length; id++) {
            equal(expected[id - 1], new RegularCustomer(id).chooseItem(menu).getName(), "C" + id + " choice");
        }
        illegal(() -> new RegularCustomer(1).chooseItem(null));
    }

    private static void fullLifecycleHappyPath() {
        Customer c = new RegularCustomer(1);
        MenuItem burger = c.chooseItem(menu);
        ok(c.seat());
        equal(CustomerStatus.SEATED, c.getStatus(), "seated");
        ok(c.placeOrder(burger, burger.getPriceCents(), 2));
        equal(CustomerStatus.WAITING_FOR_FOOD, c.getStatus(), "waiting for food");
        Order order = c.getOrder();
        equal(1, order.getId(), "order id equals customer id");
        check(order.getCustomer() == c, "order points back to customer");
        equal(1500, order.getBillCents(), "bill");
        ok(order.advancePreparation(2));
        ok(c.serve(3));
        equal(CustomerStatus.SERVED, c.getStatus(), "served");
        equal(3, c.getServedTurn(), "served turn");
        check(!c.isWaitingUnserved(), "SERVED is not unserved");
        ok(c.finishEating(4));
        equal(CustomerStatus.READY_TO_PAY, c.getStatus(), "ready");
        equal(4, c.getReadyToPayTurn(), "ready turn");
        ok(c.pay(5));
        equal(CustomerStatus.PAID, c.getStatus(), "paid");
        equal(OrderStatus.PAID, order.getStatus(), "order paid");
        equal(100, c.getSatisfactionAtPayment(), "satisfaction frozen at payment");
    }

    private static void seatingGuards() {
        Customer c = new RegularCustomer(1);
        ok(c.seat());
        refused(c.seat());
        equal(CustomerStatus.SEATED, c.getStatus(), "double seat keeps status");
    }

    private static void orderingGuards() {
        MenuItem burger = menu.getItems().get(0);
        Customer waiting = new RegularCustomer(1);
        refused(waiting.placeOrder(burger, 1500, 1));
        check(waiting.getOrder() == null && waiting.getStatus() == CustomerStatus.WAITING, "unseated cannot order");

        Customer c = new RegularCustomer(2);
        ok(c.seat());
        refused(c.placeOrder(null, 1500, 1));
        refused(c.placeOrder(burger, 1500, 0));
        refused(c.placeOrder(burger, -1, 1));
        check(c.getOrder() == null && c.getStatus() == CustomerStatus.SEATED, "bad orders leave customer SEATED");
        ok(c.placeOrder(burger, 0, 1));
        equal(0, c.getOrder().getBillCents(), "zero bill allowed");
        refused(c.placeOrder(burger, 1500, 2));
        equal(0, c.getOrder().getBillCents(), "second order does not replace the first");
    }

    private static void servingGuards() {
        Customer seated = new RegularCustomer(1);
        ok(seated.seat());
        refused(seated.serve(2));

        Customer c = orderedCustomer(2, 2);
        refused(c.serve(3));
        equal(CustomerStatus.WAITING_FOR_FOOD, c.getStatus(), "unprepared food cannot be served");
        ok(c.getOrder().advancePreparation(3));
        refused(c.serve(3));
        equal(CustomerStatus.WAITING_FOR_FOOD, c.getStatus(), "cannot serve on the ready turn");
        ok(c.serve(4));
        refused(c.serve(5));
        equal(4, c.getServedTurn(), "double serve keeps the first turn");
    }

    private static void eatingAndPaymentTiming() {
        Customer c = orderedCustomer(1, 1);
        refused(c.finishEating(2));
        ok(c.getOrder().advancePreparation(1));
        ok(c.serve(2));
        refused(c.pay(3));
        refused(c.finishEating(2));
        equal(CustomerStatus.SERVED, c.getStatus(), "cannot finish on serving turn");
        ok(c.finishEating(3));
        refused(c.finishEating(4));
        refused(c.pay(3));
        equal(CustomerStatus.READY_TO_PAY, c.getStatus(), "cannot pay on ready turn");
        ok(c.pay(4));
        refused(c.pay(5));
        equal(OrderStatus.PAID, c.getOrder().getStatus(), "pays once");
    }

    private static void satisfactionLossAndDeparture() {
        Customer c = new RegularCustomer(1);
        for (int i = 1; i <= 12; i++) {
            c.loseWaitingSatisfaction();
            equal(100 - 8 * i, c.getSatisfaction(), "after " + i + " losses");
            equal(CustomerStatus.WAITING, c.getStatus(), "still waiting after " + i);
        }
        c.loseWaitingSatisfaction();
        equal(0, c.getSatisfaction(), "13th loss clamps to 0, never negative");
        equal(CustomerStatus.LEFT, c.getStatus(), "leaves at 0");
        check(!c.isWaitingUnserved(), "LEFT is not unserved");
        c.loseWaitingSatisfaction();
        equal(0, c.getSatisfaction(), "no loss after leaving");
        refused(c.seat());
    }

    private static void departureCancelsUnfinishedOrder() {
        Customer placed = orderedCustomer(1, 1);
        drain(placed);
        equal(CustomerStatus.LEFT, placed.getStatus(), "PLACED order customer leaves");
        equal(OrderStatus.CANCELLED, placed.getOrder().getStatus(), "PLACED order cancelled");

        Customer preparing = orderedCustomer(3, 1);
        ok(preparing.getOrder().advancePreparation(1));
        equal(OrderStatus.PREPARING, preparing.getOrder().getStatus(), "pasta half done");
        drain(preparing);
        equal(OrderStatus.CANCELLED, preparing.getOrder().getStatus(), "PREPARING order cancelled");

        Customer ready = orderedCustomer(2, 1);
        ok(ready.getOrder().advancePreparation(1));
        drain(ready);
        equal(OrderStatus.CANCELLED, ready.getOrder().getStatus(), "READY but unserved order cancelled");
        refused(ready.serve(20));
    }

    private static void servedCustomersDoNotLoseSatisfaction() {
        Customer c = orderedCustomer(1, 1);
        c.loseWaitingSatisfaction();
        c.loseWaitingSatisfaction();
        ok(c.getOrder().advancePreparation(2));
        ok(c.serve(3));
        for (int i = 0; i < 20; i++) {
            c.loseWaitingSatisfaction();
        }
        equal(84, c.getSatisfaction(), "loss stops once served");
        equal(CustomerStatus.SERVED, c.getStatus(), "served customer never leaves");
        ok(c.finishEating(4));
        c.loseWaitingSatisfaction();
        ok(c.pay(5));
        equal(84, c.getSatisfactionAtPayment(), "payment records the frozen value");
    }

    private static void subclassControlsLossRate() {
        Customer impatient = new Customer(9) {
            @Override public String getTypeLabel() { return "Impatient"; }
            @Override protected int waitingLossPerTurn() { return 30; }
        };
        int[] expected = {70, 40, 10, 0};
        for (int value : expected) {
            impatient.loseWaitingSatisfaction();
            equal(value, impatient.getSatisfaction(), "impatient loss");
        }
        equal(CustomerStatus.LEFT, impatient.getStatus(), "impatient leaves after 4 turns");
        equal("Impatient", impatient.getTypeLabel(), "subclass label");
    }

    private static void bindingToOneRestaurant() {
        Customer c = new RegularCustomer(1);
        Restaurant first = restaurant();
        Restaurant second = restaurant();
        illegal(() -> c.bindTo(null));
        c.bindTo(first);
        c.bindTo(first);
        try {
            c.bindTo(second);
            throw new AssertionError("second restaurant should be rejected");
        } catch (IllegalStateException expected) {
            // ok
        }
    }

    private static void tableSeatAndRelease() {
        illegal(() -> new Table(0));
        Table t = new Table(1);
        check(t.isFree() && t.getOccupant() == null, "new table free");
        ok(t.release());
        refused(t.seat(null));
        Customer c = new RegularCustomer(1);
        ok(t.seat(c));
        check(t.getOccupant() == c && !t.isFree(), "occupied");
        equal(CustomerStatus.SEATED, c.getStatus(), "table seating seats the customer");
        refused(t.seat(new RegularCustomer(2)));
        check(t.getOccupant() == c, "occupied table keeps its customer");
        refused(t.release());
        check(t.getOccupant() == c, "cannot release a SEATED customer");

        Table other = new Table(2);
        refused(other.seat(c));
        check(other.isFree(), "already-seated customer cannot take a second table");

        drain(c);
        ok(t.release());
        check(t.isFree(), "LEFT customer frees the table");
    }

    // ---- helpers ----

    private static Restaurant restaurant() {
        return new Restaurant(new Menu(), new Waiter(), new Chef(), new Cashier());
    }

    /** Seated customer with an order placed on the given turn. */
    private static Customer orderedCustomer(int id, int turn) {
        Customer c = new RegularCustomer(id);
        ok(c.seat());
        MenuItem item = c.chooseItem(menu);
        ok(c.placeOrder(item, item.getPriceCents(), turn));
        return c;
    }

    private static void drain(Customer c) {
        for (int i = 0; i < 20 && c.getStatus() != CustomerStatus.LEFT; i++) {
            c.loseWaitingSatisfaction();
        }
        equal(CustomerStatus.LEFT, c.getStatus(), "customer left");
    }

    private static void illegal(Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("Expected IllegalArgumentException");
    }

    private static void ok(String reason) { check(reason == null, "Expected success: " + reason); }
    private static void refused(String reason) { check(reason != null && !reason.isBlank(), "Expected refusal reason"); }
    private static void check(boolean value, String message) { if (!value) { throw new AssertionError(message); } }
    private static void equal(Object expected, Object actual, String message) {
        check(Objects.equals(expected, actual), message + ": expected " + expected + ", got " + actual);
    }
}
