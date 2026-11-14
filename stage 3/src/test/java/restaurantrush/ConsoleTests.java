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

/** ConsoleUI: parsing, retries, end of input and output. */
public final class ConsoleTests {

    /**
     * Fixture directory: -Dstage1.fixtures wins; otherwise look relative to
     * the repository root or to the "stage 2" folder.
     */
    static final Path FIXTURES = locateFixtures();

    private static Path locateFixtures() {
        String configured = System.getProperty("stage1.fixtures");
        if (configured != null) {
            return Path.of(configured);
        }
        for (Path candidate : List.of(Path.of("stage 2", "test-fixtures"), Path.of("test-fixtures"))) {
            if (Files.isDirectory(candidate)) {
                return candidate.toAbsolutePath();
            }
        }
        return Path.of("stage 2", "test-fixtures").toAbsolutePath();
    }

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
        r.add("Console: Stage 2 header and supplied subtype labels", ConsoleTests::displaysSubtypeLabels);
        r.add("Console: no paid customers shows N/A and free tables", ConsoleTests::displaysEmptyState);
        r.add("Console: end-of-turn and final result lines", ConsoleTests::displaysSummaries);
        r.add("Console: defeat verdict prints FAIL targets", ConsoleTests::displaysDefeat);
        r.add("Console: lists every Waiter verb and Chef order with IDs", ConsoleTests::listsAllOptions);
        r.add("Console: mid-turn state update, invalid action and interruption lines", ConsoleTests::displaysNotices);
        r.add("Console: accepts large IDs and rejects null Chef input", ConsoleTests::idBoundaries);
        r.add("Fixtures: four retry-free files are 18 valid Waiter+Chef turns", ConsoleTests::fixturesParse);
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

    /** State from the victory run at the start of turn 9. */
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

    private static void displaysSubtypeLabels() {
        GameView.TurnState state = new GameView.TurnState(2, 7, 18,
                1500, 1, OptionalDouble.of(84.0),
                List.of(new GameView.CustomerLine(3, "Critic", 100, "WAITING")),
                List.of(new GameView.TableLine(1,
                        new GameView.CustomerLine(2, "VIP", 90, "READY_TO_PAY"))),
                List.of(new GameView.OrderLine(2, "Salad", 1, 1, "SERVED", 810)),
                List.of(), List.of(), List.of(), List.of(), "no eligible payment");
        Console c = console("");
        c.ui.showTurnStart(state);
        Check.contains(c.output(), "Stage 2 - Turn 7 / 18", "injected stage label");
        Check.contains(c.output(), "C3 Critic (satisfaction 100, WAITING)", "waiting type label");
        Check.contains(c.output(), "C2 VIP (satisfaction 90, READY_TO_PAY)", "table type label");
        Check.contains(c.output(), "O2: Salad | $8.10 locked", "already-dispatched bill");
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

    private static void displaysDefeat() {
        GameView.TurnSummary end = new GameView.TurnSummary(18, 18, 1, 1, 4, 1200, OptionalDouble.of(52.0));
        GameView.FinalReport report = new GameView.FinalReport(false, end, List.of(
                new GameView.TargetLine("Revenue", ">= $45.00", "$12.00", false),
                new GameView.TargetLine("Departures", "<= 1", "4", false)));
        Console c = console("");
        c.ui.showFinalResult(report);
        String text = c.output();
        Check.contains(text, "===== DEFEAT =====", "verdict");
        Check.contains(text, "Unhappy departures: 4", "departures");
        Check.contains(text, "[FAIL] Revenue: required >= $45.00, actual $12.00", "revenue target");
        Check.contains(text, "[FAIL] Departures: required <= 1, actual 4", "departure target");
        Check.isTrue(!text.contains("VICTORY"), "defeat must not mention VICTORY");
    }

    private static void listsAllOptions() {
        GameView.CustomerLine c1 = new GameView.CustomerLine(1, "Regular", 90, "WAITING");
        GameView.CustomerLine c2 = new GameView.CustomerLine(2, "Regular", 95, "WAITING");
        GameView.CustomerLine c3 = new GameView.CustomerLine(3, "Regular", 80, "SEATED");
        GameView.TurnState state = new GameView.TurnState(1, 6, 18,
                1200, 1, OptionalDouble.of(88.0),
                List.of(c1, c2),
                List.of(new GameView.TableLine(1, c3), new GameView.TableLine(2, null)),
                List.of(new GameView.OrderLine(4, "Burger", 2, 2, "READY", 1200),
                        new GameView.OrderLine(5, "Salad", 0, 1, "PLACED", 800)),
                List.of(1, 2), List.of(3), List.of(4), List.of(5, 6),
                "no eligible payment");
        Console c = console("");
        c.ui.showTurnStart(state);
        String text = c.output();
        Check.contains(text, "Waiting: C1 Regular (satisfaction 90, WAITING), C2 Regular (satisfaction 95, WAITING)",
                "queue keeps arrival order");
        Check.contains(text, "Waiter options: seat C1, C2 | order C3 | serve O4 | wait", "all Waiter verbs");
        Check.contains(text, "Chef options: O5, O6", "all Chef orders");
        Check.contains(text, "O4: Burger | $12.00 locked | 2/2 units | READY", "ready order");
        Check.isTrue(!text.contains("Orders: none"), "orders present, no 'none' line");
    }

    private static void displaysNotices() {
        GameView.TurnState state = new GameView.TurnState(1, 4, 18,
                0, 0, OptionalDouble.empty(),
                List.of(), List.of(new GameView.TableLine(1, null)), List.of(),
                List.of(), List.of(), List.of(), List.of(), "no eligible payment");
        Console c = console("");
        c.ui.showActionState(state);
        c.ui.showInvalidAction("C9 is not waiting");
        c.ui.showInterrupted("Input ended before the game finished");
        String text = c.output();
        Check.contains(text, "State update for turn 4", "state update header");
        Check.isTrue(!text.contains("Stage 1 - Turn"), "state update is not a new turn header");
        Check.contains(text, "Not allowed: C9 is not waiting - choose again.", "invalid action line");
        Check.contains(text, "Interrupted: Input ended before the game finished", "interruption line");
    }

    private static void idBoundaries() {
        Check.equal(WaiterAction.serve(Integer.MAX_VALUE),
                ConsoleUI.parseWaiterAction("serve " + Integer.MAX_VALUE), "largest int ID");
        Check.equal(Integer.MAX_VALUE, ConsoleUI.parseChefChoice(String.valueOf(Integer.MAX_VALUE)), "largest Chef ID");
        Check.throwsType(IllegalArgumentException.class, () -> ConsoleUI.parseChefChoice(null), "null Chef input");
        Check.throwsType(IllegalArgumentException.class,
                () -> ConsoleUI.parseChefChoice(String.valueOf(Integer.MAX_VALUE + 1L)), "overflowing Chef ID");
    }

    // ---- fixtures --------------------------------------------------------

    private static void fixturesParse() {
        for (String name : List.of("victory-input.txt", "defeat-input.txt",
                "vip-priority-input.txt", "regular-priority-input.txt")) {
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
     * Runs the invalid-input file through ConsoleUI. Lines 10 and 14 parse fine
     * but the controller would reject them, so we skip them. What's left
     * should match the clean victory file.
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
