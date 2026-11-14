package restaurantrush.domain;

import java.util.List;
import java.util.function.IntFunction;

import static restaurantrush.Check.throwsType;
import static restaurantrush.domain.CustomerTests.*;

public final class RecoveryTests {
    private static final List<IntFunction<Customer>> FACTORIES = List.of(
            RegularCustomer::new, VIPCustomer::new, CriticCustomer::new);

    private RecoveryTests() { }

    public static void main(String[] args) {
        runAll();
        System.out.println("RecoveryTests: 6 scenarios passed");
    }

    public static void runAll() {
        eligibleStatesAndSubtypes(); amountGuardsAndOverflow(); excludedLifecycleStates();
        aggregateDelegation(); expandedMenuChoices(); recoveryThenOrdinaryLoss();
    }

    private static Restaurant restaurant() {
        return new Restaurant(new Menu(), new Waiter(), new Chef(), new Cashier());
    }

    private static MenuItem pasta() { return new Menu(false).getItems().get(2); }

    private static void eligibleStatesAndSubtypes() {
        for (IntFunction<Customer> factory : FACTORIES) {
            // WAITING, SEATED, PLACED, PREPARING and READY are all eligible.
            for (int step = 0; step <= 4; step++) {
                Customer customer = factory.apply(1);
                if (step >= 1) { ok(customer.seat()); }
                if (step >= 2) { ok(customer.placeOrder(pasta(), 1800, 1)); }
                if (step >= 3) { ok(customer.getOrder().advancePreparation(1)); }
                if (step >= 4) { ok(customer.getOrder().advancePreparation(2)); }
                customer.loseWaitingSatisfaction(); customer.loseWaitingSatisfaction();
                int before = customer.getSatisfaction();
                CustomerStatus status = customer.getStatus();
                Order order = customer.getOrder();
                OrderStatus orderStatus = order == null ? null : order.getStatus();
                customer.recoverWaitingSatisfaction(15);
                check(customer.isWaitingUnserved()
                        && customer.getSatisfaction() == Math.min(100, before + 15),
                        "every waiting state recovers through the parent Customer API");
                check(customer.getStatus() == status && customer.getOrder() == order
                        && (order == null || order.getStatus() == orderStatus
                        && order.getBillCents() == 1800), "recovery never advances or reprices an order");
            }
        }
    }

    private static void amountGuardsAndOverflow() {
        Customer customer = new VIPCustomer(1);
        for (int i = 0; i < 10; i++) { customer.loseWaitingSatisfaction(); }
        check(customer.getSatisfaction() == 50, "real VIP loss fixture reaches 50");
        customer.recoverWaitingSatisfaction(15);
        check(customer.getSatisfaction() == 65, "50 plus 15 is 65");
        customer.recoverWaitingSatisfaction(0);
        check(customer.getSatisfaction() == 65, "zero recovery is inert");
        throwsType(IllegalArgumentException.class, () -> customer.recoverWaitingSatisfaction(-1),
                "negative recovery rejected");
        check(customer.getSatisfaction() == 65, "invalid recovery leaves satisfaction intact");
        customer.recoverWaitingSatisfaction(Integer.MAX_VALUE);
        check(customer.getSatisfaction() == 100, "widened addition clamps without integer overflow");
        customer.recoverWaitingSatisfaction(Integer.MAX_VALUE);
        check(customer.getSatisfaction() == 100, "upper bound remains 100");
        Restaurant empty = restaurant();
        throwsType(IllegalArgumentException.class, () -> empty.recoverWaitingSatisfaction(-1),
                "empty aggregate also validates its argument");
    }

    private static void excludedLifecycleStates() {
        for (IntFunction<Customer> factory : FACTORIES) {
            Customer customer = factory.apply(1);
            customer.loseWaitingSatisfaction(); customer.loseWaitingSatisfaction();
            int frozen = customer.getSatisfaction();
            ok(customer.seat()); ok(customer.placeOrder(pasta(), 1800, 1));
            ok(customer.getOrder().advancePreparation(1)); ok(customer.getOrder().advancePreparation(2));
            ok(customer.serve(3));
            assertExcluded(customer, CustomerStatus.SERVED, frozen);
            ok(customer.finishEating(4));
            assertExcluded(customer, CustomerStatus.READY_TO_PAY, frozen);
            ok(customer.pay(5));
            assertExcluded(customer, CustomerStatus.PAID, frozen);
            check(customer.getSatisfactionAtPayment() == frozen, "paid snapshot remains frozen");

            Customer departed = factory.apply(2);
            for (int i = 0; i < 20; i++) { departed.loseWaitingSatisfaction(); }
            assertExcluded(departed, CustomerStatus.LEFT, 0);
        }
    }

    private static void assertExcluded(Customer customer, CustomerStatus status, int satisfaction) {
        check(customer.getStatus() == status && !customer.isWaitingUnserved(), "excluded fixture state");
        customer.recoverWaitingSatisfaction(15);
        customer.recoverWaitingSatisfaction(Integer.MAX_VALUE);
        throwsType(IllegalArgumentException.class, () -> customer.recoverWaitingSatisfaction(-1),
                "negative amount rejected even for an excluded customer");
        check(customer.getStatus() == status && customer.getSatisfaction() == satisfaction,
                "served, ready, paid and left never recover");
    }

    private static void aggregateDelegation() {
        Restaurant owner = restaurant(), other = restaurant();
        Customer queued = new VIPCustomer(1), food = new RegularCustomer(2), served = new CriticCustomer(3);
        Customer foreign = new RegularCustomer(4), notArrived = new RegularCustomer(5), left = new RegularCustomer(6);
        for (Customer customer : List.of(queued, food, served, left)) { owner.registerArrival(customer); }
        other.registerArrival(foreign);
        for (int i = 0; i < 13; i++) { left.loseWaitingSatisfaction(); }
        ok(owner.seatCustomerWithHost(food, 1)); ok(owner.seatCustomer(served, 1));
        ok(food.placeOrder(pasta(), 1800, 2));
        ok(served.placeOrder(pasta(), 1800, 2));
        ok(served.getOrder().advancePreparation(2)); ok(served.getOrder().advancePreparation(3));
        owner.applyWaitingLoss();
        ok(served.serve(4));
        foreign.loseWaitingSatisfaction(); notArrived.loseWaitingSatisfaction();
        List<Customer> queue = owner.getWaitingQueue();
        List<Customer> history = List.copyOf(owner.getCustomers());
        Table foodTable = owner.findTableFor(food), servedTable = owner.findTableFor(served);

        owner.recoverWaitingSatisfaction(15);
        check(queued.getSatisfaction() == 100 && food.getSatisfaction() == 100
                && served.getSatisfaction() == 88 && left.getSatisfaction() == 0,
                "aggregate delegates eligibility to every arrived customer");
        check(foreign.getSatisfaction() == 92 && notArrived.getSatisfaction() == 92,
                "foreign and unregistered customers are outside recovery scope");
        check(owner.getWaitingQueue().equals(queue) && owner.getCustomers().equals(history)
                && owner.findTableFor(food) == foodTable && owner.findTableFor(served) == servedTable
                && owner.getHost().getTaskTurn() == 1 && owner.getWaiter().getTaskTurn() == 1
                && owner.getRevenueCents() == 0 && owner.getAveragePaidSatisfaction().isEmpty(),
                "recovery preserves aggregate ownership, tables, staff and paid-only statistics");
        owner.advanceEating(5); ok(owner.collectPayment(served, 6));
        owner.recoverWaitingSatisfaction(15);
        check(served.getSatisfaction() == 88 && served.getSatisfactionAtPayment() == 88
                && owner.getRevenueCents() == 1800 && owner.getPaidSatisfactionTotal() == 88,
                "aggregate recovery leaves paid snapshot and locked revenue unchanged");
    }

    private static void expandedMenuChoices() {
        Menu expanded = new Menu(), base = new Menu(false);
        check(expanded.getItems().size() == 4 && base.getItems().size() == 3,
                "explicit three-item regression menu and four-item production menu");
        for (IntFunction<Customer> factory : List.<IntFunction<Customer>>of(RegularCustomer::new, VIPCustomer::new)) {
            for (int id = 1; id <= 12; id++) {
                Customer customer = factory.apply(id);
                check(customer.chooseItem(expanded) == expanded.getItems().get((id - 1) % 4),
                        "expanded deterministic rotation uses actual choice count");
                check(customer.chooseItem(base) == base.getItems().get((id - 1) % 3),
                        "base fixture retains inherited rotation");
            }
            check(factory.apply(4).chooseItem(expanded) instanceof ComboMeal
                    && factory.apply(5).chooseItem(expanded).getName().equals("Burger")
                    && factory.apply(6).chooseItem(expanded).getName().equals("Salad"),
                    "Stage 3 C4 combo, C5 Burger, C6 Salad");
            check(factory.apply(Integer.MAX_VALUE).chooseItem(expanded)
                    == expanded.getItems().get((Integer.MAX_VALUE - 1) % 4), "large ID remains bounded");
        }
        for (int id = 1; id <= 8; id++) {
            check(new CriticCustomer(id).chooseItem(expanded).getName().equals("Pasta"),
                    "Critic's named preference survives menu expansion");
        }
    }

    private static void recoveryThenOrdinaryLoss() {
        Restaurant restaurant = restaurant();
        Customer customer = new RegularCustomer(1); restaurant.registerArrival(customer);
        restaurant.applyWaitingLoss();
        check(customer.getSatisfaction() == 92, "first ordinary waiting loss");
        restaurant.recoverWaitingSatisfaction(15);
        check(customer.getSatisfaction() == 100, "92 plus 15 clamps at turn start");
        restaurant.applyWaitingLoss();
        check(customer.getSatisfaction() == 92, "recovery does not replace end-of-turn loss");
        restaurant.applyWaitingLoss();
        check(customer.getSatisfaction() == 84, "loss continues when controller does not invoke recovery");
        throwsType(IllegalArgumentException.class, () -> restaurant.recoverWaitingSatisfaction(-15),
                "invalid aggregate recovery rejected before mutations");
        check(customer.getSatisfaction() == 84 && restaurant.findWaitingCustomer(1) == customer,
                "invalid recovery leaves all aggregate state intact");
    }
}
