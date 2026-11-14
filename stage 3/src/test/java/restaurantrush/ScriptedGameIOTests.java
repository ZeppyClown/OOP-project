package restaurantrush;

import java.util.List;
import java.util.OptionalDouble;

import restaurantrush.control.GameView;
import restaurantrush.control.InputEndedException;
import restaurantrush.control.WaiterAction;

/** Makes sure the replay harness fails loudly when misused. */
public final class ScriptedGameIOTests {

    private ScriptedGameIOTests() {
    }

    public static void register(AllTests.Registry r) {
        r.add("Scripted: returns choices in order", ScriptedGameIOTests::returnsInOrder);
        r.add("Scripted: exhausted script throws, never Waits", ScriptedGameIOTests::exhaustionThrows);
        r.add("Scripted: role mismatch throws", ScriptedGameIOTests::roleMismatchThrows);
        r.add("Scripted: records displays and checks completion", ScriptedGameIOTests::recordsObservations);
        r.add("Scripted: alternating rejects bad fixture lines with line number", ScriptedGameIOTests::badFixtureLine);
    }

    private static void returnsInOrder() {
        ScriptedGameIO io = new ScriptedGameIO().turn(WaiterAction.seat(1), 0).turn(WaiterAction.order(1), 1);
        Check.equal(WaiterAction.seat(1), io.chooseWaiterAction(), "1st");
        Check.equal(0, io.chooseChefOrder(), "2nd");
        Check.equal(WaiterAction.order(1), io.chooseWaiterAction(), "3rd");
        Check.equal(1, io.chooseChefOrder(), "4th");
        Check.equal(0, io.remainingSteps(), "consumed");
    }

    private static void exhaustionThrows() {
        ScriptedGameIO io = new ScriptedGameIO().waiter(WaiterAction.waitAction());
        io.chooseWaiterAction();
        InputEndedException e = Check.throwsType(InputEndedException.class, io::chooseChefOrder, "exhausted");
        Check.contains(e.getMessage(), "request #2", "message names the request");
    }

    private static void roleMismatchThrows() {
        ScriptedGameIO io = new ScriptedGameIO().chef(2);
        Check.throwsType(IllegalStateException.class, io::chooseWaiterAction, "asked Waiter, scripted Chef");
    }

    private static void recordsObservations() {
        ScriptedGameIO io = new ScriptedGameIO().turn(WaiterAction.waitAction(), 0);
        Check.throwsType(AssertionError.class, io::requireFinished, "no final result yet");
        io.chooseWaiterAction();
        io.showInvalidAction("C9 does not exist");
        io.showUpdate("C1 arrives");
        GameView.TurnSummary end = new GameView.TurnSummary(1, 18, 0, 0, 0, 0, OptionalDouble.empty());
        io.showTurnEnd(end);
        io.showFinalResult(new GameView.FinalReport(false, end, List.of()));
        Check.throwsType(AssertionError.class, io::requireFinished, "Chef step unused");
        io.chooseChefOrder();
        io.requireFinished();
        Check.equal(List.of("C9 does not exist"), io.invalidReasons(), "invalid reasons");
        Check.equal(List.of("C1 arrives"), io.updates(), "updates");
        Check.equal(List.of(end), io.turnSummaries(), "summaries");
        Check.throwsType(IllegalStateException.class,
                () -> io.showFinalResult(new GameView.FinalReport(false, end, List.of())), "final twice");
    }

    private static void badFixtureLine() {
        Check.throwsType(IllegalArgumentException.class,
                () -> ScriptedGameIO.alternating(List.of("wait")), "incomplete role pair");
        IllegalArgumentException e = Check.throwsType(IllegalArgumentException.class,
                () -> ScriptedGameIO.alternating(List.of("seat 1", "wait", "dance", "wait")), "bad line");
        Check.contains(e.getMessage(), "Fixture line 3", "line number");
    }
}
