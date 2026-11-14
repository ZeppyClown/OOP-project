package restaurantrush.domain;

import java.lang.reflect.Modifier;
import java.util.List;

public final class StaffTests {
    private StaffTests() { }

    public static void main(String[] args) {
        runAll();
        System.out.println("StaffTests: 10 scenarios passed");
    }

    public static void runAll() {
        staffMutationVisibility();
        aggregateSeatingAndPayment();
        waiterSeatingCapacity();
        waiterOrderRefusals();
        waiterServingRefusals();
        chefCapacityAndRetry();
        chefCannotBypassOrderTurnGuard();
        cashierCapacityAndSynchronization();
        cashierEarlyPaymentRetry();
        pastaConsumerPath();
    }

    private static void staffMutationVisibility() {
        try {
            int access = Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE;
            check((Waiter.class.getDeclaredMethod("seat", Customer.class, Table.class, int.class)
                    .getModifiers() & access) == 0, "Waiter seating must stay package-private");
            check((Cashier.class.getDeclaredMethod("collect", Customer.class, int.class)
                    .getModifiers() & access) == 0, "Cashier collection must stay package-private");
            check(Waiter.class.getMethod("takeOrder", Customer.class, MenuItem.class, int.class, int.class)
                    .getReturnType() == String.class, "controller ordering API remains public");
            check(Waiter.class.getMethod("serve", Customer.class, int.class).getReturnType() == String.class,
                    "controller serving API remains public");
            check(Chef.class.getMethod("prepare", Order.class, int.class).getReturnType() == String.class,
                    "controller preparation API remains public");
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("Staff action API changed", failure);
        }
    }

    /** Uses public actions only, as a caller outside the domain package must. */
    private static void aggregateSeatingAndPayment() {
        Restaurant restaurant = new Restaurant(new Menu(false), new Waiter(), new Chef(), new Cashier());
        Customer first = new RegularCustomer(1);
        Customer second = new RegularCustomer(2);
        restaurant.registerArrival(first);
        restaurant.registerArrival(second);
        refused(restaurant.seatCustomer(first, 0));
        check(restaurant.getWaitingQueue().equals(List.of(first, second))
                && restaurant.getTables().stream().allMatch(Table::isFree)
                && restaurant.getWaiter().canAct(1), "invalid seating preserves queue, tables and capacity");
        ok(restaurant.seatCustomer(first, 1));
        Table firstTable = restaurant.findTableFor(first);
        check(firstTable != null && restaurant.getWaitingQueue().equals(List.of(second)),
                "successful seating removes exactly the seated customer from the queue");
        refused(restaurant.seatCustomer(second, 1));
        check(restaurant.findTableFor(second) == null
                && restaurant.getWaitingQueue().equals(List.of(second)), "used Waiter cannot partially seat");
        ok(restaurant.seatCustomer(second, 2));
        Table secondTable = restaurant.findTableFor(second);
        check(secondTable != null && restaurant.getWaitingQueue().isEmpty(), "next-turn seating completes");

        MenuItem burger = restaurant.getMenu().getItems().get(0);
        MenuItem salad = restaurant.getMenu().getItems().get(1);
        ok(restaurant.getWaiter().takeOrder(first, burger, burger.getPriceCents(), 3));
        ok(restaurant.getChef().prepare(first.getOrder(), 3));
        ok(restaurant.getWaiter().serve(first, 4));
        ok(restaurant.getWaiter().takeOrder(second, salad, salad.getPriceCents(), 5));
        ok(restaurant.getChef().prepare(second.getOrder(), 5));
        ok(restaurant.getWaiter().serve(second, 6));
        refused(restaurant.collectPayment(first, 6));
        restaurant.advanceEating(7);
        refused(restaurant.collectPayment(first, 7));
        check(firstTable.getOccupant() == first && secondTable.getOccupant() == second
                && first.getStatus() == CustomerStatus.READY_TO_PAY
                && first.getOrder().getStatus() == OrderStatus.SERVED
                && restaurant.getRevenueCents() == 0 && restaurant.getCashier().canAct(7),
                "early payment preserves tables, statuses, revenue and capacity");
        ok(restaurant.collectPayment(first, 8));
        check(firstTable.isFree() && first.getStatus() == CustomerStatus.PAID
                && first.getOrder().getStatus() == OrderStatus.PAID
                && first.getSatisfactionAtPayment() == 100 && restaurant.getRevenueCents() == 1500,
                "successful payment synchronizes statuses, freezes satisfaction and releases the table");
        refused(restaurant.collectPayment(second, 8));
        check(secondTable.getOccupant() == second && second.getStatus() == CustomerStatus.READY_TO_PAY
                && restaurant.getPaidCount() == 1, "Cashier capacity refusal preserves the second payer");
        refused(restaurant.collectPayment(first, 9));
        check(restaurant.getCashier().canAct(9), "duplicate aggregate payment preserves next-turn capacity");
        ok(restaurant.collectPayment(second, 9));
        check(secondTable.isFree() && restaurant.getPaidCount() == 2 && restaurant.getRevenueCents() == 2400
                && restaurant.getWaitingQueue().isEmpty(), "next-turn retry pays once and frees the last table");
    }

    private static void waiterSeatingCapacity() {
        Waiter waiter = new Waiter();
        Customer first = new RegularCustomer(1);
        Customer second = new RegularCustomer(2);
        Table one = new Table(1);
        Table two = new Table(2);
        for (int turn : new int[] {0, -1}) {
            refused(waiter.seat(first, one, turn));
            check(one.isFree() && first.getStatus() == CustomerStatus.WAITING, "bad turn cannot seat");
        }
        refused(waiter.seat(null, one, 1));
        refused(waiter.seat(first, null, 1));
        check(waiter.canAct(1), "invalid target preserves capacity");
        ok(waiter.seat(first, one, 1));
        refused(waiter.seat(second, two, 1));
        check(two.isFree() && second.getStatus() == CustomerStatus.WAITING, "second task atomic");
        check(!waiter.canAct(1) && waiter.canAct(2), "capacity resets by turn");
        refused(waiter.seat(second, one, 2));
        refused(waiter.seat(first, two, 2));
        check(one.getOccupant() == first && two.isFree(), "occupied/duplicate seating atomic");
        ok(waiter.seat(second, two, 2));
        refused(waiter.seat(new RegularCustomer(3), new Table(3), 1));
        check(waiter.getTaskTurn() == 2, "backwards task preserves capacity timestamp");
    }

    private static void waiterOrderRefusals() {
        Waiter waiter = new Waiter();
        Customer customer = new RegularCustomer(1);
        MenuItem burger = item(0);
        refused(waiter.takeOrder(customer, burger, 1500, 1));
        check(customer.getOrder() == null && waiter.canAct(1), "unseated order rejected without use");
        ok(waiter.seat(customer, new Table(1), 1));
        refused(waiter.takeOrder(customer, burger, 1500, 1));
        check(customer.getOrder() == null, "seating consumed Waiter task");
        refused(waiter.takeOrder(null, burger, 1500, 2));
        refused(waiter.takeOrder(customer, null, 1500, 2));
        refused(waiter.takeOrder(customer, burger, -1, 2));
        check(customer.getOrder() == null && waiter.canAct(2), "invalid item/bill leaves state and capacity");
        ok(waiter.takeOrder(customer, burger, 1500, 2));
        Order order = customer.getOrder();
        refused(waiter.takeOrder(customer, item(1), 900, 3));
        check(customer.getOrder() == order && order.getBillCents() == 1500 && waiter.canAct(3),
                "existing order and bill cannot be replaced; refusal preserves task");
        ok(waiter.seat(new RegularCustomer(2), new Table(2), 3));
    }

    private static void waiterServingRefusals() {
        Waiter waiter = new Waiter();
        Customer waiting = new RegularCustomer(2);
        Customer customer = placed(1, 0, 2);
        refused(waiter.serve(null, 2));
        refused(waiter.serve(waiting, 2));
        refused(waiter.serve(customer, 2));
        ok(new Chef().prepare(customer.getOrder(), 2));
        refused(waiter.serve(customer, 2));
        check(customer.getStatus() == CustomerStatus.WAITING_FOR_FOOD
                && customer.getOrder().getStatus() == OrderStatus.READY && waiter.canAct(2),
                "same-turn readiness cannot serve or consume task");
        ok(waiter.seat(waiting, new Table(2), 2));
        ok(waiter.serve(customer, 3));
        check(customer.getStatus() == CustomerStatus.SERVED
                && customer.getOrder().getStatus() == OrderStatus.SERVED, "serving synchronizes status");
        refused(waiter.serve(customer, 4));
        check(waiter.canAct(4), "repeated serving preserves next-turn task");
        Customer abandoned = placed(3, 2, 2);
        for (int i = 0; i < 13; i++) { abandoned.loseWaitingSatisfaction(); }
        refused(waiter.serve(abandoned, 4));
        check(waiter.canAct(4), "cancelled serving preserves task");
        ok(waiter.seat(new RegularCustomer(4), new Table(4), 4));
    }

    private static void chefCapacityAndRetry() {
        Chef chef = new Chef();
        Customer pasta = placed(3, 2, 2);
        Customer burger = placed(1, 0, 2);
        refused(chef.prepare(pasta.getOrder(), 0));
        refused(chef.prepare(pasta.getOrder(), 1));
        refused(chef.prepare(null, 2));
        check(chef.canAct(2), "failed cooking preserves task");
        ok(chef.prepare(pasta.getOrder(), 2));
        refused(chef.prepare(burger.getOrder(), 2));
        check(burger.getOrder().getCompletedPreparationUnits() == 0, "Chef cannot cook second order");
        refused(chef.prepare(pasta.getOrder(), 1));
        ok(chef.prepare(pasta.getOrder(), 3));
        refused(chef.prepare(pasta.getOrder(), 4));
        check(chef.canAct(4), "READY order rejection preserves task");
        ok(chef.prepare(burger.getOrder(), 4));
        Customer abandoned = placed(6, 2, 2);
        for (int i = 0; i < 13; i++) { abandoned.loseWaitingSatisfaction(); }
        refused(chef.prepare(abandoned.getOrder(), 5));
        check(chef.canAct(5) && abandoned.getOrder().getCompletedPreparationUnits() == 0,
                "cancelled cooking preserves progress and task");
        ok(chef.prepare(placed(2, 1, 2).getOrder(), 5));
    }

    private static void chefCannotBypassOrderTurnGuard() {
        Order pasta = placed(3, 2, 2).getOrder();
        ok(pasta.advancePreparation(2));
        Chef chef = new Chef();
        refused(chef.prepare(pasta, 2));
        check(pasta.getCompletedPreparationUnits() == 1 && chef.canAct(2),
                "direct preparation cannot be followed by a second Chef unit");
        ok(chef.prepare(placed(1, 0, 2).getOrder(), 2));
        ok(chef.prepare(pasta, 3));
    }

    private static void cashierCapacityAndSynchronization() {
        Cashier cashier = new Cashier();
        Customer first = readyToPay(1);
        Customer second = readyToPay(2);
        refused(cashier.collect(first, 0));
        refused(cashier.collect(null, 5));
        ok(cashier.collect(first, 5));
        check(first.getStatus() == CustomerStatus.PAID && first.getOrder().getStatus() == OrderStatus.PAID,
                "Cashier synchronizes Customer and Order");
        check(first.getSatisfactionAtPayment() == 100, "Cashier freezes satisfaction");
        refused(cashier.collect(second, 5));
        check(second.getStatus() == CustomerStatus.READY_TO_PAY
                && second.getOrder().getStatus() == OrderStatus.SERVED, "one payment per turn");
        refused(cashier.collect(first, 6));
        check(cashier.canAct(6), "duplicate payment preserves task");
        refused(cashier.collect(second, 4));
        ok(cashier.collect(second, 6));
        check(second.getStatus() == CustomerStatus.PAID, "next-turn payer succeeds");
    }

    private static void cashierEarlyPaymentRetry() {
        Cashier cashier = new Cashier();
        Customer customer = placed(1, 0, 2);
        refused(cashier.collect(customer, 2));
        ok(new Chef().prepare(customer.getOrder(), 2));
        ok(new Waiter().serve(customer, 3));
        refused(cashier.collect(customer, 3));
        refused(cashier.collect(customer, 4));
        ok(customer.finishEating(4));
        refused(cashier.collect(customer, 4));
        check(cashier.canAct(4) && customer.getStatus() == CustomerStatus.READY_TO_PAY
                && customer.getOrder().getStatus() == OrderStatus.SERVED, "early payment atomic");
        ok(cashier.collect(readyToPay(2), 5));
        ok(cashier.collect(customer, 6));
    }

    private static void pastaConsumerPath() {
        Customer customer = new RegularCustomer(3);
        Table table = new Table(1);
        Waiter waiter = new Waiter();
        Chef chef = new Chef();
        Cashier cashier = new Cashier();
        ok(waiter.seat(customer, table, 1));
        customer.loseWaitingSatisfaction();
        MenuItem pasta = customer.chooseItem(new Menu(false));
        check(pasta.getName().equals("Pasta"), "Customer selects Pasta deterministically");
        ok(waiter.takeOrder(customer, pasta, pasta.getPriceCents(), 2));
        ok(chef.prepare(customer.getOrder(), 2));
        customer.loseWaitingSatisfaction();
        ok(chef.prepare(customer.getOrder(), 3));
        customer.loseWaitingSatisfaction();
        check(customer.getSatisfaction() == 76, "Pasta waiting costs 24");
        ok(waiter.serve(customer, 4));
        customer.loseWaitingSatisfaction();
        refused(customer.finishEating(4));
        refused(table.release());
        ok(customer.finishEating(5));
        refused(cashier.collect(customer, 5));
        refused(table.release());
        ok(cashier.collect(customer, 6));
        check(table.getOccupant() == customer, "Cashier leaves aggregate table cleanup to Restaurant");
        ok(table.release());
        check(table.isFree() && customer.getSatisfactionAtPayment() == 76
                && customer.getOrder().getBillCents() == 1800, "Pasta example paid turn 6 at 76/$18");
    }

    private static Customer placed(int id, int itemIndex, int turn) {
        Customer customer = new RegularCustomer(id);
        ok(new Table(id).seat(customer));
        MenuItem item = item(itemIndex);
        ok(customer.placeOrder(item, item.getPriceCents(), turn));
        return customer;
    }

    private static Customer readyToPay(int id) {
        Customer customer = placed(id, 0, 2);
        ok(new Chef().prepare(customer.getOrder(), 2));
        ok(new Waiter().serve(customer, 3));
        ok(customer.finishEating(4));
        return customer;
    }

    private static MenuItem item(int index) { return new Menu(false).getItems().get(index); }
    private static void ok(String reason) { check(reason == null, "Expected success: " + reason); }
    private static void refused(String reason) { check(reason != null && !reason.isBlank(), "Expected refusal reason"); }
    private static void check(boolean value, String message) { if (!value) { throw new AssertionError(message); } }
}
