package restaurantrush;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

import restaurantrush.boundary.ConsoleUI;
import restaurantrush.control.GameIO;
import restaurantrush.control.GameView;
import restaurantrush.control.InputEndedException;
import restaurantrush.control.WaiterAction;

/**
 * Reproducible GameIO for full replays (ReplayTests); Victor's TestGameIO
 * serves TurnTests. Unlike TestGameIO it keeps role choices in one
 * ordered script, so it also catches a role asked out of order.
 *
 * Choices are an ordered script of role-tagged steps. A request for the wrong
 * role, or a request after the script is used up, fails immediately: a silent
 * Wait would hide a broken turn loop or an unexpected retry. Every display
 * call is recorded so tests can assert what the player would have seen.
 */
public class ScriptedGameIO implements GameIO {

    public enum Role { HAPPY_HOUR, HOST, WAITER, CHEF }

    private record Step(Role role, WaiterAction waiter, int chef, boolean activate) {
    }

    private final Deque<Step> script = new ArrayDeque<>();
    private int requestCount;
    private boolean explicitStage3;
    public record HappyHourState(int turn, int remaining, boolean used) { }
    private final List<HappyHourState> happyHourStates = new ArrayList<>();
    private final List<GameView.TurnState> actionStates = new ArrayList<>();
    private final List<String> interruptions = new ArrayList<>();

    private final List<GameView.TurnState> turnStates = new ArrayList<>();
    private final List<String> invalidReasons = new ArrayList<>();
    private final List<String> updates = new ArrayList<>();
    private final List<GameView.TurnSummary> turnSummaries = new ArrayList<>();
    private GameView.FinalReport finalReport;

    // ---- building the script ---------------------------------------------

    public ScriptedGameIO waiter(WaiterAction action) {
        script.addLast(new Step(Role.WAITER, action, 0, false));
        return this;
    }

    /** {@code orderId} 0 means Wait. */
    public ScriptedGameIO chef(int orderId) {
        if (orderId < 0) {
            throw new IllegalArgumentException("Chef choice must be an order ID or 0");
        }
        script.addLast(new Step(Role.CHEF, null, orderId, false));
        return this;
    }

    /** Enable strict Stage 3 role order, including conditional activation requests. */
    public ScriptedGameIO happyHour(boolean activate) {
        explicitStage3 = true;
        script.addLast(new Step(Role.HAPPY_HOUR, null, 0, activate));
        return this;
    }

    public ScriptedGameIO host(int customerId) {
        if (customerId < 0) throw new IllegalArgumentException("Host choice must be an ID or 0");
        explicitStage3 = true;
        script.addLast(new Step(Role.HOST, null, customerId, false));
        return this;
    }

    /** Parse a complete retry-free 18-turn production fixture. */
    public static ScriptedGameIO stage3(List<String> lines) {
        ScriptedGameIO io = new ScriptedGameIO();
        boolean used = false;
        int line = 0;
        try {
            for (int turn = 1; turn <= 18; turn++) {
                if (!used) {
                    boolean activate = ConsoleUI.parseHappyHourActivation(lines.get(line++));
                    io.happyHour(activate);
                    used = activate;
                }
                io.host(ConsoleUI.parseHostChoice(lines.get(line++)));
                io.waiter(ConsoleUI.parseWaiterAction(lines.get(line++)));
                io.chef(ConsoleUI.parseChefChoice(lines.get(line++)));
            }
            if (line != lines.size()) throw new IllegalArgumentException("unused lines after turn 18");
        } catch (IndexOutOfBoundsException | IllegalArgumentException e) {
            throw new IllegalArgumentException("Stage 3 fixture near line " + line + ": " + e.getMessage(), e);
        }
        return io;
    }

    /** One turn without retries: a Waiter choice then a Chef choice. */
    public ScriptedGameIO turn(WaiterAction waiterAction, int chefOrderId) {
        return waiter(waiterAction).chef(chefOrderId);
    }

    /**
     * Builds a script from console fixture lines that contain no retries:
     * even lines are Waiter commands, odd lines Chef commands, parsed with the
     * same grammar as ConsoleUI.
     */
    public static ScriptedGameIO alternating(List<String> lines) {
        if (lines.size() % 2 != 0) {
            throw new IllegalArgumentException("Alternating fixture requires complete Waiter/Chef pairs");
        }
        ScriptedGameIO io = new ScriptedGameIO();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            try {
                if (i % 2 == 0) {
                    io.waiter(ConsoleUI.parseWaiterAction(line));
                } else {
                    io.chef(ConsoleUI.parseChefChoice(line));
                }
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Fixture line " + (i + 1) + " '" + line + "': "
                        + e.getMessage(), e);
            }
        }
        return io;
    }

    // ---- GameIO requests -------------------------------------------------

    @Override
    public boolean chooseHappyHourActivation() {
        return explicitStage3 ? next(Role.HAPPY_HOUR).activate() : false;
    }

    @Override
    public int chooseHostCustomer() {
        return explicitStage3 ? next(Role.HOST).chef() : 0;
    }

    @Override
    public void showHappyHour(int turn, int remaining, boolean used) {
        happyHourStates.add(new HappyHourState(turn, remaining, used));
    }

    public List<HappyHourState> happyHourStates() { return List.copyOf(happyHourStates); }

    @Override
    public WaiterAction chooseWaiterAction() {
        return next(Role.WAITER).waiter();
    }

    @Override
    public int chooseChefOrder() {
        return next(Role.CHEF).chef();
    }

    private Step next(Role requested) {
        requestCount++;
        Step step = script.pollFirst();
        if (step == null) {
            throw new InputEndedException("Script exhausted: request #" + requestCount
                    + " asked for " + requested + " after " + turnStates.size() + " turn(s) started");
        }
        if (step.role() != requested) {
            throw new IllegalStateException("Request #" + requestCount + " asked for " + requested
                    + " but the script's next step is " + step.role());
        }
        return step;
    }

    // ---- displays (recorded) ---------------------------------------------

    @Override
    public void showTurnStart(GameView.TurnState state) {
        turnStates.add(state);
    }

    @Override
    public void showInvalidAction(String reason) {
        invalidReasons.add(reason);
    }

    @Override
    public void showActionState(GameView.TurnState state) { actionStates.add(state); }

    @Override
    public void showInterrupted(String message) { interruptions.add(message); }

    public List<GameView.TurnState> actionStates() { return List.copyOf(actionStates); }
    public List<String> interruptions() { return List.copyOf(interruptions); }

    public void showUpdate(String message) {
        updates.add(message);
    }

    @Override
    public void showTurnEnd(GameView.TurnSummary summary) {
        turnSummaries.add(summary);
    }

    @Override
    public void showFinalResult(GameView.FinalReport report) {
        if (finalReport != null) {
            throw new IllegalStateException("Final result shown twice");
        }
        finalReport = report;
    }

    // ---- observations ----------------------------------------------------

    public int remainingSteps() {
        return script.size();
    }

    /** Fails unless the game ended with every scripted choice consumed. */
    public void requireFinished() {
        if (finalReport == null) {
            throw new AssertionError("Game ended without showing a final result");
        }
        if (!script.isEmpty()) {
            throw new AssertionError(script.size() + " scripted choice(s) were never requested");
        }
    }

    public List<GameView.TurnState> turnStates() {
        return Collections.unmodifiableList(turnStates);
    }

    public List<String> invalidReasons() {
        return Collections.unmodifiableList(invalidReasons);
    }

    public List<String> updates() {
        return Collections.unmodifiableList(updates);
    }

    public List<GameView.TurnSummary> turnSummaries() {
        return Collections.unmodifiableList(turnSummaries);
    }

    public GameView.FinalReport finalReport() {
        return finalReport;
    }
}
