# Stage 1 - Chat 01: Control test cases

- **Member:** Victor
- **AI tool:** Claude Code (Anthropic Claude)
- **Date:** 11 Nov 2026
- **Format:** Conversation excerpts documenting test development, with original wording, shortened tool outputs and personal-data placeholders.

---

## Prompt 1  <sub>2026-11-11 11:20</sub>

for the java, please write test cases for it. i need show evidence of it shown here 

**[Pasted text]**

GitHub evidence for the group and each member

08

Group evidence
 
Runnable stage tags and comparisons between stages.

Individual
evidence

Each member’s AI chats in their own branch.

Example folder on your personal branch

ai-history/stage-1/chat-01.md

ai-history/stage-2/chat-01.md

ai-history/stage-3/chat-01.md

**[End of pasted text]**

 for the files i have pushed

### AI reply 1

<details><summary><b>Action (Bash):</b> Inspect original project for tests and build setup</summary>

Input:

```
cd ~/Documents/OOP/stage-1; ls -a; find . -maxdepth 3 -not -path '*/.git*' | head -40; find . -path '*test*' -name '*.java' | head -30; cat "~/Documents/new oop/OOP-project/stage 1/src/main/java/restaurantrush/control/WaiterAction.java"; which java javac mvn gradle; java -version 2>&1 | head -1
```

Output:

```
.
..
BASELINE_SHA256.txt
EVIDENCE.md
FIXPASS_SHA256.txt
IMPLEMENTATION_CONTRACT.md
PLAN.md
README.md
REMEDIATED_SHA256.txt
SOURCE_GUIDE.md
TURN_TESTS.md
VICTOR_NEXT_STEPS.md
issues
src
test-fixtures
uml
.
./SOURCE_GUIDE.md
./uml
./FIXPASS_SHA256.txt
./EVIDENCE.md
./IMPLEMENTATION_CONTRACT.md
./REMEDIATED_SHA256.txt
./README.md
./BASELINE_SHA256.txt
./test-fixtures
./TURN_TESTS.md
./PLAN.md
./VICTOR_NEXT_STEPS.md
./issues
./src
./uml/README.md
./uml/rendered
./uml/src
./test-fixtures/victory-invalid-input.txt
./test-fixtures/README.md
./test-fixtures/defeat-input.txt
./test-fixtures/victory-input.txt
./issues/02-staff-orders.md
./issues/03-controller.md
./issues/07-staff-boundary.md
./issues/09-build-docs.md
./issues/REMEDIATION_PLAN.md
./issues/README.md
./issues/05-integration.md
./issues/06-domain-boundary.md
./issues/01-domain.md
./issues/04-cli-replays.md
./issues/08-controller-guidance.md
./src/Waiter.java
./src/test
./src/Customer.java
./src/Personnel.java
./src/Game.java
./src/Restaurant.java
./src/main
./src/test/java/restaurantrush/AllTests.java
./src/test/java/restaurantrush/ScriptedGameIOTests.java
./src/test/java/restaurantrush/ReplayTests.java
./src/test/java/restaurantrush/Check.java
./src/test/java/restaurantrush/TestGameIO.java
./src/test/java/restaurantrush/TestGameIOTests.java
./src/test/java/restaurantrush/ConsoleTests.java
./src/test/java/restaurantrush/TurnTests.java
./src/test/java/restaurantrush/LauncherTests.java
./src/test/java/restaurantrush/ScriptedGameIO.java
./src/test/java/restaurantrush/domain/RestaurantTests.java
./src/test/java/restaurantrush/domain/OrderTests.java
./src/test/java/restaurantrush/domain/CustomerTests.java
./src/test/java/restaurantrush/domain/StaffTests.java
package restaurantrush.control;

/** One waiter action per turn. SEAT and ORDER use a customer ID;
 * SERVE uses an order ID (same as the customer ID). WAIT uses 0. */
public record WaiterAction(Type type, int targetId) {
    public enum Type { SEAT, ORDER, SERVE,
… [output shortened: 1074 more characters]
```

</details>

<details><summary><b>Action (Bash):</b> Look for a JDK and read test harness</summary>

Input:

```
ls /Library/Java/JavaVirtualMachines ~/Library/Java/JavaVirtualMachines /opt/homebrew/opt 2>/dev/null | grep -i -E 'jdk|java|temurin|openjdk'; ls ~/.sdkman/candidates/java 2>/dev/null; cd ~/Documents/OOP/stage-1/src/test/java/restaurantrush; wc -l *.java; cat Check.java AllTests.java TestGameIO.java
```

Output:

```
/Library/Java/JavaVirtualMachines:
openjdk@17
      70 AllTests.java
      41 Check.java
     224 ConsoleTests.java
     191 LauncherTests.java
     113 ReplayTests.java
     189 ScriptedGameIO.java
      71 ScriptedGameIOTests.java
     159 TestGameIO.java
     103 TestGameIOTests.java
     409 TurnTests.java
    1570 total
package restaurantrush;

import java.util.Objects;

/** Assertion helpers that work whether or not -ea is enabled. */
public final class Check {

    private Check() {
    }

    public static void isTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static void equal(Object expected, Object actual, String message) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(message + ": expected <" + expected + "> but was <" + actual + ">");
        }
    }

    public static void contains(String haystack, String needle, String message) {
        if (haystack == null || !haystack.contains(needle)) {
            throw new AssertionError(message + ": missing <" + needle + "> in:\n" + haystack);
        }
    }

    public static <T extends Throwable> T throwsType(Class<T> type, Runnable body, String message) {
        try {
            body.run();
        } catch (Throwable thrown) {
            if (type.isInstance(thrown)) {
                return type.cast(thrown);
            }
            throw new AssertionError(message + ": expected " + type.getSimpleName()
                    + " but got " + thrown, thrown);
        }
        throw new AssertionError(message + ": expected " + type.getSimpleName() + " but nothing was thrown");
    }
}
package restaurantrush;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Plain standard-library test runner for Stage 1:
 *   java -ea -cp build/stage-1/test-classes restaurantrush.AllTests
 *
 * Registers parser/harness/replay checks and wraps th
… [output shortened: 7132 more characters]
```

</details>

[Mixed JDK/transcript lookup omitted.]

<details><summary><b>Action (Bash):</b> Search for an installed JDK</summary>

Input:

```
ls -d /opt/homebrew/opt/openjdk* /opt/homebrew/Cellar/openjdk* /usr/local/opt/openjdk* 2>/dev/null; find /opt/homebrew /usr/local ~ -maxdepth 6 -name javac -type f 2>/dev/null | head -5; /usr/libexec/java_home -V 2>&1 | head
```

Output:

```
(eval):1: no matches found: /usr/local/opt/openjdk*
The operation couldn’t be completed. Unable to locate a Java Runtime.
Please visit http://www.java.com for information on installing Java.
```

</details>

<details><summary><b>Action (Bash):</b> Check Homebrew for OpenJDK</summary>

Input:

```
ls -d /opt/homebrew/opt/openjdk* /opt/homebrew/Cellar/openjdk* 2>&1; ls /opt/homebrew/opt/openjdk@17/bin 2>&1 | head; which brew
```

Output:

```
/opt/homebrew/Cellar/openjdk@17
/opt/homebrew/opt/openjdk@17
jar
jarsigner
java
javac
javadoc
javap
jcmd
jconsole
jdb
jdeprscan
/opt/homebrew/bin/brew
```

</details>

Found JDK 17 under Homebrew. Next I'll check `InputEndedException`, then write tests for the four control classes that compile on their own.

<details><summary><b>Action (Bash):</b> Read InputEndedException source</summary>

Input:

```
cat "~/Documents/new oop/OOP-project/stage 1/src/main/java/restaurantrush/control/InputEndedException.java"
```

Output:

```
package restaurantrush.control;

/** Input ended before the player made a choice. */
public class InputEndedException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public InputEndedException(String message) {
        super(message);
    }
}
```

</details>

<details><summary><b>Action (Write):</b> Wrote <code>~/Documents/new oop/OOP-project/stage 1/src/test/java/restaurantrush/control/ControlTests.java</code></summary>

Input:

```
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
                seatable, List.of(), List.of(), List.of(), "no eligible paym
… [output shortened: 6923 more characters]
```

Output:

```
File created successfully at: ~/Documents/new oop/OOP-project/stage 1/src/test/java/restaurantrush/control/ControlTests.java (file state is current in your context — no need to Read it back)
```

</details>

<details><summary><b>Action (Bash):</b> Compile and run the new tests with JDK 17</summary>

Input:

```
cd "~/Documents/new oop/OOP-project/stage 1"; J=/opt/homebrew/opt/openjdk@17/bin; rm -rf <scratchpad>/tc; $J/javac -d <scratchpad>/tc src/main/java/restaurantrush/control/{WaiterAction,InputEndedException,GameView,GameIO}.java src/test/java/restaurantrush/control/ControlTests.java && $J/java -cp <scratchpad>/tc restaurantrush.control.ControlTests
```

Output:

```
PASS WaiterAction.seat targets the customer
PASS WaiterAction.order targets the customer
PASS WaiterAction.serve targets the order
PASS WaiterAction.waitAction uses target 0
PASS WaiterAction rejects a null type
PASS WaiterAction rejects WAIT with a non-zero target
PASS WaiterAction rejects zero target for SEAT, ORDER and SERVE
PASS WaiterAction rejects negative targets
PASS WaiterAction has value equality
PASS WaiterAction.Type has exactly four actions
PASS InputEndedException keeps its message
PASS InputEndedException is unchecked
PASS CustomerLine stores its fields
PASS TableLine allows a null occupant for a free table
PASS OrderLine stores progress and bill
PASS TurnState copies its lists so later changes do not leak in
PASS TurnState lists are unmodifiable
PASS TurnState rejects null lists
PASS TurnState keeps scalar values
PASS TurnSummary stores running totals
PASS FinalReport copies and locks its targets
PASS TargetLine records whether a target was met
PASS GameIO display hooks default to doing nothing
PASS GameIO implementations signal EOF with InputEndedException
PASS GameIO chef choice 0 means Wait
25 passed, 0 failed, 25 total
```

</details>

All 25 tests pass.
