package restaurantrush;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Starts a real JVM to check Main's exit code and error output. */
public final class LauncherTests {
    private static Path output;
    private static int processes;
    private static int passed;

    private LauncherTests() { }

    public static void main(String[] args) {
        runAll();
    }

    public static void runAll() {
        passed = 0;
        processes = 0;
        try {
            output = Files.createTempDirectory(Path.of("/tmp"), "oop-launcher-tests-");
            usageErrors();
            missingScript();
            invalidScriptPath();
            unreadableScriptInput();
            inputIoFailure();
            programmingFailureEscapes();
            spacedScriptCompletes();
            defaultStage2Victory();
            eofRemainsAnInterruption();
            chefAdviceWorksThroughParser();
        } catch (IOException failure) {
            throw new AssertionError("Launcher test infrastructure I/O failed", failure);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Launcher tests interrupted", interrupted);
        }
        System.out.println("LauncherTests: " + passed + " passed (" + processes + " subprocesses; " + output + ")");
    }

    private static void usageErrors() throws IOException, InterruptedException {
        for (List<String> args : List.of(List.of("--unknown"), List.of("--script"),
                List.of("--script", "unused", "extra"))) {
            Result result = launch("restaurantrush.Main", "", args.toArray(String[]::new));
            expectedError(result, 2, "Usage: restaurantrush.Main [--script <path>]");
            check(result.stdout().isEmpty(), "usage must fail before starting gameplay");
        }
        passed++;
    }

    private static void missingScript() throws IOException, InterruptedException {
        Result result = launch("restaurantrush.Main", "", "--script", output.resolve("missing.txt").toString());
        expectedError(result, 1, "Script I/O error:");
        check(result.stdout().isEmpty(), "missing script must fail before gameplay");
        passed++;
    }

    private static void invalidScriptPath() throws IOException, InterruptedException {
        expectedError(launch(FailingInput.class.getName(), "", "invalid-path"), 1, "Cannot open script: invalid path");
        passed++;
    }

    private static void unreadableScriptInput() throws IOException, InterruptedException {
        // On Unix a directory opens fine but fails when read.
        Result result = launch("restaurantrush.Main", "", "--script", output.toString());
        check(result.exit() == 1 && result.stderr().contains("I/O error:"), "directory is an expected I/O failure");
        conciseError(result);
        noVerdict(result);
        passed++;
    }

    private static void inputIoFailure() throws IOException, InterruptedException {
        Result result = launch(FailingInput.class.getName(), "", "io");
        expectedError(result, 1, "Input I/O error: injected read failure");
        check(!result.stdout().contains("Interrupted:"), "I/O failure is not EOF");
        noVerdict(result);
        passed++;
    }

    private static void programmingFailureEscapes() throws IOException, InterruptedException {
        Result result = launch(FailingInput.class.getName(), "", "bug");
        check(result.exit() != 0 && result.stderr().contains("IllegalStateException: injected programming failure")
                && result.stderr().contains("at restaurantrush."), "unexpected failure retains its stack trace");
        noVerdict(result);
        passed++;
    }

    private static void spacedScriptCompletes() throws IOException, InterruptedException {
        Path script = Files.createDirectories(output.resolve("script folder")).resolve("all wait.txt");
        Files.writeString(script, "wait\nwait\n".repeat(18), StandardCharsets.UTF_8);
        Result result = launch("restaurantrush.Main", "", "--script", script.toString());
        check(result.exit() == 0 && result.stderr().isEmpty(), "valid spaced script succeeds");
        check(result.stdout().contains("===== DEFEAT =====")
                && result.stdout().split("-- End of turn ", -1).length - 1 == 18, "full script has 18 turn summaries");
        passed++;
    }

    private static void eofRemainsAnInterruption() throws IOException, InterruptedException {
        Result result = launch("restaurantrush.Main", "");
        check(result.exit() == 0 && result.stderr().isEmpty(), "EOF remains a normal interruption");
        check(result.stdout().contains("Input ended during turn 1, Waiter"), "EOF identifies role and turn");
        noVerdict(result);
        passed++;
    }

    private static void defaultStage2Victory() throws IOException, InterruptedException {
        Path fixtures = Path.of(ConsoleTests.FIXTURES.toString());
        Path script = Files.createDirectories(output.resolve("stage 2 scripts")).resolve("victory input.txt");
        Files.copy(fixtures.resolve("victory-input.txt"), script);
        Result result = launch("restaurantrush.Main", "", "--script", script.toString());
        check(result.exit() == 0 && result.stderr().isEmpty(), "Stage 2 victory script succeeds");
        check(result.stdout().contains("Stage 2 - Turn 1 / 18")
                && result.stdout().contains("C2 VIP") && result.stdout().contains("C3 Critic"),
                "real Main assembles Stage 2 customer mix");
        check(result.stdout().split("-- End of turn ", -1).length - 1 == 18
                && result.stdout().split("===== ", -1).length - 1 == 1, "18 turns and one final report");
        check(result.stdout().contains("===== VICTORY ====="), "Stage 2 victory verdict");
        String report = result.stdout().substring(result.stdout().indexOf("===== VICTORY ====="));
        check(report.contains("Served: 5\n") && report.contains("Paid: 5\n")
                && report.contains("Revenue: $65.10\n")
                && report.contains("Average satisfaction of paid customers: 78.0\n")
                && report.contains("Unhappy departures: 0\n") && report.contains("Turns used: 18 / 18"),
                "default Stage 2 victory totals");
        check(report.contains("[PASS] Paid customers: required 4, actual 5")
                && report.contains("[PASS] Revenue: required $45.00, actual $65.10")
                && report.contains("[PASS] Average paid satisfaction: required 60, actual 78.0"),
                "unchanged victory thresholds");
        passed++;
    }

    private static void chefAdviceWorksThroughParser() throws IOException, InterruptedException {
        Result result = launch("restaurantrush.Main", "wait\n999\nwait\n");
        check(result.exit() == 0 && result.stderr().isEmpty(), "Chef retry then EOF succeeds");
        check(result.stdout().contains("does not exist; use 'wait' to Wait")
                && !result.stdout().contains("use 0") && !result.stdout().contains("Invalid Chef command"),
                "Chef's suggested command is accepted by the real parser");
        check(result.stdout().contains("-- End of turn 1 --")
                && result.stdout().contains("Input ended during turn 2, Waiter"), "retry completes only the current phase");
        noVerdict(result);
        passed++;
    }

    private record Result(int exit, String stdout, String stderr) { }

    private static Result launch(String mainClass, String input, String... args)
            throws IOException, InterruptedException {
        List<String> command = new ArrayList<>(List.of(
                Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-ea", "-cp", System.getProperty("java.class.path"), mainClass));
        command.addAll(List.of(args));
        int id = ++processes;
        Path stdout = output.resolve(id + ".stdout");
        Path stderr = output.resolve(id + ".stderr");
        Process process = new ProcessBuilder(command).redirectOutput(stdout.toFile())
                .redirectError(stderr.toFile()).start();
        try {
            try (var stdin = process.getOutputStream()) {
                stdin.write(input.getBytes(StandardCharsets.UTF_8));
            }
            check(process.waitFor(15, TimeUnit.SECONDS), "subprocess timed out: " + mainClass);
            return new Result(process.exitValue(), Files.readString(stdout), Files.readString(stderr));
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    private static void expectedError(Result result, int status, String message) {
        check(result.exit() == status && result.stderr().contains(message), "expected status " + status + ": " + result);
        conciseError(result);
    }

    private static void conciseError(Result result) {
        check(result.stderr().lines().count() == 1 && !result.stderr().contains("Exception")
                && !result.stderr().contains("\tat "), "expected error has one concise diagnostic");
    }

    private static void noVerdict(Result result) {
        check(!result.stdout().contains("===== VICTORY =====") && !result.stdout().contains("===== DEFEAT ====="),
                "unfinished game must not display a verdict");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError("LauncherTests: " + message);
        }
    }

    /** Runs Main in a child JVM with a broken stream. */
    public static final class FailingInput {
        private FailingInput() { }

        public static void main(String[] args) {
            if (args[0].equals("invalid-path")) {
                Main.main(new String[] {"--script", "invalid\0path"});
                return;
            }
            System.setIn(new InputStream() {
                @Override public int read() throws IOException {
                    if (args[0].equals("io")) {
                        throw new IOException("injected read failure");
                    }
                    throw new IllegalStateException("injected programming failure");
                }
            });
            Main.main(new String[0]);
        }
    }
}
