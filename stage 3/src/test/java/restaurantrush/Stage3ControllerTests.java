package restaurantrush;

import java.util.ArrayList;
import java.util.List;

import restaurantrush.control.GameController;
import restaurantrush.control.GameView;
import restaurantrush.control.HappyHour;
import restaurantrush.control.WaiterAction;
import restaurantrush.domain.Cashier;
import restaurantrush.domain.Chef;
import restaurantrush.domain.ComboMeal;
import restaurantrush.domain.Customer;
import restaurantrush.domain.CustomerStatus;
import restaurantrush.domain.Menu;
import restaurantrush.domain.OrderStatus;
import restaurantrush.domain.Restaurant;
import restaurantrush.domain.RegularCustomer;
import restaurantrush.domain.VIPCustomer;
import restaurantrush.domain.Waiter;

/** Real Stage 3 turn integration. No state setters or substituted gameplay model. */
public final class Stage3ControllerTests {
    private static int passed;

    private Stage3ControllerTests() { }

    public static void main(String[] args) { runAll(); }

    public static void runAll() {
        passed = 0;
        timestampWindowAndOnceOnlyActivation();
        stageMetadataDoesNotDisableFeatures();
        hostRetriesThenWaiterOrdersSameTurn();
        hostAndWaiterSeatIndependently();
        hostCannotUseTableFreedLaterThisTurn();
        for (int orderTurn = 1; orderTurn <= 4; orderTurn++) {
            priceLocksAcrossActivationAndExpiry(orderTurn);
        }
        recoveryUsesStartEligibilityAndExpires();
        activationOnTurn18EndsAt18();
        focusedVipPriority(true);
        focusedVipPriority(false);
        defaultVictoryIncludesComboAndFastestC6();
        allWaitDefeat();
        happyHourEofStopsBeforeRecoveryAndStaff();
        hostEofAndRetryDoNotAdvanceTurn();
        waiterEofPreservesHostSeating();
        System.out.println("Stage3ControllerTests: " + passed + " passed");
    }

    private static Restaurant restaurant() {
        return new Restaurant(new Menu(), new Waiter(), new Chef(), new Cashier());
    }

    private static void run(Restaurant r, TestGameIO io) {
        new GameController(r, io, Main::createCustomer, 3).run();
    }

    private static TestGameIO activationAt(int turn) {
        return new TestGameIO().happyHourRepeated(false, turn - 1).happyHour(true);
    }

    private static void timestampWindowAndOnceOnlyActivation() {
        HappyHour hh = new HappyHour();
        for (int turn : new int[] {Integer.MIN_VALUE, 0, 1, 18, Integer.MAX_VALUE}) {
            Check.isTrue(!hh.isActiveAt(turn) && hh.remainingAt(turn) == 0, "unused window inactive");
        }
        for (int turn : new int[] {Integer.MIN_VALUE, 0, 19, Integer.MAX_VALUE}) {
            Check.isTrue(hh.activate(turn) != null && !hh.hasBeenActivated(), "invalid turn preserves activation");
        }
        Check.equal(null, hh.activate(10), "activate10");
        Check.equal(0, hh.remainingAt(9), "no recovery before activation");
        Check.equal(2, hh.remainingAt(10), "activation turn remaining2");
        Check.equal(1, hh.remainingAt(11), "second turn remaining1");
        Check.equal(0, hh.remainingAt(12), "expiry remaining0");
        Check.isTrue(hh.activate(12) != null && hh.activate(10) != null, "no reuse after expiry or same turn");
        Check.isTrue(hh.hasBeenActivated() && !hh.isActiveAt(Integer.MAX_VALUE), "used state persists");
        HappyHour late = new HappyHour();
        Check.equal(null, late.activate(18), "last legal activation");
        Check.equal(2, late.remainingAt(18), "last turn full activation window");
        Check.equal(1, late.remainingAt(19), "timestamp window does not create a game turn");
        Check.equal(0, late.remainingAt(20), "last window expiry");
        passed++;
    }

    private static void stageMetadataDoesNotDisableFeatures() {
        for (int stage : new int[] {1, 2, 3}) {
            Restaurant r = restaurant();
            TestGameIO io = activationAt(1).host(1).waiter(WaiterAction.order(1)).chef(1);
            new GameController(r, io, Main::createCustomer, stage).run();
            Check.equal(stage, io.turnStarts().get(0).stage(), "metadata displayed");
            Check.equal(1200, r.findOrder(1).getBillCents(), "HH applies at every metadata value");
            Check.equal(1, r.findOrder(1).getReadyTurn(), "Host then Waiter then Chef at every metadata value");
        }
        passed++;
    }

    private static void hostRetriesThenWaiterOrdersSameTurn() {
        Restaurant r = restaurant();
        TestGameIO io = new TestGameIO().host(99, -1, 1)
                .waiter(WaiterAction.seat(1), WaiterAction.order(1)).chef(1);
        run(r, io);
        Check.equal(3, io.invalidMessages().size(), "two invalid Host targets and already-seated Waiter retry");
        Check.equal(1, io.turnEnds().size(), "retries all stay on turn1");
        Check.equal(1, r.findOrder(1).getReadyTurn(), "Host seat and Waiter order allow same-turn preparation");
        Check.equal(List.of(1), io.actionStates().get(0).orderableCustomerIds(), "Host refresh shows new order target");
        Check.equal(List.of(1), io.actionStates().get(1).preparableOrderIds(), "Waiter refresh shows new Chef target");
        Check.isTrue(r.getWaitingQueue().isEmpty(), "Host seating removes queue entry");
        Check.equal(92, r.findCustomer(1).getSatisfaction(), "one loss after retries");
        Customer extra = new RegularCustomer(7);
        r.registerArrival(extra);
        Check.isTrue(r.findFreeTable() != null, "second table still free for capacity check");
        Check.isTrue(r.seatCustomerWithHost(extra, 1) != null, "Host cannot perform a second successful task");
        Check.isTrue(r.seatCustomer(extra, 1) != null, "Waiter cannot perform a second successful task");
        Check.equal(CustomerStatus.WAITING, extra.getStatus(), "capacity refusals retain queue customer");
        Check.isTrue(io.interruptions().get(0).contains("turn 2, Host"), "explicit Host input exhaustion");
        passed++;
    }

    private static void hostAndWaiterSeatIndependently() {
        Restaurant r = restaurant();
        TestGameIO io = new TestGameIO().host(0, 0, 0, 1)
                .waiterRepeated(WaiterAction.waitAction(), 3).waiter(WaiterAction.seat(2)).chefRepeated(0, 4);
        run(r, io);
        Check.equal(2L, r.getTables().stream().filter(t -> !t.isFree()).count(), "both tables filled on turn4");
        Check.isTrue(r.findTableFor(r.findCustomer(1)) != r.findTableFor(r.findCustomer(2)), "separate tables");
        Check.isTrue(io.invalidMessages().isEmpty(), "independent role capacities");
        passed++;
    }

    private static void hostCannotUseTableFreedLaterThisTurn() {
        Restaurant r = restaurant();
        TestGameIO io = new TestGameIO().host(1, 0, 0, 2, 0, 0, 3, 0, 3)
                .waiter(WaiterAction.order(1), WaiterAction.waitAction(), WaiterAction.waitAction(),
                        WaiterAction.waitAction(), WaiterAction.serve(1), WaiterAction.waitAction(),
                        WaiterAction.seat(3), WaiterAction.waitAction(), WaiterAction.order(3))
                .chef(1, 0, 0, 0, 0, 0, 0, 3);
        run(r, io);
        Check.equal(2, io.invalidMessages().size(), "Host and Waiter both refuse full tables on turn7");
        Check.isTrue(io.invalidMessages().stream().allMatch(s -> s.contains("No free table")), "same table guard");
        Check.equal(0, io.turnEnds().get(5).paidCount(), "no payment before turn7");
        Check.equal(1, io.turnEnds().get(6).paidCount(), "payment frees table only after both staff phases");
        Check.isTrue(io.turnStarts().get(6).seatableCustomerIds().isEmpty(), "full start-of-turn capacity");
        Check.equal(8, r.findOrder(3).getPlacedTurn(), "Host can seat and Waiter order on turn8");
        passed++;
    }

    private static void priceLocksAcrossActivationAndExpiry(int orderTurn) {
        Restaurant r = restaurant();
        TestGameIO io = activationAt(2).host(1).hostRepeated(0, 17)
                .waiterRepeated(WaiterAction.waitAction(), orderTurn - 1)
                .waiter(WaiterAction.order(1), WaiterAction.serve(1))
                .waiterRepeated(WaiterAction.waitAction(), 17 - orderTurn)
                .chefRepeated(0, orderTurn - 1).chef(1).chefRepeated(0, 18 - orderTurn);
        new GameController(r, io, id -> id == 1 ? new VIPCustomer(id) : Main.createCustomer(id), 3).run();
        int expectedBill = orderTurn == 2 || orderTurn == 3 ? 1080 : 1350;
        Customer c = r.findCustomer(1);
        Check.equal(expectedBill, c.getOrder().getBillCents(), "VIP Burger locked at placement turn" + orderTurn);
        Check.equal(expectedBill, r.getRevenueCents(), "locked bill collected after expiry");
        Check.equal(1, r.getPaidCount(), "one paid");
        Check.equal(1, io.turnEnds().get(orderTurn + 2).paidCount(), "payment three turns after placement");
        Check.equal(0, io.turnEnds().get(orderTurn + 1).paidCount(), "no payment when newly ready");
        Check.equal(2, io.happyHourRequests(), "never offered after activation or expiry");
        complete(io, false);
        passed++;
    }

    private static void recoveryUsesStartEligibilityAndExpires() {
        // Actual flows reach READY food, SERVED, READY_TO_PAY, PAID and LEFT before recovery.
        for (int activation : new int[] {2, 3, 4, 5, 14}) {
            Restaurant r = restaurant();
            TestGameIO io = activationAt(activation).host(1).hostRepeated(0, 17)
                    .waiter(WaiterAction.order(1), activation == 14 ? WaiterAction.waitAction() : WaiterAction.serve(1))
                    .waiterRepeated(WaiterAction.waitAction(), 16)
                    .chef(activation == 14 ? 0 : 1).chefRepeated(0, 17);
            run(r, io);
            Customer c1 = r.findCustomer(1);
            if (activation == 14) {
                Check.equal(CustomerStatus.LEFT, c1.getStatus(), "left customer stays left");
                Check.equal(0, c1.getSatisfaction(), "left customer cannot recover");
                Check.equal(OrderStatus.CANCELLED, c1.getOrder().getStatus(), "left order stays cancelled");
            } else {
                int expected = activation == 2 ? 100 : 92;
                Check.equal(expected, c1.getSatisfactionAtPayment(), "eligible READY only, no served/ready/paid recovery");
            }
            // C2 stays queued and receives exactly two start boosts, then ordinary losses.
            if (activation == 5) {
                Check.equal(100, waiting(io, 5, 2).satisfaction(), "waiting95 recovers capped100");
                Check.equal(100, waiting(io, 6, 2).satisfaction(), "second recovery at turn start");
                Check.equal(95, waiting(io, 7, 2).satisfaction(), "expiry leaves previous loss intact");
            }
            Check.equal(activation, io.happyHourRequests(), "one offer per turn until activation");
            complete(io, false);
        }
        passed++;
    }

    private static GameView.CustomerLine waiting(TestGameIO io, int turn, int id) {
        return io.turnStarts().get(turn - 1).waiting().stream().filter(c -> c.id() == id).findFirst().orElseThrow();
    }

    private static void activationOnTurn18EndsAt18() {
        Restaurant r = restaurant();
        TestGameIO io = activationAt(18).hostRepeated(0, 18)
                .waiterRepeated(WaiterAction.waitAction(), 18).chefRepeated(0, 18);
        run(r, io);
        Check.equal(18, io.happyHourRequests(), "declines offered until turn18 activation");
        Check.equal(new TestGameIO.HappyHourDisplay(18, 2, true),
                io.happyHourDisplays().get(io.happyHourDisplays().size() - 1), "remaining2 on final turn");
        Check.equal(45, waiting(io, 18, 2).satisfaction(), "turn18 recovery precedes ordinary loss");
        Check.equal(40, r.findCustomer(2).getSatisfaction(), "turn18 ordinary loss still applies");
        complete(io, false);
        passed++;
    }

    private static void focusedVipPriority(boolean vipFirst) {
        Restaurant r = restaurant();
        TestGameIO io = activationAt(4).host(1, 0, 0, 2).hostRepeated(0, 14)
                .waiterRepeated(WaiterAction.waitAction(), 3).chefRepeated(0, 3);
        if (vipFirst) {
            io.waiter(WaiterAction.order(2), WaiterAction.serve(2))
                    .waiterRepeated(WaiterAction.waitAction(), 13).chef(2).chefRepeated(0, 14);
        } else {
            io.waiter(WaiterAction.order(1), WaiterAction.order(2), WaiterAction.serve(2))
                    .waiterRepeated(WaiterAction.waitAction(), 12).chef(1, 2).chefRepeated(0, 13);
        }
        run(r, io);
        int paidTurn = vipFirst ? 7 : 8;
        Customer vip = r.findCustomer(2);
        Check.equal(648, vip.getOrder().getBillCents(), "VIP then HH Salad price locked");
        Check.equal(paidTurn - 3, vip.getOrder().getReadyTurn(), "focused READY timestamp");
        Check.equal(paidTurn - 2, vip.getServedTurn(), "focused serving timestamp");
        Check.equal(paidTurn - 1, vip.getReadyToPayTurn(), "focused eating timestamp");
        Check.equal(0, io.turnEnds().get(paidTurn - 2).paidCount(), "newly ready cannot pay");
        Check.equal(1, io.turnEnds().get(paidTurn - 1).paidCount(), "focused payment turn" + paidTurn);
        Check.equal(vipFirst ? 100 : 95, vip.getSatisfactionAtPayment(), "manager choice changes paid satisfaction");
        Check.equal(648, r.getRevenueCents(), "only VIP pays, after expiry");
        Check.equal(4, io.happyHourRequests(), "one-time activation on4 in both alternatives");
        Check.equal(2, r.getUnhappyDepartureCount(), "C1 and C3 leave after boosts expire");
        Check.equal(0, io.turnEnds().get(13).departures(), "recovery postpones C1 departure beyond14");
        Check.equal(1, io.turnEnds().get(14).departures(), "Critic C3 leaves15");
        Check.equal(1, io.turnEnds().get(15).departures(), "C1 survives16 after boosts");
        Check.equal(2, io.turnEnds().get(16).departures(), "C1 leaves17");
        if (!vipFirst) {
            Check.equal(1200, r.findOrder(1).getBillCents(), "earlier C1 HH order locks price too");
            Check.equal(OrderStatus.CANCELLED, r.findOrder(1).getStatus(), "unserved C1 READY order cancelled");
        }
        complete(io, false);
        passed++;
    }

    private static void defaultVictoryIncludesComboAndFastestC6() {
        Restaurant r = restaurant();
        TestGameIO io = activationAt(10).host(1, 0, 0, 2, 0, 0, 3, 0, 0, 4, 0, 0, 5, 0, 0, 6, 0, 0)
                .waiter(WaiterAction.order(1), WaiterAction.serve(1), WaiterAction.waitAction(),
                        WaiterAction.order(2), WaiterAction.serve(2), WaiterAction.waitAction(),
                        WaiterAction.order(3), WaiterAction.waitAction(), WaiterAction.serve(3),
                        WaiterAction.order(4), WaiterAction.waitAction(), WaiterAction.serve(4),
                        WaiterAction.order(5), WaiterAction.serve(5), WaiterAction.waitAction(),
                        WaiterAction.order(6), WaiterAction.serve(6), WaiterAction.waitAction())
                .chef(1, 0, 0, 2, 0, 0, 3, 3, 0, 4, 4, 0, 5, 0, 0, 6, 0, 0);
        run(r, io);
        Check.equal(5, r.getPaidCount(), "Stage3 five paid");
        Check.equal(6, r.getServedCount(), "Stage3 six served");
        Check.equal(7338, r.getRevenueCents(), "Stage3 revenue");
        Check.equal(447, r.getPaidSatisfactionTotal(), "Stage3 paid satisfaction total");
        Check.equal(89.4, r.getAveragePaidSatisfaction().orElseThrow(), "Stage3 paid average");
        Check.equal(List.of(92, 95, 76, 92, 92), r.getCustomers().stream()
                .filter(c -> c.getStatus() == CustomerStatus.PAID).map(Customer::getSatisfactionAtPayment).toList(),
                "paid snapshots");
        Check.equal(List.of(1500, 810, 1800, 1728, 1500, 900), r.getCustomers().stream()
                .map(c -> c.getOrder().getBillCents()).toList(), "complete locked menu pricing");
        Check.isTrue(r.findOrder(4).getItem() instanceof ComboMeal, "C4 chooses actual composed menu item");
        Check.equal(2, r.findOrder(4).getItem().getPreparationUnits(), "combo requires sum of units");
        Check.equal(1, io.actionStates().get(29).orders().stream().filter(o -> o.orderId() == 4)
                .findFirst().orElseThrow().completedUnits(), "combo only unit1 on turn10");
        Check.equal(11, r.findOrder(4).getReadyTurn(), "combo second preparation turn");
        Check.equal(12, r.findCustomer(4).getServedTurn(), "combo cannot serve on completion turn");
        Check.equal(16, r.findOrder(6).getReadyTurn(), "fastest C6 preparation on arrival");
        Check.equal(17, r.findCustomer(6).getServedTurn(), "fastest C6 serving next turn");
        Check.equal(18, r.findCustomer(6).getReadyToPayTurn(), "fastest C6 readiness");
        Check.equal(CustomerStatus.READY_TO_PAY, r.findCustomer(6).getStatus(), "C6 stays unpaid at game end");
        Check.isTrue(r.oldestEligiblePayer(18) == null && r.oldestEligiblePayer(19) == r.findCustomer(6),
                "C6 earliest theoretical payment19 does not extend game");
        Check.equal(0, r.getUnhappyDepartureCount(), "no departures");
        Check.equal(10, io.happyHourRequests(), "no second activation offer after expiry");
        for (int turn = 1; turn <= 18; turn++) {
            final int currentTurn = turn;
            var displays = io.happyHourDisplays().stream().filter(d -> d.turn() == currentTurn).toList();
            var last = displays.get(displays.size() - 1);
            Check.equal(turn == 10 ? 2 : turn == 11 ? 1 : 0, last.remaining(), "actual duration display");
            Check.equal(turn >= 10, last.used(), "used remains true after expiry");
        }
        complete(io, true);
        passed++;
    }

    private static void allWaitDefeat() {
        Restaurant r = restaurant();
        TestGameIO io = new TestGameIO().happyHourRepeated(false, 18).hostRepeated(0, 18)
                .waiterRepeated(WaiterAction.waitAction(), 18).chefRepeated(0, 18);
        run(r, io);
        Check.equal(0, r.getPaidCount(), "none paid");
        Check.equal(0, r.getServedCount(), "none served");
        Check.equal(0, r.getRevenueCents(), "zero unpaid revenue");
        Check.equal(2, r.getUnhappyDepartureCount(), "two departures");
        Check.isTrue(r.getAveragePaidSatisfaction().isEmpty(), "N/A paid average");
        Check.equal(25, r.findCustomer(2).getSatisfaction(), "VIP waits through18");
        Check.equal(18, io.happyHourRequests(), "declining continues offering");
        complete(io, false);
        passed++;
    }

    private static void happyHourEofStopsBeforeRecoveryAndStaff() {
        Restaurant r = restaurant();
        TestGameIO io = new TestGameIO().happyHour();
        run(r, io);
        Check.equal(1, r.getCustomers().size(), "arrival occurs before activation prompt");
        Check.equal(100, r.findCustomer(1).getSatisfaction(), "EOF skips loss");
        Check.isTrue(io.turnStarts().isEmpty() && io.hostRequests() == 0 && io.waiterRequests() == 0,
                "activation EOF stops remaining phases");
        interrupted(io, "turn 1, Happy Hour", 0);
        passed++;
    }

    private static void hostEofAndRetryDoNotAdvanceTurn() {
        Restaurant r = restaurant();
        TestGameIO io = activationAt(1).host(99);
        run(r, io);
        Check.equal(1, io.invalidMessages().size(), "invalid Host target explained");
        Check.equal(2, io.hostRequests(), "Host EOF during retry");
        Check.equal(0, io.waiterRequests(), "no later staff phase");
        Check.equal(100, r.findCustomer(1).getSatisfaction(), "EOF skips ordinary loss");
        Check.equal(1, io.turnStarts().size(), "start shown after activation/recovery");
        interrupted(io, "turn 1, Host", 0);
        passed++;
    }

    private static void waiterEofPreservesHostSeating() {
        Restaurant r = restaurant();
        TestGameIO io = new TestGameIO().host(1);
        run(r, io);
        Check.equal(CustomerStatus.SEATED, r.findCustomer(1).getStatus(), "successful Host action retained");
        Check.isTrue(r.getWaitingQueue().isEmpty(), "seating cleanup retained");
        Check.equal(0, io.chefRequests(), "Waiter EOF skips Chef");
        Check.equal(100, r.findCustomer(1).getSatisfaction(), "no loss before incomplete turn");
        interrupted(io, "turn 1, Waiter", 0);
        passed++;
    }

    private static void interrupted(TestGameIO io, String where, int completed) {
        Check.equal(completed, io.turnEnds().size(), "only complete turns summarized");
        Check.isTrue(io.finalReports().isEmpty() && io.interruptions().size() == 1
                && io.interruptions().get(0).contains(where), "precise EOF role and no verdict");
    }

    private static void complete(TestGameIO io, boolean victory) {
        Check.equal(18, io.turnStarts().size(), "18 start snapshots");
        Check.equal(18, io.turnEnds().size(), "18 completed turns");
        Check.equal(54, io.actionStates().size(), "Host/Waiter/automatic refresh each turn");
        Check.equal(18, io.hostRequests(), "one successful Host choice per turn");
        Check.equal(18, io.waiterRequests(), "one successful Waiter choice per turn");
        Check.equal(18, io.chefRequests(), "one successful Chef choice per turn");
        Check.isTrue(io.invalidMessages().isEmpty() && io.interruptions().isEmpty() && io.allChoicesUsed(),
                "valid complete script consumed exactly");
        Check.equal(1, io.finalReports().size(), "one final result");
        Check.equal(victory, io.finalReports().get(0).victory(), "turn18 verdict");
        Check.equal(18, io.finalReports().get(0).totals().turn(), "result at18 only");
        List<String> expectedCalls = new ArrayList<>();
        for (int turn = 1; turn <= 18; turn++) {
            expectedCalls.add("showHappyHour");
            if (turn <= io.happyHourRequests()) {
                expectedCalls.addAll(List.of("chooseHappyHourActivation", "showHappyHour"));
            }
            expectedCalls.addAll(List.of("showTurnStart", "chooseHostCustomer", "showActionState",
                    "chooseWaiterAction", "showActionState", "chooseChefOrder", "showActionState", "showTurnEnd"));
        }
        expectedCalls.add("showFinalResult");
        Check.equal(expectedCalls, io.calls(), "exact Stage3 phase order and conditional activation offers");
    }
}
