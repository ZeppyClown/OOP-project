# Stage 3 - Chat 01: Order, Pricing, Staff and Stage 3 component test cases

- **Member:** Abraham
- **AI tool:** Claude (Anthropic), Claude Sonnet 5.5 in the Claude app (chat interface)
- **Date:** 14 Nov 2026
- **Format:** Conversation excerpts documenting test development, with original wording, shortened tool outputs and personal-data placeholders.
- **What the AI produced:** `OrderTests.java` and `PricingTests.java` (first drafts, plain Java, no JUnit), then `Stage3ComponentTests.java` (13 scenario methods covering `Host`, `ComboMeal` and `PricingPolicy`).
- **What I supplied:** the domain source files (`Customer`, `Restaurant`, `Order`, `Table`, `Host`, `Waiter`, `Chef`, `Cashier`, `Staff`, `Menu`, `MenuItem`, `ComboMeal`, `Money`, the pricing policies and the customer subclasses), then later `CustomerStatus.java` and my own `OrderTests.java`, `PricingTests.java` and `StaffTests.java`, which became the style reference for the Stage 3 file.
- **What the AI did not produce:** `StaffTests.java`. I had already written it and shared it in Prompt 2, so the AI left it unchanged.
- **Not run in this chat:** the AI could not compile or run any of the tests (`CustomerStatus` and `GameResult` were missing at first, and package installs were blocked in the session). Any compile or run results come from my own machine, not from this conversation.
- **Superseded output:** the first-draft `OrderTests.java` and `PricingTests.java` from Prompt 1 were replaced by my own versions, which I shared in Prompt 2. Only `Stage3ComponentTests.java` was adopted as generated.

---

## Prompt 1  <sub>2026-11-14 20:45</sub>

Act as an expert Java SDET (Software Development Engineer in Test). I need you to write four comprehensive, standalone test classes for a domain-driven restaurant management system: `OrderTests.java`, `PricingTests.java`, `StaffTests.java`, and `Stage3ComponentTests.java`.

*(Constraints in the prompt: no JUnit or external libraries; plain Java classes with `public static void main`; custom helpers `check`, `ok`, `refused`, `equal`, `illegal` in each file; the exact test method names for each file; generate the files one by one, starting with `OrderTests.java` and `PricingTests.java`.)*

**[Attached files]** `Customer.java`, `HappyHourPricingPolicy.java`, `Host.java`, `Menu.java`, `MenuItem.java`, `Money.java`, `Order.java`, `OrderStatus.java`, `PricingPolicy.java`, `RegularCustomer.java`, `Restaurant.java`, `Staff.java`, `StandardPricingPolicy.java`, `Table.java`, `VIPCustomer.java`, `Waiter.java`, `Cashier.java`, `Chef.java`, `ComboMeal.java`, `CriticCustomer.java`

### AI reply 1

<details><summary><b>Action (Write):</b> Wrote <code>OrderTests.java</code></summary>

Input (shortened):

```java
package restaurantrush.domain;

public final class OrderTests {
    static void check(boolean value, String message) { /* throws AssertionError */ }
    static void ok(String reason)      { /* asserts reason == null */ }
    static void refused(String reason) { /* asserts non-null, non-blank */ }
    static void equal(Object expected, Object actual, String message) { /* Objects.equals */ }
    static void illegal(Runnable action) { /* expects IllegalArgumentException */ }

    static void pastaPreparationTiming() {
        Customer c = new RegularCustomer(3);
        Order o = place(c, pasta(), 1800, 3);
        ok(o.advancePreparation(3));
        equal(OrderStatus.PREPARING, o.getStatus(), "Pasta is PREPARING after first unit");
        refused(o.advancePreparation(3));   // same turn again
        ok(o.advancePreparation(4));
        equal(OrderStatus.READY, o.getStatus(), "Pasta READY after second unit");
        equal(4, o.getReadyTurn(), "Ready on the second preparation turn");
    }
    // ... eight more scenarios: menuAndMoney, constructionGuards, singleUnitAndLockedBill,
    //     servingGuards, paymentRequiresEatingDelay, cancellationGuards,
    //     abandonmentCancelsPreparation, terminalOrdersRejectPreparation
}
```

Output:

```
File created successfully at: OrderTests.java
```

</details>

<details><summary><b>Action (Write):</b> Wrote <code>PricingTests.java</code></summary>

Input (shortened):

```java
static void halfUpRounding() {
    int[][] cases = {
        {1, 50, 1}, {3, 50, 2}, {5, 50, 3},      // ties go up
        {25, 10, 3},                             // HALF_EVEN would give 2
        {1944, 80, 1555}, {1945, 80, 1556}, ...
    };
    for (int[] c : cases) {
        equal(c[2], Money.applyPercent(c[0], c[1]), "applyPercent(" + c[0] + "," + c[1] + ")");
        // also compared against BigDecimal ... RoundingMode.HALF_UP
    }
}

private static int oracle(int cents, int percent) {
    return BigDecimal.valueOf(cents).multiply(BigDecimal.valueOf(percent))
            .divide(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).intValueExact();
}
```

Output:

```
File created successfully at: PricingTests.java
```

</details>

<details><summary><b>Action (SendUserFile):</b> Delivered both files</summary>

Output:

```
2 files delivered to user.
  OrderTests.java, PricingTests.java
```

</details>

Reply summary: delivered the first two files with the five helpers and a `run(...)` wrapper that prints PASS/FAIL per test. Stated that nothing had been compiled or run because `CustomerStatus` and `GameResult` were not shared and installs were blocked, and that the Critic's full-price quote was assumed from the class comment. Offered to continue with `StaffTests.java` and `Stage3ComponentTests.java`.

---

## Prompt 2  <sub>2026-11-14 20:50</sub>

go ahead heres more information you need

**[Attached files]** `CustomerStatus.java`, `PricingTests.java` (mine), `OrderTests.java` (mine), `StaffTests.java` (mine)

### AI reply 2

<details><summary><b>Action (Write):</b> Wrote <code>Stage3ComponentTests.java</code></summary>

Input (shortened):

```java
public final class Stage3ComponentTests {
    public static void runAll() {
        hostMutationVisibility();  hostPrimitiveGuards();  hostThenWaiterOrders();
        independentSeatingCapacity();  waiterThenHostSeating();  aggregateSeatingRefusals();
        occupiedUntilPayment();  defaultAndBaseMenus();  composedValuesAndImmutability();
        comboConstructionGuards();  comboOrdinaryLifecycle();  policyQuotesAndDispatch();
        realPlacementAcrossPricingBoundary();
    }

    private static void independentSeatingCapacity() {
        Host host = new Host();
        Waiter waiter = new Waiter();
        ok(host.seat(a, one, 1));
        ok(waiter.seat(b, two, 1));            // separate capacity per role
        refused(host.seat(c, three, 1));
        refused(waiter.seat(c, three, 1));     // two seatings per turn at most
        check(three.isFree() && c.getStatus() == CustomerStatus.WAITING, "refusals are atomic");
    }

    private static void comboConstructionGuards() {
        illegal(() -> new ComboMeal(null, burger));
        illegal(() -> new ComboMeal(max, penny));   // price sum overflows int
        // overflow cause must be an ArithmeticException; near-limit sums are accepted
    }
}
```

Output:

```
File created successfully at: Stage3ComponentTests.java
```

</details>

<details><summary><b>Action (SendUserFile):</b> Delivered the file</summary>

Output:

```
1 file delivered to user.
  Stage3ComponentTests.java
```

</details>

Reply summary: noticed that my attached `StaffTests.java` already covered all ten requested methods and left it unchanged. Matched the style of my attached files (`runAll()`, private scenario methods, helpers at the bottom). Noted these points:

- `Restaurant` seats only through the `Waiter`, so the Host tests call `Host.seat` directly.
- Expected prices, such as the VIP happy-hour combo quote of 1555 cents and the 52-cent revenue total, were calculated by hand using HALF_UP rounding, so a failing assertion should be checked against the arithmetic first.
- The file was not compiled or run in the chat.

---

## Prompt 3  <sub>2026-11-14 20:55</sub>

generate a report like this

**[Attached file]** `chat-01.md` (the Stage 1 report used as the template for this document)

### AI reply 3

Wrote this report in the same format as the template, covering the three prompts of this chat.
