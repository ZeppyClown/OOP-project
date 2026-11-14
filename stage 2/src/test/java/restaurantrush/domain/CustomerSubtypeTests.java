package restaurantrush.domain;

import java.util.List;
import java.util.function.IntFunction;

import static restaurantrush.Check.throwsType;

/** The same parent-typed behavioural contract is exercised for all three siblings. */
public final class CustomerSubtypeTests {
    private static final List<IntFunction<Customer>> FACTORIES = List.of(
            RegularCustomer::new, VIPCustomer::new, CriticCustomer::new);
    private static final int[] LOSSES = {8, 5, 12};
    private static final String[] LABELS = {"Regular", "VIP", "Critic"};

    private CustomerSubtypeTests() { }

    public static void main(String[] args) {
        runAll();
        System.out.println("CustomerSubtypeTests: 7 scenarios passed (each covers all three subtypes)");
    }

    public static void runAll() {
        behaviourDispatch(); constructionAndChoiceGuards(); sharedLifecycle();
        invalidTransitions(); abandonmentAndCancellation(); permanentOwnership();
        rejectedArrivalDoesNotClaimOwnership();
    }

    private static Restaurant restaurant() {
        return new Restaurant(new Menu(), new Waiter(), new Chef(), new Cashier());
    }

    private static void behaviourDispatch() {
        Menu menu = new Menu();
        String[][] choices = {{"Burger", "Salad", "Pasta"}, {"Burger", "Salad", "Pasta"},
                {"Pasta", "Pasta", "Pasta"}};
        int[][] prices = {{1500, 900, 1800}, {1350, 810, 1620}, {1500, 900, 1800}};
        for (int type = 0; type < FACTORIES.size(); type++) {
            for (int id = 1; id <= 6; id++) {
                Customer customer = FACTORIES.get(type).apply(id);
                check(customer.getTypeLabel().equals(LABELS[type]), "type label dispatch");
                check(customer.getStatus() == CustomerStatus.WAITING && customer.getSatisfaction() == 100
                        && customer.getOrder() == null, "initial parent contract");
                check(customer.chooseItem(menu).getName().equals(choices[type][(id - 1) % 3]),
                        "customer chooses independently of manager, including Critic IDs 1 and 4");
                for (int item = 0; item < menu.getItems().size(); item++) {
                    check(customer.applyDiscount(menu.getItems().get(item).getPriceCents()) == prices[type][item],
                            "parent-typed quote for every base menu item");
                }
                customer.loseWaitingSatisfaction();
                check(customer.getSatisfaction() == 100 - LOSSES[type], "parent-typed waiting loss");
                check(customer.applyDiscount(0) == 0, "zero-price quote");
                check(customer.applyDiscount(5) == 5, "half-cent rounds up for VIP; full price otherwise");
                check(customer.applyDiscount(1) == 1, "one-cent quote");
                check(customer.applyDiscount(Integer.MAX_VALUE)
                        == (type == 1 ? 1932735282 : Integer.MAX_VALUE), "maximum valid price quote");
            }
        }
    }

    private static void constructionAndChoiceGuards() {
        for (IntFunction<Customer> factory : FACTORIES) {
            for (int id : new int[] {Integer.MIN_VALUE, -1, 0}) {
                throwsType(IllegalArgumentException.class, () -> factory.apply(id), "positive ID required");
            }
            Customer customer = factory.apply(Integer.MAX_VALUE);
            throwsType(IllegalArgumentException.class, () -> customer.chooseItem(null), "null menu rejected");
            check(customer.getId() == Integer.MAX_VALUE && customer.getOrder() == null
                    && customer.getStatus() == CustomerStatus.WAITING, "invalid choice leaves state intact");
            check(new Menu().getItems().stream().anyMatch(item -> item.getName()
                    .equals(customer.chooseItem(new Menu()).getName())), "large ID still chooses a menu item");
        }
    }

    private static void sharedLifecycle() {
        for (int type = 0; type < FACTORIES.size(); type++) {
            Restaurant owner = restaurant();
            Customer customer = FACTORIES.get(type).apply(1);
            owner.registerArrival(customer);
            owner.applyWaitingLoss();
            ok(owner.seatCustomer(customer, 2));
            Table occupied = owner.findTableFor(customer);
            owner.applyWaitingLoss();
            MenuItem item = customer.chooseItem(owner.getMenu());
            int quote = customer.applyDiscount(item.getPriceCents());
            ok(owner.getWaiter().takeOrder(customer, item, quote, 3));
            Order order = customer.getOrder();
            check(order.getCustomer() == customer && order.getItem() == item
                    && order.getBillCents() == quote, "one owned order locks dispatched price");
            for (int unit = 0; unit < item.getPreparationUnits(); unit++) {
                ok(owner.getChef().prepare(order, 3 + unit));
            }
            int readyTurn = order.getReadyTurn();
            owner.applyWaitingLoss();
            check(customer.getSatisfaction() == 100 - 3 * LOSSES[type]
                    && order.getBillCents() == quote, "READY loses subtype patience without repricing");
            check(owner.getRevenueCents() == 0 && owner.getPaidCount() == 0, "unpaid order excluded");
            refused(owner.getWaiter().serve(customer, readyTurn));
            int servedTurn = readyTurn + 1;
            ok(owner.getWaiter().serve(customer, servedTurn));
            refused(occupied.release());
            refused(customer.finishEating(servedTurn));
            owner.applyWaitingLoss();
            owner.advanceEating(servedTurn);
            check(customer.getStatus() == CustomerStatus.SERVED, "cannot eat on serving turn");
            owner.advanceEating(servedTurn + 1);
            check(customer.getStatus() == CustomerStatus.READY_TO_PAY
                    && owner.oldestEligiblePayer(servedTurn + 1) == null, "new readiness cannot pay");
            refused(owner.collectPayment(customer, servedTurn + 1));
            owner.applyWaitingLoss();
            check(owner.getCashier().canAct(servedTurn + 2), "refused payment retains cashier capacity");
            ok(owner.collectPayment(customer, servedTurn + 2));
            owner.applyWaitingLoss();
            check(customer.getStatus() == CustomerStatus.PAID && order.getStatus() == OrderStatus.PAID
                    && occupied.isFree() && owner.getPaidCount() == 1 && owner.getRevenueCents() == quote,
                    "aggregate payment synchronizes customer, order, revenue and table");
            check(customer.getServedTurn() == servedTurn && customer.getReadyToPayTurn() == servedTurn + 1
                    && customer.getSatisfactionAtPayment() == 100 - 3 * LOSSES[type]
                    && customer.getSatisfaction() == customer.getSatisfactionAtPayment(),
                    "same delay guards and frozen paid satisfaction");
            refused(owner.collectPayment(customer, servedTurn + 3));
            check(owner.getRevenueCents() == quote, "payment never counted twice");
        }
    }

    private static void invalidTransitions() {
        for (IntFunction<Customer> factory : FACTORIES) {
            Customer customer = factory.apply(1);
            MenuItem item = customer.chooseItem(new Menu());
            int bill = customer.applyDiscount(item.getPriceCents());
            refused(customer.placeOrder(item, bill, 1)); refused(customer.serve(1));
            refused(customer.finishEating(1)); refused(customer.pay(1));
            check(customer.getStatus() == CustomerStatus.WAITING && customer.getOrder() == null,
                    "unseated failures are atomic");
            ok(customer.seat()); refused(customer.seat());
            refused(customer.placeOrder(null, bill, 2)); refused(customer.placeOrder(item, -1, 2));
            refused(customer.placeOrder(item, bill, 0)); refused(customer.placeOrder(item, bill, -1));
            check(customer.getStatus() == CustomerStatus.SEATED && customer.getOrder() == null,
                    "invalid order preserves seated state");
            ok(customer.placeOrder(item, bill, 2));
            Order order = customer.getOrder();
            refused(customer.placeOrder(item, 0, 3)); refused(customer.serve(3));
            refused(customer.finishEating(3)); refused(customer.pay(3));
            check(customer.getOrder() == order && order.getBillCents() == bill
                    && order.getStatus() == OrderStatus.PLACED, "failed transitions preserve locked order");
            for (int unit = 0; unit < item.getPreparationUnits(); unit++) {
                ok(order.advancePreparation(2 + unit));
            }
            int servedTurn = order.getReadyTurn() + 1;
            refused(customer.serve(order.getReadyTurn())); ok(customer.serve(servedTurn));
            refused(customer.seat()); refused(customer.serve(servedTurn + 1));
            for (int turn : new int[] {-1, 0, servedTurn - 1, servedTurn}) {
                refused(customer.finishEating(turn));
            }
            check(customer.getReadyToPayTurn() == -1 && customer.getStatus() == CustomerStatus.SERVED,
                    "early eating leaves readiness unset");
            ok(customer.finishEating(servedTurn + 1));
            for (int turn : new int[] {-1, 0, servedTurn, servedTurn + 1}) { refused(customer.pay(turn)); }
            check(customer.getSatisfactionAtPayment() == -1 && order.getStatus() == OrderStatus.SERVED,
                    "early payment leaves customer and order unchanged");
            ok(customer.pay(servedTurn + 2)); refused(customer.pay(servedTurn + 3));
            refused(customer.finishEating(servedTurn + 3)); refused(customer.placeOrder(item, bill, servedTurn + 3));
        }
    }

    private static void abandonmentAndCancellation() {
        for (int type = 0; type < FACTORIES.size(); type++) {
            for (int progress = -2; progress <= 2; progress++) {
                Restaurant owner = restaurant();
                Customer customer = FACTORIES.get(type).apply(3);
                owner.registerArrival(customer);
                if (progress >= -1) { ok(owner.seatCustomer(customer, 1)); }
                if (progress >= 0) {
                    MenuItem pasta = customer.chooseItem(owner.getMenu());
                    ok(owner.getWaiter().takeOrder(customer, pasta, customer.applyDiscount(pasta.getPriceCents()), 2));
                    for (int unit = 0; unit < progress; unit++) {
                        ok(owner.getChef().prepare(customer.getOrder(), 2 + unit));
                    }
                }
                int lossesUntilDeparture = (100 + LOSSES[type] - 1) / LOSSES[type];
                for (int count = 1; count < lossesUntilDeparture; count++) { owner.applyWaitingLoss(); }
                check(customer.getSatisfaction() > 0 && customer.getSatisfaction() <= 100
                        && customer.isWaitingUnserved(), "positive satisfaction before exact departure loss");
                owner.applyWaitingLoss(); owner.applyWaitingLoss();
                check(customer.getSatisfaction() == 0 && customer.getStatus() == CustomerStatus.LEFT
                        && !customer.isWaitingUnserved(), "subtype loss clamps at zero and departure is terminal");
                check(owner.getWaitingQueue().isEmpty() && owner.getTables().stream().allMatch(Table::isFree)
                        && owner.getCustomers().equals(List.of(customer)) && owner.getUnhappyDepartureCount() == 1,
                        "departure clears queue and table while retaining history exactly once");
                if (customer.getOrder() != null) {
                    check(customer.getOrder().getStatus() == OrderStatus.CANCELLED, "PLACED/PREPARING/READY cancelled");
                    refused(owner.getChef().prepare(customer.getOrder(), 30));
                }
                refused(customer.seat()); refused(customer.serve(30)); refused(customer.finishEating(30));
                refused(owner.collectPayment(customer, 30));
                check(owner.getRevenueCents() == 0 && owner.getAveragePaidSatisfaction().isEmpty(),
                        "abandoned bills never enter paid statistics");
            }
        }
    }

    private static void permanentOwnership() {
        for (IntFunction<Customer> factory : FACTORIES) {
            Restaurant owner = restaurant(), other = restaurant();
            Customer customer = factory.apply(1);
            owner.registerArrival(customer);
            throwsType(IllegalStateException.class, () -> other.registerArrival(customer), "waiting owner is exclusive");
            refused(other.seatCustomer(customer, 1)); refused(other.collectPayment(customer, 1));
            check(other.getCustomers().isEmpty() && other.getWaitingQueue().isEmpty()
                    && other.getTables().stream().allMatch(Table::isFree)
                    && other.getWaiter().getTaskTurn() == -1 && other.getCashier().getTaskTurn() == -1,
                    "foreign operations preserve target collections and staff capacity");
            ok(owner.seatCustomer(customer, 1));
            throwsType(IllegalArgumentException.class, () -> other.registerArrival(customer), "seated cannot transfer");
            MenuItem item = customer.chooseItem(owner.getMenu());
            int bill = customer.applyDiscount(item.getPriceCents());
            ok(owner.getWaiter().takeOrder(customer, item, bill, 2));
            for (int unit = 0; unit < item.getPreparationUnits(); unit++) {
                ok(owner.getChef().prepare(customer.getOrder(), 2 + unit));
            }
            int serveTurn = customer.getOrder().getReadyTurn() + 1;
            ok(owner.getWaiter().serve(customer, serveTurn)); owner.advanceEating(serveTurn + 1);
            ok(owner.collectPayment(customer, serveTurn + 2));
            // Package-level bind checks the permanent owner even after aggregate arrival rejects terminal status.
            throwsType(IllegalStateException.class, () -> customer.bindTo(other), "paid ownership remains permanent");
            throwsType(IllegalArgumentException.class, () -> other.registerArrival(customer), "paid cannot re-arrive");
            check(owner.getCustomers().equals(List.of(customer)) && owner.getRevenueCents() == bill
                    && other.getCustomers().isEmpty() && other.getRevenueCents() == 0, "paid history stays with owner");
            Restaurant departedOwner = restaurant();
            Customer departed = factory.apply(2); departedOwner.registerArrival(departed);
            for (int loss = 0; loss < 20; loss++) { departedOwner.applyWaitingLoss(); }
            throwsType(IllegalStateException.class, () -> departed.bindTo(other), "departed ownership remains permanent");
            throwsType(IllegalArgumentException.class, () -> other.registerArrival(departed), "LEFT cannot re-arrive");
            check(departedOwner.getCustomers().equals(List.of(departed)) && other.getCustomers().isEmpty(),
                    "departed history stays with owner");
        }
    }

    private static void rejectedArrivalDoesNotClaimOwnership() {
        for (IntFunction<Customer> factory : FACTORIES) {
            Restaurant duplicate = restaurant(), eventualOwner = restaurant();
            Customer resident = new RegularCustomer(1), candidate = factory.apply(1);
            duplicate.registerArrival(resident);
            throwsType(IllegalStateException.class, () -> duplicate.registerArrival(candidate), "duplicate ID refused");
            eventualOwner.registerArrival(candidate);
            throwsType(IllegalStateException.class, () -> eventualOwner.registerArrival(candidate), "repeat arrival refused");
            check(duplicate.getCustomers().equals(List.of(resident)) && duplicate.getWaitingQueue().equals(List.of(resident))
                    && eventualOwner.getCustomers().equals(List.of(candidate))
                    && eventualOwner.getWaitingQueue().equals(List.of(candidate)), "failed arrival claims no ownership");
        }
    }
    private static void check(boolean value, String message) {
        if (!value) { throw new AssertionError(message); }
    }
    private static void ok(String reason) {
        check(reason == null, "Expected success: " + reason);
    }
    private static void refused(String reason) {
        check(reason != null && !reason.isBlank(), "Expected explained refusal");
    }
}
