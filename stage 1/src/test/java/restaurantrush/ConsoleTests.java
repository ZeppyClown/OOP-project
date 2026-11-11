package restaurantrush;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.OptionalDouble;

import restaurantrush.boundary.ConsoleUI;
import restaurantrush.control.GameView;
import restaurantrush.control.InputEndedException;
import restaurantrush.control.WaiterAction;

/** Talon: ConsoleUI parsing, syntax retry, EOF and display (S1-T1). */
public final class ConsoleTests {

    /** Tests run from the repository root. */
    static final Path FIXTURES = Path.of(System.getProperty("stage1.fixtures"));

    private ConsoleTests() {
    }

    public static void register(AllTests.Registry r) {
        r.add("Console: parses seat/order/serve/wait", ConsoleTests::parsesWaiterCommands);
        r.add("Console: trims and ignores case", ConsoleTests::trimsAndIgnoresCase);
        r.add("Console: rejects malformed Waiter commands", ConsoleTests::rejectsMalformedWaiter);
        r.add("Console: parses Chef ID or wait", ConsoleTests::parsesChef);
        r.add("Console: rejects malformed Chef commands", ConsoleTests::rejectsMalformedChef);
        r.add("Console: re-prompts on syntax error in the same request", ConsoleTests::retriesUntilValid);
        r.add("Console: EOF throws instead of choosing Wait", ConsoleTests::eofThrows);
        r.add("Console: start-of-turn display shows every required field", ConsoleTests::displaysTurnState);
        r.add("Console: no paid customers shows N/A and free tables", ConsoleTests::displaysEmptyState);
        r.add("Console: end-of-turn and final result lines", ConsoleTests::displaysSummaries);
        r.add("Fixtures: victory/defeat are 18 valid Waiter+Chef turns", ConsoleTests::fixturesParse);
        r.add("Fixtures: invalid-input file reaches the same choices via retries", ConsoleTests::invalidFixtureRecovers);
    }

    // ---- parsing ---------------------------------------------------------

    private static void parsesWaiterCommands() {
        Check.equal(WaiterAction.seat(2), ConsoleUI.parseWaiterAction("seat 2"), "seat");
        Check.equal(WaiterAction.order(1), ConsoleUI.parseWaiterAction("order 1"), "order");
        Check.equal(WaiterAction.serve(3), ConsoleUI.parseWaiterAction("serve 3"), "serve");
        Check.equal(WaiterAction.waitAction(), ConsoleUI.parseWaiterAction("wait"), "wait");
    }

    private static void trimsAndIgnoresCase() {
        Check.equal(WaiterAction.seat(4), ConsoleUI.parseWaiterAction("  SeAt    4 \t"), "mixed case");
        Check.equal(WaiterAction.waitAction(), ConsoleUI.parseWaiterAction(" WAIT "), "upper wait");
        Check.equal(0, ConsoleUI.parseChefChoice("Wait"), "chef wait");
    }

    private static void rejectsMalformedWaiter() {
        for (String bad : List.of("", "   ", "dance", "seat", "seat x", "seat -1", "seat 0",
                "seat 1 2", "wait 1", "serve 2.5", "order 99999999999", "seat C1")) {
            Check.throwsType(IllegalArgumentException.class, () -> ConsoleUI.parseWaiterAction(bad),
                    "Waiter input '" + bad + "'");
        }
        Check.throwsType(IllegalArgumentException.class, () -> ConsoleUI.parseWaiterAction(null), "null");
    }

    private static void parsesChef() {
        Check.equal(3, ConsoleUI.parseChefChoice("3"), "id");
        Check.equal(0, ConsoleUI.parseChefChoice("wait"), "wait");
    }

    private static void rejectsMalformedChef() {
        for (String bad : List.of("", "two", "0", "-3", "3 4", "cook 3", "wait now")) {
            Check.throwsType(IllegalArgumentException.class, () -> ConsoleUI.parseChefChoice(bad),
                    "Chef input '" + bad + "'");
        }
    }

    private static void retriesUntilValid() {
        Console c = console("dance\nseat\n  seat 2\nnope\n5\n");
        Check.equal(WaiterAction.seat(2), c.ui.chooseWaiterAction(), "third line accepted");
        Check.equal(5, c.ui.chooseChefOrder(), "second Chef line accepted");
        String text = c.output();
        Check.contains(text, "Invalid Waiter command: unknown command 'dance'", "unknown verb message");
        Check.contains(text, "Invalid Waiter command: seat needs exactly one ID", "missing ID message");
        Check.contains(text, "Invalid Chef command: 'nope' is not a whole-number ID", "chef message");
    }

    private static void eofThrows() {
        Console empty = console("");
        Check.throwsType(InputEndedException.class, empty.ui::chooseWaiterAction, "Waiter at EOF");
        Console badThenEof = console("dance\n");
        Check.throwsType(InputEndedException.class, badThenEof.ui::chooseChefOrder, "Chef after bad line");
    }

    // ---- displays --------------------------------------------------------

    /** Values from the reference victory at the start of turn 9. */
    private static void displaysTurnState() {
        GameView.CustomerLine c3 = new GameView.CustomerLine(3, "Regular", 84, "WAITING_FOR_FOOD");
        GameView.TurnState state = new GameView.TurnState(1, 9, 18,
                2400, 2, OptionalDouble.of(84.0),
                List.of(),
                List.of(new GameView.TableLine(1, c3), new GameView.TableLine(2, null)),
                List.of(new GameView.OrderLine(3, "Pasta", 1, 2, "PREPARING", 1800)),
                List.of(), List.of(), List.of(), List.of(3),
                "no eligible payment");
        Console c = console("");
        c.ui.showTurnStart(state);
        String text = c.output();
        Check.contains(text, "Stage 1 - Turn 9 / 18", "header");
        Check.contains(text, "Revenue: $24.00 | Paid: 2 | Average paid satisfaction: 84.0", "totals");
        Check.contains(text, "Waiting: none", "queue");
        Check.contains(text, "Table 1: C3 Regular (satisfaction 84, WAITING_FOR_FOOD)", "occupied table");
        Check.contains(text, "Table 2: free", "free table");
        Check.contains(text, "O3: Pasta | $18.00 locked | 1/2 units | PREPARING", "order line");
        Check.contains(text, "Waiter options: wait", "waiter options");
        Check.contains(text, "Chef options: O3", "chef options");
        Check.contains(text, "Cashier: automatic, no eligible payment", "cashier");
    }

    private static void displaysEmptyState() {
        GameView.CustomerLine c1 = new GameView.CustomerLine(1, "Regular", 100, "WAITING");
        GameView.TurnState state = new GameView.TurnState(1, 1, 18,
                0, 0, OptionalDouble.empty(),
                List.of(c1),
                List.of(new GameView.TableLine(1, null), new GameView.TableLine(2, null)),
                List.of(),
                List.of(1), List.of(), List.of(), List.of(),
                "no eligible payment");
        Console c = console("");
        c.ui.showTurnStart(state);
        String text = c.output();
        Check.contains(text, "Revenue: $0.00 | Paid: 0 | Average paid satisfaction: N/A", "N/A average");
        Check.contains(text, "Waiting: C1 Regular (satisfaction 100, WAITING)", "queue entry");
        Check.contains(text, "Orders: none", "no orders");
        Check.contains(text, "Waiter options: seat C1 | wait", "seat option");
        Check.contains(text, "Chef options: wait", "chef wait only");
    }

    private static void displaysSummaries() {
        GameView.TurnSummary end = new GameView.TurnSummary(18, 18, 5, 5, 0, 6600, OptionalDouble.of(79.2));
        GameView.FinalReport report = new GameView.FinalReport(true, end, List.of(
                new GameView.TargetLine("Revenue", ">= $45.00", "$66.00", true),
                new GameView.TargetLine("Departures", "<= 1", "0", true)));
        Console c = console("");
        c.ui.showTurnEnd(end);
        c.ui.showFinalResult(report);
        String text = c.output();
        Check.contains(text, "-- End of turn 18 --", "turn end header");
        Check.contains(text, "Served: 5", "served");
        Check.contains(text, "Paid: 5", "paid");
        Check.contains(text, "Unhappy departures: 0", "departures");
        Check.contains(text, "Revenue: $66.00", "revenue");
        Check.contains(text, "Average satisfaction of paid customers: 79.2", "average");
        Check.contains(text, "Turns used: 18 / 18", "turns");
        Check.contains(text, "===== VICTORY =====", "verdict");
        Check.contains(text, "[PASS] Revenue: required >= $45.00, actual $66.00", "target line");
    }

    // ---- fixtures --------------------------------------------------------

    private static void fixturesParse() {
        for (String name : List.of("victory-input.txt", "defeat-input.txt")) {
            List<String> lines = fixture(name);
            Check.equal(36, lines.size(), name + " line count");
            ScriptedGameIO io = ScriptedGameIO.alternating(lines);
            Check.equal(36, io.remainingSteps(), name + " typed steps");
        }
        ScriptedGameIO victory = ScriptedGameIO.alternating(fixture("victory-input.txt"));
        Check.equal(WaiterAction.seat(1), victory.chooseWaiterAction(), "turn 1 Waiter");
        Check.equal(0, victory.chooseChefOrder(), "turn 1 Chef");
        Check.equal(WaiterAction.order(1), victory.chooseWaiterAction(), "turn 2 Waiter");
        Check.equal(1, victory.chooseChefOrder(), "turn 2 Chef");
    }

    /**
     * Feeds the invalid-input fixture through ConsoleUI, accepting each
     * syntactically valid choice. Lines 10 and 14 are semantic errors the
     * controller rejects, so they are skipped here the way the controller's
     * retry would. The remaining choices must equal the clean victory file.
     */
    private static void invalidFixtureRecovers() {
        String all = String.join("\n", fixture("victory-invalid-input.txt")) + "\n";
        Console c = console(all);
        ScriptedGameIO expected = ScriptedGameIO.alternating(fixture("victory-input.txt"));
        for (int turn = 1; turn <= 18; turn++) {
            WaiterAction w = c.ui.chooseWaiterAction();
            if (turn == 2) {
                Check.equal(WaiterAction.serve(1), w, "turn 2 semantic-invalid Waiter line");
                w = c.ui.chooseWaiterAction();
            }
            Check.equal(expected.chooseWaiterAction(), w, "turn " + turn + " Waiter");
            int chef = c.ui.chooseChefOrder();
            if (turn == 3) {
                Check.equal(1, chef, "turn 3 semantic-invalid Chef line");
                chef = c.ui.chooseChefOrder();
            }
            Check.equal(expected.chooseChefOrder(), chef, "turn " + turn + " Chef");
        }
        Check.equal(0, expected.remainingSteps(), "all clean steps matched");
        Check.throwsType(InputEndedException.class, c.ui::chooseWaiterAction, "nothing left over");
        Check.contains(c.output(), "Invalid Chef command: 'two'", "turn 1 Chef syntax line");
    }

    // ---- helpers ---------------------------------------------------------

    private record Console(ConsoleUI ui, StringWriter buffer) {
        String output() {
            return buffer.toString();
        }
    }

    private static Console console(String input) {
        StringWriter buffer = new StringWriter();
        return new Console(new ConsoleUI(new StringReader(input), new PrintWriter(buffer)), buffer);
    }

    static List<String> fixture(String name) {
        try {
            return Files.readAllLines(FIXTURES.resolve(name));
        } catch (IOException e) {
            throw new AssertionError("Cannot read fixture " + name + " (set -Dstage1.fixtures to the absolute fixture directory)", e);
        }
    }
}
