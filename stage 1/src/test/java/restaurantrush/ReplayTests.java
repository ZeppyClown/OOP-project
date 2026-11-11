package restaurantrush;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import restaurantrush.boundary.ConsoleUI;
import restaurantrush.control.GameController;
import restaurantrush.domain.*;

/** Exercises actual fixture bytes through the production parser and controller. */
public final class ReplayTests {
    private ReplayTests() { }

    public static void register(AllTests.Registry r) {
        r.add("Replay: reference victory", () -> victory("victory-input.txt", false));
        r.add("Replay: syntax and semantic retries preserve victory", () -> victory("victory-invalid-input.txt", true));
        r.add("Replay: all-Wait defeat", ReplayTests::defeat);
        r.add("Replay: ordered scripted victory and departure timing", ReplayTests::scripted);
        r.add("Replay: EOF interrupts without final result", ReplayTests::interrupted);
    }

    private static Restaurant restaurant() {
        return new Restaurant(new Menu(), new Waiter(), new Chef(), new Cashier());
    }

    private record Run(Restaurant restaurant, String output) { }

    private static Run run(String input) {
        Restaurant restaurant = restaurant();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        new GameController(restaurant, new ConsoleUI(
                new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)),
                new PrintStream(output, true, StandardCharsets.UTF_8))).run();
        return new Run(restaurant, output.toString(StandardCharsets.UTF_8));
    }

    private static Run fixture(String name) {
        return run(String.join("\n", ConsoleTests.fixture(name)) + "\n");
    }

    private static void victory(String file, boolean invalid) {
        Run run = fixture(file);
        Restaurant r = run.restaurant();
        Check.equal(5, r.getPaidCount(), "paid");
        Check.equal(5, r.getServedCount(), "served");
        Check.equal(6600, r.getRevenueCents(), "revenue cents");
        Check.equal(396, r.getPaidSatisfactionTotal(), "paid satisfaction total");
        Check.equal(List.of(84, 84, 76, 76, 76), r.getCustomers().stream()
                .filter(c -> c.getStatus() == CustomerStatus.PAID)
                .map(Customer::getSatisfactionAtPayment).toList(), "frozen payment satisfaction");
        Check.equal(0, r.getUnhappyDepartureCount(), "departures");
        Check.equal(GameResult.VICTORY, r.evaluateResult(), "result");
        Check.contains(run.output(), "===== VICTORY =====", "printed verdict");
        Check.contains(run.output(), "Average satisfaction of paid customers: 79.2", "printed average");
        complete(run.output());
        if (invalid) {
            Check.contains(run.output(), "Invalid Waiter command", "syntax retry");
            Check.contains(run.output(), "Invalid Chef command", "Chef syntax retry");
            Check.equal(2, occurrences(run.output(), "Not allowed:"), "semantic retries");
        }
    }

    private static void defeat() {
        Run run = fixture("defeat-input.txt");
        Restaurant r = run.restaurant();
        Check.equal(0, r.getPaidCount(), "paid");
        Check.equal(0, r.getServedCount(), "served");
        Check.equal(0, r.getRevenueCents(), "revenue");
        Check.equal(2, r.getUnhappyDepartureCount(), "departures");
        Check.equal(4, r.findCustomer(3).getSatisfaction(), "C3 final satisfaction");
        Check.isTrue(r.getAveragePaidSatisfaction().isEmpty(), "average absent");
        Check.contains(run.output(), "===== DEFEAT =====", "printed defeat");
        Check.contains(run.output(), "Average satisfaction of paid customers: N/A", "N/A");
        complete(run.output());
    }

    private static void scripted() {
        ScriptedGameIO io = ScriptedGameIO.alternating(ConsoleTests.fixture("victory-input.txt"));
        Restaurant r = restaurant();
        new GameController(r, io).run();
        io.requireFinished();
        Check.equal(18, io.turnStates().size(), "18 starts");
        Check.equal(18, io.turnSummaries().size(), "18 ends");
        Check.equal(6600, r.getRevenueCents(), "scripted revenue");
        io = ScriptedGameIO.alternating(ConsoleTests.fixture("defeat-input.txt"));
        new GameController(restaurant(), io).run();
        io.requireFinished();
        Check.equal(0, io.turnSummaries().get(11).departures(), "none through turn12");
        Check.equal(1, io.turnSummaries().get(12).departures(), "C1 leaves13");
        Check.equal(1, io.turnSummaries().get(14).departures(), "one through15");
        Check.equal(2, io.turnSummaries().get(15).departures(), "C2 leaves16");
    }

    private static void interrupted() {
        for (String input : List.of("", "seat 1\n", "seat x\n", "wait\ntwo\n")) {
            Run run = run(input);
            Check.contains(run.output(), "Interrupted:", "explicit interruption");
            Check.isTrue(!run.output().contains("===== VICTORY") && !run.output().contains("===== DEFEAT"),
                    "no final result on EOF");
        }
    }

    private static void complete(String text) {
        Check.equal(18, occurrences(text, "-- End of turn "), "exactly18 completed turns");
        Check.equal(1, occurrences(text, "===== "), "one final report");
    }

    private static int occurrences(String text, String token) {
        return (text.length() - text.replace(token, "").length()) / token.length();
    }
}
