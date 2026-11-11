package restaurantrush;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

import restaurantrush.control.GameIO;
import restaurantrush.control.GameView;
import restaurantrush.control.InputEndedException;
import restaurantrush.control.WaiterAction;

/**
 * GameIO for TurnTests: answers from preset Waiter and Chef choices, records
 * every call in order, and throws InputEndedException when a list runs out,
 * the same as real EOF. Never falls back to Wait.
 */
public final class TestGameIO implements GameIO {
    private final Deque<WaiterAction> waiterChoices = new ArrayDeque<>();
    private final Deque<Integer> chefChoices = new ArrayDeque<>();
    private final List<String> calls = new ArrayList<>();
    private final List<String> invalidMessages = new ArrayList<>();
    private final List<GameView.TurnState> turnStarts = new ArrayList<>();
    private final List<GameView.TurnState> actionStates = new ArrayList<>();
    private final List<GameView.TurnSummary> turnEnds = new ArrayList<>();
    private final List<GameView.FinalReport> finalReports = new ArrayList<>();
    private final List<String> interruptions = new ArrayList<>();
    private int waiterRequests;
    private int chefRequests;

    public TestGameIO waiter(WaiterAction... actions) {
        for (WaiterAction action : actions) {
            waiterChoices.addLast(action);
        }
        return this;
    }

    /** Adds the same Waiter action several times, e.g. Wait for every turn. */
    public TestGameIO waiterRepeated(WaiterAction action, int times) {
        for (int i = 0; i < times; i++) {
            waiterChoices.addLast(action);
        }
        return this;
    }

    /** Order IDs for the Chef; 0 means Wait. */
    public TestGameIO chef(int... orderIds) {
        for (int orderId : orderIds) {
            chefChoices.addLast(orderId);
        }
        return this;
    }

    public TestGameIO chefRepeated(int orderId, int times) {
        for (int i = 0; i < times; i++) {
            chefChoices.addLast(orderId);
        }
        return this;
    }

    @Override
    public WaiterAction chooseWaiterAction() {
        waiterRequests++;
        calls.add("chooseWaiterAction");
        if (waiterChoices.isEmpty()) {
            throw new InputEndedException("Scripted input ended at a Waiter choice");
        }
        return waiterChoices.removeFirst();
    }

    @Override
    public int chooseChefOrder() {
        chefRequests++;
        calls.add("chooseChefOrder");
        if (chefChoices.isEmpty()) {
            throw new InputEndedException("Scripted input ended at a Chef choice");
        }
        return chefChoices.removeFirst();
    }

    @Override
    public void showInvalidAction(String message) {
        calls.add("showInvalidAction");
        invalidMessages.add(message);
    }

    @Override
    public void showTurnStart(GameView.TurnState state) {
        calls.add("showTurnStart");
        turnStarts.add(state);
    }

    @Override
    public void showActionState(GameView.TurnState state) {
        calls.add("showActionState");
        actionStates.add(state);
    }

    @Override
    public void showTurnEnd(GameView.TurnSummary summary) {
        calls.add("showTurnEnd");
        turnEnds.add(summary);
    }

    @Override
    public void showFinalResult(GameView.FinalReport report) {
        calls.add("showFinalResult");
        finalReports.add(report);
    }

    @Override
    public void showInterrupted(String message) {
        calls.add("showInterrupted");
        interruptions.add(message);
    }

    public List<GameView.TurnState> turnStarts() {
        return Collections.unmodifiableList(turnStarts);
    }

    public List<GameView.TurnState> actionStates() {
        return Collections.unmodifiableList(actionStates);
    }

    public List<GameView.TurnSummary> turnEnds() {
        return Collections.unmodifiableList(turnEnds);
    }

    public List<GameView.FinalReport> finalReports() {
        return Collections.unmodifiableList(finalReports);
    }

    public List<String> interruptions() {
        return Collections.unmodifiableList(interruptions);
    }

    /** Every GameIO call in the order it happened. */
    public List<String> calls() {
        return Collections.unmodifiableList(calls);
    }

    public List<String> invalidMessages() {
        return Collections.unmodifiableList(invalidMessages);
    }

    public int waiterRequests() {
        return waiterRequests;
    }

    public int chefRequests() {
        return chefRequests;
    }

    /** True when every scripted choice was used, so no planned step was skipped. */
    public boolean allChoicesUsed() {
        return waiterChoices.isEmpty() && chefChoices.isEmpty();
    }
}
