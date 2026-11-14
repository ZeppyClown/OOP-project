package restaurantrush.domain;

import java.util.List;
import java.util.Locale;

/** Focused checks use explicit failures, so they also run without -ea. */
public final class OrderTests {
    private OrderTests() { }

    public static void main(String[] args) {
        runAll();
        System.out.println("OrderTests: 9 scenarios passed");
    }

    public static void runAll() {
        menuAndMoney();
        constructionGuards();
        singleUnitAndLockedBill();
        pastaPreparationTiming();
        servingGuards();
        paymentRequiresEatingDelay();
        cancellationGuards();
        abandonmentCancelsPreparation();
        terminalOrdersRejectPreparation();
    }

    private static void menuAndMoney() {
        Menu menu = new Menu(false);
        List<MenuItem> choices = menu.getItems();
        equal(3, choices.size(), "three menu items");
        String[] names = {"Burger", "Salad", "Pasta"};
        int[] prices = {1500, 900, 1800};
        int[] units = {1, 1, 2};
        for (int i = 0; i < choices.size(); i++) {
            equal(names[i], choices.get(i).getName(), "choice order");
            equal(prices[i], choices.get(i).getPriceCents(), "full price");
            equal(units[i], choices.get(i).getPreparationUnits(), "preparation units");
        }
        try {
            choices.clear();
            throw new AssertionError("Menu must be immutable");
        } catch (UnsupportedOperationException expected) {
            equal(3, menu.getItems().size(), "failed mutation preserves menu");
        }
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.FRANCE);
            equal("$0.00", Money.format(0), "zero");
            equal("$0.05", Money.format(5), "fraction padding");
            equal("$15.00", Money.format(1500), "dollars");
            equal("-$0.05", Money.format(-5), "negative fraction");
            equal("-$21474836.48", Money.format(Integer.MIN_VALUE), "minimum int");
            equal("$21474836.47", Money.format(Integer.MAX_VALUE), "maximum int");
        } finally {
            Locale.setDefault(previous);
        }
    }

    private static void constructionGuards() {
        Customer customer = new RegularCustomer(1);
        MenuItem item = item(0);
        illegal(() -> new MenuItem(null, 900, 1));
        illegal(() -> new MenuItem(" ", 900, 1));
        illegal(() -> new MenuItem("Bad", -1, 1));
        illegal(() -> new MenuItem("Bad", 900, 0));
        illegal(() -> new Order(0, customer, item, 1500, 2));
        illegal(() -> new Order(1, null, item, 1500, 2));
        illegal(() -> new Order(1, customer, null, 1500, 2));
        illegal(() -> new Order(1, customer, item, -1, 2));
        illegal(() -> new Order(1, customer, item, 1500, 0));
    }

    private static void singleUnitAndLockedBill() {
        Customer customer = new RegularCustomer(1);
        MenuItem burger = item(0);
        Order order = new Order(7, customer, burger, burger.getPriceCents(), 2);
        equal(7, order.getId(), "ID");
        check(order.getCustomer() == customer && order.getItem() == burger, "references retained");
        equal(2, order.getPlacedTurn(), "placement turn");
        equal(OrderStatus.PLACED, order.getStatus(), "initial status");
        equal(-1, order.getReadyTurn(), "initial readiness");
        equal(-1, order.getServedTurn(), "initial serving");
        String before = snapshot(order);
        refused(order.advancePreparation(1));
        equal(before, snapshot(order), "before-placement attempt is atomic");
        ok(order.advancePreparation(2));
        equal(OrderStatus.READY, order.getStatus(), "same-turn first preparation permitted");
        equal(1, order.getCompletedPreparationUnits(), "one unit completed");
        equal(2, order.getReadyTurn(), "ready timestamp");
        equal(1500, order.getBillCents(), "bill unchanged by preparation");
    }

    private static void pastaPreparationTiming() {
        Order order = order(3, 2);
        ok(order.advancePreparation(2));
        equal(OrderStatus.PREPARING, order.getStatus(), "Pasta needs a second unit");
        equal(1, order.getCompletedPreparationUnits(), "Pasta 1/2");
        equal(-1, order.getReadyTurn(), "incomplete dish has no readiness");
        String before = snapshot(order);
        for (int turn : new int[] {2, 1, 0, -1}) {
            refused(order.advancePreparation(turn));
            equal(before, snapshot(order), "duplicate/backwards/nonpositive prep is atomic");
        }
        ok(order.advancePreparation(3));
        equal(2, order.getCompletedPreparationUnits(), "Pasta 2/2");
        equal(OrderStatus.READY, order.getStatus(), "Pasta ready");
        equal(3, order.getReadyTurn(), "Pasta readiness timestamp");
    }

    private static void servingGuards() {
        Order order = order(1, 2);
        String before = snapshot(order);
        refused(order.markServed(3));
        equal(before, snapshot(order), "unprepared serving is atomic");
        ok(order.advancePreparation(2));
        before = snapshot(order);
        for (int turn : new int[] {-1, 0, 1, 2}) {
            check(!order.canBeServedAt(turn), "serving must follow readiness");
            refused(order.markServed(turn));
            equal(before, snapshot(order), "early serving is atomic");
        }
        check(order.canBeServedAt(3), "next-turn serving eligible");
        ok(order.markServed(3));
        equal(3, order.getServedTurn(), "serving timestamp");
        before = snapshot(order);
        refused(order.markServed(4));
        refused(order.cancel());
        equal(before, snapshot(order), "served order cannot be served twice or cancelled");
    }

    private static void paymentRequiresEatingDelay() {
        Customer customer = new RegularCustomer(1);
        ok(new Table(1).seat(customer));
        ok(customer.placeOrder(item(0), 1500, 2));
        Order order = customer.getOrder();
        ok(order.advancePreparation(2));
        ok(customer.serve(3));
        check(!order.canBePaidAt(4), "SERVED alone is insufficient for payment");
        String before = snapshot(order);
        refused(order.markPaid(4));
        equal(before, snapshot(order), "payment before eating is atomic");
        ok(customer.finishEating(4));
        for (int turn : new int[] {-1, 0, 3, 4}) {
            check(!order.canBePaidAt(turn), "payment must follow readiness to pay");
            refused(order.markPaid(turn));
            equal(before, snapshot(order), "early payment leaves order unchanged");
        }
        check(order.canBePaidAt(5), "payment eligible on following turn");
        ok(customer.pay(5));
        equal(CustomerStatus.PAID, customer.getStatus(), "customer paid");
        equal(OrderStatus.PAID, order.getStatus(), "order paid");
        equal(100, customer.getSatisfactionAtPayment(), "payment satisfaction frozen");
        before = snapshot(order);
        refused(order.markPaid(6));
        equal(before, snapshot(order), "duplicate payment is atomic");
        equal(1500, order.getBillCents(), "bill retained through payment");
    }

    private static void cancellationGuards() {
        for (int progress = 0; progress <= 2; progress++) {
            Order order = order(3, 2);
            for (int i = 0; i < progress; i++) { ok(order.advancePreparation(2 + i)); }
            ok(order.cancel());
            equal(OrderStatus.CANCELLED, order.getStatus(), "cancel unserved state " + progress);
            String before = snapshot(order);
            refused(order.cancel());
            refused(order.advancePreparation(5));
            refused(order.markServed(5));
            refused(order.markPaid(5));
            check(!order.canBeServedAt(5) && !order.canBePaidAt(5), "cancelled order ineligible");
            equal(before, snapshot(order), "cancelled order remains unchanged");
        }
    }

    private static void abandonmentCancelsPreparation() {
        Customer customer = new RegularCustomer(3);
        ok(new Table(1).seat(customer));
        ok(customer.placeOrder(item(2), 1800, 2));
        ok(customer.getOrder().advancePreparation(2));
        for (int i = 0; i < 13; i++) { customer.loseWaitingSatisfaction(); }
        equal(CustomerStatus.LEFT, customer.getStatus(), "unserved customer leaves at zero");
        equal(OrderStatus.CANCELLED, customer.getOrder().getStatus(), "departure cancels food");
        String before = snapshot(customer.getOrder());
        refused(customer.getOrder().advancePreparation(14));
        equal(before, snapshot(customer.getOrder()), "departure prevents further cooking");
    }

    private static void terminalOrdersRejectPreparation() {
        Order ready = order(1, 2);
        ok(ready.advancePreparation(2));
        String before = snapshot(ready);
        refused(ready.advancePreparation(3));
        equal(before, snapshot(ready), "READY rejects extra unit");
        ok(ready.markServed(3));
        before = snapshot(ready);
        refused(ready.advancePreparation(4));
        equal(before, snapshot(ready), "SERVED rejects extra unit");
        Customer customer = new RegularCustomer(2);
        ok(new Table(2).seat(customer));
        ok(customer.placeOrder(item(1), 900, 2));
        ok(customer.getOrder().advancePreparation(2));
        ok(customer.serve(3));
        ok(customer.finishEating(4));
        ok(customer.pay(5));
        before = snapshot(customer.getOrder());
        refused(customer.getOrder().advancePreparation(6));
        refused(customer.getOrder().cancel());
        equal(before, snapshot(customer.getOrder()), "PAID rejects cooking and cancellation");
    }

    private static Order order(int id, int turn) {
        MenuItem item = item((id - 1) % 3);
        return new Order(id, new RegularCustomer(id), item, item.getPriceCents(), turn);
    }

    private static MenuItem item(int index) { return new Menu(false).getItems().get(index); }

    private static String snapshot(Order order) {
        return order.getStatus() + ":" + order.getCompletedPreparationUnits() + ":"
                + order.getReadyTurn() + ":" + order.getServedTurn() + ":"
                + order.getBillCents() + ":" + order.getPlacedTurn();
    }

    private static void illegal(Runnable action) {
        try {
            action.run();
            throw new AssertionError("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) { /* constructor guard */ }
    }

    private static void ok(String reason) { check(reason == null, "Expected success: " + reason); }
    private static void refused(String reason) { check(reason != null && !reason.isBlank(), "Expected refusal reason"); }
    private static void check(boolean value, String message) { if (!value) { throw new AssertionError(message); } }
    private static void equal(Object expected, Object actual, String message) {
        check(java.util.Objects.equals(expected, actual), message + ": expected " + expected + ", got " + actual);
    }
}
