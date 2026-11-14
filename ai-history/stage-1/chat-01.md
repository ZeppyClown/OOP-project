# Stage 1 - Chat 01: Order and Staff test cases

- **Member:** Abraham
- **AI tool:** Claude (Anthropic), Claude Sonnet 5.5 in the Claude app (chat interface)
- **Date:** 14 Nov 2026
- **Format:** Conversation excerpts documenting test development, with original wording, shortened tool outputs and personal-data placeholders.
- **What the AI produced:** JUnit 5 drafts for Order and Staff (`TestFixtures.java`, `OrderTest.java`, `StaffTest.java`), then `StaffTests.java` in the project's own plain-Java style, plus two extra scenario methods (`pastaFullLifecycle` for Order, `chefOneTaskPerTurn` for Staff).
- **What I supplied:** the domain source files, and `OrderTests.java` (already written by me), which was the style reference for the later prompts.
- **Not run in this chat:** the AI could not compile or run the tests, because `Customer`, `CustomerStatus`, `Table` and `RegularCustomer` were not shared until later. Any compile or run results come from my own machine, not from this conversation.

---

## Prompt 1  <sub>2026-11-14 16:22</sub>

**[Attached image: slide "GitHub evidence for the group and each member"]**

*(Screenshot of the assignment slide: group evidence is runnable stage tags and comparisons between stages; individual evidence is each member's AI chats in their own branch, e.g. `ai-history/stage-1/chat-01.md`; the README must link to every stage and every member's AI history.)*

### AI reply 1

Summarised the slide as a checklist (stage tags, stage comparisons, per-member `ai-history/stage-N/chat-NN.md` files on a personal branch, README links) and gave example git commands and a README skeleton:

```bash
git tag stage-1
git push origin --tags
git checkout -b yourname
mkdir -p ai-history/stage-1
git add ai-history && git commit -m "Add stage 1 AI chat"
git push -u origin yourname
```

---

## Prompt 2  <sub>2026-11-14 19:48</sub>

i want u to generate test cases for my code to test order and to test staff

**[Attached files]** `Menu.java`, `MenuItem.java`, `Money.java`, `Order.java`, `OrderStatus.java`, `Staff.java`, `Waiter.java`, `Cashier.java`, `Chef.java`

### AI reply 2

<details><summary><b>Action (Write):</b> Wrote <code>TestFixtures.java</code></summary>

Input (shortened):

```java
private static void pastaFullLifecycle() {
    Customer customer = new RegularCustomer(3);
    ok(new Table(1).seat(customer));
    ok(customer.placeOrder(item(2), 1800, 2));
    Order order = customer.getOrder();
    equal(OrderStatus.PLACED, order.getStatus(), "starts PLACED");

    ok(order.advancePreparation(2));
    equal(OrderStatus.PREPARING, order.getStatus(), "pasta needs two units");
    ok(order.advancePreparation(3));
    equal(OrderStatus.READY, order.getStatus(), "second unit completes pasta");
    equal(3, order.getReadyTurn(), "ready turn recorded");

    String before = snapshot(order);
    refused(order.markServed(3));
    equal(before, snapshot(order), "same-turn serve is atomic");
    ok(customer.serve(4));
    equal(OrderStatus.SERVED, order.getStatus(), "served");

    ok(customer.finishEating(5));
    refused(order.markPaid(5));
    ok(customer.pay(6));
    equal(OrderStatus.PAID, order.getStatus(), "paid");
    equal(1800, order.getBillCents(), "bill unchanged through the lifecycle");
}
```


Reply note: both methods use the helpers already in my test files (`ok`, `refused`, `equal`, `item`, `order`, `snapshot`); to use them, paste each into its test class, add it to `runAll()` and update the printed scenario count.


Output:

```
File created successfully at: OrderTest.java
```

</details>

<details><summary><b>Action (Write):</b> Wrote <code>StaffTest.java</code> (JUnit 5)</summary>

Input (shortened):

**Staff test case: the chef gets one task per turn**

```java
private static void chefOneTaskPerTurn() {
    Chef chef = new Chef();
    Order first = order(1, 2);
    Order second = order(2, 2);
    ok(chef.prepare(first, 2));
    equal(2, chef.getTaskTurn(), "capacity recorded");

    String before = snapshot(second);
    equal("Chef has already acted this turn", chef.prepare(second, 2), "second task same turn");
    equal(before, snapshot(second), "refused task leaves the order untouched");
    equal(2, chef.getTaskTurn(), "refusal uses no capacity");

    ok(chef.prepare(second, 3));
    equal(3, chef.getTaskTurn(), "chef acts again next turn");
}
```

Output:

```
File created successfully at: StaffTest.java
```

</details>

<details><summary><b>Action (SendUserFile):</b> Delivered the three files</summary>

Output:

```
2 files delivered to user.
  OrderTest.java, StaffTest.java
```

</details>

Reply summary: wrote JUnit 5 tests for `Order` and for the `Staff` classes.
