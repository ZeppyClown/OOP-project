package restaurantrush.domain;

import java.util.function.Function;
import java.util.function.Supplier;

import restaurantrush.AllTests;
import restaurantrush.Check;

import static restaurantrush.domain.CustomerTests.*;


public final class RestaurantOrderingTests {
    private static final String NOT_CUSTOMER = "Customer is not registered in this restaurant";
    private static final String NOT_ORDER = "Order is not registered in this restaurant";

    private RestaurantOrderingTests() { }

    public static void main(String[] args) { runAll(); System.out.println("RestaurantOrderingTests: 4 scenarios passed"); }

    public static void runAll() {
        takeOrderQuotesWithSuppliedPolicy(); serveAndPrepareSucceed(); refusalsLeaveCapacityAndForeignObjectsAlone();
        refusalTextMatchesDirectStaffPath();
    }

    public static void register(AllTests.Registry r) {
        r.add("Restaurant ordering: takeOrder quotes with the supplied policy", RestaurantOrderingTests::takeOrderQuotesWithSuppliedPolicy);
        r.add("Restaurant ordering: serveOrder and prepareOrder advance the order", RestaurantOrderingTests::serveAndPrepareSucceed);
        r.add("Restaurant ordering: refusals keep capacity and reject foreign objects", RestaurantOrderingTests::refusalsLeaveCapacityAndForeignObjectsAlone);
        r.add("Restaurant ordering: refusal text equals the direct Waiter/Chef text", RestaurantOrderingTests::refusalTextMatchesDirectStaffPath);
    }

    private static Restaurant restaurant() { return new Restaurant(new Menu(false), new Waiter(), new Chef(), new Cashier()); }
    private static Customer arrive(Restaurant r, Customer c) { r.registerArrival(c); return c; }
    private static Customer seated(Restaurant r, Customer c, int turn) { arrive(r, c); ok(r.seatCustomer(c, turn)); return c; }

    private static void takeOrderQuotesWithSuppliedPolicy() {
        PricingPolicy standard = new StandardPricingPolicy(), happy = new HappyHourPricingPolicy();
        Customer[] customers = {new RegularCustomer(1), new VIPCustomer(2), new CriticCustomer(3)};
        PricingPolicy[] policies = {standard, happy, happy};
        String[] items = {"Burger", "Salad", "Pasta"};
        int[] bills = {1500, 648, 1440};
        for (int i = 0; i < customers.length; i++) {
            Restaurant r = restaurant(); Customer c = seated(r, customers[i], 1);
            ok(r.takeOrder(c, policies[i], 2));
            Order order = c.getOrder();
            check(order != null && order.getItem() == c.chooseItem(r.getMenu()) && order.getItem().getName().equals(items[i]),
                    "item chosen from the restaurant's own menu");
            check(order.getBillCents() == policies[i].priceFor(c, order.getItem()) && order.getBillCents() == bills[i],
                    "bill equals the supplied policy's quote");
            check(order.getStatus() == OrderStatus.PLACED && order.getPlacedTurn() == 2
                    && c.getStatus() == CustomerStatus.WAITING_FOR_FOOD && r.findOrder(c.getId()) == order, "order registered");
            check(r.getWaiter().getTaskTurn() == 2 && r.getChef().getTaskTurn() == -1, "only the Waiter task is used");
        }
        Restaurant r = restaurant(); Customer c = seated(r, new RegularCustomer(1), 1);
        Check.throwsType(IllegalArgumentException.class, () -> r.takeOrder(c, null, 2), "null pricing rejected");
        check(c.getOrder() == null && c.getStatus() == CustomerStatus.SEATED && r.getWaiter().getTaskTurn() == 1,
                "null pricing changes nothing");
    }

    private static void serveAndPrepareSucceed() {
        Restaurant r = restaurant(); Customer c = seated(r, new CriticCustomer(3), 1);   // Pasta needs two units
        ok(r.takeOrder(c, new StandardPricingPolicy(), 2));
        Order order = c.getOrder();
        ok(r.prepareOrder(order, 2));
        check(order.getStatus() == OrderStatus.PREPARING && order.getCompletedPreparationUnits() == 1
                && r.getChef().getTaskTurn() == 2, "first unit: PREPARING, Chef task used");
        ok(r.prepareOrder(order, 3));
        check(order.getStatus() == OrderStatus.READY && order.getReadyTurn() == 3 && r.getChef().getTaskTurn() == 3, "second unit: READY");
        ok(r.serveOrder(order, 4));
        check(order.getStatus() == OrderStatus.SERVED && order.getServedTurn() == 4
                && c.getStatus() == CustomerStatus.SERVED && c.getServedTurn() == 4 && r.getWaiter().getTaskTurn() == 4,
                "serve moves order and customer together");
    }

    private static void refusalsLeaveCapacityAndForeignObjectsAlone() {
        Restaurant r = restaurant(), other = restaurant();
        Customer own = seated(r, new RegularCustomer(1), 1), foreign = seated(other, new RegularCustomer(1), 1);
        ok(other.takeOrder(foreign, new StandardPricingPolicy(), 2));
        ok(other.prepareOrder(foreign.getOrder(), 2));
        Order foreignOrder = foreign.getOrder();   // same order ID 1 as r's order will get
        Customer stranger = new RegularCustomer(9);

        // takeOrder: null, unregistered and foreign customers get the registration text, nothing is used.
        Check.equal(NOT_CUSTOMER, r.takeOrder(null, new StandardPricingPolicy(), 2), "null customer");
        Check.equal(NOT_CUSTOMER, r.takeOrder(stranger, new StandardPricingPolicy(), 2), "unregistered customer");
        Check.equal(NOT_CUSTOMER, r.takeOrder(foreign, new StandardPricingPolicy(), 3), "foreign customer");
        check(r.getWaiter().getTaskTurn() == 1 && own.getOrder() == null && other.getWaiter().getTaskTurn() == 2,
                "registration refusals use no capacity");
        // Not SEATED is reported before Waiter capacity: the Waiter already seated on turn 1.
        Customer waiting = arrive(r, new RegularCustomer(2));
        Check.equal("Customer 2 must be SEATED to order; currently WAITING", r.takeOrder(waiting, new StandardPricingPolicy(), 1),
                "state before capacity");
        Check.equal("Waiter has already acted this turn", r.takeOrder(own, new StandardPricingPolicy(), 1), "capacity refusal");
        check(own.getOrder() == null && r.getWaiter().getTaskTurn() == 1, "refused order creates nothing");
        ok(r.takeOrder(own, new StandardPricingPolicy(), 2));   // the retry still works

        // serveOrder / prepareOrder: null and foreign orders, even with an equal order ID.
        Order mine = own.getOrder();
        check(mine.getId() == foreignOrder.getId() && mine != foreignOrder, "orders share an ID but are different objects");
        Check.equal(NOT_ORDER, r.serveOrder(null, 3), "null order serve");
        Check.equal(NOT_ORDER, r.serveOrder(foreignOrder, 3), "foreign order serve");
        Check.equal(NOT_ORDER, r.prepareOrder(null, 3), "null order prepare");
        Check.equal(NOT_ORDER, r.prepareOrder(foreignOrder, 3), "foreign order prepare");
        check(foreignOrder.getStatus() == OrderStatus.READY && r.getChef().getTaskTurn() == -1
                && r.getWaiter().getTaskTurn() == 2 && other.getWaiter().getTaskTurn() == 2 && other.getChef().getTaskTurn() == 2,
                "foreign order and both restaurants' staff untouched");

        // A state refusal does not consume the Chef or Waiter, so the same turn can still succeed.
        ok(r.prepareOrder(mine, 3));                              // Burger: one unit -> READY on turn 3
        Check.equal("Order 1 is not ready to serve on turn 3", r.serveOrder(mine, 3), "ready turn is not serve-able");
        check(r.getWaiter().getTaskTurn() == 2 && mine.getStatus() == OrderStatus.READY, "refused serve keeps Waiter free");
        ok(r.serveOrder(mine, 4));
    }

    // The three wrappers must not change what the user sees: compare with the earlier path,
    // where the controller called getWaiter()/getChef() itself, on an identical twin restaurant.
    private static void refusalTextMatchesDirectStaffPath() {
        StandardPricingPolicy standard = new StandardPricingPolicy();
        // Variants are (builder, turn, expected text) so every row also pins the wording.
        Supplier<Restaurant> waiting = () -> { Restaurant r = restaurant(); arrive(r, new RegularCustomer(1)); return r; };
        Supplier<Restaurant> seatedT1 = () -> { Restaurant r = restaurant(); seated(r, new RegularCustomer(1), 1); return r; };
        Supplier<Restaurant> ordered = () -> { Restaurant r = seatedT1.get(); ok(r.takeOrder(r.findCustomer(1), standard, 2)); return r; };
        Supplier<Restaurant> readyT3 = () -> { Restaurant r = ordered.get(); ok(r.prepareOrder(r.findOrder(1), 3)); return r; };
        Supplier<Restaurant> readyT2 = () -> { Restaurant r = ordered.get(); ok(r.getChef().prepare(r.findOrder(1), 2)); return r; };
        Supplier<Restaurant> departed = () -> {
            Restaurant r = ordered.get();
            for (int i = 0; i < 13; i++) { r.applyWaitingLoss(); }
            check(r.findCustomer(1).getStatus() == CustomerStatus.LEFT && r.findOrder(1).getStatus() == OrderStatus.CANCELLED, "departed fixture");
            return r;
        };

        sameTakeRefusal(waiting, 1, "Customer 1 must be SEATED to order; currently WAITING", standard);
        sameTakeRefusal(seatedT1, 1, "Waiter has already acted this turn", standard);
        sameTakeRefusal(ordered, 3, "Customer 1 must be SEATED to order; currently WAITING_FOR_FOOD", standard);
        sameTakeRefusal(departed, 14, "Customer 1 must be SEATED to order; currently LEFT", standard);

        sameServeRefusal(ordered, 3, "Order 1 is not ready to serve on turn 3");
        sameServeRefusal(readyT2, 2, "Waiter has already acted this turn");
        sameServeRefusal(readyT3, 3, "Order 1 is not ready to serve on turn 3");
        sameServeRefusal(departed, 14, "Customer 1 is not waiting for food; status is LEFT");

        samePrepareRefusal(readyT2, 2, "Chef has already acted this turn");
        samePrepareRefusal(readyT2, 3, "Order 1 is not awaiting preparation: READY");
        samePrepareRefusal(departed, 14, "Order 1 is not awaiting preparation: CANCELLED");
    }

    private static void sameTakeRefusal(Supplier<Restaurant> build, int turn, String expected, PricingPolicy pricing) {
        sameText(build, expected,
                r -> r.takeOrder(r.findCustomer(1), pricing, turn),
                r -> {   // the controller's earlier sequence
                    Customer c = r.findCustomer(1);
                    if (c.getStatus() != CustomerStatus.SEATED) {
                        return "Customer " + c.getId() + " must be SEATED to order; currently " + c.getStatus();
                    }
                    MenuItem item = c.chooseItem(r.getMenu());
                    return r.getWaiter().takeOrder(c, item, pricing.priceFor(c, item), turn);
                });
    }

    private static void sameServeRefusal(Supplier<Restaurant> build, int turn, String expected) {
        sameText(build, expected, r -> r.serveOrder(r.findOrder(1), turn),
                r -> r.getWaiter().serve(r.findOrder(1).getCustomer(), turn));
    }

    private static void samePrepareRefusal(Supplier<Restaurant> build, int turn, String expected) {
        sameText(build, expected, r -> r.prepareOrder(r.findOrder(1), turn), r -> r.getChef().prepare(r.findOrder(1), turn));
    }

    private static void sameText(Supplier<Restaurant> build, String expected,
                                 Function<Restaurant, String> viaRestaurant, Function<Restaurant, String> direct) {
        Restaurant twinA = build.get(), twinB = build.get();
        String before = direct.apply(twinB), now = viaRestaurant.apply(twinA);
        Check.equal(expected, before, "text of the earlier path");
        Check.equal(before, now, "Restaurant wrapper repeats it unchanged");
        Check.equal(twinB.getWaiter().getTaskTurn(), twinA.getWaiter().getTaskTurn(), "same Waiter capacity afterwards");
        Check.equal(twinB.getChef().getTaskTurn(), twinA.getChef().getTaskTurn(), "same Chef capacity afterwards");
    }
}
