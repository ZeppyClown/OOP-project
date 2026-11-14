package restaurantrush;

import java.util.List;

import restaurantrush.control.GameController;
import restaurantrush.control.GameIO;
import restaurantrush.control.GameView;
import restaurantrush.control.WaiterAction;
import restaurantrush.domain.Cashier;
import restaurantrush.domain.Chef;
import restaurantrush.domain.CustomerStatus;
import restaurantrush.domain.Menu;
import restaurantrush.domain.OrderStatus;
import restaurantrush.domain.Restaurant;
import restaurantrush.domain.Waiter;

/** GameController turn loop driven by TestGameIO: order of calls, retries, state lists and results. */
public final class TurnTests {
    private static final WaiterAction WAIT = WaiterAction.waitAction();

    private TurnTests() {
    }

    public static void main(String[] args) {
        runAll();
        System.out.println("TurnTests passed");
    }

    public static void runAll() {
        constructorRejectsNulls();
        callOrderPerTurn();
        arrivalsEveryThreeTurns();
        allWaitDefeatReport();
        victoryReportAndAutomaticPayment();
        turnStateListsFollowAvailability();
        waiterSemanticRetries();
        nullWaiterActionIsRetried();
        chefSemanticRetries();
        noFreeTableHidesSeating();
        departureFreesTableDuringGame();
        interruptionNamesTurnAndRole();
    }

    private static void constructorRejectsNulls() {
        Check.throwsType(NullPointerException.class, () -> new GameController(null, new TestGameIO()), "null restaurant");
        Check.throwsType(NullPointerException.class, () -> new GameController(restaurant(), null), "null io");
    }

    private static void callOrderPerTurn() {
        TestGameIO io = allWait();
        new GameController(restaurant(), io).run();
        List<String> perTurn = List.of("showTurnStart", "chooseWaiterAction", "showActionState",
                "chooseChefOrder", "showActionState", "showTurnEnd");
        List<String> calls = io.calls();
        Check.equal(18 * perTurn.size() + 1, calls.size(), "call count");
        for (int turn = 0; turn < 18; turn++) {
            Check.equal(perTurn, calls.subList(turn * 6, turn * 6 + 6), "calls on turn " + (turn + 1));
        }
        Check.equal("showFinalResult", calls.get(calls.size() - 1), "final call");
        Check.isTrue(io.allChoicesUsed(), "exactly one choice per role per turn");
        Check.equal(18, io.waiterRequests(), "waiter requests");
        Check.equal(18, io.chefRequests(), "chef requests");
        Check.isTrue(io.interruptions().isEmpty(), "not interrupted");
    }

    private static void arrivalsEveryThreeTurns() {
        TestGameIO io = allWait();
        Restaurant r = restaurant();
        new GameController(r, io).run();
        Check.equal(6, r.getCustomers().size(), "six arrivals in 18 turns");
        int[] arrivalTurns = {1, 4, 7, 10, 13, 16};
        for (int i = 0; i < arrivalTurns.length; i++) {
            int turn = arrivalTurns[i];
            int id = i + 1;
            GameView.TurnState start = io.turnStarts().get(turn - 1);
            Check.isTrue(start.waiting().stream().anyMatch(c -> c.id() == id && c.satisfaction() == 100),
                    "C" + (i + 1) + " arrives at 100 on turn " + turn);
            if (turn > 1) {
                Check.isTrue(io.turnStarts().get(turn - 2).waiting().stream().noneMatch(c -> c.id() == id),
                        "C" + (i + 1) + " not present before turn " + turn);
            }
        }
        for (GameView.TurnState s : io.turnStarts()) {
            Check.equal(1, s.stage(), "stage 1");
            Check.equal(18, s.maxTurns(), "max turns");
        }
    }

    private static void allWaitDefeatReport() {
        TestGameIO io = allWait();
        new GameController(restaurant(), io).run();
        Check.equal(1, io.finalReports().size(), "one final report");
        GameView.FinalReport report = io.finalReports().get(0);
        Check.isTrue(!report.victory(), "all-wait is a defeat");
        Check.equal(18, report.totals().turn(), "totals at turn 18");
        Check.equal(List.of(
                new GameView.TargetLine("Paid customers", "4", "0", false),
                new GameView.TargetLine("Revenue", "$45.00", "$0.00", false),
                new GameView.TargetLine("Average paid satisfaction", "60", "N/A", false)),
                report.targets(), "defeat targets");
        for (int t = 0; t < 18; t++) {
            Check.equal("no eligible payment this turn", io.actionStates().get(t * 2 + 1).cashierStatus(),
                    "no payment on turn " + (t + 1));
        }
    }

    private static void victoryReportAndAutomaticPayment() {
        TestGameIO io = victoryScript();
        Restaurant r = restaurant();
        new GameController(r, io).run();
        Check.isTrue(io.invalidMessages().isEmpty(), "reference schedule has no refusals: " + io.invalidMessages());
        GameView.FinalReport report = io.finalReports().get(0);
        Check.isTrue(report.victory(), "victory");
        Check.equal(List.of(
                new GameView.TargetLine("Paid customers", "4", "5", true),
                new GameView.TargetLine("Revenue", "$45.00", "$66.00", true),
                new GameView.TargetLine("Average paid satisfaction", "60", "79.2", true)),
                report.targets(), "victory targets");
        Check.equal(5, report.totals().servedCount(), "served");
        Check.equal(0, report.totals().departures(), "no departures");

        // C1: served turn 3, ready turn 4, pays turn 5.
        Check.equal("no eligible payment this turn", endState(io, 4).cashierStatus(), "not on ready turn");
        Check.equal("collected $15.00 from C1; customer and order PAID, table released",
                endState(io, 5).cashierStatus(), "pays the turn after ready");
        Check.equal(1, io.turnEnds().get(4).paidCount(), "paid count after turn 5");
        Check.equal(1500, io.turnEnds().get(4).revenueCents(), "revenue after turn 5");
        Check.isTrue(endState(io, 5).tables().get(0).occupant() == null, "C1's table 1 released");
        Check.equal(2, endState(io, 5).tables().get(1).occupant().id(), "C2 still at table 2");
        Check.equal(0, io.turnEnds().get(3).paidCount(), "nobody paid through turn 4");

        Check.equal(CustomerStatus.WAITING_FOR_FOOD, r.findCustomer(6).getStatus(), "C6 still waiting at the end");
        Check.equal(OrderStatus.PREPARING, r.findOrder(6).getStatus(), "C6 Pasta half cooked at the end");
        Check.equal(1, r.findOrder(6).getCompletedPreparationUnits(), "C6 Pasta 1 of 2 units");
    }

    private static void turnStateListsFollowAvailability() {
        TestGameIO io = new TestGameIO()
                .waiter(WaiterAction.seat(1), WaiterAction.order(1), WAIT)
                .chef(0, 0, 1);
        new GameController(restaurant(), io).run();
        GameView.TurnState start1 = io.turnStarts().get(0);
        Check.equal(List.of(1), start1.seatableCustomerIds(), "C1 seatable on turn 1");
        Check.equal(List.of(), start1.orderableCustomerIds(), "nothing orderable");
        Check.equal(List.of(), start1.preparableOrderIds(), "nothing to cook");
        GameView.TurnState afterWaiter1 = io.actionStates().get(0);
        Check.equal(List.of(), afterWaiter1.seatableCustomerIds(), "Waiter used: nothing seatable");
        Check.equal(List.of(), afterWaiter1.waiting(), "queue empty after seating");
        Check.equal(1, afterWaiter1.tables().get(0).occupant().id(), "C1 at table 1");
        Check.equal("SEATED", afterWaiter1.tables().get(0).occupant().status(), "C1 seated");

        GameView.TurnState start2 = io.turnStarts().get(1);
        Check.equal(92, start2.tables().get(0).occupant().satisfaction(), "C1 lost 8 on turn 1");
        Check.equal(List.of(1), start2.orderableCustomerIds(), "C1 orderable on turn 2");
        GameView.TurnState afterWaiter2 = io.actionStates().get(2);
        Check.equal(List.of(1), afterWaiter2.preparableOrderIds(), "Chef still free after Waiter orders");
        Check.equal(List.of(), afterWaiter2.orderableCustomerIds(), "Waiter used");
        Check.equal(List.of(new GameView.OrderLine(1, "Burger", 0, 1, "PLACED", 1500)), afterWaiter2.orders(), "order line");

        GameView.TurnState end3 = endState(io, 3);
        Check.equal(List.of(), end3.preparableOrderIds(), "end-of-turn state offers no Chef work");
        Check.equal(List.of(), end3.servableOrderIds(), "ready this turn: not servable yet");
        Check.equal(List.of(new GameView.OrderLine(1, "Burger", 1, 1, "READY", 1500)), end3.orders(), "ready line");
        Check.equal(List.of(1), io.turnStarts().get(3).servableOrderIds(), "servable the turn after it is ready");
    }

    private static void waiterSemanticRetries() {
        TestGameIO io = new TestGameIO()
                .waiter(WaiterAction.seat(2), WaiterAction.order(1), WaiterAction.serve(1), WaiterAction.order(5),
                        WaiterAction.seat(1))
                .chef(0);
        Restaurant r = restaurant();
        new GameController(r, io).run();
        List<String> messages = io.invalidMessages();
        Check.equal(4, messages.size(), "four refusals before success");
        Check.equal("Customer 2 has not arrived", messages.get(0), "unknown customer");
        Check.contains(messages.get(1), "must be SEATED", "order before seating");
        Check.equal("Order 1 does not exist", messages.get(2), "serve missing order");
        Check.equal("Customer 5 has not arrived", messages.get(3), "order unknown customer");
        Check.equal(6, io.waiterRequests(), "5 asks on turn 1 until valid, then 1 on turn 2 that hits end of input");
        Check.equal(1, io.turnEnds().size(), "turn 1 still completes after retries");
        Check.equal(CustomerStatus.SEATED, r.findCustomer(1).getStatus(), "final seat applied");
        Check.isTrue(r.getWaiter().getTaskTurn() == 1, "Waiter used exactly once on turn 1");
    }

    private static void nullWaiterActionIsRetried() {
        WaiterAction[] script = {null, WAIT};
        int[] next = {0};
        RecordingIO io = new RecordingIO() {
            @Override
            public WaiterAction chooseWaiterAction() {
                if (next[0] >= script.length) {
                    throw new restaurantrush.control.InputEndedException("done");
                }
                return script[next[0]++];
            }
        };
        new GameController(restaurant(), io).run();
        Check.equal(List.of("Choose a Waiter action or Wait"), io.invalid, "null action refused with a reason");
        Check.equal("Input ended during turn 2, Waiter choice. Game interrupted.", io.interrupted, "then turn 2");
    }

    private static void chefSemanticRetries() {
        TestGameIO io = new TestGameIO()
                .waiter(WaiterAction.seat(1), WaiterAction.order(1), WAIT)
                .chef(4, 0, 1, 1, 0);
        Restaurant r = restaurant();
        new GameController(r, io).run();
        List<String> messages = io.invalidMessages();
        Check.equal(2, messages.size(), "two Chef refusals");
        Check.equal("Order 4 does not exist; use 'wait' to Wait", messages.get(0), "missing order");
        Check.contains(messages.get(1), "is not awaiting preparation", "cannot cook a READY order");
        Check.equal(5, io.chefRequests(), "Chef re-asked after each refusal");
        Check.equal(OrderStatus.READY, r.findOrder(1).getStatus(), "cooked once on turn 2");
        Check.equal(2, r.findOrder(1).getReadyTurn(), "ready on turn 2");
    }

    private static void noFreeTableHidesSeating() {
        TestGameIO io = new TestGameIO()
                .waiter(WaiterAction.seat(1), WAIT, WAIT, WaiterAction.seat(2), WAIT, WAIT,
                        WaiterAction.seat(3), WAIT)
                .chefRepeated(0, 7);
        Restaurant r = restaurant();
        new GameController(r, io).run();
        GameView.TurnState start7 = io.turnStarts().get(6);
        Check.isTrue(start7.waiting().stream().anyMatch(c -> c.id() == 3), "C3 waiting on turn 7");
        Check.equal(List.of(), start7.seatableCustomerIds(), "full restaurant: no seat option");
        Check.equal(List.of("No free table"), io.invalidMessages(), "seat refused with a reason");
        Check.equal(CustomerStatus.WAITING, r.findCustomer(3).getStatus(), "C3 still queued");
        Check.isTrue(io.allChoicesUsed(), "Wait accepted after the refusal");
    }

    private static void departureFreesTableDuringGame() {
        TestGameIO io = new TestGameIO().waiter(WaiterAction.seat(1)).waiterRepeated(WAIT, 17).chefRepeated(0, 18);
        Restaurant r = restaurant();
        new GameController(r, io).run();
        Check.equal(0, io.turnEnds().get(11).departures(), "C1 still here after turn 12");
        Check.equal(1, io.turnEnds().get(12).departures(), "C1 leaves on turn 13");
        Check.isTrue(endState(io, 13).tables().stream().allMatch(t -> t.occupant() == null), "table freed on departure");
        Check.isTrue(!io.turnStarts().get(13).seatableCustomerIds().isEmpty(), "freed table can be used on turn 14");
        Check.equal(CustomerStatus.LEFT, r.findCustomer(1).getStatus(), "C1 LEFT");
    }

    private static void interruptionNamesTurnAndRole() {
        TestGameIO empty = new TestGameIO();
        new GameController(restaurant(), empty).run();
        Check.equal(List.of("Input ended during turn 1, Waiter choice. Game interrupted."), empty.interruptions(), "waiter");
        Check.isTrue(empty.finalReports().isEmpty() && empty.turnEnds().isEmpty(), "no result or turn end");
        Check.equal(List.of("showTurnStart", "chooseWaiterAction", "showInterrupted"), empty.calls(), "stops immediately");

        TestGameIO chefMissing = new TestGameIO().waiter(WAIT, WAIT).chef(0);
        new GameController(restaurant(), chefMissing).run();
        Check.equal(List.of("Input ended during turn 2, Chef choice. Game interrupted."), chefMissing.interruptions(), "chef");
        Check.equal(1, chefMissing.turnEnds().size(), "turn 1 completed before interruption");
        Check.isTrue(chefMissing.finalReports().isEmpty(), "no final result");
    }

    // ---- helpers ----

    private static Restaurant restaurant() {
        return new Restaurant(new Menu(), new Waiter(), new Chef(), new Cashier());
    }

    private static TestGameIO allWait() {
        return new TestGameIO().waiterRepeated(WAIT, 18).chefRepeated(0, 18);
    }

    /** End-of-turn state (second showActionState) for a 1-based turn. */
    private static GameView.TurnState endState(TestGameIO io, int turn) {
        return io.actionStates().get(turn * 2 - 1);
    }

    /** The reference victory schedule from test-fixtures/README.md. */
    private static TestGameIO victoryScript() {
        return new TestGameIO()
                .waiter(WaiterAction.seat(1), WaiterAction.order(1), WaiterAction.serve(1),
                        WaiterAction.seat(2), WaiterAction.order(2), WaiterAction.serve(2),
                        WaiterAction.seat(3), WaiterAction.order(3), WAIT, WaiterAction.serve(3),
                        WaiterAction.seat(4), WaiterAction.order(4), WaiterAction.serve(4),
                        WaiterAction.seat(5), WaiterAction.order(5), WaiterAction.serve(5),
                        WaiterAction.seat(6), WaiterAction.order(6))
                .chef(0, 1, 0, 0, 2, 0, 0, 3, 3, 0, 0, 4, 0, 0, 5, 0, 0, 6);
    }

    /** Minimal GameIO for cases TestGameIO cannot script (e.g. a null action). Chef always waits. */
    private abstract static class RecordingIO implements GameIO {
        final java.util.List<String> invalid = new java.util.ArrayList<>();
        String interrupted;

        @Override
        public int chooseChefOrder() {
            return 0;
        }

        @Override
        public void showInvalidAction(String message) {
            invalid.add(message);
        }

        @Override
        public void showInterrupted(String message) {
            interrupted = message;
        }
    }
}
