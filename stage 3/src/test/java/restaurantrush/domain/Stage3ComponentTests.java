package restaurantrush.domain;

import java.lang.reflect.Modifier;
import java.util.List;
import restaurantrush.control.HappyHour;

public final class Stage3ComponentTests{
    private Stage3ComponentTests(){}

    public static void main(String[] args){
        runAll();
        System.out.println("Stage3ComponentTests: 13 scenarios passed");
    }

    public static void runAll(){
        hostMutationVisibility();
        hostPrimitiveGuards();
        hostThenWaiterOrders();
        independentSeatingCapacity();
        waiterThenHostSeating();
        aggregateSeatingRefusals();
        occupiedUntilPayment();
        defaultAndBaseMenus();
        composedValuesAndImmutability();
        comboConstructionGuards();
        comboOrdinaryLifecycle();
        policyQuotesAndDispatch();
        realPlacementAcrossPricingBoundary();
    }

    private static void hostMutationVisibility(){
        try{
            int access=Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE;
            equal(0, Host.class.getDeclaredMethod("seat", Customer.class, Table.class, int.class)
                    .getModifiers() & access, "Host seating stays package-private");
            check(Host.class.getSuperclass()==Staff.class, "Host shares the Staff capacity contract");
            for (String getter : List.of("getName", "getPriceCents", "getPreparationUnits"))check(Modifier.isFinal(MenuItem.class.getMethod(getter).getModifiers()),"menu values cannot be overridden: " + getter);
        }
        catch (ReflectiveOperationException failure){throw new AssertionError("Stage 3 API mismatch", failure);}
    }

    private static void hostPrimitiveGuards(){
        Host host=new Host();
        Customer first=new RegularCustomer(1);
        Customer second=new RegularCustomer(2);
        Table one=new Table(1);
        Table two=new Table(2);
        for (int turn : new int[]{0, -1}){refused(host.seat(first, one, turn)); }
        refused(host.seat(null, one, 1));
        refused(host.seat(first, null, 1));
        check(host.canAct(1) && one.isFree() && first.getStatus()==CustomerStatus.WAITING,"invalid primitive calls preserve capacity and state");
        ok(host.seat(first, one, 1));
        refused(host.seat(second, two, 1));
        check(two.isFree() && second.getStatus()==CustomerStatus.WAITING,"Host cannot seat twice in one turn");
        refused(host.seat(second, one, 2));
        refused(host.seat(first, two, 2));
        check(host.canAct(2) && two.isFree(), "occupied/duplicate targets preserve next-turn capacity");
        ok(host.seat(second, two, 2));
        refused(host.seat(new RegularCustomer(3), new Table(3), 1));
        equal(2, host.getTaskTurn(), "backwards request cannot move capacity timestamp");
    }

    private static void hostThenWaiterOrders(){
        Restaurant restaurant=restaurant();
        Customer customer=arrive(restaurant, 1);
        ok(restaurant.seatCustomerWithHost(customer, 1));
        check(restaurant.getWaitingQueue().isEmpty() && restaurant.findTableFor(customer) !=null,
                "Host seating updates both queue and table");
        check(!restaurant.getHost().canAct(1) && restaurant.getWaiter().canAct(1),
                "Host seating consumes only its own task");
        MenuItem item=customer.chooseItem(restaurant.getMenu());
        ok(restaurant.getWaiter().takeOrder(customer, item,
                new StandardPricingPolicy().priceFor(customer, item), 1));
        ok(restaurant.getChef().prepare(customer.getOrder(), 1));
        equal(OrderStatus.READY, customer.getOrder().getStatus(), "same-turn order and preparation");
        refused(restaurant.getWaiter().serve(customer, 1));
        ok(restaurant.getWaiter().serve(customer, 2));
    }

    private static void independentSeatingCapacity(){
        Restaurant restaurant=restaurant();
        Customer first=arrive(restaurant, 1);
        Customer second=arrive(restaurant, 2);
        ok(restaurant.seatCustomerWithHost(first, 1));
        refused(restaurant.seatCustomerWithHost(second, 1));
        check(restaurant.findTableFor(second)==null && restaurant.getWaitingQueue().equals(List.of(second)),
                "second Host attempt preserves queue/table state");
        ok(restaurant.seatCustomer(second, 1));
        check(restaurant.getTables().stream().noneMatch(Table::isFree)
                && restaurant.getWaitingQueue().isEmpty(), "both roles can fill separate tables in one turn");
        check(!restaurant.getHost().canAct(1) && !restaurant.getWaiter().canAct(1),
                "each role consumes exactly its own task");
    }

    private static void waiterThenHostSeating(){
        Restaurant restaurant=restaurant();
        Customer first=arrive(restaurant, 1);
        Customer second=arrive(restaurant, 2);
        ok(restaurant.seatCustomer(first, 1));
        check(restaurant.getHost().canAct(1), "Waiter action leaves Host capacity available");
        ok(restaurant.seatCustomerWithHost(second, 1));
        check(restaurant.findTableFor(first) !=restaurant.findTableFor(second),
                "Waiter and Host seat in distinct tables");
    }

    private static void aggregateSeatingRefusals(){
        Restaurant restaurant=restaurant();
        Customer first=arrive(restaurant, 1);
        Customer second=arrive(restaurant, 2);
        refused(restaurant.seatCustomerWithHost(null, 1));
        refused(restaurant.seatCustomerWithHost(new RegularCustomer(1), 1));
        Restaurant other=restaurant();
        refused(restaurant.seatCustomerWithHost(arrive(other, 3), 1));
        refused(restaurant.seatCustomerWithHost(first, 0));
        check(restaurant.getWaitingQueue().equals(List.of(first, second))
                && restaurant.getTables().stream().allMatch(Table::isFree)
                && restaurant.getHost().canAct(1), "invalid aggregate choices are atomic and retryable");
        ok(restaurant.seatCustomerWithHost(first, 1));
        refused(restaurant.seatCustomerWithHost(first, 2));
        check(restaurant.getHost().canAct(2), "nonqueued target does not consume the next Host task");
        ok(restaurant.seatCustomerWithHost(second, 2));
    }

    private static void occupiedUntilPayment(){
        Restaurant restaurant=restaurant();
        Customer first=arrive(restaurant, 1);
        Customer second=arrive(restaurant, 2);
        Customer third=arrive(restaurant, 3);
        ok(restaurant.seatCustomerWithHost(first, 1));
        MenuItem burger=first.chooseItem(restaurant.getMenu());
        ok(restaurant.getWaiter().takeOrder(first, burger, 1500, 1));
        ok(restaurant.getChef().prepare(first.getOrder(), 1));
        ok(restaurant.seatCustomerWithHost(second, 2));
        ok(restaurant.getWaiter().serve(first, 2));
        restaurant.advanceEating(3);
        equal(CustomerStatus.READY_TO_PAY, first.getStatus(), "first customer awaits automatic payment");
        refused(restaurant.seatCustomerWithHost(third, 4));
        check(restaurant.getHost().canAct(4) && restaurant.getWaitingQueue().equals(List.of(third)),
                "table to be freed later is still occupied during the Host phase");
        ok(restaurant.collectPayment(first, 4));
        // The coordinator does not revisit the earlier Host phase after this payment.
        ok(restaurant.seatCustomerWithHost(third, 5));
        check(restaurant.getWaitingQueue().isEmpty(), "following turn can use the released table");
    }

    private static void defaultAndBaseMenus(){
        Menu menu=new Menu();
        equal(4, menu.getItems().size(), "default offers the required fourth choice");
        String[] names={"Burger", "Salad", "Pasta", "Salad + Burger Combo"};
        for (int i=0; i < names.length; i++){
            equal(names[i], menu.getItems().get(i).getName(), "documented default order");
        }
        ComboMeal combo=(ComboMeal) menu.getItems().get(3);
        check(combo.getFirst()==menu.getItems().get(1) && combo.getSecond()==menu.getItems().get(0),
                "offered combo retains the existing Salad and Burger objects");
        equal(2160, combo.getPriceCents(), "offered combo price");
        equal(2, combo.getPreparationUnits(), "offered combo units");
        immutable(() -> menu.getItems().clear());
        Menu base=new Menu(false);
        equal(3, base.getItems().size(), "explicit inherited regression menu");
        immutable(() -> base.getItems().clear());
        check(new RegularCustomer(4).chooseItem(menu)==combo, "C4 chooses combo without manager input");
        equal("Burger", new RegularCustomer(5).chooseItem(menu).getName(), "C5 rotates to Burger");
        equal("Salad", new RegularCustomer(6).chooseItem(menu).getName(), "C6 rotates to Salad");
        equal("Burger", new RegularCustomer(4).chooseItem(base).getName(), "base3 rotation remains explicit");
        equal("Pasta", new CriticCustomer(4).chooseItem(menu).getName(), "Critic retains its Pasta preference");
    }

    private static void composedValuesAndImmutability(){
        Menu menu=new Menu();
        MenuItem salad=menu.getItems().get(1);
        MenuItem pasta=menu.getItems().get(2);
        ComboMeal combo=new ComboMeal(salad, pasta);
        equal("Salad + Pasta Combo", combo.getName(), "name derives from real components");
        equal(2430, combo.getPriceCents(), "brief's Salad/Pasta price");
        equal(3, combo.getPreparationUnits(), "brief's Salad/Pasta units");
        equal(2, combo.getComponents().size(), "exactly two components");
        check(combo.getFirst()==salad && combo.getSecond()==pasta
                && combo.getComponents().get(0)==salad && combo.getComponents().get(1)==pasta,
                "component references and order retained");
        immutable(() -> combo.getComponents().set(0, pasta));
        immutable(() -> combo.getComponents().add(salad));
        equal(900, salad.getPriceCents(), "component price unchanged");
        ComboMeal repeated=new ComboMeal(salad, salad);
        check(repeated.getFirst()==repeated.getSecond(), "the same component can be selected twice");
        equal(1620, repeated.getPriceCents(), "repeated component counted twice");
        equal(2, repeated.getPreparationUnits(), "repeated preparation counted twice");
        equal(5, new ComboMeal(new MenuItem("Tiny A", 2, 1), new MenuItem("Tiny B", 3, 1))
                .getPriceCents(), "4.5-cent combo rounds HALF_UP");
    }

    private static void comboConstructionGuards(){
        MenuItem item=new MenuItem("Normal", 1, 1);
        illegal(() -> new ComboMeal(null, item));
        illegal(() -> new ComboMeal(item, null));
        illegal(() -> new ComboMeal(null, null));
        illegal(() -> new ComboMeal(new MenuItem("Huge price", Integer.MAX_VALUE, 1), item));
        illegal(() -> new ComboMeal(item, new MenuItem("Huge price", Integer.MAX_VALUE, 1)));
        illegal(() -> new ComboMeal(new MenuItem("Huge preparation", 0, Integer.MAX_VALUE), item));
        ComboMeal maximum=new ComboMeal(new MenuItem("Maximum", Integer.MAX_VALUE, Integer.MAX_VALUE - 1),
                new MenuItem("Zero price", 0, 1));
        equal(1932735282, maximum.getPriceCents(), "maximum nonoverflowing price sum");
        equal(Integer.MAX_VALUE, maximum.getPreparationUnits(), "maximum nonoverflowing unit sum");
    }

    private static void comboOrdinaryLifecycle(){
        Restaurant restaurant=restaurant();
        Customer customer=arrive(restaurant, 4);
        ok(restaurant.seatCustomerWithHost(customer, 10));
        MenuItem combo=customer.chooseItem(restaurant.getMenu());
        ok(restaurant.getWaiter().takeOrder(customer, combo,
                new HappyHourPricingPolicy().priceFor(customer, combo), 10));
        Order order=customer.getOrder();
        check(order.getItem()==combo, "ordinary Order retains the actual ComboMeal");
        equal(1728, order.getBillCents(), "Happy Hour combo quote locked");
        ok(restaurant.getChef().prepare(order, 10));
        equal(OrderStatus.PREPARING, order.getStatus(), "combo needs both preparation units");
        refused(restaurant.getChef().prepare(order, 10));
        refused(restaurant.getWaiter().serve(customer, 11));
        ok(restaurant.getChef().prepare(order, 11));
        equal(OrderStatus.READY, order.getStatus(), "second unit finishes combo");
        refused(restaurant.getWaiter().serve(customer, 11));
        check(restaurant.getWaiter().canAct(11), "same-turn READY refusal leaves Waiter task available");
        ok(restaurant.getWaiter().serve(customer, 12));
        restaurant.advanceEating(13);
        refused(restaurant.collectPayment(customer, 13));
        equal(0, restaurant.getRevenueCents(), "unpaid combo adds no revenue");
        ok(restaurant.collectPayment(customer, 14));
        equal(1728, restaurant.getRevenueCents(), "expired policy does not alter the collected combo bill");
        equal(OrderStatus.PAID, order.getStatus(), "combo uses the ordinary completed lifecycle");
    }

    private static void policyQuotesAndDispatch(){
        Menu menu=new Menu();
        MenuItem burger=menu.getItems().get(0);
        MenuItem combo=menu.getItems().get(3);
        PricingPolicy standard=new StandardPricingPolicy();
        PricingPolicy happyHour=new HappyHourPricingPolicy();
        Customer regular=new RegularCustomer(1);
        Customer vip=new VIPCustomer(2);
        equal(1500, standard.priceFor(regular, burger), "Regular Burger standard");
        equal(1200, happyHour.priceFor(regular, burger), "Regular Burger Happy Hour");
        equal(1350, standard.priceFor(vip, burger), "VIP Burger standard");
        equal(1080, happyHour.priceFor(vip, burger), "VIP Burger applies 90% then 80%");
        equal(1944, standard.priceFor(vip, combo), "VIP combo before Happy Hour");
        equal(1555, happyHour.priceFor(vip, combo), "VIP combo rounds after both price steps");
        ComboMeal saladPasta=new ComboMeal(menu.getItems().get(1), menu.getItems().get(2));
        equal(1944, happyHour.priceFor(regular, saladPasta), "Regular Salad/Pasta Happy Hour");
        equal(2, happyHour.priceFor(vip, new MenuItem("Two cents", 2, 1)),
                "round each discount separately instead of multiplying by 72%");
        Customer custom=new Customer(7){
            @Override public String getTypeLabel(){return "Test discount"; }
            @Override protected int waitingLossPerTurn(){return 8; }
            @Override public int applyDiscount(int cents){return cents - 123; }
        };
        equal(1377, standard.priceFor(custom, burger), "standard dispatches through arbitrary Customer");
        equal(1102, happyHour.priceFor(custom, burger), "Happy Hour dispatches before its own discount");
        for (PricingPolicy policy : List.of(standard, happyHour)){
            illegal(() -> policy.priceFor(null, burger));
            illegal(() -> policy.priceFor(regular, null));
            illegal(() -> policy.priceFor(null, null));
        }
        equal(2160, combo.getPriceCents(), "quotes never mutate offered menu prices");
    }

    private static void realPlacementAcrossPricingBoundary(){
        PricingPolicy standard=new StandardPricingPolicy();
        PricingPolicy active=new HappyHourPricingPolicy();
        int[] expectedBills={1350, 1080, 1080, 1350};
        for (int turn=9; turn <=12; turn++){
            HappyHour happyHour=new HappyHour();
            if (turn >=10){ok(happyHour.activate(10)); }
            Restaurant restaurant=restaurant();
            Customer vip=new VIPCustomer(1);
            restaurant.registerArrival(vip);
            ok(restaurant.seatCustomerWithHost(vip, turn));
            MenuItem burger=vip.chooseItem(restaurant.getMenu());
            PricingPolicy selected=happyHour.isActiveAt(turn) ? active : standard;
            ok(restaurant.getWaiter().takeOrder(vip, burger, selected.priceFor(vip, burger), turn));
            int expected=expectedBills[turn - 9];
            equal(expected, vip.getOrder().getBillCents(), "bill at actual placement on turn " + turn);
            if (turn==9){
                ok(happyHour.activate(10));
                equal(1350, vip.getOrder().getBillCents(), "later activation leaves an existing bill unchanged");
            }
            // Include activation/expiry in the elapsed time before cooking/payment.
            int prepareTurn=Math.max(12, turn);
            ok(restaurant.getChef().prepare(vip.getOrder(), prepareTurn));
            ok(restaurant.getWaiter().serve(vip, prepareTurn + 1));
            restaurant.advanceEating(prepareTurn + 2);
            check(!happyHour.isActiveAt(prepareTurn + 3), "payment occurs after expiry");
            ok(restaurant.collectPayment(vip, prepareTurn + 3));
            equal(expected, vip.getOrder().getBillCents(), "bill unchanged through post-expiry payment");
            equal(expected, restaurant.getRevenueCents(), "only the original locked bill is collected");
        }
    }

    private static Restaurant restaurant(){
        return new Restaurant(new Menu(), new Waiter(), new Chef(), new Cashier());
    }

    private static Customer arrive(Restaurant restaurant, int id){
        Customer customer=new RegularCustomer(id);
        restaurant.registerArrival(customer);
        return customer;
    }

    private static void illegal(Runnable action){
        try{
            action.run();
            throw new AssertionError("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected){/* validation contract */ }
    }

    private static void immutable(Runnable action){
        try{
            action.run();
            throw new AssertionError("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected){/* immutable view */ }
    }

    private static void ok(String reason){check(reason==null, "Expected success: " + reason); }
    private static void refused(String reason){check(reason !=null && !reason.isBlank(), "Expected refusal reason"); }
    private static void check(boolean value, String message){if (!value){throw new AssertionError(message); } }
    private static void equal(Object expected, Object actual, String message){
        check(java.util.Objects.equals(expected, actual), message + ": expected " + expected + ", got " + actual);
    }
}
