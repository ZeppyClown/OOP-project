package restaurantrush;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Runs every Stage 1 test and exits nonzero if any fail.
 *   java -ea -cp build/test-classes restaurantrush.AllTests
 */
public final class AllTests {

    private static final List<Consumer<Registry>> SUITES = List.of(
            ConsoleTests::register,
            ScriptedGameIOTests::register,
            r -> r.add("TestGameIO self-tests", () -> TestGameIOTests.main(new String[0])),
            r -> r.add("Customer domain", restaurantrush.domain.CustomerTests::runAll),
            r -> r.add("Restaurant domain", restaurantrush.domain.RestaurantTests::runAll),
            r -> r.add("Order domain", restaurantrush.domain.OrderTests::runAll),
            r -> r.add("Staff domain", restaurantrush.domain.StaffTests::runAll),
            r -> r.add("Controller turns", TurnTests::runAll),
            r -> r.add("Launcher arguments and script errors", LauncherTests::runAll),
            ReplayTests::register
    );

    public static final class Registry {
        private final List<String> names = new ArrayList<>();
        private final List<Runnable> checks = new ArrayList<>();

        public void add(String name, Runnable check) {
            names.add(name);
            checks.add(check);
        }
    }

    private AllTests() {
    }

    public static void main(String[] args) {
        boolean assertionsEnabled = false;
        assert assertionsEnabled = true;
        if (!assertionsEnabled) {
            System.err.println("Run with java -ea so domain assert statements are checked.");
            System.exit(2);
        }

        Registry registry = new Registry();
        SUITES.forEach(suite -> suite.accept(registry));

        int failed = 0;
        for (int i = 0; i < registry.checks.size(); i++) {
            String name = registry.names.get(i);
            try {
                registry.checks.get(i).run();
                System.out.println("PASS " + name);
            } catch (Throwable t) {
                failed++;
                System.out.println("FAIL " + name + " -> " + t);
            }
        }
        int total = registry.checks.size();
        System.out.printf("%d passed, %d failed, %d total%n", total - failed, failed, total);
        if (failed > 0) {
            System.exit(1);
        }
    }
}
