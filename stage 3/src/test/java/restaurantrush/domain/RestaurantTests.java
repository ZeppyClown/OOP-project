package restaurantrush.domain;

import java.util.List;
import static restaurantrush.domain.CustomerTests.*;

public final class RestaurantTests {
    public static void main(String[] args) { runAll(); System.out.println("RestaurantTests: 11 scenarios passed"); }
    public static void runAll() {
        arrivalsAndSeating(); exclusiveOwnership(); failedArrivalDoesNotClaimOwnership();
        cleanup(); paymentAndPriority(); thresholds(); victorySchedule(); defeatSchedule();
        hostConstructionAndOwnership(); independentSeatingCapacity(); hostCannotAnticipatePayment();
    }
    private static Restaurant restaurant() { return new Restaurant(new Menu(false), new Waiter(), new Chef(), new Cashier()); }
    private static Customer arrive(Restaurant r, int id) { Customer c = new RegularCustomer(id); r.registerArrival(c); return c; }
    private static void arrivalsAndSeating() {
        Restaurant r = restaurant(); Customer a = arrive(r, 1), b = arrive(r, 2), c = arrive(r, 3);
        check(r.getTables().size() == 2 && r.getAveragePaidSatisfaction().isEmpty(), "initial aggregate");
        try { r.registerArrival(new RegularCustomer(1)); throw new AssertionError("duplicate accepted"); }
        catch (IllegalStateException expected) { }
        for (List<?> list : List.of(r.getCustomers(), r.getTables(), r.getWaitingQueue())) {
            try { list.clear(); throw new AssertionError("mutable query"); }
            catch (UnsupportedOperationException expected) { }
        }
        refused(r.seatCustomer(new RegularCustomer(2), 1));
        ok(r.seatCustomer(b, 1)); check(r.getWaitingQueue().equals(List.of(a, c)), "select second queued customer");
        refused(r.seatCustomer(a, 1)); check(r.findWaitingCustomer(1) == a, "capacity refusal retains queue");
        ok(r.seatCustomer(a, 2)); refused(r.seatCustomer(c, 3));
        check(r.findFreeTable() == null && r.findWaitingCustomer(3) == c, "third seating atomic");
        check(r.findCustomer(99) == null && r.findOrder(99) == null && r.findTableFor(null) == null, "missing queries");
    }
    private static void exclusiveOwnership() {
        Restaurant owner = restaurant(), other = restaurant();
        Customer customer = arrive(owner, 1), resident = arrive(other, 2);
        try { other.registerArrival(customer); throw new AssertionError("shared customer accepted"); }
        catch (IllegalStateException expected) { }
        check(owner.getCustomers().equals(List.of(customer))
                && owner.getWaitingQueue().equals(List.of(customer)), "owner retains sole registration");
        check(other.getCustomers().equals(List.of(resident))
                && other.getWaitingQueue().equals(List.of(resident)), "rejected registration leaves target unchanged");
        check(customer.getStatus() == CustomerStatus.WAITING && customer.getSatisfaction() == 100
                && customer.getOrder() == null, "rejected registration leaves customer unchanged");
        refused(other.seatCustomer(customer, 1));
        refused(other.collectPayment(customer, 1));
        check(other.getTables().stream().allMatch(Table::isFree)
                && other.getWaiter().getTaskTurn() == -1 && other.getCashier().getTaskTurn() == -1,
                "foreign operations preserve tables and staff capacity");
        ok(other.seatCustomer(resident, 1));
        ok(owner.seatCustomer(customer, 1));
        check(owner.getWaitingQueue().isEmpty() && owner.findTableFor(customer) != null
                && other.findTableFor(customer) == null, "seating changes only the owning aggregate");
        ready(customer, 4); ok(owner.collectPayment(customer, 5));
        try { other.registerArrival(customer); throw new AssertionError("paid customer transferred"); }
        catch (IllegalArgumentException | IllegalStateException expected) { }
        check(owner.getCustomers().equals(List.of(customer)) && owner.getRevenueCents() == 1500
                && other.getCustomers().equals(List.of(resident)) && other.getRevenueCents() == 0,
                "paid history remains exclusively with owner");
    }
    private static void failedArrivalDoesNotClaimOwnership() {
        Restaurant fullId = restaurant(), eventualOwner = restaurant();
        Customer existing = arrive(fullId, 1), candidate = new RegularCustomer(1);
        try { fullId.registerArrival(candidate); throw new AssertionError("duplicate ID accepted"); }
        catch (IllegalStateException expected) { }
        eventualOwner.registerArrival(candidate);
        check(fullId.getCustomers().equals(List.of(existing)) && fullId.getWaitingQueue().equals(List.of(existing))
                && eventualOwner.getCustomers().equals(List.of(candidate))
                && eventualOwner.getWaitingQueue().equals(List.of(candidate)), "invalid arrival does not claim ownership");
        try { eventualOwner.registerArrival(candidate); throw new AssertionError("repeat arrival accepted"); }
        catch (IllegalStateException expected) { }
        check(eventualOwner.getCustomers().size() == 1 && eventualOwner.getWaitingQueue().size() == 1,
                "repeat arrival leaves collections unchanged");
        for (int turn = 1; turn <= 13; turn++) { eventualOwner.applyWaitingLoss(); }
        try { restaurant().registerArrival(candidate); throw new AssertionError("departed customer transferred"); }
        catch (IllegalArgumentException | IllegalStateException expected) { }
        check(eventualOwner.getUnhappyDepartureCount() == 1 && eventualOwner.getCustomers().equals(List.of(candidate)),
                "departure retains registered history");
    }
    private static void cleanup() {
        Restaurant r = restaurant(); Customer queued = arrive(r, 1), seated = arrive(r, 3);
        ok(r.seatCustomer(seated, 1));
        ok(r.getWaiter().takeOrder(seated, seated.chooseItem(r.getMenu()), 1800, 2));
        ok(r.getChef().prepare(seated.getOrder(), 2));
        for (int i = 0; i < 13; i++) { r.applyWaitingLoss(); }
        check(r.getWaitingQueue().isEmpty() && r.getTables().stream().allMatch(Table::isFree), "departure cleanup");
        check(r.getCustomers().equals(List.of(queued, seated)) && r.getUnhappyDepartureCount() == 2, "history retained");
        check(seated.getOrder().getStatus() == OrderStatus.CANCELLED && r.getRevenueCents() == 0, "cancelled unpaid");
        r.applyWaitingLoss(); check(r.getUnhappyDepartureCount() == 2, "cleanup idempotent for departures");
    }
    private static void ready(Customer c, int readyTurn) {
        MenuItem item = new Menu(false).getItems().get(0);
        ok(c.placeOrder(item, 1500, 2)); ok(c.getOrder().advancePreparation(2)); ok(c.serve(3)); ok(c.finishEating(readyTurn));
    }
    private static void paymentAndPriority() {
        for (boolean tie : new boolean[] {false, true}) {
            Restaurant r = restaurant(); Customer high = arrive(r, 2), low = arrive(r, 1);
            ok(r.seatCustomer(high, 1)); ok(r.seatCustomer(low, 2));
            ready(high, 4); ready(low, tie ? 4 : 5);
            check(r.oldestEligiblePayer(4) == null, "newly ready cannot pay");
            Customer first = tie ? low : high, second = tie ? high : low;
            check(r.oldestEligiblePayer(6) == first, "readiness then ID priority");
            check(r.getRevenueCents() == 0 && r.getPaidCount() == 0, "unpaid bills excluded");
            refused(r.collectPayment(new RegularCustomer(first.getId()), 6));
            ok(r.collectPayment(first, 6)); refused(r.collectPayment(second, 6)); refused(r.collectPayment(first, 7));
            check(r.findTableFor(first) == null && r.findTableFor(second) != null && r.getRevenueCents() == 1500, "payment release and once-only revenue");
            ok(r.collectPayment(second, 7)); check(r.getPaidCount() == 2 && r.getRevenueCents() == 3000, "next payment");
        }
    }
    private static Restaurant paidFixture(int count, int bill, int losses) {
        Restaurant r = restaurant();
        for (int id = 1; id <= count; id++) {
            Customer c = arrive(r, id);
            for (int j = 0; j < losses; j++) { c.loseWaitingSatisfaction(); }
            int t = id * 10;
            ok(r.seatCustomer(c, t));
            ok(r.getWaiter().takeOrder(c, r.getMenu().getItems().get(0), bill, t + 1));
            ok(r.getChef().prepare(c.getOrder(), t + 1)); ok(r.getWaiter().serve(c, t + 2));
            r.advanceEating(t + 3); ok(r.collectPayment(c, t + 4));
        }
        return r;
    }
    private static void thresholds() {
        Restaurant exact = paidFixture(4, 1125, 5);
        check(exact.getPaidCount() == 4 && exact.getRevenueCents() == 4500
                && exact.getPaidSatisfactionTotal() == 240 && exact.getAveragePaidSatisfaction().orElseThrow() == 60,
                "exact thresholds fixture");
        check(exact.evaluateResult() == GameResult.VICTORY, "exact boundaries pass");
        check(paidFixture(3, 1500, 5).evaluateResult() == GameResult.DEFEAT, "paid target alone fails");
        check(paidFixture(4, 1124, 5).evaluateResult() == GameResult.DEFEAT, "revenue target alone fails");
        check(paidFixture(4, 1125, 6).evaluateResult() == GameResult.DEFEAT, "satisfaction target alone fails");
        check(restaurant().evaluateResult() == GameResult.DEFEAT, "zero paid defeat");
    }
    private static void victorySchedule() {
        Restaurant r = restaurant();
        int[] seats = {1,4,7,11,14,17}, orders = {2,5,8,12,15,18}, serves = {3,6,10,13,16};
        for (int turn = 1; turn <= 18; turn++) {
            if ((turn - 1) % 3 == 0) { arrive(r, (turn - 1) / 3 + 1); }
            for (int i = 0; i < seats.length; i++) {
                Customer c = r.findCustomer(i + 1);
                if (turn == seats[i]) { ok(r.seatCustomer(c, turn)); }
                if (turn == orders[i]) { MenuItem item = c.chooseItem(r.getMenu()); ok(r.getWaiter().takeOrder(c, item, item.getPriceCents(), turn)); }
                if (i < serves.length && turn == serves[i]) { ok(r.getWaiter().serve(c, turn)); }
            }
            if (turn == 2 || turn == 5 || turn == 8 || turn == 9 || turn == 12 || turn == 15 || turn == 18) {
                int id = turn == 9 ? 3 : turn == 18 ? 6 : turn == 15 ? 5 : turn == 12 ? 4 : turn == 8 ? 3 : turn == 5 ? 2 : 1;
                ok(r.getChef().prepare(r.findOrder(id), turn));
            }
            r.advanceEating(turn); Customer payer = r.oldestEligiblePayer(turn);
            if (payer != null) { ok(r.collectPayment(payer, turn)); }
            r.applyWaitingLoss();
        }
        check(r.getPaidCount() == 5 && r.getServedCount() == 5 && r.getRevenueCents() == 6600
                && r.getPaidSatisfactionTotal() == 396 && r.getUnhappyDepartureCount() == 0
                && r.getAveragePaidSatisfaction().orElseThrow() == 79.2 && r.evaluateResult() == GameResult.VICTORY, "reference victory totals");
    }
    private static void defeatSchedule() {
        Restaurant r = restaurant();
        for (int turn = 1; turn <= 18; turn++) {
            if ((turn - 1) % 3 == 0) { arrive(r, (turn - 1) / 3 + 1); }
            r.advanceEating(turn); r.applyWaitingLoss();
            if (turn == 13) { check(r.findCustomer(1).getStatus() == CustomerStatus.LEFT, "C1 leaves 13"); }
            if (turn == 16) { check(r.findCustomer(2).getStatus() == CustomerStatus.LEFT, "C2 leaves 16"); }
        }
        check(r.getPaidCount() == 0 && r.getServedCount() == 0 && r.getRevenueCents() == 0
                && r.getAveragePaidSatisfaction().isEmpty() && r.getUnhappyDepartureCount() == 2
                && r.findCustomer(3).getSatisfaction() == 4 && r.evaluateResult() == GameResult.DEFEAT, "reference defeat totals");
    }

    private static void hostConstructionAndOwnership() {
        Host host = new Host(); Menu menu = new Menu(false);
        Restaurant owner = new Restaurant(menu, new Waiter(), new Chef(), new Cashier(), host);
        check(owner.getHost() == host && restaurant().getHost() != null, "both constructor forms provide a Host");
        restaurantrush.Check.throwsType(IllegalArgumentException.class,
                () -> new Restaurant(menu, new Waiter(), new Chef(), new Cashier(), null), "null Host rejected");
        Restaurant other = restaurant(); Customer customer = arrive(owner, 1);
        for (Customer invalid : new Customer[] {null, new RegularCustomer(1), new RegularCustomer(99)}) {
            refused(owner.seatCustomerWithHost(invalid, 1));
        }
        refused(other.seatCustomerWithHost(customer, 1));
        refused(owner.seatCustomerWithHost(customer, 0));
        check(owner.getWaitingQueue().equals(List.of(customer)) && owner.getTables().stream().allMatch(Table::isFree)
                && owner.getHost().getTaskTurn() == -1 && owner.getWaiter().getTaskTurn() == -1
                && other.getHost().getTaskTurn() == -1, "invalid Host attempts preserve both aggregates and capacity");
        ok(owner.seatCustomerWithHost(customer, 1));
        refused(owner.seatCustomerWithHost(customer, 2));
        check(owner.getWaitingQueue().isEmpty() && owner.findTableFor(customer) != null
                && owner.getHost().getTaskTurn() == 1, "only successful registered seating removes queue entry");
    }

    private static void independentSeatingCapacity() {
        for (boolean hostFirst : new boolean[] {true, false}) {
            Restaurant r = restaurant(); Customer a = arrive(r, 1), b = arrive(r, 2), c = arrive(r, 3);
            if (hostFirst) { ok(r.seatCustomerWithHost(a, 1)); }
            else { ok(r.seatCustomer(a, 1)); }
            check((hostFirst ? r.getWaiter() : r.getHost()).canAct(1), "one role leaves the other's task available");
            if (hostFirst) {
                refused(r.seatCustomerWithHost(b, 1));
                ok(r.seatCustomer(b, 1));
            } else {
                refused(r.seatCustomer(b, 1));
                ok(r.seatCustomerWithHost(b, 1));
            }
            refused(r.seatCustomerWithHost(c, 2));
            check(r.findFreeTable() == null && r.getWaitingQueue().equals(List.of(c))
                    && c.getStatus() == CustomerStatus.WAITING && r.getHost().canAct(2)
                    && r.getHost().getTaskTurn() == 1 && r.getWaiter().getTaskTurn() == 1,
                    "two roles fill two tables; full-table refusal preserves next-turn capacity and queue");
        }
        Restaurant r = restaurant(); Customer customer = arrive(r, 1);
        ok(r.seatCustomerWithHost(customer, 1));
        MenuItem item = customer.chooseItem(r.getMenu());
        ok(r.getWaiter().takeOrder(customer, item, item.getPriceCents(), 1));
        ok(r.getChef().prepare(customer.getOrder(), 1));
        refused(r.getWaiter().serve(customer, 1));
        check(customer.getOrder().getStatus() == OrderStatus.READY && customer.getStatus() == CustomerStatus.WAITING_FOR_FOOD,
                "Host enables same-turn ordering/preparation while prior-turn serving guard survives");
    }

    private static void hostCannotAnticipatePayment() {
        Restaurant r = restaurant(); Customer payer = arrive(r, 1), occupied = arrive(r, 2), queued = arrive(r, 3);
        ok(r.seatCustomerWithHost(payer, 1)); ok(r.seatCustomer(occupied, 1));
        ready(payer, 4);
        check(r.oldestEligiblePayer(4) == null, "readiness still delays payment");
        refused(r.seatCustomerWithHost(queued, 5));
        check(r.getHost().canAct(5) && r.findWaitingCustomer(3) == queued,
                "Host phase cannot use a table still occupied before payment");
        ok(r.collectPayment(payer, 5));
        check(r.findTableFor(payer) == null && r.getRevenueCents() == 1500,
                "existing aggregate payment releases table and records paid revenue");
        // The coordinator has ended turn 5's staff phases; next turn can reuse the table.
        ok(r.seatCustomerWithHost(queued, 6));
        check(r.getWaitingQueue().isEmpty() && r.findTableFor(queued) != null
                && r.findTableFor(occupied) != null, "next-turn Host seating reuses only released table");
    }
}
