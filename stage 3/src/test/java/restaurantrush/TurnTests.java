package restaurantrush;

import java.util.ArrayList;
import java.util.List;

import restaurantrush.control.GameController;
import restaurantrush.control.GameIO;
import restaurantrush.control.GameView;
import restaurantrush.control.WaiterAction;
import restaurantrush.domain.Cashier;
import restaurantrush.domain.Chef;
import restaurantrush.domain.Customer;
import restaurantrush.domain.CustomerStatus;
import restaurantrush.domain.Menu;
import restaurantrush.domain.OrderStatus;
import restaurantrush.domain.Restaurant;
import restaurantrush.domain.Waiter;

/** Controller integration checks using real guarded domain operations, without status setters. */
public final class TurnTests {
    private static int passed;

    private TurnTests() { }

    public static void main(String[] args) {
        runAll();
    }

    public static void runAll() {
        passed = 0;
        arrivalsPhasesAndAllWaitDefeat();
        orderRefreshAndBurgerPaymentTiming();
        waitLeavesPreparationUntouched();
        semanticRetriesStayInTheirRole();
        chefRecoveryAdviceUsesParserGrammar();
        prematureServeRetriesBeforeChef();
        pastaKeepsItsSecondUnitAndEatingDelays();
        tableCannotBeReusedUntilAfterPayment();
        abandonmentCancelsAndReleasesBeforeNextTurn();
        victoryStillCompletesTurn18();
        seatingPriorityDelaysPastaPayment();
        waiterEofStopsBeforeAutomaticPhases();
        chefEofPreservesSuccessfulWaiterAction();
        eofAfterSemanticErrorStopsRetry();
        initialEofStillShowsArrival();
        nullWaiterActionGetsReadableRetry();
        unexpectedFailuresEscape();
        displaySnapshotsStayImmutable();
        System.out.println("TurnTests: " + passed + " passed");
    }

    private static Restaurant restaurant() {
        return new Restaurant(new Menu(false), new Waiter(), new Chef(), new Cashier());
    }

    private static TestGameIO waits(int turns) {
        return new TestGameIO().waiterRepeated(WaiterAction.waitAction(), turns).chefRepeated(0, turns);
    }

    private static void run(Restaurant restaurant, GameIO io) {
        new GameController(restaurant, io).run();
    }

    // T-01..03, T-06, T-16..17, T-22: complete trace, not just separate request counts.
    private static void arrivalsPhasesAndAllWaitDefeat() {
        Restaurant r = restaurant();
        TestGameIO io = waits(18);
        run(r, io);
        check(io.waiterRequests() == 18 && io.chefRequests() == 18 && io.allChoicesUsed(), "18 role pairs");
        List<String> expectedCalls = new ArrayList<>();
        for (int turn = 1; turn <= 18; turn++) {
            expectedCalls.addAll(List.of("showHappyHour", "chooseHappyHourActivation", "showHappyHour",
                    "showTurnStart", "chooseHostCustomer", "showActionState", "chooseWaiterAction", "showActionState",
                    "chooseChefOrder", "showActionState", "showTurnEnd"));
            GameView.TurnState start = io.turnStarts().get(turn - 1);
            check(start.turn() == turn && start.maxTurns() == 18 && start.stage() == 1, "turn header");
            if ((turn - 1) % 3 == 0) {
                int id = (turn - 1) / 3 + 1;
                GameView.CustomerLine arrival = start.waiting().stream().filter(c -> c.id() == id)
                        .findFirst().orElseThrow(() -> new AssertionError("missing arrival C" + id));
                check(arrival.satisfaction() == 100, "arrival shown before loss");
            }
            List<Integer> expectedWaiting = new ArrayList<>();
            for (int id = 1; id <= 6; id++) {
                int arrivalTurn = 3 * id - 2;
                if (arrivalTurn <= turn && turn < arrivalTurn + 13) {
                    expectedWaiting.add(id);
                }
            }
            check(start.waiting().stream().map(GameView.CustomerLine::id).toList().equals(expectedWaiting),
                    "exact queue at start of turn " + turn);
        }
        expectedCalls.add("showFinalResult");
        check(io.calls().equals(expectedCalls), "exact phase/display order for every turn and final result");
        check(r.getCustomers().size() == 6 && r.getUnhappyDepartureCount() == 2, "six arrivals, two departures");
        check(r.findCustomer(1).getStatus() == CustomerStatus.LEFT && r.findCustomer(3).getSatisfaction() == 4,
                "all-Wait loss and departure timing");
        check(r.getRevenueCents() == 0 && r.getServedCount() == 0 && r.getPaidCount() == 0, "no unpaid revenue");
        check(r.getAveragePaidSatisfaction().isEmpty(), "absent paid average");
        check(io.turnEnds().get(12).departures() == 1 && io.turnEnds().get(15).departures() == 2,
                "departures on turns 13 and 16");
        check(io.finalReports().size() == 1 && !io.finalReports().get(0).victory(), "one final defeat");
        check(io.invalidMessages().isEmpty() && io.interruptions().isEmpty(), "Wait is always a valid choice");
        passed++;
    }

    // T-04, T-12, T-16: a newly placed order is visible/preparable before Chef input.
    private static void orderRefreshAndBurgerPaymentTiming() {
        Restaurant r = restaurant();
        TestGameIO io = new TestGameIO().waiter(WaiterAction.seat(1), WaiterAction.order(1),
                WaiterAction.serve(1)).waiterRepeated(WaiterAction.waitAction(), 2).chef(0, 1, 0, 0, 0);
        run(r, io);
        GameView.TurnState afterOrder = io.actionStates().get(4);
        check(afterOrder.turn() == 2 && afterOrder.preparableOrderIds().equals(List.of(1)), "Chef sees new order");
        check(afterOrder.orders().get(0).item().equals("Burger") && afterOrder.orders().get(0).billCents() == 1500,
                "customer deterministic item and full locked price");
        check(afterOrder.orderableCustomerIds().isEmpty() && afterOrder.seatableCustomerIds().isEmpty(),
                "Waiter phase finished");
        check(io.turnStarts().get(2).servableOrderIds().equals(List.of(1)), "ready order offered next turn");
        check(io.turnEnds().get(2).servedCount() == 1 && io.turnEnds().get(2).paidCount() == 0, "serve turn no payment");
        check(io.turnStarts().get(3).tables().get(0).occupant().status().equals("SERVED"), "no same-turn eating");
        check(endState(io, 4).tables().get(0).occupant().status().equals("READY_TO_PAY"), "eating next turn");
        check(io.turnEnds().get(3).revenueCents() == 0 && io.turnEnds().get(4).revenueCents() == 1500,
                "payment earliest turn 5");
        Customer c = r.findCustomer(1);
        check(c.getServedTurn() == 3 && c.getReadyToPayTurn() == 4 && c.getSatisfactionAtPayment() == 84,
                "Burger timestamps and frozen satisfaction");
        GameView.TurnState paid = endState(io, 5);
        check(paid.tables().get(0).occupant() == null && paid.orders().get(0).status().equals("PAID")
                && paid.cashierStatus().contains("C1") && paid.cashierStatus().contains("PAID"),
                "payment outcome and table release displayed");
        check(io.turnStarts().get(0).tables().get(0).occupant() == null, "earlier snapshot retained");
        passed++;
    }

    // T-05: both role Wait choices complete a phase without consuming a domain task.
    private static void waitLeavesPreparationUntouched() {
        Restaurant r = restaurant();
        TestGameIO io = new TestGameIO().waiter(WaiterAction.seat(1), WaiterAction.order(1)).chef(0, 0);
        run(r, io);
        check(r.findOrder(1).getCompletedPreparationUnits() == 0 && r.findOrder(1).getStatus() == OrderStatus.PLACED,
                "Chef Wait leaves order placed");
        check(io.invalidMessages().isEmpty(), "Wait needs no retry");
        passed++;
    }

    // T-07..10: bad targets/states/negative Chef IDs all retry before the next phase.
    private static void semanticRetriesStayInTheirRole() {
        Restaurant r = restaurant();
        TestGameIO io = new TestGameIO().waiter(WaiterAction.seat(99), WaiterAction.order(1),
                WaiterAction.serve(99), WaiterAction.seat(1), WaiterAction.order(1)).chef(0, -1, 99, 1);
        run(r, io);
        check(io.invalidMessages().size() == 5 && io.invalidMessages().stream().allMatch(s -> s != null && !s.isBlank()),
                "one readable reason per semantic failure");
        check(io.calls().subList(0, 16).equals(List.of("showHappyHour", "chooseHappyHourActivation", "showHappyHour",
                "showTurnStart", "chooseHostCustomer", "showActionState", "chooseWaiterAction", "showInvalidAction",
                "chooseWaiterAction", "showInvalidAction", "chooseWaiterAction", "showInvalidAction",
                "chooseWaiterAction", "showActionState", "chooseChefOrder", "showActionState")),
                "Waiter retries precede Chef");
        check(r.findOrder(1).getReadyTurn() == 2 && r.findOrder(1).getCompletedPreparationUnits() == 1,
                "invalid choices preserved both staff tasks");
        check(r.getWaitingQueue().isEmpty() && r.findTableFor(r.findCustomer(1)) != null, "aggregate seated cleanup");
        passed++;
    }

    private static void chefRecoveryAdviceUsesParserGrammar() {
        TestGameIO io = new TestGameIO().waiter(WaiterAction.waitAction()).chef(999, 0);
        run(restaurant(), io);
        String reason = io.invalidMessages().get(0);
        check(reason.contains("'wait'") && !reason.contains("use 0"), "Chef advice names the text command");
        check(restaurantrush.boundary.ConsoleUI.parseChefChoice("wait") == 0, "suggested command parses as Wait");
        check(io.chefRequests() == 2 && io.turnEnds().size() == 1, "retry stays in Chef phase until Wait");
        passed++;
    }

    // T-11: an unprepared order cannot be served in the earlier Waiter phase.
    private static void prematureServeRetriesBeforeChef() {
        Restaurant r = restaurant();
        TestGameIO io = new TestGameIO().waiter(WaiterAction.seat(1), WaiterAction.order(1),
                WaiterAction.serve(1), WaiterAction.waitAction(), WaiterAction.serve(1)).chef(0, 0, 1, 0);
        run(r, io);
        check(io.invalidMessages().size() == 1, "premature serve refused once");
        check(r.findOrder(1).getReadyTurn() == 3 && r.findCustomer(1).getServedTurn() == 4,
                "Chef completion after Waiter can only be served next turn");
        check(r.findCustomer(1).getSatisfaction() == 76, "ready/unserved loses satisfaction, serving freezes it");
        passed++;
    }

    // T-13: default C3 Pasta needs two Chef phases and the two subsequent delays.
    private static void pastaKeepsItsSecondUnitAndEatingDelays() {
        Restaurant r = restaurant();
        TestGameIO io = waits(6).waiter(WaiterAction.seat(3), WaiterAction.order(3),
                WaiterAction.serve(3), WaiterAction.waitAction(), WaiterAction.serve(3))
                .waiterRepeated(WaiterAction.waitAction(), 2).chef(0, 3, 3, 0, 0, 0);
        run(r, io);
        check(io.invalidMessages().size() == 1, "Pasta not servable after first unit");
        check(endState(io, 8).orders().get(0).completedUnits() == 1
                && !endState(io, 8).orders().get(0).status().equals("READY"), "Pasta unit 1/2");
        Customer c = r.findCustomer(3);
        check(c.getOrder().getReadyTurn() == 9 && c.getServedTurn() == 10 && c.getReadyToPayTurn() == 11,
                "Pasta timestamps");
        check(io.turnEnds().get(10).paidCount() == 0 && io.turnEnds().get(11).revenueCents() == 1800,
                "Pasta payment turn 12");
        check(c.getSatisfactionAtPayment() == 76, "Pasta recorded satisfaction");
        passed++;
    }

    // T-15: full tables during choices even though a payment will free one later that turn.
    private static void tableCannotBeReusedUntilAfterPayment() {
        Restaurant r = restaurant();
        TestGameIO io = new TestGameIO().waiter(WaiterAction.seat(1), WaiterAction.order(1),
                WaiterAction.waitAction(), WaiterAction.seat(2), WaiterAction.waitAction(), WaiterAction.serve(1),
                WaiterAction.waitAction(), WaiterAction.seat(3), WaiterAction.waitAction(), WaiterAction.seat(3))
                .chef(0, 1).chefRepeated(0, 7);
        run(r, io);
        check(io.invalidMessages().size() == 1, "full-table seating rejected");
        check(io.turnStarts().get(7).seatableCustomerIds().isEmpty(), "no free table before payment");
        check(io.turnEnds().get(6).paidCount() == 0 && io.turnEnds().get(7).paidCount() == 1, "payment after choices on 8");
        check(io.turnStarts().get(8).seatableCustomerIds().contains(3) && r.findTableFor(r.findCustomer(3)) != null,
                "table reusable on 9");
        passed++;
    }

    // T-17..18: end-of-turn departure cleans an unfinished order/table, then both roles reject that order.
    private static void abandonmentCancelsAndReleasesBeforeNextTurn() {
        Restaurant r = restaurant();
        TestGameIO io = new TestGameIO().waiter(WaiterAction.seat(1), WaiterAction.order(1),
                WaiterAction.waitAction(), WaiterAction.seat(2))
                .waiterRepeated(WaiterAction.waitAction(), 9)
                .waiter(WaiterAction.seat(1), WaiterAction.serve(1), WaiterAction.seat(3))
                .chefRepeated(0, 13).chef(1, 0);
        run(r, io);
        check(r.findCustomer(1).getStatus() == CustomerStatus.LEFT && r.findOrder(1).getStatus() == OrderStatus.CANCELLED,
                "unfinished order cancelled at zero");
        check(endState(io, 13).tables().stream().anyMatch(t -> t.occupant() == null), "table released at departure");
        check(r.findTableFor(r.findCustomer(3)) != null, "next-turn seating succeeds after invalid actions");
        check(io.invalidMessages().size() == 3 && r.getRevenueCents() == 0, "LEFT/cancelled targets refused; no revenue");
        check(!io.turnStarts().get(13).preparableOrderIds().contains(1), "cancelled food not actionable");
        passed++;
    }

    // T-14, T-22: targets are already met on 15; keep asking through 18 and report only then.
    private static void victoryStillCompletesTurn18() {
        Restaurant r = restaurant();
        TestGameIO io = victoryChoices(true);
        run(r, io);
        check(io.turnEnds().get(14).paidCount() == 4 && io.turnEnds().get(14).revenueCents() == 5700,
                "targets met before final turn");
        check(io.waiterRequests() == 18 && io.chefRequests() == 18 && io.allChoicesUsed(), "no early end");
        int previousPaid = 0;
        for (GameView.TurnSummary end : io.turnEnds()) {
            check(end.paidCount() - previousPaid <= 1, "at most one automatic payer per turn");
            previousPaid = end.paidCount();
        }
        check(r.getPaidCount() == 5 && r.getRevenueCents() == 6600 && r.getPaidSatisfactionTotal() == 396,
                "reference victory totals");
        check(io.finalReports().size() == 1 && io.finalReports().get(0).victory()
                && io.finalReports().get(0).targets().stream().allMatch(GameView.TargetLine::met), "one final victory");
        check(io.calls().get(io.calls().size() - 2).equals("showTurnEnd")
                && io.calls().get(io.calls().size() - 1).equals("showFinalResult"), "final follows turn 18 end");
        passed++;
    }

    private static TestGameIO victoryChoices(boolean serveFirst) {
        return new TestGameIO().waiter(WaiterAction.seat(1), WaiterAction.order(1), WaiterAction.serve(1),
                WaiterAction.seat(2), WaiterAction.order(2), WaiterAction.serve(2), WaiterAction.seat(3),
                WaiterAction.order(3), WaiterAction.waitAction(),
                serveFirst ? WaiterAction.serve(3) : WaiterAction.seat(4),
                serveFirst ? WaiterAction.seat(4) : WaiterAction.serve(3),
                WaiterAction.order(4), WaiterAction.serve(4), WaiterAction.seat(5), WaiterAction.order(5),
                WaiterAction.serve(5), WaiterAction.seat(6), WaiterAction.order(6))
                .chef(0, 1, 0, 0, 2, 0, 0, 3, 3, 0, 0, 4, 0, 0, 5, 0, 0, 6);
    }

    private static void seatingPriorityDelaysPastaPayment() {
        Restaurant r = restaurant();
        TestGameIO io = victoryChoices(false);
        run(r, io);
        Customer c3 = r.findCustomer(3);
        check(c3.getOrder().getReadyTurn() == 9 && c3.getServedTurn() == 11 && c3.getReadyToPayTurn() == 12,
                "seating C4 first delays C3 service and eating");
        check(io.turnEnds().get(11).paidCount() == 2 && io.turnEnds().get(12).paidCount() == 3,
                "C3 payment moves from turn 12 to 13");
        check(c3.getSatisfactionAtPayment() == 68 && r.getPaidCount() == 5 && r.getRevenueCents() == 6600
                && r.getPaidSatisfactionTotal() == 388, "counterfactual freezes 68 and ends at average 77.6");
        check(io.allChoicesUsed() && io.invalidMessages().isEmpty() && io.finalReports().get(0).victory(),
                "counterfactual completes all turns and still wins");
        passed++;
    }

    // T-19: turn 5 EOF must not collect C1's now-eligible payment or apply loss to the arrival queue.
    private static void waiterEofStopsBeforeAutomaticPhases() {
        Restaurant r = restaurant();
        TestGameIO io = new TestGameIO().waiter(WaiterAction.seat(1), WaiterAction.order(1),
                WaiterAction.serve(1), WaiterAction.waitAction()).chef(0, 1, 0, 0);
        run(r, io);
        interrupted(io, 5, "Waiter", 4);
        check(r.getRevenueCents() == 0 && r.findCustomer(1).getStatus() == CustomerStatus.READY_TO_PAY,
                "EOF skips automatic payment");
        check(r.findCustomer(2).getSatisfaction() == 92 && io.chefRequests() == 4, "EOF skips loss and Chef");
        passed++;
    }

    // T-21: no whole-turn rollback; already completed Waiter work remains.
    private static void chefEofPreservesSuccessfulWaiterAction() {
        Restaurant r = restaurant();
        TestGameIO io = new TestGameIO().waiter(WaiterAction.seat(1));
        run(r, io);
        interrupted(io, 1, "Chef", 0);
        check(r.findCustomer(1).getStatus() == CustomerStatus.SEATED && r.findCustomer(1).getSatisfaction() == 100,
                "seat retained without turn-end loss");
        check(io.actionStates().size() == 2 && io.actionStates().get(1).tables().get(0).occupant() != null,
                "post-Waiter state displayed before EOF");
        passed++;
    }

    // T-20 plus EOF during a Chef retry.
    private static void eofAfterSemanticErrorStopsRetry() {
        Restaurant r = restaurant();
        TestGameIO waiterIo = waits(2).waiter(WaiterAction.serve(99));
        run(r, waiterIo);
        interrupted(waiterIo, 3, "Waiter", 2);
        check(waiterIo.invalidMessages().size() == 1 && waiterIo.chefRequests() == 2, "Waiter invalid then EOF");
        TestGameIO chefIo = new TestGameIO().waiter(WaiterAction.waitAction()).chef(99);
        run(restaurant(), chefIo);
        interrupted(chefIo, 1, "Chef", 0);
        check(chefIo.invalidMessages().size() == 1 && chefIo.chefRequests() == 2, "Chef invalid then EOF");
        passed++;
    }

    private static void initialEofStillShowsArrival() {
        Restaurant r = restaurant();
        TestGameIO io = new TestGameIO();
        run(r, io);
        interrupted(io, 1, "Waiter", 0);
        check(io.turnStarts().size() == 1 && r.findCustomer(1).getSatisfaction() == 100 && io.chefRequests() == 0,
                "initial EOF after arrival display, before mutations/loss");
        passed++;
    }

    private static void nullWaiterActionGetsReadableRetry() {
        TestGameIO capture = waits(1);
        GameIO io = new GameIO() {
            private boolean first = true;

            @Override public WaiterAction chooseWaiterAction() {
                if (first) {
                    first = false;
                    return null;
                }
                return capture.chooseWaiterAction();
            }

            @Override public int chooseChefOrder() { return capture.chooseChefOrder(); }
            @Override public void showInvalidAction(String message) { capture.showInvalidAction(message); }
            @Override public void showInterrupted(String message) { capture.showInterrupted(message); }
        };
        run(restaurant(), io);
        check(capture.invalidMessages().size() == 1 && !capture.invalidMessages().get(0).isBlank(), "null action explained");
        check(capture.chefRequests() == 1 && capture.interruptions().size() == 1, "null retry does not advance phase");
        passed++;
    }

    private static void unexpectedFailuresEscape() {
        GameIO io = new GameIO() {
            @Override public WaiterAction chooseWaiterAction() { throw new IllegalStateException("diagnostic failure"); }
            @Override public int chooseChefOrder() { throw new AssertionError("Chef must not run"); }
            @Override public void showInvalidAction(String message) { throw new AssertionError("must not be swallowed"); }
        };
        try {
            run(restaurant(), io);
            throw new AssertionError("unexpected failure swallowed");
        } catch (IllegalStateException expected) {
            check(expected.getMessage().equals("diagnostic failure"), "only EOF caught");
        }
        passed++;
    }

    private static void displaySnapshotsStayImmutable() {
        List<GameView.CustomerLine> waiting = new ArrayList<>();
        waiting.add(new GameView.CustomerLine(1, "Regular", 100, "WAITING"));
        GameView.TurnState state = new GameView.TurnState(1, 1, 18, 0, 0, java.util.OptionalDouble.empty(),
                waiting, List.of(), List.of(), List.of(1), List.of(), List.of(), List.of(), "available");
        waiting.clear();
        check(state.waiting().size() == 1, "snapshot copies mutable input");
        try {
            state.seatableCustomerIds().add(2);
            throw new AssertionError("snapshot ID list is mutable");
        } catch (UnsupportedOperationException expected) {
            // Expected: the boundary cannot mutate controller state.
        }
        passed++;
    }

    private static GameView.TurnState endState(TestGameIO io, int turn) {
        return io.actionStates().get(3 * turn - 1);
    }

    private static void interrupted(TestGameIO io, int turn, String role, int completedTurns) {
        check(io.interruptions().size() == 1 && io.interruptions().get(0).contains("turn " + turn + ", " + role),
                "one interruption naming current turn and role");
        check(io.turnEnds().size() == completedTurns && io.finalReports().isEmpty(), "no incomplete end/final result");
        check(io.calls().get(io.calls().size() - 1).equals("showInterrupted"), "interruption is last callback");
    }

    private static void check(boolean condition, String label) {
        if (!condition) {
            throw new AssertionError("TurnTests: " + label);
        }
    }
}
