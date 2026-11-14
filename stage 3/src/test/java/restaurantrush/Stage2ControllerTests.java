package restaurantrush;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

import restaurantrush.control.GameController;
import restaurantrush.control.GameView;
import restaurantrush.control.WaiterAction;
import restaurantrush.domain.Cashier;
import restaurantrush.domain.Chef;
import restaurantrush.domain.Customer;
import restaurantrush.domain.CustomerStatus;
import restaurantrush.domain.Menu;
import restaurantrush.domain.MenuItem;
import restaurantrush.domain.OrderStatus;
import restaurantrush.domain.RegularCustomer;
import restaurantrush.domain.Restaurant;
import restaurantrush.domain.Waiter;

/** Inherited Stage 2 behavior on an explicit base3 menu and No/Host-Wait choices. */
public final class Stage2ControllerTests {
    private static int passed;

    private Stage2ControllerTests() { }

    public static void main(String[] args) {
        runAll();
    }

    public static void runAll() {
        passed = 0;
        validatesConfiguration();
        factoryRunsOncePerScheduledArrival();
        rejectsInvalidFactoryResultsBeforeRegistration();
        defaultVictoryKeepsTimingAndLockedBills();
        defaultDefeatUsesSubtypeLoss();
        dispatchesUnknownCustomerBehaviour();
        vipPriorityAlternative(true);
        vipPriorityAlternative(false);
        vipRetriesPreserveStaffTasks();
        System.out.println("Stage2ControllerTests: " + passed + " passed");
    }

    private static Restaurant restaurant() {
        return new Restaurant(new Menu(false), new Waiter(), new Chef(), new Cashier());
    }

    private static TestGameIO waits(int turns) {
        return new TestGameIO().waiterRepeated(WaiterAction.waitAction(), turns).chefRepeated(0, turns);
    }

    private static void run(Restaurant r, TestGameIO io) {
        new GameController(r, io, Main::createCustomer, 2).run();
    }

    private static void validatesConfiguration() {
        Check.throwsType(NullPointerException.class,
                () -> new GameController(null, waits(0), RegularCustomer::new, 2), "null restaurant");
        Check.throwsType(NullPointerException.class,
                () -> new GameController(restaurant(), null, RegularCustomer::new, 2), "null IO");
        Check.throwsType(NullPointerException.class,
                () -> new GameController(restaurant(), waits(0), null, 2), "null factory");
        for (int stage : new int[] {Integer.MIN_VALUE, 0, 4, Integer.MAX_VALUE}) {
            Check.throwsType(IllegalArgumentException.class,
                    () -> new GameController(restaurant(), waits(0), RegularCustomer::new, stage), "invalid stage");
        }
        for (int stage : new int[] {1, 2, 3}) {
            TestGameIO io = waits(1);
            new GameController(restaurant(), io, RegularCustomer::new, stage).run();
            Check.equal(stage, io.turnStarts().get(0).stage(), "injected start stage");
            Check.isTrue(io.actionStates().stream().allMatch(s -> s.stage() == stage), "injected action stage");
        }
        passed++;
    }

    private static void factoryRunsOncePerScheduledArrival() {
        Restaurant r = restaurant();
        TestGameIO io = waits(18);
        List<Integer> calls = new ArrayList<>();
        List<Customer> created = new ArrayList<>();
        new GameController(r, io, id -> {
            Check.equal(3 * (id - 1), io.turnEnds().size(), "factory called at scheduled turn start");
            calls.add(id);
            Customer c = Main.createCustomer(id);
            created.add(c);
            return c;
        }, 2).run();
        Check.equal(List.of(1, 2, 3, 4, 5, 6), calls, "one factory call per arrival");
        Check.equal(List.of("Regular", "VIP", "Critic", "Regular", "Regular", "Regular"),
                r.getCustomers().stream().map(Customer::getTypeLabel).toList(), "default setup labels");
        for (int id = 1; id <= 6; id++) {
            Check.isTrue(r.findCustomer(id) == created.get(id - 1), "register exact factory object");
            GameView.TurnState start = io.turnStarts().get(3 * (id - 1));
            final int arrivalId = id;
            Check.equal(100, start.waiting().stream().filter(c -> c.id() == arrivalId)
                    .findFirst().orElseThrow().satisfaction(), "arrival shown before first loss");
        }
        Check.isTrue(io.turnStarts().stream().allMatch(s -> s.stage() == 2), "stage 2 all turn headers");
        complete(io, false);
        passed++;
    }

    private static void rejectsInvalidFactoryResultsBeforeRegistration() {
        for (IntFunction<Customer> factory : List.<IntFunction<Customer>>of(id -> null,
                id -> new RegularCustomer(id + 1))) {
            Restaurant r = restaurant();
            TestGameIO io = waits(18);
            int[] calls = {0};
            Check.throwsType(IllegalStateException.class, () -> new GameController(r, io, id -> {
                calls[0]++;
                return factory.apply(id);
            }, 2).run(), "invalid factory result");
            Check.equal(1, calls[0], "no factory retry");
            Check.isTrue(r.getCustomers().isEmpty() && io.calls().isEmpty(), "reject before registration/display");
        }
        IllegalStateException failure = new IllegalStateException("factory failure");
        Check.isTrue(Check.throwsType(IllegalStateException.class,
                () -> new GameController(restaurant(), waits(0), id -> { throw failure; }, 2).run(),
                "factory failure escapes") == failure, "factory failure preserved");
        passed++;
    }

    private static void defaultVictoryKeepsTimingAndLockedBills() {
        Restaurant r = restaurant();
        TestGameIO io = new TestGameIO().waiter(WaiterAction.seat(1), WaiterAction.order(1),
                WaiterAction.serve(1), WaiterAction.seat(2), WaiterAction.order(2), WaiterAction.serve(2),
                WaiterAction.seat(3), WaiterAction.order(3), WaiterAction.waitAction(), WaiterAction.serve(3),
                WaiterAction.seat(4), WaiterAction.order(4), WaiterAction.serve(4), WaiterAction.seat(5),
                WaiterAction.order(5), WaiterAction.serve(5), WaiterAction.seat(6), WaiterAction.order(6))
                .chef(0, 1, 0, 0, 2, 0, 0, 3, 3, 0, 0, 4, 0, 0, 5, 0, 0, 6);
        run(r, io);
        Check.equal(5, r.getPaidCount(), "five paid");
        Check.equal(5, r.getServedCount(), "five served");
        Check.equal(6510, r.getRevenueCents(), "Stage 2 revenue");
        Check.equal(390, r.getPaidSatisfactionTotal(), "Stage 2 recorded satisfaction total");
        Check.equal(78.0, r.getAveragePaidSatisfaction().orElseThrow(), "Stage 2 average");
        Check.equal(List.of(84, 90, 64, 76, 76), r.getCustomers().stream()
                .filter(c -> c.getStatus() == CustomerStatus.PAID).map(Customer::getSatisfactionAtPayment).toList(),
                "subtype payment satisfaction");
        Check.equal(List.of(1500, 810, 1800, 1500, 900, 1800), r.getCustomers().stream()
                .map(c -> c.getOrder().getBillCents()).toList(), "all locked bills including unpaid C6");
        Check.equal(0, r.getUnhappyDepartureCount(), "no departures");
        for (int id = 1; id <= 5; id++) {
            Customer c = r.findCustomer(id);
            int serveTurn = id == 1 ? 3 : id == 2 ? 6 : 3 * id + 1;
            Check.equal(serveTurn - 1, c.getOrder().getReadyTurn(), "READY before serving");
            Check.equal(serveTurn, c.getServedTurn(), "serve timestamp");
            Check.equal(serveTurn + 1, c.getReadyToPayTurn(), "full turn eating");
            Check.equal(id - 1, io.turnEnds().get(serveTurn).paidCount(), "not paid when newly ready");
            Check.equal(id, io.turnEnds().get(serveTurn + 1).paidCount(), "payment following readiness");
            Check.isTrue(r.findTableFor(c) == null && c.getOrder().getStatus() == OrderStatus.PAID,
                    "aggregate payment and table release");
        }
        Check.equal(OrderStatus.PREPARING, r.findOrder(6).getStatus(), "late arrival cannot pay");
        Check.equal(1, r.findOrder(6).getCompletedPreparationUnits(), "Pasta still needs second unit");
        complete(io, true);
        passed++;
    }

    private static void defaultDefeatUsesSubtypeLoss() {
        Restaurant r = restaurant();
        TestGameIO io = waits(18);
        run(r, io);
        Check.equal(0, io.turnEnds().get(11).departures(), "no departure through turn 12");
        Check.equal(1, io.turnEnds().get(12).departures(), "Regular leaves turn 13");
        Check.equal(1, io.turnEnds().get(13).departures(), "Critic survives through turn 14");
        Check.equal(2, io.turnEnds().get(14).departures(), "Critic leaves turn 15");
        Check.equal(CustomerStatus.LEFT, r.findCustomer(3).getStatus(), "Critic left");
        Check.equal(CustomerStatus.WAITING, r.findCustomer(2).getStatus(), "VIP survives");
        Check.equal(25, r.findCustomer(2).getSatisfaction(), "VIP waiting loss through turn 18");
        Check.equal(0, r.getRevenueCents(), "no unpaid revenue");
        Check.equal(0, r.getServedCount(), "none served");
        Check.equal(0, r.getPaidCount(), "none paid");
        Check.isTrue(r.getAveragePaidSatisfaction().isEmpty(), "no paid average");
        complete(io, false);
        passed++;
    }

    private static void dispatchesUnknownCustomerBehaviour() {
        Restaurant r = restaurant();
        int[] choices = {0};
        int[] quotes = {0};
        Customer probe = new Customer(1) {
            @Override public String getTypeLabel() { return "Probe"; }
            @Override protected int waitingLossPerTurn() { return 7; }
            @Override public MenuItem chooseItem(Menu menu) {
                choices[0]++;
                return menu.getItems().stream().filter(i -> i.getName().equals("Pasta"))
                        .findFirst().orElseThrow();
            }
            @Override public int applyDiscount(int cents) {
                quotes[0]++;
                Check.equal(1800, cents, "quote receives chosen item's full price");
                return 1234;
            }
        };
        TestGameIO io = new TestGameIO().waiter(WaiterAction.seat(1), WaiterAction.order(1),
                WaiterAction.waitAction(), WaiterAction.serve(1)).waiterRepeated(WaiterAction.waitAction(), 2)
                .chef(0, 1, 1, 0, 0, 0);
        new GameController(r, io, id -> id == 1 ? probe : new RegularCustomer(id), 2).run();
        Check.equal(1, choices[0], "choice dispatched once for legal order");
        Check.equal(1, quotes[0], "discount dispatched once and bill locked");
        Check.equal(1234, r.getRevenueCents(), "unknown subtype discounted payment");
        Check.equal(79, probe.getSatisfactionAtPayment(), "unknown subtype waiting loss dispatched");
        Check.equal("Probe", io.actionStates().get(1).tables().get(0).occupant().type(), "label dispatched");
        Check.equal(1, io.interruptions().size(), "short trace ends via real EOF");
        Check.isTrue(io.finalReports().isEmpty(), "EOF has no verdict");
        passed++;
    }

    private static void vipPriorityAlternative(boolean vipFirst) {
        Restaurant r = restaurant();
        TestGameIO io = new TestGameIO().waiter(WaiterAction.seat(1), WaiterAction.waitAction(),
                WaiterAction.waitAction());
        if (vipFirst) {
            io.waiter(WaiterAction.seat(2), WaiterAction.order(2), WaiterAction.serve(2))
                    .waiterRepeated(WaiterAction.waitAction(), 12)
                    .chef(0, 0, 0, 0, 2, 0).chefRepeated(0, 12);
        } else {
            io.waiter(WaiterAction.order(1), WaiterAction.seat(2), WaiterAction.order(2), WaiterAction.serve(2))
                    .waiterRepeated(WaiterAction.waitAction(), 11)
                    .chef(0, 0, 0, 1, 0, 2, 0).chefRepeated(0, 11);
        }
        run(r, io);
        int paidTurn = vipFirst ? 8 : 9;
        int satisfaction = vipFirst ? 90 : 85;
        Customer vip = r.findCustomer(2);
        Check.equal(paidTurn - 3, vip.getOrder().getReadyTurn(), "VIP READY turn");
        Check.equal(paidTurn - 2, vip.getServedTurn(), "VIP serving turn");
        Check.equal(paidTurn - 1, vip.getReadyToPayTurn(), "VIP readiness turn");
        Check.equal(0, io.turnEnds().get(paidTurn - 2).paidCount(), "newly ready cannot pay");
        Check.equal(1, io.turnEnds().get(paidTurn - 1).paidCount(), "VIP payment turn " + paidTurn);
        Check.equal(satisfaction, vip.getSatisfactionAtPayment(), "manager priority changes satisfaction");
        Check.equal(810, vip.getOrder().getBillCents(), "same locked VIP quote in both alternatives");
        Check.equal(810, r.getRevenueCents(), "only VIP bill collected");
        Check.equal((double) satisfaction, r.getAveragePaidSatisfaction().orElseThrow(), "alternative average");
        Check.equal(1, r.getPaidCount(), "one paid");
        Check.equal(1, r.getServedCount(), "one served");
        Check.equal(2, r.getUnhappyDepartureCount(), "C1 and C3 leave");
        if (!vipFirst) {
            Check.equal(OrderStatus.CANCELLED, r.findOrder(1).getStatus(), "unserved C1 READY order cancelled");
        }
        complete(io, false);
        passed++;
    }

    private static void vipRetriesPreserveStaffTasks() {
        Restaurant r = restaurant();
        TestGameIO io = waits(3).waiter(WaiterAction.order(2), WaiterAction.seat(2),
                WaiterAction.order(2), WaiterAction.serve(2), WaiterAction.waitAction(), WaiterAction.serve(2))
                .chef(0, 999, 0, 2, 0);
        run(r, io);
        Check.equal(3, io.invalidMessages().size(), "bad order state, Chef target, premature serve refused");
        Check.equal(6, r.findOrder(2).getReadyTurn(), "Chef retry preserves preparation task");
        Check.equal(7, r.findCustomer(2).getServedTurn(), "premature serve retry stays before Chef");
        Check.equal(810, r.findOrder(2).getBillCents(), "VIP locked quote survives retries");
        Check.equal(85, r.findCustomer(2).getSatisfaction(), "extra unserved turn loses five");
        Check.equal(0, r.getRevenueCents(), "EOF before payment gives no revenue");
        Check.equal(1, io.interruptions().size(), "input exhaustion handled");
        passed++;
    }

    private static void complete(TestGameIO io, boolean victory) {
        Check.equal(18, io.turnStarts().size(), "18 turn starts");
        Check.equal(18, io.turnEnds().size(), "18 completed turns");
        Check.equal(18, io.waiterRequests(), "one successful Waiter choice each turn");
        Check.equal(18, io.chefRequests(), "one successful Chef choice each turn");
        Check.isTrue(io.allChoicesUsed() && io.invalidMessages().isEmpty() && io.interruptions().isEmpty(),
                "complete valid script consumed without fallback");
        Check.equal(1, io.finalReports().size(), "one final report");
        Check.equal(victory, io.finalReports().get(0).victory(), "final verdict");
        Check.equal(18, io.finalReports().get(0).totals().turn(), "result only after turn 18");
        List<String> calls = new ArrayList<>();
        for (int turn = 1; turn <= 18; turn++) {
            calls.addAll(List.of("showHappyHour", "chooseHappyHourActivation", "showHappyHour",
                    "showTurnStart", "chooseHostCustomer", "showActionState", "chooseWaiterAction", "showActionState",
                    "chooseChefOrder", "showActionState", "showTurnEnd"));
        }
        calls.add("showFinalResult");
        Check.equal(calls, io.calls(), "Stage 3 phases with inherited No/Wait choices");
    }
}
