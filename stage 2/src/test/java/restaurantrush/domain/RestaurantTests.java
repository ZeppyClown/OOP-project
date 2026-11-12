package restaurantrush.domain;

import java.util.List;
import java.util.Objects;

/**
 * Restaurant: arrivals, lookups, seating through the queue, departures,
 * eating/payment ordering, totals and the victory rule.
 */
public final class RestaurantTests {
    private RestaurantTests() {
    }

    public static void main(String[] args) {
        runAll();
        System.out.println("RestaurantTests passed");
    }

    public static void runAll() {
        constructionGuards();
        startsEmptyWithTwoTables();
        arrivalGuards();
        lookups();
        exposedCollectionsAreReadOnly();
        seatingGuards();
        departureLeavesQueueAndFreesTable();
        paymentGuards();
        advanceEatingWaitsOneTurn();
        oldestEligiblePayerOrdering();
        totalsAndAverage();
        victoryThresholds();
    }

    private static void constructionGuards() {
        illegal(() -> new Restaurant(null, new Waiter(), new Chef(), new Cashier()));
        illegal(() -> new Restaurant(new Menu(), null, new Chef(), new Cashier()));
        illegal(() -> new Restaurant(new Menu(), new Waiter(), null, new Cashier()));
        illegal(() -> new Restaurant(new Menu(), new Waiter(), new Chef(), null));
    }

    private static void startsEmptyWithTwoTables() {
        Restaurant r = restaurant();
        equal(2, r.getTables().size(), "two tables");
        equal(1, r.getTables().get(0).getId(), "table 1");
        equal(2, r.getTables().get(1).getId(), "table 2");
        check(r.getTables().stream().allMatch(Table::isFree), "tables free");
        check(r.getCustomers().isEmpty() && r.getWaitingQueue().isEmpty(), "no customers");
        check(r.findFreeTable() == r.getTables().get(0), "first free table is table 1");
        equal(0, r.getRevenueCents(), "no revenue");
        equal(0, r.getPaidCount(), "no payers");
        equal(0, r.getServedCount(), "none served");
        equal(0, r.getUnhappyDepartureCount(), "no departures");
        check(r.getAveragePaidSatisfaction().isEmpty(), "average empty with no payers");
        equal(GameResult.DEFEAT, r.evaluateResult(), "empty restaurant is a defeat");
        check(r.oldestEligiblePayer(5) == null, "no payer");
    }

    private static void arrivalGuards() {
        Restaurant r = restaurant();
        illegal(() -> r.registerArrival(null));
        Customer c1 = new RegularCustomer(1);
        r.registerArrival(c1);
        try {
            r.registerArrival(new RegularCustomer(1));
            throw new AssertionError("duplicate id should throw");
        } catch (IllegalStateException expected) {
            // ok
        }
        Customer seated = new RegularCustomer(2);
        ok(seated.seat());
        illegal(() -> r.registerArrival(seated));
        equal(List.of(c1), r.getCustomers(), "rejected arrivals are not added");
        equal(List.of(c1), r.getWaitingQueue(), "rejected arrivals are not queued");

        Restaurant other = restaurant();
        try {
            other.registerArrival(c1);
            throw new AssertionError("customer bound elsewhere should throw");
        } catch (IllegalStateException expected) {
            // ok
        }
        check(other.getCustomers().isEmpty(), "other restaurant unchanged");
    }

    private static void lookups() {
        Restaurant r = restaurant();
        Customer c1 = arrive(r, 1);
        Customer c2 = arrive(r, 2);
        check(r.findCustomer(1) == c1 && r.findCustomer(2) == c2, "findCustomer");
        check(r.findCustomer(3) == null, "unknown customer");
        check(r.findWaitingCustomer(2) == c2, "waiting lookup");
        check(r.findOrder(1) == null, "no orders yet");
        check(r.findTableFor(c1) == null && r.findTableFor(null) == null, "no table yet");

        ok(r.seatCustomer(c1, 1));
        check(r.findWaitingCustomer(1) == null, "seated customer is no longer waiting");
        check(r.findTableFor(c1) == r.getTables().get(0), "C1 at table 1");
        check(r.findFreeTable() == r.getTables().get(1), "table 2 now first free");
        ok(r.getWaiter().takeOrder(c1, c1.chooseItem(r.getMenu()), 1500, 2));
        check(r.findOrder(1) == c1.getOrder(), "findOrder by id");
        check(r.findOrder(2) == null, "C2 has no order");
    }

    private static void exposedCollectionsAreReadOnly() {
        Restaurant r = restaurant();
        arrive(r, 1);
        unsupported(() -> r.getTables().clear());
        unsupported(() -> r.getCustomers().clear());
        unsupported(() -> r.getWaitingQueue().clear());
        List<Customer> snapshot = r.getWaitingQueue();
        arrive(r, 2);
        equal(1, snapshot.size(), "queue view is a snapshot");
        equal(2, r.getWaitingQueue().size(), "fresh view sees new arrival");
    }

    private static void seatingGuards() {
        Restaurant r = restaurant();
        Customer c1 = arrive(r, 1);
        Customer c2 = arrive(r, 2);
        Customer c3 = arrive(r, 3);
        refused(r.seatCustomer(null, 1));
        refused(r.seatCustomer(new RegularCustomer(1), 1));
        refused(r.seatCustomer(new RegularCustomer(9), 1));
        check(r.getWaiter().canAct(1), "lookup refusals do not use the Waiter");

        ok(r.seatCustomer(c1, 1));
        refused(r.seatCustomer(c1, 2));
        check(r.getWaiter().canAct(2), "re-seating a seated customer does not use the Waiter");
        ok(r.seatCustomer(c2, 2));
        refused(r.seatCustomer(c3, 3));
        equal(List.of(c3), r.getWaitingQueue(), "no free table keeps C3 queued");
        equal(CustomerStatus.WAITING, c3.getStatus(), "C3 still WAITING");
        check(r.getWaiter().canAct(3), "full restaurant does not use the Waiter");
    }

    private static void departureLeavesQueueAndFreesTable() {
        Restaurant r = restaurant();
        Customer c1 = arrive(r, 1);
        ok(r.seatCustomer(c1, 1));
        ok(r.getWaiter().takeOrder(c1, c1.chooseItem(r.getMenu()), 1500, 2));
        Customer c2 = arrive(r, 2);
        for (int i = 0; i < 12; i++) {
            r.applyWaitingLoss();
        }
        equal(4, c1.getSatisfaction(), "C1 at 4 after 12 losses");
        equal(0, r.getUnhappyDepartureCount(), "nobody left yet");
        r.applyWaitingLoss();
        equal(CustomerStatus.LEFT, c1.getStatus(), "C1 left");
        equal(CustomerStatus.LEFT, c2.getStatus(), "C2 left from the queue");
        equal(OrderStatus.CANCELLED, c1.getOrder().getStatus(), "C1 order cancelled");
        check(r.getTables().stream().allMatch(Table::isFree), "C1 table released");
        check(r.getWaitingQueue().isEmpty(), "C2 removed from queue");
        equal(2, r.getUnhappyDepartureCount(), "two departures");
        equal(2, r.getCustomers().size(), "departed customers stay on record");
        refused(r.seatCustomer(c2, 20));
    }

    private static void paymentGuards() {
        Restaurant r = restaurant();
        Customer c1 = arrive(r, 1);
        refused(r.collectPayment(null, 5));
        refused(r.collectPayment(new RegularCustomer(1), 5));
        refused(r.collectPayment(c1, 5));
        check(r.getCashier().canAct(5), "no-table refusal does not use the Cashier");
        ok(r.seatCustomer(c1, 1));
        refused(r.collectPayment(c1, 2));
        check(r.getCashier().canAct(2) && !r.findTableFor(c1).isFree(), "seated customer cannot pay");
    }

    private static void advanceEatingWaitsOneTurn() {
        Restaurant r = restaurant();
        Customer c1 = servedCustomer(r, 1, 1);
        r.advanceEating(3);
        equal(CustomerStatus.SERVED, c1.getStatus(), "still eating on serving turn");
        r.advanceEating(4);
        equal(CustomerStatus.READY_TO_PAY, c1.getStatus(), "ready the turn after serving");
        equal(4, c1.getReadyToPayTurn(), "ready turn recorded");
        r.advanceEating(5);
        equal(4, c1.getReadyToPayTurn(), "ready turn not overwritten");
        check(r.oldestEligiblePayer(4) == null, "not eligible on the ready turn");
        check(r.oldestEligiblePayer(5) == c1, "eligible the turn after");
    }

    private static void oldestEligiblePayerOrdering() {
        Restaurant r = restaurant();
        Customer c1 = servedCustomer(r, 1, 1);
        Customer c2 = servedCustomer(r, 2, 2);
        r.advanceEating(5);
        check(c1.getReadyToPayTurn() == 5 && c2.getReadyToPayTurn() == 5, "both ready on turn 5");
        check(r.oldestEligiblePayer(6) == c1, "tie on ready turn goes to lower id");
        ok(r.collectPayment(c1, 6));
        check(r.oldestEligiblePayer(6) == c2, "C2 next");
        refused(r.collectPayment(c2, 6));
        check(c2.getStatus() == CustomerStatus.READY_TO_PAY, "Cashier used: C2 waits");
        ok(r.collectPayment(c2, 7));
        check(r.oldestEligiblePayer(8) == null, "everyone paid");

        Restaurant later = restaurant();
        Customer b2 = servedCustomer(later, 2, 1);
        later.advanceEating(4);
        Customer a1 = servedCustomer(later, 1, 2);
        later.advanceEating(5);
        check(b2.getReadyToPayTurn() == 4 && a1.getReadyToPayTurn() == 5, "different ready turns");
        check(later.oldestEligiblePayer(6) == b2, "earlier ready turn beats lower id");
    }

    private static void totalsAndAverage() {
        Restaurant r = restaurant();
        Customer c1 = servedCustomer(r, 1, 1);
        Customer c2 = arrive(r, 2);
        ok(r.seatCustomer(c2, 4));
        for (int i = 0; i < 2; i++) {
            r.applyWaitingLoss();
        }
        equal(1, r.getServedCount(), "served counts SERVED");
        r.advanceEating(4);
        equal(1, r.getServedCount(), "served still counts READY_TO_PAY");
        ok(r.collectPayment(c1, 5));
        equal(1, r.getServedCount(), "served still counts PAID");
        equal(1500, r.getRevenueCents(), "revenue after one payment");
        equal(100, r.getPaidSatisfactionTotal(), "C1 paid at 100");
        check(r.getAveragePaidSatisfaction().getAsDouble() == 100.0, "average 100");
        equal(84, c2.getSatisfaction(), "C2 lost 16 while seated");
    }

    private static void victoryThresholds() {
        equal(GameResult.VICTORY, played(new int[]{60, 60, 60, 60}).evaluateResult(),
                "4 paid, $57.00, average exactly 60 wins");
        equal(GameResult.DEFEAT, played(new int[]{60, 60, 60}).evaluateResult(), "3 paid loses");
        Restaurant low = played(new int[]{60, 60, 60, 52});
        equal(232, low.getPaidSatisfactionTotal(), "total below 240");
        equal(GameResult.DEFEAT, low.evaluateResult(), "average 58 loses");

        Restaurant cheap = restaurant();
        for (int id = 1; id <= 4; id++) {
            payAtSatisfaction(cheap, id, 100, 1000);
        }
        equal(4000, cheap.getRevenueCents(), "$40.00 revenue");
        equal(GameResult.DEFEAT, cheap.evaluateResult(), "revenue below $45.00 loses");

        Restaurant exact = restaurant();
        for (int id = 1; id <= 4; id++) {
            payAtSatisfaction(exact, id, 100, 1125);
        }
        equal(4500, exact.getRevenueCents(), "exactly $45.00");
        equal(GameResult.VICTORY, exact.evaluateResult(), "exact revenue threshold wins");
    }

    // ---- helpers ----

    private static Restaurant restaurant() {
        return new Restaurant(new Menu(), new Waiter(), new Chef(), new Cashier());
    }

    private static Customer arrive(Restaurant r, int id) {
        Customer c = new RegularCustomer(id);
        r.registerArrival(c);
        return c;
    }

    /** Seated by the Waiter on seatTurn, Burger ordered and cooked on seatTurn+1, served seatTurn+2. */
    private static Customer servedCustomer(Restaurant r, int id, int seatTurn) {
        Customer c = arrive(r, id);
        ok(r.seatCustomer(c, seatTurn));
        MenuItem burger = r.getMenu().getItems().get(0);
        ok(c.placeOrder(burger, burger.getPriceCents(), seatTurn + 1));
        ok(c.getOrder().advancePreparation(seatTurn + 1));
        ok(c.serve(seatTurn + 2));
        return c;
    }

    /** One paid customer per entry, frozen at that satisfaction (100 - 8k), paying the menu price. */
    private static Restaurant played(int[] satisfactions) {
        Restaurant r = restaurant();
        for (int i = 0; i < satisfactions.length; i++) {
            payAtSatisfaction(r, i + 1, satisfactions[i], -1);
        }
        return r;
    }

    /** Registers and pays customer id at the target satisfaction; bill -1 means the menu price. */
    private static void payAtSatisfaction(Restaurant r, int id, int target, int bill) {
        check((100 - target) % 8 == 0 && target > 0, "target reachable in 8-point steps");
        Customer c = arrive(r, id);
        ok(c.seat());
        for (int k = 0; k < (100 - target) / 8; k++) {
            c.loseWaitingSatisfaction();
        }
        MenuItem item = c.chooseItem(r.getMenu());
        ok(c.placeOrder(item, bill < 0 ? item.getPriceCents() : bill, 1));
        for (int turn = 1; c.getOrder().getStatus() != OrderStatus.READY; turn++) {
            ok(c.getOrder().advancePreparation(turn));
        }
        ok(c.serve(5));
        ok(c.finishEating(6));
        ok(c.pay(7));
        equal(target, c.getSatisfactionAtPayment(), "frozen satisfaction");
    }

    private static void illegal(Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("Expected IllegalArgumentException");
    }

    private static void unsupported(Runnable action) {
        try {
            action.run();
        } catch (UnsupportedOperationException expected) {
            return;
        }
        throw new AssertionError("Expected UnsupportedOperationException");
    }

    private static void ok(String reason) { check(reason == null, "Expected success: " + reason); }
    private static void refused(String reason) { check(reason != null && !reason.isBlank(), "Expected refusal reason"); }
    private static void check(boolean value, String message) { if (!value) { throw new AssertionError(message); } }
    private static void equal(Object expected, Object actual, String message) {
        check(Objects.equals(expected, actual), message + ": expected " + expected + ", got " + actual);
    }
}
