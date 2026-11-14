package restaurantrush.domain;

public final class CustomerTests {
    public static void main(String[] args) { runAll(); System.out.println("CustomerTests: 5 scenarios passed"); }
    public static void runAll() {
        choicesAndInitialState(); lifecycle(); invalidTransitions(); abandonment(); tableGuards();
    }
    private static void choicesAndInitialState() {
        Menu menu = new Menu(false);
        for (int id = 1; id <= 6; id++) {
            Customer c = new RegularCustomer(id);
            check(c.getSatisfaction() == 100 && c.getStatus() == CustomerStatus.WAITING, "initial state");
            check(c.getServedTurn() == -1 && c.getReadyToPayTurn() == -1
                    && c.getSatisfactionAtPayment() == -1 && c.getOrder() == null, "unset lifecycle values");
            check(c.chooseItem(menu) == menu.getItems().get((id - 1) % 3), "choice rotation");
        }
        try { new RegularCustomer(0); throw new AssertionError("invalid ID accepted"); }
        catch (IllegalArgumentException expected) { }
        try { new RegularCustomer(1).chooseItem(null); throw new AssertionError("null menu accepted"); }
        catch (IllegalArgumentException expected) { }
    }
    private static void lifecycle() {
        Customer c = new RegularCustomer(3);
        ok(c.seat()); c.loseWaitingSatisfaction(); check(c.getSatisfaction() == 92, "first loss");
        MenuItem pasta = c.chooseItem(new Menu(false));
        ok(c.placeOrder(pasta, 1800, 2)); ok(c.getOrder().advancePreparation(2));
        c.loseWaitingSatisfaction(); check(c.getSatisfaction() == 84, "second loss");
        ok(c.getOrder().advancePreparation(3)); c.loseWaitingSatisfaction();
        check(c.getSatisfaction() == 76, "READY still loses satisfaction");
        ok(c.serve(4)); c.loseWaitingSatisfaction();
        refused(c.finishEating(4)); ok(c.finishEating(5)); refused(c.pay(5)); ok(c.pay(6));
        c.loseWaitingSatisfaction();
        check(c.getStatus() == CustomerStatus.PAID && c.getOrder().getStatus() == OrderStatus.PAID, "payment sync");
        check(c.getSatisfactionAtPayment() == 76 && c.getSatisfaction() == 76, "frozen payment satisfaction");
        check(c.getServedTurn() == 4 && c.getReadyToPayTurn() == 5, "timestamps");
        refused(c.pay(7));
    }
    private static void invalidTransitions() {
        Customer c = new RegularCustomer(1); MenuItem item = c.chooseItem(new Menu(false));
        refused(c.placeOrder(item, 1500, 1)); refused(c.serve(1)); refused(c.finishEating(1)); refused(c.pay(1));
        check(c.getStatus() == CustomerStatus.WAITING && c.getOrder() == null, "unseated failures atomic");
        ok(c.seat()); refused(c.seat());
        refused(c.placeOrder(null, 1500, 2)); refused(c.placeOrder(item, -1, 2));
        refused(c.placeOrder(item, 1500, 0)); refused(c.placeOrder(item, 1500, -1));
        check(c.getOrder() == null && c.getStatus() == CustomerStatus.SEATED, "invalid order atomic");
        ok(c.placeOrder(item, 1500, 2)); Order order = c.getOrder();
        refused(c.placeOrder(item, 900, 3)); refused(c.serve(3));
        ok(order.advancePreparation(2)); refused(c.serve(2)); refused(c.serve(0)); ok(c.serve(3));
        for (int t : new int[] {-1, 0, 2, 3}) { refused(c.finishEating(t)); }
        check(c.getReadyToPayTurn() == -1 && c.getStatus() == CustomerStatus.SERVED, "early eating atomic");
        ok(c.finishEating(4));
        for (int t : new int[] {-1, 0, 3, 4}) { refused(c.pay(t)); }
        check(c.getSatisfactionAtPayment() == -1 && order.getStatus() == OrderStatus.SERVED, "early payment atomic");
    }
    private static void abandonment() {
        for (int progress = -1; progress <= 2; progress++) {
            Customer c = new RegularCustomer(3);
            if (progress >= 0) {
                ok(c.seat()); ok(c.placeOrder(c.chooseItem(new Menu(false)), 1800, 2));
                for (int i = 0; i < progress; i++) { ok(c.getOrder().advancePreparation(2 + i)); }
            }
            for (int i = 0; i < 12; i++) { c.loseWaitingSatisfaction(); }
            check(c.getSatisfaction() == 4 && c.isWaitingUnserved(), "12 losses");
            c.loseWaitingSatisfaction(); c.loseWaitingSatisfaction();
            check(c.getSatisfaction() == 0 && c.getStatus() == CustomerStatus.LEFT, "clamp and departure");
            if (c.getOrder() != null) { check(c.getOrder().getStatus() == OrderStatus.CANCELLED, "unfinished cancellation"); }
            refused(c.seat()); refused(c.serve(20)); refused(c.pay(20));
        }
    }
    private static void tableGuards() {
        Table table = new Table(1); Customer c = new RegularCustomer(1);
        refused(table.seat(null)); ok(table.seat(c)); refused(table.release());
        refused(new Table(2).seat(c)); refused(table.seat(new RegularCustomer(2)));
        check(table.getOccupant() == c, "table retained active occupant");
        for (int i = 0; i < 13; i++) { c.loseWaitingSatisfaction(); }
        ok(table.release()); ok(table.release()); check(table.isFree(), "terminal release");
    }
    static void check(boolean b, String message) { if (!b) { throw new AssertionError(message); } }
    static void ok(String reason) { check(reason == null, "Expected success: " + reason); }
    static void refused(String reason) { check(reason != null && !reason.isBlank(), "Expected explained refusal"); }
}
