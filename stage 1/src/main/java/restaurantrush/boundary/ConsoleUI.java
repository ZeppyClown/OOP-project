package restaurantrush.boundary;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Locale;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

import restaurantrush.control.GameIO;
import restaurantrush.control.GameView;
import restaurantrush.control.InputEndedException;
import restaurantrush.control.WaiterAction;

/**
 * Text boundary: turns console lines into typed choices and formats the
 * snapshots the controller supplies. It re-prompts on syntax errors only;
 * whether a choice is legal in the game is the controller's decision.
 *
 */
public class ConsoleUI implements GameIO {

    private final BufferedReader in;
    private final PrintWriter out;

    public ConsoleUI(Reader in, PrintWriter out) {
        this.in = in instanceof BufferedReader buffered ? buffered : new BufferedReader(in);
        this.out = out;
    }

    public ConsoleUI(java.io.InputStream in, java.io.PrintStream out) {
        this(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8), new PrintWriter(out));
    }

    @Override
    public void showActionState(GameView.TurnState state) {
        out.println("State update for turn " + state.turn());
        printState(state);
    }

    @Override
    public void showInterrupted(String message) {
        out.println("Interrupted: " + message);
        out.flush();
    }

    // ---- requests ---------------------------------------------------------

    @Override
    public WaiterAction chooseWaiterAction() {
        while (true) {
            String line = prompt("Waiter (seat <C>, order <C>, serve <O>, wait)> ");
            try {
                return parseWaiterAction(line);
            } catch (IllegalArgumentException e) {
                out.println("Invalid Waiter command: " + e.getMessage());
            }
        }
    }

    @Override
    public int chooseChefOrder() {
        while (true) {
            String line = prompt("Chef (<order ID> or wait)> ");
            try {
                return parseChefChoice(line);
            } catch (IllegalArgumentException e) {
                out.println("Invalid Chef command: " + e.getMessage());
            }
        }
    }

    private String prompt(String text) {
        out.print(text);
        out.flush();
        String line;
        try {
            line = in.readLine();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        if (line == null) {
            out.println();
            out.flush();
            throw new InputEndedException("Input ended before the game finished");
        }
        return line;
    }

    // ---- parsing (public so tests and fixture loaders share one grammar) --

    public static WaiterAction parseWaiterAction(String line) {
        String[] words = words(line);
        String verb = words[0].toLowerCase(Locale.ROOT);
        if (verb.equals("wait")) {
            requireArgs(words, 1, "wait takes no ID");
            return WaiterAction.waitAction();
        }
        WaiterAction.Type type = switch (verb) {
            case "seat" -> WaiterAction.Type.SEAT;
            case "order" -> WaiterAction.Type.ORDER;
            case "serve" -> WaiterAction.Type.SERVE;
            default -> throw new IllegalArgumentException(
                    "unknown command '" + words[0] + "'; use seat, order, serve or wait");
        };
        requireArgs(words, 2, verb + " needs exactly one ID, e.g. '" + verb + " 1'");
        return new WaiterAction(type, parseId(words[1]));
    }

    /** Returns an order ID, or 0 for wait. */
    public static int parseChefChoice(String line) {
        String[] words = words(line);
        requireArgs(words, 1, "enter one order ID or wait");
        if (words[0].equalsIgnoreCase("wait")) {
            return 0;
        }
        return parseId(words[0]);
    }

    private static String[] words(String line) {
        String trimmed = line == null ? "" : line.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("empty input");
        }
        return trimmed.split("\\s+");
    }

    private static void requireArgs(String[] words, int count, String message) {
        if (words.length != count) {
            throw new IllegalArgumentException(message);
        }
    }

    private static int parseId(String text) {
        int id;
        try {
            id = Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("'" + text + "' is not a whole-number ID");
        }
        if (id <= 0) {
            throw new IllegalArgumentException("IDs start at 1");
        }
        return id;
    }

    // ---- displays ---------------------------------------------------------

    @Override
    public void showTurnStart(GameView.TurnState s) {
        out.println();
        out.printf("Stage %d - Turn %d / %d%n", s.stage(), s.turn(), s.maxTurns());
        printState(s);
    }

    private void printState(GameView.TurnState s) {
        out.printf("Revenue: %s | Paid: %d | Average paid satisfaction: %s%n",
                formatCents(s.revenueCents()), s.paidCount(), formatAverage(s.averagePaidSatisfaction()));

        out.println(s.waiting().isEmpty() ? "Waiting: none"
                : "Waiting: " + s.waiting().stream().map(ConsoleUI::customer).collect(Collectors.joining(", ")));
        for (GameView.TableLine t : s.tables()) {
            out.printf("Table %d: %s%n", t.tableId(), t.occupant() == null ? "free" : customer(t.occupant()));
        }
        if (s.orders().isEmpty()) {
            out.println("Orders: none");
        }
        for (GameView.OrderLine o : s.orders()) {
            out.printf("O%d: %s | %s locked | %d/%d units | %s%n", o.orderId(), o.item(),
                    formatCents(o.billCents()), o.completedUnits(), o.totalUnits(), o.status());
        }

        out.println("Waiter options: " + waiterOptions(s));
        out.println("Chef options: " + options(s.preparableOrderIds(), "O"));
        out.println("Cashier: automatic, " + s.cashierStatus());
        out.flush();
    }

    @Override
    public void showInvalidAction(String reason) {
        out.println("Not allowed: " + reason + " - choose again.");
        out.flush();
    }

    @Override
    public void showTurnEnd(GameView.TurnSummary t) {
        out.println("-- End of turn " + t.turn() + " --");
        printSummary(t);
        out.flush();
    }

    @Override
    public void showFinalResult(GameView.FinalReport r) {
        out.println();
        out.println("===== " + (r.victory() ? "VICTORY" : "DEFEAT") + " =====");
        printSummary(r.totals());
        for (GameView.TargetLine target : r.targets()) {
            out.printf("[%s] %s: required %s, actual %s%n",
                    target.met() ? "PASS" : "FAIL", target.name(), target.required(), target.actual());
        }
        out.flush();
    }

    private void printSummary(GameView.TurnSummary t) {
        out.println("Served: " + t.servedCount());
        out.println("Paid: " + t.paidCount());
        out.println("Unhappy departures: " + t.departures());
        out.println("Revenue: " + formatCents(t.revenueCents()));
        out.println("Average satisfaction of paid customers: " + formatAverage(t.averagePaidSatisfaction()));
        out.println("Turns used: " + t.turn() + " / " + t.maxTurns());
    }

    private static String waiterOptions(GameView.TurnState s) {
        StringBuilder sb = new StringBuilder();
        appendOption(sb, "seat", s.seatableCustomerIds(), "C");
        appendOption(sb, "order", s.orderableCustomerIds(), "C");
        appendOption(sb, "serve", s.servableOrderIds(), "O");
        return sb.length() == 0 ? "wait" : sb.append(" | wait").toString();
    }

    private static void appendOption(StringBuilder sb, String verb, List<Integer> ids, String prefix) {
        if (ids.isEmpty()) {
            return;
        }
        if (sb.length() > 0) {
            sb.append(" | ");
        }
        sb.append(verb).append(' ').append(options(ids, prefix));
    }

    private static String options(List<Integer> ids, String prefix) {
        return ids.isEmpty() ? "wait"
                : ids.stream().map(id -> prefix + id).collect(Collectors.joining(", "));
    }

    private static String customer(GameView.CustomerLine c) {
        return "C" + c.id() + " " + c.type() + " (satisfaction " + c.satisfaction() + ", " + c.status() + ")";
    }

    static String formatCents(int cents) {
        return restaurantrush.domain.Money.format(cents);
    }

    static String formatAverage(OptionalDouble average) {
        return average.isPresent() ? String.format(Locale.ROOT, "%.1f", average.getAsDouble()) : "N/A";
    }
}
