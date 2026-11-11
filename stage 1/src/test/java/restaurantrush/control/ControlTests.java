package restaurantrush.control;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

/**
 * Plain standard-library tests for the Stage 1 control types that do not
 * depend on the domain package: WaiterAction, InputEndedException, GameView
 * and GameIO.
 *
 * Compile and run from "stage 1":
 *   javac -d build/test-classes src/main/java/restaurantrush/control/{WaiterAction,InputEndedException,GameView,GameIO}.java src/test/java/restaurantrush/control/ControlTests.java
 *   java -cp build/test-classes restaurantrush.control.ControlTests
 */
public final class ControlTests {

    private static final List<String> NAMES = new ArrayList<>();
    private static final List<Runnable> CHECKS = new ArrayList<>();

    private ControlTests() {
    }

    private static void test(String name, Runnable check) {
        NAMES.add(name);
        CHECKS.add(check);
    }

    // ---------- assertion helpers ----------

    private static void isTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void equal(Object expected, Object actual, String message) {
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError(message + ": expected <" + expected + "> but was <" + actual + ">");
        }
    }

    private static <T extends Throwable> T throwsType(Class<T> type, Runnable body, String message) {
        try {
            body.run();
        } catch (Throwable thrown) {
            if (type.isInstance(thrown)) {
                return type.cast(thrown);
            }
            throw new AssertionError(message + ": expected " + type.getSimpleName() + " but got " + thrown, thrown);
        }
        throw new AssertionError(message + ": expected " + type.getSimpleName() + " but nothing was thrown");
    }

    // ---------- WaiterAction ----------

    private static void waiterActionTests() {
        test("WaiterAction.seat targets the customer", () -> {
            WaiterAction a = WaiterAction.seat(3);
            equal(WaiterAction.Type.SEAT, a.type(), "type");
            equal(3, a.targetId(), "target");
        });
        test("WaiterAction.order targets the customer", () -> {
            WaiterAction a = WaiterAction.order(2);
            equal(WaiterAction.Type.ORDER, a.type(), "type");
            equal(2, a.targetId(), "target");
        });
        test("WaiterAction.serve targets the order", () -> {
            WaiterAction a = WaiterAction.serve(5);
            equal(WaiterAction.Type.SERVE, a.type(), "type");
            equal(5, a.targetId(), "target");
        });
        test("WaiterAction.waitAction uses target 0", () -> {
            WaiterAction a = WaiterAction.waitAction();
            equal(WaiterAction.Type.WAIT, a.type(), "type");
            equal(0, a.targetId(), "target");
        });
        test("WaiterAction rejects a null type", () -> {
            IllegalArgumentException e = throwsType(IllegalArgumentException.class,
                    () -> new WaiterAction(null, 1), "null type");
            equal("Action type is required", e.getMessage(), "message");
        });
        test("WaiterAction rejects WAIT with a non-zero target", () -> {
            IllegalArgumentException e = throwsType(IllegalArgumentException.class,
                    () -> new WaiterAction(WaiterAction.Type.WAIT, 1), "WAIT 1");
            equal("WAIT must have target 0", e.getMessage(), "message");
        });
        test("WaiterAction rejects zero target for SEAT, ORDER and SERVE", () -> {
            for (WaiterAction.Type type : List.of(WaiterAction.Type.SEAT, WaiterAction.Type.ORDER,
                    WaiterAction.Type.SERVE)) {
                IllegalArgumentException e = throwsType(IllegalArgumentException.class,
                        () -> new WaiterAction(type, 0), type + " 0");
                equal(type + " needs a positive target ID", e.getMessage(), "message");
            }
        });
        test("WaiterAction rejects negative targets", () -> {
            throwsType(IllegalArgumentException.class, () -> WaiterAction.seat(-1), "seat -1");
            throwsType(IllegalArgumentException.class, () -> WaiterAction.order(-4), "order -4");
            throwsType(IllegalArgumentException.class, () -> WaiterAction.serve(-2), "serve -2");
        });
        test("WaiterAction has value equality", () -> {
            equal(WaiterAction.seat(1), new WaiterAction(WaiterAction.Type.SEAT, 1), "same seat");
            isTrue(!WaiterAction.seat(1).equals(WaiterAction.order(1)), "seat 1 != order 1");
            isTrue(!WaiterAction.seat(1).equals(WaiterAction.seat(2)), "seat 1 != seat 2");
        });
        test("WaiterAction.Type has exactly four actions", () ->
                equal(List.of(WaiterAction.Type.SEAT, WaiterAction.Type.ORDER, WaiterAction.Type.SERVE,
                        WaiterAction.Type.WAIT), List.of(WaiterAction.Type.values()), "values"));
    }

    // ---------- InputEndedException ----------

    private static void inputEndedTests() {
        test("InputEndedException keeps its message", () ->
                equal("EOF at Waiter", new InputEndedException("EOF at Waiter").getMessage(), "message"));
        test("InputEndedException is unchecked", () -> {
            Object e = new InputEndedException("x");
            isTrue(e instanceof RuntimeException, "is a RuntimeException");
        });
    }

    // ---------- GameView ----------

    private static GameView.CustomerLine customer(int id) {
        return new GameView.CustomerLine(id, "Regular", 80, "WAITING");
    }

    private static GameView.TurnState turnState(List<Integer> seatable) {
        return new GameView.TurnState(1, 2, 18, 1500, 1, OptionalDouble.of(75.0),
                List.of(customer(1)), List.of(new GameView.TableLine(1, null)), List.of(),
                seatable, List.of(), List.of(), List.of(), "no eligible payment this turn");
    }

    private static void gameViewTests() {
        test("CustomerLine stores its fields", () -> {
            GameView.CustomerLine c = new GameView.CustomerLine(4, "Regular", 55, "SEATED");
            equal(4, c.id(), "id");
            equal("Regular", c.type(), "type");
            equal(55, c.satisfaction(), "satisfaction");
            equal("SEATED", c.status(), "status");
        });
        test("TableLine allows a null occupant for a free table", () -> {
            GameView.TableLine free = new GameView.TableLine(2, null);
            equal(2, free.tableId(), "table id");
            equal(null, free.occupant(), "occupant");
            equal(customer(1), new GameView.TableLine(1, customer(1)).occupant(), "occupied");
        });
        test("OrderLine stores progress and bill", () -> {
            GameView.OrderLine o = new GameView.OrderLine(1, "Burger", 1, 2, "PREPARING", 1200);
            equal(1, o.orderId(), "order id");
            equal("Burger", o.item(), "item");
            equal(1, o.completedUnits(), "completed");
            equal(2, o.totalUnits(), "total");
            equal("PREPARING", o.status(), "status");
            equal(1200, o.billCents(), "bill");
        });
        test("TurnState copies its lists so later changes do not leak in", () -> {
            List<Integer> seatable = new ArrayList<>(List.of(1));
            GameView.TurnState state = turnState(seatable);
            seatable.add(2);
            equal(List.of(1), state.seatableCustomerIds(), "seatable snapshot");
        });
        test("TurnState lists are unmodifiable", () -> {
            GameView.TurnState state = turnState(List.of(1));
            throwsType(UnsupportedOperationException.class, () -> state.waiting().clear(), "waiting");
            throwsType(UnsupportedOperationException.class, () -> state.tables().clear(), "tables");
            throwsType(UnsupportedOperationException.class, () -> state.orders().clear(), "orders");
            throwsType(UnsupportedOperationException.class, () -> state.seatableCustomerIds().clear(), "seatable");
            throwsType(UnsupportedOperationException.class, () -> state.orderableCustomerIds().clear(), "orderable");
            throwsType(UnsupportedOperationException.class, () -> state.servableOrderIds().clear(), "servable");
            throwsType(UnsupportedOperationException.class, () -> state.preparableOrderIds().clear(), "preparable");
        });
        test("TurnState rejects null lists", () ->
                throwsType(NullPointerException.class, () -> turnState(null), "null seatable"));
        test("TurnState keeps scalar values", () -> {
            GameView.TurnState s = turnState(List.of());
            equal(1, s.stage(), "stage");
            equal(2, s.turn(), "turn");
            equal(18, s.maxTurns(), "max turns");
            equal(1500, s.revenueCents(), "revenue");
            equal(1, s.paidCount(), "paid");
            equal(OptionalDouble.of(75.0), s.averagePaidSatisfaction(), "average");
            equal("no eligible payment this turn", s.cashierStatus(), "cashier");
        });
        test("TurnSummary stores running totals", () -> {
            GameView.TurnSummary t = new GameView.TurnSummary(18, 18, 5, 4, 1, 4800, OptionalDouble.empty());
            equal(18, t.turn(), "turn");
            equal(5, t.servedCount(), "served");
            equal(4, t.paidCount(), "paid");
            equal(1, t.departures(), "departures");
            equal(4800, t.revenueCents(), "revenue");
            isTrue(t.averagePaidSatisfaction().isEmpty(), "no average");
        });
        test("FinalReport copies and locks its targets", () -> {
            List<GameView.TargetLine> targets = new ArrayList<>();
            targets.add(new GameView.TargetLine("Paid customers", "4", "4", true));
            GameView.TurnSummary totals = new GameView.TurnSummary(18, 18, 4, 4, 0, 4500, OptionalDouble.of(60));
            GameView.FinalReport report = new GameView.FinalReport(true, totals, targets);
            targets.add(new GameView.TargetLine("Revenue", "$45.00", "$10.00", false));
            equal(1, report.targets().size(), "target snapshot");
            throwsType(UnsupportedOperationException.class, () -> report.targets().clear(), "unmodifiable");
            isTrue(report.victory(), "victory");
            equal(totals, report.totals(), "totals");
        });
        test("TargetLine records whether a target was met", () -> {
            GameView.TargetLine t = new GameView.TargetLine("Revenue", "$45.00", "$30.00", false);
            equal("Revenue", t.name(), "name");
            equal("$45.00", t.required(), "required");
            equal("$30.00", t.actual(), "actual");
            isTrue(!t.met(), "not met");
        });
    }

    // ---------- GameIO ----------

    /** Implements only the required methods, leaving every display hook as its default. */
    private static final class MinimalIO implements GameIO {
        @Override
        public WaiterAction chooseWaiterAction() {
            throw new InputEndedException("no input");
        }

        @Override
        public int chooseChefOrder() {
            return 0;
        }

        @Override
        public void showInvalidAction(String message) {
        }
    }

    private static void gameIOTests() {
        test("GameIO display hooks default to doing nothing", () -> {
            GameIO io = new MinimalIO();
            GameView.TurnState state = turnState(List.of());
            GameView.TurnSummary summary = new GameView.TurnSummary(1, 18, 0, 0, 0, 0, OptionalDouble.empty());
            io.showTurnStart(state);
            io.showActionState(state);
            io.showTurnEnd(summary);
            io.showFinalResult(new GameView.FinalReport(false, summary, List.of()));
            io.showInterrupted("stopped");
        });
        test("GameIO implementations signal EOF with InputEndedException", () ->
                throwsType(InputEndedException.class, () -> new MinimalIO().chooseWaiterAction(), "EOF"));
        test("GameIO chef choice 0 means Wait", () ->
                equal(0, new MinimalIO().chooseChefOrder(), "wait sentinel"));
    }

    public static void main(String[] args) {
        waiterActionTests();
        inputEndedTests();
        gameViewTests();
        gameIOTests();

        int failed = 0;
        for (int i = 0; i < CHECKS.size(); i++) {
            try {
                CHECKS.get(i).run();
                System.out.println("PASS " + NAMES.get(i));
            } catch (Throwable t) {
                failed++;
                System.out.println("FAIL " + NAMES.get(i) + " -> " + t);
            }
        }
        int total = CHECKS.size();
        System.out.printf("%d passed, %d failed, %d total%n", total - failed, failed, total);
        if (failed > 0) {
            System.exit(1);
        }
    }
}
