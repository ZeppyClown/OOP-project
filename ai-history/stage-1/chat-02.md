# Stage 1 - Chat 02: Customer, Restaurant and turn-loop test cases

- **Member:** Victor
- **AI tool:** Claude Code (Anthropic Claude)
- **Date:** 12 Nov 2026
- **Format:** Conversation excerpts documenting test development, with original wording, shortened tool outputs and personal-data placeholders.

---

## Prompt 1  <sub>2026-11-12 09:56</sub>

**[Pasted text]**

Individual
evidence
Each member’s AI chats in their own branch.
Example folder on your personal branch
ai-history/stage-1/chat-01.md
ai-history/stage-2/chat-01.md
ai-history/stage-3/chat-01.md
README: link to every stage and every member’s AI history.

**[End of pasted text]**


write test cases for the code in restaurantrush. put it in victor's branch

### AI reply 1

[Repository setup and branch inspection omitted.]

<details><summary><b>Action (Bash):</b> Read domain and control source files</summary>

Input:

```
cd "<scratchpad>/victor-wt/stage 1/src/main/java/restaurantrush" && for f in domain/*.java control/*.java; do echo "=== $f"; cat "$f"; done
```

Output:

```
<persisted-output>
Output too large (34.7KB). Full output saved to: ~/.claude/projects/-Users-USER-Documents-new-oop/d5b69ef9-aed1-40d8-9c04-c9b8f7cb718a/tool-results/bhk1a24qd.txt

Preview (first 2KB):
=== domain/Cashier.java
package restaurantrush.domain;

public final class Cashier extends Staff {
    public Cashier() { super("Cashier"); }
    String collect(Customer customer,int turn){
        String reason=availabilityReason(turn);
        if (reason!=null)return reason;
        if (customer==null)return "Customer is required";
        reason=customer.pay(turn);
        if (reason==null)markTaskUsed(turn);
        return reason;
    }
}
=== domain/Chef.java
package restaurantrush.domain;

public final class Chef extends Staff{
    public Chef(){super("Chef");}
    public String prepare(Order order,int turn){
        String reason=availabilityReason(turn);
        if(reason!=null)return reason;
        if(order==null)return "Order is required";
        reason=order.advancePreparation(turn);
        if(reason==null)markTaskUsed(turn);
        return reason;
    }
}
=== domain/Customer.java
package restaurantrush.domain;

import java.util.List;

public abstract class Customer {
    private final int id;
    private Restaurant restaurant;
    private int satisfaction = 100;
    private CustomerStatus status = CustomerStatus.WAITING;
    private Order order;
    private int servedTurn = -1;
    private int readyToPayTurn = -1;
    private int satisfactionAtPayment = -1;

    protected Customer(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("Customer ID must be positive: " + id);
        }
        this.id = id;
    }

    public final int getId(){
        return id;
    }

    public final int getSatisfaction(){
        return satisfaction;
    }

    public final CustomerStatus getStatus(){
        return status;
    }

    public final Order getOrder(){
        return order;
    }

    public final int getServe
… [output shortened: 299 more characters]
```

</details>

Reading the RestaurantRush source and existing tests to find gaps before writing new ones.

<details><summary><b>Action (Read):</b> Read <code>~/.claude/projects/-Users-USER-Documents-new-oop/d5b69ef9-aed1-40d8-9c04-c9b8f7cb718a/tool-results/bhk1a24qd.txt</code></summary>

Output:

```
1	=== domain/Cashier.java
2	package restaurantrush.domain;
3	
4	public final class Cashier extends Staff {
5	    public Cashier() { super("Cashier"); }
6	    String collect(Customer customer,int turn){
7	        String reason=availabilityReason(turn);
8	        if (reason!=null)return reason;
9	        if (customer==null)return "Customer is required";
10	        reason=customer.pay(turn);
11	        if (reason==null)markTaskUsed(turn);
12	        return reason;
13	    }
14	}
15	=== domain/Chef.java
16	package restaurantrush.domain;
17	
18	public final class Chef extends Staff{
19	    public Chef(){super("Chef");}
20	    public String prepare(Order order,int turn){
21	        String reason=availabilityReason(turn);
22	        if(reason!=null)return reason;
23	        if(order==null)return "Order is required";
24	        reason=order.advancePreparation(turn);
25	        if(reason==null)markTaskUsed(turn);
26	        return reason;
27	    }
28	}
29	=== domain/Customer.java
30	package restaurantrush.domain;
31	
32	import java.util.List;
33	
34	public abstract class Customer {
35	    private final int id;
36	    private Restaurant restaurant;
37	    private int satisfaction = 100;
38	    private CustomerStatus status = CustomerStatus.WAITING;
39	    private Order order;
40	    private int servedTurn = -1;
41	    private int readyToPayTurn = -1;
42	    private int satisfactionAtPayment = -1;
43	
44	    protected Customer(int id) {
45	        if (id <= 0) {
46	            throw new IllegalArgumentException("Customer ID must be positive: " + id);
47	        }
48	        this.id = id;
49	    }
50	
51	    public final int getId(){
52	        return id;
53	    }
54	
55	    public final int getSatisfaction(){
56	        return satisfaction;
57	    }
58	
59	    public final CustomerStatus getStatus(){
60	        return status;
61	    }
62	
63	    public final Order getOrder(){
64	        return order;
65	    }
66	
67	    public final int getServedTurn(){
68	        return served
… [output shortened: 37507 more characters]
```

</details>

<details><summary><b>Action (Bash):</b> Read test harness and list existing tests <i>(returned an error)</i></summary>

Input:

```
cd "<scratchpad>/victor-wt/stage 1/src/test/java/restaurantrush" && cat AllTests.java Check.java; cat domain/OrderTests.java | head -80; grep -n "static void\|check(\|name" domain/StaffTests.java | head -60; grep -n "private static void\|void test" control/ControlTests.java domain/OrderTests.java ConsoleTests.java LauncherTests.java ReplayTests.java; ls ../../../../ ; ls ../../../../../scripts 2>/dev/null; cat ../../../../../scripts/*.sh 2>/dev/null
```

Output:

```
Exit code 1
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
               
… [output shortened: 8039 more characters]
```

</details>

AllTests references `CustomerTests`, `RestaurantTests`, and `TurnTests`, but none of those files exist, so the suite can't compile. Checking whether they're on any other branch.

<details><summary><b>Action (Bash):</b> Search branches for missing test files</summary>

Input:

```
cd "~/Documents/new oop/OOP-project"; git fetch -q origin; for b in origin/main origin/Abraham origin/Danielle origin/Talon origin/Victor; do echo "== $b"; git ls-tree -r --name-only $b | grep -iE "test|ai-history|README" ; done; git log --all --oneline -- "*CustomerTests*" "*RestaurantTests*" "*TurnTests*"
```

Output:

```
== origin/main
stage 1/src/test/java/restaurantrush/AllTests.java
stage 1/src/test/java/restaurantrush/Check.java
stage 1/src/test/java/restaurantrush/ConsoleTests.java
stage 1/src/test/java/restaurantrush/LauncherTests.java
stage 1/src/test/java/restaurantrush/ReplayTests.java
stage 1/src/test/java/restaurantrush/ScriptedGameIO.java
stage 1/src/test/java/restaurantrush/ScriptedGameIOTests.java
stage 1/src/test/java/restaurantrush/TestGameIO.java
stage 1/src/test/java/restaurantrush/TestGameIOTests.java
stage 1/src/test/java/restaurantrush/control/ControlTests.java
stage 1/src/test/java/restaurantrush/domain/OrderTests.java
stage 1/src/test/java/restaurantrush/domain/StaffTests.java
stage 1/test-fixtures/README.md
stage 1/test-fixtures/defeat-input.txt
stage 1/test-fixtures/victory-input.txt
stage 1/test-fixtures/victory-invalid-input.txt
== origin/Abraham
README.md
stage 1/src/test/java/restaurantrush/domain/OrderTests.java
stage 1/src/test/java/restaurantrush/domain/StaffTests.java
== origin/Danielle
== origin/Talon
stage 1/run-tests.sh
stage 1/src/test/java/restaurantrush/AllTests.java
stage 1/src/test/java/restaurantrush/Check.java
stage 1/src/test/java/restaurantrush/ConsoleTests.java
stage 1/src/test/java/restaurantrush/LauncherTests.java
stage 1/src/test/java/restaurantrush/ReplayTests.java
stage 1/src/test/java/restaurantrush/ScriptedGameIO.java
stage 1/src/test/java/restaurantrush/ScriptedGameIOTests.java
stage 1/src/test/java/restaurantrush/TestGameIO.java
stage 1/src/test/java/restaurantrush/TestGameIOTests.java
stage 1/src/test/java/restaurantrush/control/ControlTests.java
stage 1/src/test/java/restaurantrush/domain/OrderTests.java
stage 1/src/test/java/restaurantrush/domain/StaffTests.java
stage 1/test-fixtures/README.md
stage 1/test-fixtures/defeat-input.txt
stage 1/test-fixtures/victory-input.txt
stage 1/test-fixtures/victory-invalid-input.txt
== origin/Victor
stage 1/src/test/java/restaurantrush/AllTests.java
stage 1/src/test/java/restaurantrush/Chec
… [output shortened: 743 more characters]
```

</details>

<details><summary><b>Action (Bash):</b> Read test runner, TestGameIO, and sample tests <i>(returned an error)</i></summary>

Input:

```
cd "~/Documents/new oop/OOP-project"; git show "origin/Talon:stage 1/run-tests.sh"; W="<scratchpad>/victor-wt/stage 1/src/test/java/restaurantrush"; cat "$W/TestGameIO.java"; sed -n 1,60p "$W/ReplayTests.java"; sed -n 1,80p "$W/domain/StaffTests.java"; java -version 2>&1
```

Output:

```
Exit code 1
#!/bin/sh
# Compile Stage 1 into a temp dir and run every registered check.
# Usage (from anywhere): sh "stage 1/run-tests.sh"
set -eu
STAGE=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
ROOT=$(dirname -- "$STAGE")
OUT=$(mktemp -d "${TMPDIR:-/tmp}/stage1-tests.XXXXXX")
trap 'rm -rf "$OUT"' EXIT

# Some domain classes currently live in the sibling "stage-1" folder; include it until they move.
MAIN_DIRS="$STAGE/src/main/java"
[ -d "$ROOT/stage-1/src/main/java" ] && MAIN_DIRS="$MAIN_DIRS
$ROOT/stage-1/src/main/java"

printf '%s\n' "$MAIN_DIRS" | while IFS= read -r dir; do
    find "$dir" -name '*.java' -type f
done | sed 's/.*/"&"/' > "$OUT/main.txt"
javac --release 17 -d "$OUT/main" "@$OUT/main.txt"

find "$STAGE/src/test/java" -name '*.java' -type f | sed 's/.*/"&"/' > "$OUT/test.txt"
javac --release 17 -cp "$OUT/main" -d "$OUT/test" "@$OUT/test.txt"

java -ea -Dstage1.fixtures="$STAGE/test-fixtures" -cp "$OUT/main:$OUT/test" restaurantrush.AllTests
package restaurantrush;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

import restaurantrush.control.GameIO;
import restaurantrush.control.GameView;
import restaurantrush.control.InputEndedException;
import restaurantrush.control.WaiterAction;

/**
 * Fake GameIO for turn tests. Plays back preset Waiter and Chef choices,
 * records every call, and throws InputEndedException when it runs out.
 */
public final class TestGameIO implements GameIO {
    private final Deque<WaiterAction> waiterChoices = new ArrayDeque<>();
    private final Deque<Integer> chefChoices = new ArrayDeque<>();
    private final List<String> calls = new ArrayList<>();
    private final List<String> invalidMessages = new ArrayList<>();
    private final List<GameView.TurnState> turnStarts = new ArrayList<>();
    private final List<GameView.TurnState> actionStates = new ArrayList<>();
    private final List<GameView.TurnSummary> turnEnds = new ArrayList
… [output shortened: 8039 more characters]
```

</details>

No Java on PATH. Looking for an installed JDK.

<details><summary><b>Action (Bash):</b> Search for an installed JDK</summary>

Input:

```
ls /Library/Java/JavaVirtualMachines/ 2>&1; ls /opt/homebrew/opt 2>/dev/null | grep -i -E "jdk|java"; ls ~/.sdkman/candidates/java 2>/dev/null; which brew; mdfind -name javac 2>/dev/null | head
```

Output:

```
openjdk@17
/opt/homebrew/bin/brew
~/Library/Mobile Documents/com~apple~CloudDocs/Downloads/CBCT Sam 3/data/jre_x64/bin/javacpl.exe
~/Library/Mobile Documents/com~apple~CloudDocs/Downloads/CBCT Sam 3/data/jre_x64/bin/javacpl.cpl
~/Library/Mobile Documents/com~apple~CloudDocs/Downloads/CBCT Sam/data/jre_x86/bin/javacpl.exe
~/Library/Mobile Documents/com~apple~CloudDocs/Downloads/CBCT Sam/data/jre_x86/bin/javacpl.cpl
~/Library/Mobile Documents/com~apple~CloudDocs/Downloads/CBCT Sam 3/data/jre_x86/bin/javacpl.exe
~/Documents/entreprenuer/CA2/deliverables/slides/node_modules/monaco-editor/min/vs/java-CI4ZMsH9.js
~/Documents/Coco-Car-Damage-Dataset-worktrees/rag-slides/node_modules/monaco-editor/min/vs/java-CI4ZMsH9.js
~/Documents/Coco-Car-Damage-Dataset/slides/node_modules/monaco-editor/min/vs/java-CI4ZMsH9.js
~/Documents/Research Slides/rubric-generation/node_modules/monaco-editor/min/vs/java-CI4ZMsH9.js
~/Documents/anderdingus6767/slides/presentation/node_modules/monaco-editor/min/vs/java-CI4ZMsH9.js
```

</details>

Found Homebrew's openjdk@17. Reading the rest of the test helpers.

<details><summary><b>Action (Bash):</b> Read TestGameIO accessors and ReplayTests</summary>

Input:

```
W="<scratchpad>/victor-wt/stage 1/src/test/java/restaurantrush"; sed -n 125,160p "$W/TestGameIO.java"; cat "$W/ReplayTests.java"; sed -n 1,40p "$W/TestGameIOTests.java"
```

Output:

```
public List<GameView.TurnSummary> turnEnds() {
        return Collections.unmodifiableList(turnEnds);
    }

    public List<GameView.FinalReport> finalReports() {
        return Collections.unmodifiableList(finalReports);
    }

    public List<String> interruptions() {
        return Collections.unmodifiableList(interruptions);
    }

    /** All GameIO calls, in order. */
    public List<String> calls() {
        return Collections.unmodifiableList(calls);
    }

    public List<String> invalidMessages() {
        return Collections.unmodifiableList(invalidMessages);
    }

    public int waiterRequests() {
        return waiterRequests;
    }

    public int chefRequests() {
        return chefRequests;
    }

    /** True if every scripted choice was used. */
    public boolean allChoicesUsed() {
        return waiterChoices.isEmpty() && chefChoices.isEmpty();
    }
}
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
        r.add("Replay: EOF interrupts without final result", ReplayTests::interrupted);
    }

    private static Restaurant restaurant() {
        return new Restaurant(new Menu(), new Waiter(), new Chef(), new Cashier());
    }

    private r
… [output shortened: 5971 more characters]
```

</details>

<details><summary><b>Action (Bash):</b> Read ControlTests entry and ConsoleTests helpers</summary>

Input:

```
W="<scratchpad>/victor-wt/stage 1/src/test/java/restaurantrush"; sed -n 1,53p "$W/control/ControlTests.java"; grep -n "fixture(" "$W/ConsoleTests.java" | head -3; sed -n 1,42p "$W/ConsoleTests.java"
```

Output:

```
package restaurantrush.control;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

/**
 * Tests for WaiterAction, InputEndedException, GameView and GameIO.
 * Run from "stage 1":
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

163:            List<String> lines = fixture(name);
168:        ScriptedGameIO victory = ScriptedGameIO.alternating(fixture("victory-input.txt"));
181:        String all = Strin
… [output shortened: 2065 more characters]
```

</details>

---

### Test implementation and corrections (from AI reply 3)

<details><summary><b>Action (Write):</b> Wrote <code><scratchpad>/victor-wt/stage 1/src/test/java/restaurantrush/domain/CustomerTests.java</code></summary>

Input:

```
package restaurantrush.domain;

import java.util.Objects;

/**
 * Customer and Table: lifecycle guards, satisfaction loss, departure and
 * the waitingLossPerTurn() polymorphic hook.
 */
public final class CustomerTests {
    private CustomerTests() {
    }

    public static void main(String[] args) {
        runAll();
        System.out.println("CustomerTests passed");
    }

    public static void runAll() {
        constructionAndDefaults();
        menuChoiceRotatesById();
        fullLifecycleHappyPath();
        seatingGuards();
        orderingGuards();
        servingGuards();
        eatingAndPaymentTiming();
        satisfactionLossAndDeparture();
        departureCancelsUnfinishedOrder();
        servedCustomersDoNotLoseSatisfaction();
        subclassControlsLossRate();
        bindingToOneRestaurant();
        tableSeatAndRelease();
    }

    private static Menu menu = new Menu();

    private static void constructionAndDefaults() {
        illegal(() -> new RegularCustomer(0));
        illegal(() -> new RegularCustomer(-3));
        Customer c = new RegularCustomer(7);
        equal(7, c.getId(), "id");
        equal(100, c.getSatisfaction(), "starts at 100");
        equal(CustomerStatus.WAITING, c.getStatus(), "starts WAITING");
        check(c.getOrder() == null, "no order yet");
        equal(-1, c.getServedTurn(), "not served");
        equal(-1, c.getReadyToPayTurn(), "not ready");
        equal(-1, c.getSatisfactionAtPayment(), "not paid");
        equal("Regular", c.getTypeLabel(), "type label");
        equal(8, c.waitingLossPerTurn(), "regular loss rate");
        check(c.isWaitingUnserved(), "WAITING counts as unserved");
    }

    private static void menuChoiceRotatesById() {
        String[] expected = {"Burger", "Salad", "Pasta", "Burger", "Salad", "Pasta"};
        for (int id = 1; id <= expected.length; id++) {
            equal(expected[id - 1], new RegularCustomer(id).chooseItem(menu).getName(), "C" + id + " choice");
        }
        illegal(() -> new RegularCustomer(1).chooseItem(null));
    }

    private static void fullLifecycleHappyPath() {
        Customer c = new RegularCustomer(1);
        MenuItem burger = c.chooseItem(menu);
        ok(c.seat());
        equal(CustomerStatus.SEATED, c.getStatus(), "seated");
        ok(c.placeOrder(burger, burger.getPriceCents(), 2));
        equal(CustomerStatus.WAITING_FOR_FOOD, c.getStatus(), "waiting for food");
        Order order = c.getOrder();
        equal(1, order.getId(), "order id equals customer id");
        check(order.getCustomer() == c, "order points back to customer");
        equal(1500, order.getBillCents(), "bill");
        ok(order.advancePreparation(2));
        ok(c.serve(3));
        equal(CustomerStatus.SERVED, c.getStatus(), "served");
        equal(3, c.getServedTurn(), "served turn");
        check(!c.isWaitingUnserved(), "SERVED is not unserved");
        ok(c.finishEating(4));
        equal(CustomerStatus.READY_TO_PAY, c.getStatus(), "ready");
        equal(4, c.getReadyToPayTurn(), "ready turn");
        ok(c.pay(5));
        equal(CustomerStatus.PAID, c.getStatus(), "paid");
        equal(OrderStatus.PAID, order.getStatus(), "order paid");
        equal(100, c.getSatisfactionAtPayment(), "satisfaction frozen at payment");
    }

    private static void seatingGuards() {
        Customer c = new RegularCustomer(1);
        ok(c.seat());
        refused(c.seat());
        equal(CustomerStatus.SEATED, c.getStatus(), "double seat keeps status");
    }

    private static void orderingGuards() {
        MenuItem burger = menu.getItems().get(0);
        Customer waiting = new RegularCustomer(1);
        refused(waiting.placeOrder(burger, 1500, 1));
        check(waiting.getOrder() == null && waiting.getStatus() == CustomerStatus.WAITING, "unseated cannot order");

        Customer c = new RegularCustomer(2);
        ok(c.seat());
        refused(c.placeOrder(null, 1500, 1));
        refused(c.placeOrder(burger, 1500, 0));
        refused(c.placeOrder(burger, -1, 1));
        check(c.getOrder() == null && c.getStatus() == CustomerStatus.SEATED, "bad orders leave customer SEATED");
        ok(c.placeOrder(burger, 0, 1));
        equal(0, c.getOrder().getBillCents(), "zero bill allowed");
        refused(c.placeOrder(burger, 1500, 2));
        equal(0, c.getOrder().getBillCents(), "second order does not replace the first");
    }

    private static void servingGuards() {
        Customer seated = new RegularCustomer(1);
        ok(seated.seat());
        refused(seated.serve(2));

        Customer c = orderedCustomer(2, 2);
        refused(c.serve(3));
        equal(CustomerStatus.WAITING_FOR_FOOD, c.getStatus(), "unprepared food cannot be served");
        ok(c.getOrder().advancePreparation(3));
        refused(c.serve(3));
        equal(CustomerStatus.WAITING_FOR_FOOD, c.getStatus(), "cannot serve on the ready turn");
        ok(c.serve(4));
        refused(c.serve(5));
        equal(4, c.getServedTurn(), "double serve keeps the first turn");
    }

    private static void eatingAndPaymentTiming() {
        Customer c = orderedCustomer(1, 1);
        refused(c.finishEating(2));
        ok(c.getOrder().advancePreparation(1));
        ok(c.serve(2));
        refused(c.pay(3));
        refused(c.finishEating(2));
        equal(CustomerStatus.SERVED, c.getStatus(), "cannot finish on serving turn");
        ok(c.finishEating(3));
        refused(c.finishEating(4));
        refused(c.pay(3));
        equal(CustomerStatus.READY_TO_PAY, c.getStatus(), "cannot pay on ready turn");
        ok(c.pay(4));
        refused(c.pay(5));
        equal(OrderStatus.PAID, c.getOrder().getStatus(), "pays once");
    }

    private static void satisfactionLossAndDeparture() {
        Customer c = new RegularCustomer(1);
        for (int i = 1; i <= 12; i++) {
            c.loseWaitingSatisfaction();
            equal(100 - 8 * i, c.getSatisfaction(), "after " + i + " losses");
            equal(CustomerS
… [output shortened: 5637 more characters]
```

Output:

```
File created successfully at: <scratchpad>/victor-wt/stage 1/src/test/java/restaurantrush/domain/CustomerTests.java (file state is current in your context — no need to Read it back)
```

</details>

<details><summary><b>Action (Write):</b> Wrote <code><scratchpad>/victor-wt/stage 1/src/test/java/restaurantrush/domain/RestaurantTests.java</code></summary>

Input:

```
package restaurantrush.domain;

import java.util.List;
import java.util.Objects;

/**
 * Restaurant: arrivals, lookups, seating through the queue, departures,
 * eating/payment ordering, totals and the victory rule.
 */
public final class RestaurantTests {
    private RestaurantTests() {
    }

    public static void main(String[] args) {
        runAll();
        System.out.println("RestaurantTests passed");
    }

    public static void runAll() {
        constructionGuards();
        startsEmptyWithTwoTables();
        arrivalGuards();
        lookups();
        exposedCollectionsAreReadOnly();
        seatingGuards();
        departureLeavesQueueAndFreesTable();
        paymentGuards();
        advanceEatingWaitsOneTurn();
        oldestEligiblePayerOrdering();
        totalsAndAverage();
        victoryThresholds();
    }

    private static void constructionGuards() {
        illegal(() -> new Restaurant(null, new Waiter(), new Chef(), new Cashier()));
        illegal(() -> new Restaurant(new Menu(), null, new Chef(), new Cashier()));
        illegal(() -> new Restaurant(new Menu(), new Waiter(), null, new Cashier()));
        illegal(() -> new Restaurant(new Menu(), new Waiter(), new Chef(), null));
    }

    private static void startsEmptyWithTwoTables() {
        Restaurant r = restaurant();
        equal(2, r.getTables().size(), "two tables");
        equal(1, r.getTables().get(0).getId(), "table 1");
        equal(2, r.getTables().get(1).getId(), "table 2");
        check(r.getTables().stream().allMatch(Table::isFree), "tables free");
        check(r.getCustomers().isEmpty() && r.getWaitingQueue().isEmpty(), "no customers");
        check(r.findFreeTable() == r.getTables().get(0), "first free table is table 1");
        equal(0, r.getRevenueCents(), "no revenue");
        equal(0, r.getPaidCount(), "no payers");
        equal(0, r.getServedCount(), "none served");
        equal(0, r.getUnhappyDepartureCount(), "no departures");
        check(r.getAveragePaidSatisfaction().isEmpty(), "average empty with no payers");
        equal(GameResult.DEFEAT, r.evaluateResult(), "empty restaurant is a defeat");
        check(r.oldestEligiblePayer(5) == null, "no payer");
    }

    private static void arrivalGuards() {
        Restaurant r = restaurant();
        illegal(() -> r.registerArrival(null));
        Customer c1 = new RegularCustomer(1);
        r.registerArrival(c1);
        try {
            r.registerArrival(new RegularCustomer(1));
            throw new AssertionError("duplicate id should throw");
        } catch (IllegalStateException expected) {
            // ok
        }
        Customer seated = new RegularCustomer(2);
        ok(seated.seat());
        illegal(() -> r.registerArrival(seated));
        equal(List.of(c1), r.getCustomers(), "rejected arrivals are not added");
        equal(List.of(c1), r.getWaitingQueue(), "rejected arrivals are not queued");

        Restaurant other = restaurant();
        try {
            other.registerArrival(c1);
            throw new AssertionError("customer bound elsewhere should throw");
        } catch (IllegalStateException expected) {
            // ok
        }
        check(other.getCustomers().isEmpty(), "other restaurant unchanged");
    }

    private static void lookups() {
        Restaurant r = restaurant();
        Customer c1 = arrive(r, 1);
        Customer c2 = arrive(r, 2);
        check(r.findCustomer(1) == c1 && r.findCustomer(2) == c2, "findCustomer");
        check(r.findCustomer(3) == null, "unknown customer");
        check(r.findWaitingCustomer(2) == c2, "waiting lookup");
        check(r.findOrder(1) == null, "no orders yet");
        check(r.findTableFor(c1) == null && r.findTableFor(null) == null, "no table yet");

        ok(r.seatCustomer(c1, 1));
        check(r.findWaitingCustomer(1) == null, "seated customer is no longer waiting");
        check(r.findTableFor(c1) == r.getTables().get(0), "C1 at table 1");
        check(r.findFreeTable() == r.getTables().get(1), "table 2 now first free");
        ok(r.getWaiter().takeOrder(c1, c1.chooseItem(r.getMenu()), 1500, 2));
        check(r.findOrder(1) == c1.getOrder(), "findOrder by id");
        check(r.findOrder(2) == null, "C2 has no order");
    }

    private static void exposedCollectionsAreReadOnly() {
        Restaurant r = restaurant();
        arrive(r, 1);
        unsupported(() -> r.getTables().clear());
        unsupported(() -> r.getCustomers().clear());
        unsupported(() -> r.getWaitingQueue().clear());
        List<Customer> snapshot = r.getWaitingQueue();
        arrive(r, 2);
        equal(1, snapshot.size(), "queue view is a snapshot");
        equal(2, r.getWaitingQueue().size(), "fresh view sees new arrival");
    }

    private static void seatingGuards() {
        Restaurant r = restaurant();
        Customer c1 = arrive(r, 1);
        Customer c2 = arrive(r, 2);
        Customer c3 = arrive(r, 3);
        refused(r.seatCustomer(null, 1));
        refused(r.seatCustomer(new RegularCustomer(1), 1));
        refused(r.seatCustomer(new RegularCustomer(9), 1));
        check(r.getWaiter().canAct(1), "lookup refusals do not use the Waiter");

        ok(r.seatCustomer(c1, 1));
        refused(r.seatCustomer(c1, 2));
        check(r.getWaiter().canAct(2), "re-seating a seated customer does not use the Waiter");
        ok(r.seatCustomer(c2, 2));
        refused(r.seatCustomer(c3, 3));
        equal(List.of(c3), r.getWaitingQueue(), "no free table keeps C3 queued");
        equal(CustomerStatus.WAITING, c3.getStatus(), "C3 still WAITING");
        check(r.getWaiter().canAct(3), "full restaurant does not use the Waiter");
    }

    private static void departureLeavesQueueAndFreesTable() {
        Restaurant r = restaurant();
        Customer c1 = arrive(r, 1);
        ok(r.seatCustomer(c1, 1));
        ok(r.getWaiter().takeOrder(c1, c1.chooseItem(r.getMenu()), 1500, 2));
        Customer c2 = arrive(r, 2);
        for (int i = 0; i < 1
… [output shortened: 9427 more characters]
```

Output:

```
File created successfully at: <scratchpad>/victor-wt/stage 1/src/test/java/restaurantrush/domain/RestaurantTests.java (file state is current in your context — no need to Read it back)
```

</details>

The reflection hacks in those helpers aren't needed. The package-private Customer methods do the job directly, so I'm rewriting the helpers without reflection.

<details><summary><b>Action (Bash):</b> Replace reflection helpers with public-API helpers</summary>

Input:

```
cd "<scratchpad>/victor-wt/stage 1/src/test/java/restaurantrush/domain" && python3 - <<'EOF'
p='RestaurantTests.java'
s=open(p).read()
start=s.index('    private static void oldestEligiblePayerOrdering()')
end=s.index('    private static void totalsAndAverage()')
s=s[:start]+'''    private static void oldestEligiblePayerOrdering() {
        Restaurant r = restaurant();
        Customer c1 = servedCustomer(r, 1, 1);
        Customer c2 = servedCustomer(r, 2, 2);
        r.advanceEating(5);
        check(c1.getReadyToPayTurn() == 5 && c2.getReadyToPayTurn() == 5, "both ready on turn 5");
        check(r.oldestEligiblePayer(6) == c1, "tie on ready turn goes to lower id");
        ok(r.collectPayment(c1, 6));
        check(r.oldestEligiblePayer(6) == c2, "C2 next");
        refused(r.collectPayment(c2, 6));
        check(c2.getStatus() == CustomerStatus.READY_TO_PAY, "Cashier used: C2 waits");
        ok(r.collectPayment(c2, 7));
        check(r.oldestEligiblePayer(8) == null, "everyone paid");

        Restaurant later = restaurant();
        Customer b2 = servedCustomer(later, 2, 1);
        later.advanceEating(4);
        Customer a1 = servedCustomer(later, 1, 2);
        later.advanceEating(5);
        check(b2.getReadyToPayTurn() == 4 && a1.getReadyToPayTurn() == 5, "different ready turns");
        check(later.oldestEligiblePayer(6) == b2, "earlier ready turn beats lower id");
    }

'''+s[end:]
start=s.index('    // ---- helpers ----')
end=s.index('    private static void illegal(')
s=s[:start]+'''    // ---- helpers ----

    private static Restaurant restaurant() {
        return new Restaurant(new Menu(), new Waiter(), new Chef(), new Cashier());
    }

    private static Customer arrive(Restaurant r, int id) {
        Customer c = new RegularCustomer(id);
        r.registerArrival(c);
        return c;
    }

    /** Seated by the Waiter on seatTurn, Burger ordered and cooked on seatTurn+1, served seatTurn+2. */
    private static Customer servedCustomer(Restaurant r, int id, int seatTurn) {
        Customer c = arrive(r, id);
        ok(r.seatCustomer(c, seatTurn));
        MenuItem burger = r.getMenu().getItems().get(0);
        ok(c.placeOrder(burger, burger.getPriceCents(), seatTurn + 1));
        ok(c.getOrder().advancePreparation(seatTurn + 1));
        ok(c.serve(seatTurn + 2));
        return c;
    }

    /** One paid customer per entry, frozen at that satisfaction (100 - 8k), paying the menu price. */
    private static Restaurant played(int[] satisfactions) {
        Restaurant r = restaurant();
        for (int i = 0; i < satisfactions.length; i++) {
            payAtSatisfaction(r, i + 1, satisfactions[i], -1);
        }
        return r;
    }

    /** Registers and pays customer id at the target satisfaction; bill -1 means the menu price. */
    private static void payAtSatisfaction(Restaurant r, int id, int target, int bill) {
        check((100 - target) % 8 == 0 && target > 0, "target reachable in 8-point steps");
        Customer c = arrive(r, id);
        ok(c.seat());
        for (int k = 0; k < (100 - target) / 8; k++) {
            c.loseWaitingSatisfaction();
        }
        MenuItem item = c.chooseItem(r.getMenu());
        ok(c.placeOrder(item, bill < 0 ? item.getPriceCents() : bill, 1));
        for (int turn = 1; c.getOrder().getStatus() != OrderStatus.READY; turn++) {
            ok(c.getOrder().advancePreparation(turn));
        }
        ok(c.serve(5));
        ok(c.finishEating(6));
        ok(c.pay(7));
        equal(target, c.getSatisfactionAtPayment(), "frozen satisfaction");
    }

'''+s[end:]
s=s.replace('"4 paid, $63.00 incl. pasta, average exactly 60 wins"','"4 paid, $57.00, average exactly 60 wins"')
open(p,'w').write(s)
EOF
grep -n "reflect" RestaurantTests.java CustomerTests.java; sed -i '' 's/private static Menu menu = new Menu();/private static final Menu menu = new Menu();/' CustomerTests.java
```

Output:

```
Shell cwd was reset to ~/Documents/new oop
[This command modified 2 files you've previously read: ../../../..<scratchpad>/victor-wt/stage 1/src/test/java/restaurantrush/domain/CustomerTests.java, ../../../..<scratchpad>/victor-wt/stage 1/src/test/java/restaurantrush/domain/RestaurantTests.java. Call Read before editing.]
```

</details>

Now the controller turn tests.

<details><summary><b>Action (Write):</b> Wrote <code><scratchpad>/victor-wt/stage 1/src/test/java/restaurantrush/TurnTests.java</code></summary>

Input:

```
package restaurantrush;

import java.util.List;

import restaurantrush.control.GameController;
import restaurantrush.control.GameIO;
import restaurantrush.control.GameView;
import restaurantrush.control.WaiterAction;
import restaurantrush.domain.Cashier;
import restaurantrush.domain.Chef;
import restaurantrush.domain.CustomerStatus;
import restaurantrush.domain.Menu;
import restaurantrush.domain.OrderStatus;
import restaurantrush.domain.Restaurant;
import restaurantrush.domain.Waiter;

/** GameController turn loop driven by TestGameIO: order of calls, retries, state lists and results. */
public final class TurnTests {
    private static final WaiterAction WAIT = WaiterAction.waitAction();

    private TurnTests() {
    }

    public static void main(String[] args) {
        runAll();
        System.out.println("TurnTests passed");
    }

    public static void runAll() {
        constructorRejectsNulls();
        callOrderPerTurn();
        arrivalsEveryThreeTurns();
        allWaitDefeatReport();
        victoryReportAndAutomaticPayment();
        turnStateListsFollowAvailability();
        waiterSemanticRetries();
        nullWaiterActionIsRetried();
        chefSemanticRetries();
        noFreeTableHidesSeating();
        departureFreesTableDuringGame();
        interruptionNamesTurnAndRole();
    }

    private static void constructorRejectsNulls() {
        Check.throwsType(NullPointerException.class, () -> new GameController(null, new TestGameIO()), "null restaurant");
        Check.throwsType(NullPointerException.class, () -> new GameController(restaurant(), null), "null io");
    }

    private static void callOrderPerTurn() {
        TestGameIO io = allWait();
        new GameController(restaurant(), io).run();
        List<String> perTurn = List.of("showTurnStart", "chooseWaiterAction", "showActionState",
                "chooseChefOrder", "showActionState", "showTurnEnd");
        List<String> calls = io.calls();
        Check.equal(18 * perTurn.size() + 1, calls.size(), "call count");
        for (int turn = 0; turn < 18; turn++) {
            Check.equal(perTurn, calls.subList(turn * 6, turn * 6 + 6), "calls on turn " + (turn + 1));
        }
        Check.equal("showFinalResult", calls.get(calls.size() - 1), "final call");
        Check.isTrue(io.allChoicesUsed(), "exactly one choice per role per turn");
        Check.equal(18, io.waiterRequests(), "waiter requests");
        Check.equal(18, io.chefRequests(), "chef requests");
        Check.isTrue(io.interruptions().isEmpty(), "not interrupted");
    }

    private static void arrivalsEveryThreeTurns() {
        TestGameIO io = allWait();
        Restaurant r = restaurant();
        new GameController(r, io).run();
        Check.equal(6, r.getCustomers().size(), "six arrivals in 18 turns");
        int[] arrivalTurns = {1, 4, 7, 10, 13, 16};
        for (int i = 0; i < arrivalTurns.length; i++) {
            int turn = arrivalTurns[i];
            GameView.TurnState start = io.turnStarts().get(turn - 1);
            Check.isTrue(start.waiting().stream().anyMatch(c -> c.id() == i + 1 && c.satisfaction() == 100),
                    "C" + (i + 1) + " arrives at 100 on turn " + turn);
            if (turn > 1) {
                Check.isTrue(io.turnStarts().get(turn - 2).waiting().stream().noneMatch(c -> c.id() == i + 1),
                        "C" + (i + 1) + " not present before turn " + turn);
            }
        }
        for (GameView.TurnState s : io.turnStarts()) {
            Check.equal(1, s.stage(), "stage 1");
            Check.equal(18, s.maxTurns(), "max turns");
        }
    }

    private static void allWaitDefeatReport() {
        TestGameIO io = allWait();
        new GameController(restaurant(), io).run();
        Check.equal(1, io.finalReports().size(), "one final report");
        GameView.FinalReport report = io.finalReports().get(0);
        Check.isTrue(!report.victory(), "all-wait is a defeat");
        Check.equal(18, report.totals().turn(), "totals at turn 18");
        Check.equal(List.of(
                new GameView.TargetLine("Paid customers", "4", "0", false),
                new GameView.TargetLine("Revenue", "$45.00", "$0.00", false),
                new GameView.TargetLine("Average paid satisfaction", "60", "N/A", false)),
                report.targets(), "defeat targets");
        for (int t = 0; t < 18; t++) {
            Check.equal("no eligible payment this turn", io.actionStates().get(t * 2 + 1).cashierStatus(),
                    "no payment on turn " + (t + 1));
        }
    }

    private static void victoryReportAndAutomaticPayment() {
        TestGameIO io = victoryScript();
        Restaurant r = restaurant();
        new GameController(r, io).run();
        Check.isTrue(io.invalidMessages().isEmpty(), "reference schedule has no refusals: " + io.invalidMessages());
        GameView.FinalReport report = io.finalReports().get(0);
        Check.isTrue(report.victory(), "victory");
        Check.equal(List.of(
                new GameView.TargetLine("Paid customers", "4", "5", true),
                new GameView.TargetLine("Revenue", "$45.00", "$66.00", true),
                new GameView.TargetLine("Average paid satisfaction", "60", "79.2", true)),
                report.targets(), "victory targets");
        Check.equal(5, report.totals().servedCount(), "served");
        Check.equal(0, report.totals().departures(), "no departures");

        // C1: served turn 3, ready turn 4, pays turn 5.
        Check.equal("no eligible payment this turn", endState(io, 4).cashierStatus(), "not on ready turn");
        Check.equal("collected $15.00 from C1; customer and order PAID, table released",
                endState(io, 5).cashierStatus(), "pays the turn after ready");
        Check.equal(1, io.turnEnds().get(4).paidCount(), "paid count after turn 5");
        Check.equal(1500, io.turnEnds().get(4).revenueCents(), "revenue after turn 5");
        Check.isTrue(endState(io, 5).tables().s
… [output shortened: 9660 more characters]
```

Output:

```
File created successfully at: <scratchpad>/victor-wt/stage 1/src/test/java/restaurantrush/TurnTests.java (file state is current in your context — no need to Read it back)
```

</details>

Fixing one assertion. At turn 5, C2 is still sitting at table 2, so only C1's table 1 is free. I'm also adding a check that the order becomes servable the turn after it's ready.

<details><summary><b>Action (Bash):</b> Compile and run the full test suite</summary>

Input:

```
cd "<scratchpad>/victor-wt/stage 1" && python3 - <<'EOF'
p='src/test/java/restaurantrush/TurnTests.java'
s=open(p).read()
s=s.replace('''Check.isTrue(endState(io, 5).tables().stream().allMatch(t -> t.occupant() == null), "table released");''','''Check.isTrue(endState(io, 5).tables().get(0).occupant() == null, "C1's table 1 released");
        Check.equal(2, endState(io, 5).tables().get(1).occupant().id(), "C2 still at table 2");''')
s=s.replace('''Check.equal(List.of(new GameView.OrderLine(1, "Burger", 1, 1, "READY", 1500)), end3.orders(), "ready line");''','''Check.equal(List.of(new GameView.OrderLine(1, "Burger", 1, 1, "READY", 1500)), end3.orders(), "ready line");
        Check.equal(List.of(1), io.turnStarts().get(3).servableOrderIds(), "servable the turn after it is ready");''')
open(p,'w').write(s)
EOF
export PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH"; OUT=$(mktemp -d); find src/main/java -name '*.java' > $OUT/m.txt; javac --release 17 -d $OUT/main @$OUT/m.txt && find src/test/java -name '*.java' > $OUT/t.txt && javac --release 17 -Xlint:unchecked -cp $OUT/main -d $OUT/test @$OUT/t.txt && java -ea -Dstage1.fixtures="$PWD/test-fixtures" -cp $OUT/main:$OUT/test restaurantrush.AllTests; echo "exit $?"; java -cp $OUT/main:$OUT/test restaurantrush.control.ControlTests | tail -2
```

Output:

```
src/test/java/restaurantrush/TurnTests.java:75: error: local variables referenced from a lambda expression must be final or effectively final
            Check.isTrue(start.waiting().stream().anyMatch(c -> c.id() == i + 1 && c.satisfaction() == 100),
                                                                          ^
src/test/java/restaurantrush/TurnTests.java:78: error: local variables referenced from a lambda expression must be final or effectively final
                Check.isTrue(io.turnStarts().get(turn - 2).waiting().stream().noneMatch(c -> c.id() == i + 1),
                                                                                                       ^
2 errors
exit 1
Error: Could not find or load main class restaurantrush.control.ControlTests
Caused by: java.lang.ClassNotFoundException: restaurantrush.control.ControlTests
Shell cwd was reset to ~/Documents/new oop
```

</details>

<details><summary><b>Action (Bash):</b> Fix lambda capture and rerun all tests</summary>

Input:

```
cd "<scratchpad>/victor-wt/stage 1" && sed -i '' 's/            int turn = arrivalTurns\[i\];/            int turn = arrivalTurns[i];\n            int id = i + 1;/; s/c -> c.id() == i + 1/c -> c.id() == id/g' src/test/java/restaurantrush/TurnTests.java && export PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH"; OUT=$(mktemp -d); find src/main/java -name '*.java' > $OUT/m.txt; javac --release 17 -d $OUT/main @$OUT/m.txt && find src/test/java -name '*.java' > $OUT/t.txt && javac --release 17 -Xlint:unchecked -cp $OUT/main -d $OUT/test @$OUT/t.txt && java -ea -Dstage1.fixtures="$PWD/test-fixtures" -cp $OUT/main:$OUT/test restaurantrush.AllTests; echo "exit $?"; java -cp $OUT/main:$OUT/test restaurantrush.control.ControlTests | tail -2
```

Output:

```
PASS Console: parses seat/order/serve/wait
PASS Console: trims and ignores case
PASS Console: rejects malformed Waiter commands
PASS Console: parses Chef ID or wait
PASS Console: rejects malformed Chef commands
PASS Console: re-prompts on syntax error in the same request
PASS Console: EOF throws instead of choosing Wait
PASS Console: start-of-turn display shows every required field
PASS Console: no paid customers shows N/A and free tables
PASS Console: end-of-turn and final result lines
PASS Fixtures: victory/defeat are 18 valid Waiter+Chef turns
PASS Fixtures: invalid-input file reaches the same choices via retries
PASS Scripted: returns choices in order
PASS Scripted: exhausted script throws, never Waits
PASS Scripted: role mismatch throws
PASS Scripted: records displays and checks completion
PASS Scripted: alternating rejects bad fixture lines with line number
TestGameIOTests: 7 passed
PASS TestGameIO self-tests
PASS Customer domain
PASS Restaurant domain
PASS Order domain
PASS Staff domain
FAIL Controller turns -> java.lang.AssertionError: C6 food ready but unserved: expected <READY> but was <PREPARING>
LauncherTests: 9 passed (11 subprocesses; /tmp/oop-launcher-tests-11096106201147801042)
PASS Launcher arguments and script errors
PASS Replay: reference victory
PASS Replay: syntax and semantic retries preserve victory
PASS Replay: all-Wait defeat
PASS Replay: ordered scripted victory and departure timing
PASS Replay: EOF interrupts without final result
28 passed, 1 failed, 29 total
exit 1
PASS GameIO chef choice 0 means Wait
25 passed, 0 failed, 25 total
Shell cwd was reset to ~/Documents/new oop
[This command modified 1 file you've previously read: ../../../..<scratchpad>/victor-wt/stage 1/src/test/java/restaurantrush/TurnTests.java. Call Read before editing.]
```

</details>

The one failure is my mistake: C6 orders Pasta (`(6-1) % 3 = 2`), which takes 2 prep units, so after one Chef turn it really is PREPARING. Fixing the expectation.

<details><summary><b>Action (Bash):</b> Fix C6 expectation and rerun suite</summary>

Input:

```
cd "<scratchpad>/victor-wt/stage 1" && sed -i '' 's/Check.equal(OrderStatus.READY, r.findOrder(6).getStatus(), "C6 food ready but unserved");/Check.equal(OrderStatus.PREPARING, r.findOrder(6).getStatus(), "C6 Pasta half cooked at the end");\n        Check.equal(1, r.findOrder(6).getCompletedPreparationUnits(), "C6 Pasta 1 of 2 units");/' src/test/java/restaurantrush/TurnTests.java && export PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH"; OUT=$(mktemp -d); find src/main/java -name '*.java' > $OUT/m.txt; javac --release 17 -d $OUT/main @$OUT/m.txt && find src/test/java -name '*.java' > $OUT/t.txt && javac --release 17 -cp $OUT/main -d $OUT/test @$OUT/t.txt && java -ea -Dstage1.fixtures="$PWD/test-fixtures" -cp $OUT/main:$OUT/test restaurantrush.AllTests | grep -E "FAIL|passed,|domain|turns"; echo "exit $?"
```

Output:

```
PASS Fixtures: victory/defeat are 18 valid Waiter+Chef turns
PASS Scripted: returns choices in order
PASS Customer domain
PASS Restaurant domain
PASS Order domain
PASS Staff domain
FAIL Controller turns -> java.lang.AssertionError: re-asked until valid: expected <5> but was <6>
28 passed, 1 failed, 29 total
exit 0
Shell cwd was reset to ~/Documents/new oop
[This command modified 1 file you've previously read: ../../../..<scratchpad>/victor-wt/stage 1/src/test/java/restaurantrush/TurnTests.java. Call Read before editing.]
```

</details>

Another error in my expectation: `waiterRequests` also counts the turn-2 request that hit end of input, so 6 is right (5 on turn 1, 1 on turn 2).

<details><summary><b>Action (Bash):</b> Fix request count and rerun full suite</summary>

Input:

```
cd "<scratchpad>/victor-wt/stage 1" && sed -i '' 's/Check.equal(5, io.waiterRequests(), "re-asked until valid");/Check.equal(6, io.waiterRequests(), "5 asks on turn 1 until valid, then 1 on turn 2 that hits end of input");/' src/test/java/restaurantrush/TurnTests.java && export PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH"; OUT=$(mktemp -d); find src/main/java -name '*.java' > $OUT/m.txt; javac --release 17 -d $OUT/main @$OUT/m.txt && find src/test/java -name '*.java' > $OUT/t.txt && javac --release 17 -cp $OUT/main -d $OUT/test @$OUT/t.txt && java -ea -Dstage1.fixtures="$PWD/test-fixtures" -cp $OUT/main:$OUT/test restaurantrush.AllTests | grep -E "FAIL|passed,"; echo "exit ${PIPESTATUS[0]}"; for c in restaurantrush.domain.CustomerTests restaurantrush.domain.RestaurantTests restaurantrush.TurnTests; do java -ea -cp $OUT/main:$OUT/test $c; done
```

Output:

```
29 passed, 0 failed, 29 total
exit 
CustomerTests passed
RestaurantTests passed
TurnTests passed
Shell cwd was reset to ~/Documents/new oop
[This command modified 1 file you've previously read: ../../../..<scratchpad>/victor-wt/stage 1/src/test/java/restaurantrush/TurnTests.java. Call Read before editing.]
```

</details>

All 29 suites pass. Committing to Victor.


### Test coverage and result explanation (excerpt)

**Why these files:** `AllTests.java` was already set up to run `CustomerTests`, `RestaurantTests` and `TurnTests`, but none of those files existed on any branch. That meant the test code didn't compile at all, and these three areas had no tests. All three are in `stage 1/src/test/java/restaurantrush/`:

- **`domain/CustomerTests.java`**: a customer's whole life from waiting to paying, and the rules at each step (no ordering before being seated, no serving on the turn the food becomes ready, no paying on the turn they finish eating). It also checks that satisfaction drops 8 per turn, never goes below 0, and that the customer leaves at 0, which cancels an unfinished order. A made-up customer type that loses satisfaction faster checks that the loss rate can differ by type. Table seating and release are covered too.
- **`domain/RestaurantTests.java`**: arrival rules (duplicate IDs, a customer already registered elsewhere), lookups, read-only lists, seating when tables are full, and departures leaving the queue and freeing the table. It also checks who pays first when several customers are ready, the served/paid/revenue/average totals, and the win conditions exactly at their limits (average 60 wins, 58 loses; $45.00 wins, $40.00 loses).
- **`TurnTests.java`**: the 18-turn game loop with scripted input. It checks the order of steps each turn, a new customer every 3 turns, re-asking after an invalid move (with the exact error messages), and which choices are offered on screen at each point. It also checks automatic payment on turn 5, the final win/loss report, and the message when input runs out, which names the turn and role.

Two of my own expected values were wrong at first: C6 orders Pasta, which takes 2 cooking turns, and the input-request count also includes the request that hit end of input. I fixed the tests, not the game code. None of the tests showed a bug in the game code.
