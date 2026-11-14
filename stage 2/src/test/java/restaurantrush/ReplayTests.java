package restaurantrush;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import restaurantrush.boundary.ConsoleUI;
import restaurantrush.control.GameController;
import restaurantrush.domain.*;

/** Plays the fixture files through the real parser and controller. */
public final class ReplayTests {
    private ReplayTests() { }

    public static void register(AllTests.Registry r) {
        r.add("Replay: reference victory", () -> victory("victory-input.txt", false));
        r.add("Replay: syntax and semantic retries preserve victory", () -> victory("victory-invalid-input.txt", true));
        r.add("Replay: all-Wait defeat", ReplayTests::defeat);
        r.add("Replay: ordered scripted victory and departure timing", ReplayTests::scripted);
        r.add("Replay: inherited Regular-only fixtures retain original totals", ReplayTests::regularRegression);
        r.add("Replay: VIP priority changes payment timing and satisfaction", ReplayTests::vipPriorities);
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
                new PrintStream(output, true, StandardCharsets.UTF_8)), Main::createCustomer, 2).run();
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
        Check.equal(6510, r.getRevenueCents(), "revenue cents");
        Check.equal(390, r.getPaidSatisfactionTotal(), "paid satisfaction total");
        Check.equal(List.of(84, 90, 64, 76, 76), r.getCustomers().stream()
                .filter(c -> c.getStatus() == CustomerStatus.PAID)
                .map(Customer::getSatisfactionAtPayment).toList(), "frozen payment satisfaction");
        Check.equal(0, r.getUnhappyDepartureCount(), "departures");
        Check.equal(GameResult.VICTORY, r.evaluateResult(), "result");
        Check.contains(run.output(), "===== VICTORY =====", "printed verdict");
        Check.contains(run.output(), "Average satisfaction of paid customers: 78.0", "printed average");
        Check.contains(run.output(), "Stage 2 - Turn 18 / 18", "injected stage");
        Check.contains(run.output(), "C2 VIP", "VIP label through snapshot");
        Check.contains(run.output(), "C3 Critic", "Critic label through snapshot");
        Check.contains(run.output(), "O2: Salad | $8.10 locked", "VIP bill");
        Check.equal("Pasta", r.findCustomer(3).getOrder().getItem().getName(), "Critic preference");
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
        Check.equal(CustomerStatus.LEFT, r.findCustomer(1).getStatus(), "Regular left");
        Check.equal(CustomerStatus.LEFT, r.findCustomer(3).getStatus(), "Critic left");
        Check.equal(0, r.findCustomer(3).getSatisfaction(), "Critic clamps at zero");
        Check.equal(CustomerStatus.WAITING, r.findCustomer(2).getStatus(), "VIP survives");
        Check.equal(25, r.findCustomer(2).getSatisfaction(), "VIP final satisfaction");
        Check.isTrue(r.getAveragePaidSatisfaction().isEmpty(), "average absent");
        Check.contains(run.output(), "===== DEFEAT =====", "printed defeat");
        Check.contains(run.output(), "Average satisfaction of paid customers: N/A", "N/A");
        complete(run.output());
    }

    private static void scripted() {
        ScriptedGameIO io = ScriptedGameIO.alternating(ConsoleTests.fixture("victory-input.txt"));
        Restaurant r = restaurant();
        new GameController(r, io, Main::createCustomer, 2).run();
        io.requireFinished();
        Check.equal(18, io.turnStates().size(), "18 starts");
        Check.equal(18, io.turnSummaries().size(), "18 ends");
        Check.equal(6510, r.getRevenueCents(), "scripted revenue");
        io = ScriptedGameIO.alternating(ConsoleTests.fixture("defeat-input.txt"));
        new GameController(restaurant(), io, Main::createCustomer, 2).run();
        io.requireFinished();
        Check.equal(0, io.turnSummaries().get(11).departures(), "none through turn12");
        Check.equal(1, io.turnSummaries().get(12).departures(), "C1 leaves13");
        Check.equal(1, io.turnSummaries().get(13).departures(), "one through14");
        Check.equal(2, io.turnSummaries().get(14).departures(), "Critic leaves15");
        Check.equal(2, io.turnSummaries().get(17).departures(), "VIP never leaves");
    }

    private static void regularRegression() {
        ScriptedGameIO io = ScriptedGameIO.alternating(ConsoleTests.fixture("victory-input.txt"));
        Restaurant r = restaurant();
        new GameController(r, io).run();
        io.requireFinished();
        Check.equal(6600, r.getRevenueCents(), "original Regular-only revenue");
        Check.equal(396, r.getPaidSatisfactionTotal(), "original Regular-only satisfaction");
        Check.equal(79.2, r.getAveragePaidSatisfaction().orElseThrow(), "original average");
        Check.equal(List.of(84, 84, 76, 76, 76), r.getCustomers().stream()
                .filter(c -> c.getStatus() == CustomerStatus.PAID)
                .map(Customer::getSatisfactionAtPayment).toList(), "original payment records");
        Check.equal(GameResult.VICTORY, r.evaluateResult(), "original victory");
        io = ScriptedGameIO.alternating(ConsoleTests.fixture("defeat-input.txt"));
        r = restaurant();
        new GameController(r, io).run();
        io.requireFinished();
        Check.equal(1, io.turnSummaries().get(14).departures(), "original one through15");
        Check.equal(2, io.turnSummaries().get(15).departures(), "original C2 leaves16");
        Check.equal(4, r.findCustomer(3).getSatisfaction(), "original C3 ends at4");
    }

    private static void vipPriorities() {
        vipPriority("vip-priority-input.txt", 5, 6, 7, 8, 90);
        vipPriority("regular-priority-input.txt", 6, 7, 8, 9, 85);
    }

    private static void vipPriority(String fixture, int orderTurn, int servedTurn,
                                    int readyTurn, int paidTurn, int satisfaction) {
        ScriptedGameIO io = ScriptedGameIO.alternating(ConsoleTests.fixture(fixture));
        Restaurant r = restaurant();
        new GameController(r, io, Main::createCustomer, 2).run();
        io.requireFinished();
        Customer vip = r.findCustomer(2);
        Check.equal("VIP", vip.getTypeLabel(), "actual subtype");
        Check.equal(810, vip.getOrder().getBillCents(), "locked VIP bill");
        Check.equal(servedTurn, vip.getServedTurn(), "serve after preparation");
        Check.equal(readyTurn, vip.getReadyToPayTurn(), "one full eating turn");
        Check.equal(satisfaction, vip.getSatisfactionAtPayment(), "priority affects satisfaction");
        Check.equal(0, io.turnSummaries().get(paidTurn - 2).paidCount(), "no same-readiness-turn payment");
        Check.equal(1, io.turnSummaries().get(paidTurn - 1).paidCount(), "expected payment turn");
        readyVipBill(io, orderTurn);
        Check.equal(1, r.getPaidCount(), "only VIP pays");
        Check.equal(810, r.getRevenueCents(), "only actual payment is revenue");
        Check.equal(GameResult.DEFEAT, r.evaluateResult(), "both alternatives still run to defeat");
        Check.equal(18, io.turnSummaries().size(), "all 18 turns");
        Run cli = fixture(fixture);
        Check.contains(cli.output(), "Revenue: $8.10", "console replay actual bill");
        Check.contains(cli.output(), "Average satisfaction of paid customers: " + satisfaction + ".0", "console average");
        Check.contains(cli.output(), "===== DEFEAT =====", "console verdict");
        complete(cli.output());
    }

    /** Find the actual post-kitchen snapshot, rather than inferring readiness from totals. */
    private static void readyVipBill(ScriptedGameIO io, int orderTurn) {
        var ready = io.actionStates().stream().filter(s -> s.turn() == orderTurn)
                .flatMap(s -> s.orders().stream())
                .filter(o -> o.orderId() == 2 && o.status().equals("READY")).findFirst().orElseThrow();
        Check.equal(810, ready.billCents(), "actual READY order bill");
        Check.equal(1, ready.completedUnits(), "Salad prepared in one unit");
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
