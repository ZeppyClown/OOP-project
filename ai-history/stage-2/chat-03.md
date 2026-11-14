# Stage 2 - Chat 03: Stage 2 code review against the assignment brief

- **Member:** Victor
- **AI tool:** Claude Code (Anthropic Claude)
- **Date:** 14 Nov 2026
- **Format:** Conversation excerpts documenting a review of the Stage 2 release, with original wording, shortened tool outputs and personal-data placeholders. The AI was asked not to write code; no source files were changed.

---

## Prompt 1  <sub>2026-11-14</sub>

**[Pasted text]**

GitHub evidence for the group and each member
Group evidence Runnable stage tags and comparisons between stages.
Individual
evidence
Each member’s AI chats in their own branch.
Example folder on your personal branch
ai-history/stage-1/chat-01.md
ai-history/stage-2/chat-01.md
ai-history/stage-3/chat-01.md
README: link to every stage and every member’s AI history.

**[End of pasted text]**

i want you to check my stage 2 codes and check with the assignment rubrics. and tell me what is wrong. do not code for me

### AI reply 1

[Directory listing omitted.]

<details><summary><b>Action (Bash):</b> Extract the assignment brief text</summary>

```
textutil -convert txt -stdout "assignment/SC2002_Restaurant_Rush_Group_Assignment(1).docx"
```

Output (shortened): base setting (18 turns, 2 tables, arrivals on 1/4/7/10/13/16), Stage 2 table (Regular 8 / VIP 5 with 10% off / Critic 12 with a documented preference or reaction), "Different behaviour must be dispatched through the customer objects, not through a chain of type checks in the turn controller", sequence diagram must show "one complete scenario in which a manager choice changes an outcome", README transition requirements, and the marking scheme (Class 25, Sequence 20, Design 15, Implementation 20, Process 20).

</details>

<details><summary><b>Action (Bash):</b> Read all Stage 2 production sources</summary>

Read `Main`, `ConsoleUI`, `GameController`, `GameIO`, `GameView`, `WaiterAction`, `InputEndedException`, and every class in `domain/` (Customer and its three subtypes, Order, Money, Menu, MenuItem, Restaurant, Staff, Waiter, Chef, Cashier, Table, enums). Output omitted.

</details>

<details><summary><b>Action (Bash):</b> Read TRANSITION.md, EVIDENCE.md, fixture README and run scripts</summary>

Output omitted.

</details>

<details><summary><b>Action (Bash):</b> Run the test suite and the victory fixture</summary>

```
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
sh "stage 2/run-tests.sh"
sh "stage 2/run.sh" --script "stage 2/test-fixtures/victory-input.txt"
```

Output (tail):

```
40 passed, 0 failed, 40 total
===== VICTORY =====
Served: 5
Paid: 5
Unhappy departures: 0
Revenue: $65.10
Average satisfaction of paid customers: 78.0
[PASS] Paid customers: required 4, actual 5
[PASS] Revenue: required $45.00, actual $65.10
[PASS] Average paid satisfaction: required 60, actual 78.0
```

</details>

<details><summary><b>Action (Bash):</b> Check remote tags, member AI histories and the stage-1 to stage-2 diff</summary>

```
git ls-remote --tags origin
git ls-tree -r --name-only origin/<member> -- ai-history
git diff -M --stat stage-1 stage-2 -- "stage 1/src/main" "stage 2/src/main"
```

Output (shortened):

```
refs/tags/stage-1, refs/tags/stage-2   (both pushed)
Abraham / Danielle / Talon / Victor: ai-history/README.md, ai-history/stage-1/chat-01.md, chat-02.md only
Production diff: Main, GameController, Customer, Money changed; VIPCustomer, CriticCustomer added; all others unchanged
```

</details>

<details><summary><b>Action (Bash):</b> Extract labels from the Stage 2 draw.io diagrams</summary>

Confirmed the full class model, control view and customer-subtypes view include `customerFactory`, `stage`, `VIPCustomer`, `CriticCustomer` and `applyDiscount`. Output omitted.

</details>

**Final answer (AI):**

I compared your Stage 2 against the brief in `assignment/SC2002_Restaurant_Rush_Group_Assignment(1).docx` and the evidence rules you pasted. I didn't change any files.

**What works:** all 40 tests pass and the victory script reproduces (5 paid, $65.10, average 78.0, VICTORY). The `stage-2` tag is pushed. C2 is a VIP and C3 is a Critic, with six arrivals in total. The losses (8/5/12), the VIP 10% discount in integer cents, the locked bill and dispatching through the customer objects all match §4.2. `TRANSITION.md` is accurate: every production file it calls unchanged really is unchanged from `stage-1`.

#### Serious problems (these cost marks)

1. **No Stage 2 AI history exists anywhere.** Every member branch contains only `ai-history/stage-1/chat-01.md` and `chat-02.md`. The brief's evidence rule expects `ai-history/stage-2/...` on each member's branch.
2. **None of the runs show a manager choice changing the result.** The two priority scripts and the sequence diagram only move the VIP's payment from turn 8 to turn 9 and satisfaction from 90 to 85; both games still end in DEFEAT. The sequence diagram must show "one complete scenario in which a manager choice changes an outcome". A pair of runs where one decision decides a target or the verdict would be stronger evidence.
3. **The Danielle branch has no separate work.** `origin/Danielle` points at the same commit as `main` (`d985062`). Also check that git author names can be matched to members.

#### Design and code quality (smaller deductions; they make Stage 3 harder)

4. **The price is worked out in the controller.** `GameController.java:121` calls `customer.applyDiscount(item.getPriceCents())`. Polymorphic, so §4.2 is met, but Stage 3's VIP-then-Happy-Hour stacking would push more rules into the controller. Pricing belongs in a domain object.
5. **The victory thresholds are written twice:** `Restaurant.evaluateResult()` and `GameController.finalReport()` (lines 200–204) both hard-code 4 / 4500 / 60.
6. **The controller still knows Stage 1 details:** it imports `RegularCustomer`, keeps a Regular-only constructor, and has an `int stage` field that rejects anything except 1 or 2, which Stage 3 must edit (OCP smell).
7. **The food choice is fragile:** `Customer.chooseItem` uses `(id - 1) % 3` (hard-coded menu size; breaks when ComboMeal changes the menu), and `CriticCustomer` finds its dish by the string `"Pasta"`.
8. **The Critic's special behaviour is very thin.** "Always orders Pasta" passes as a distinct preference, but it is the minimum and is never shown on screen as Critic behaviour (needed for video segment 4).
9. **There is unused code:** `Restaurant.findWaitingCustomer()`, `Restaurant.getCashier()`, and the write-only `Customer.restaurant` field.

#### Evidence and documentation gaps

10. **A local `diagrams/stage 1/` folder isn't committed,** while Stage 2 deleted `stage 1/diagrams/`, so `main` has no Stage 1 diagrams.
11. **Some transition reasons are missing:** in `TRANSITION.md`, the four changed test files have no reason each, and the deleted Stage 1 files (`run.sh`, `run-tests.sh`, `EVIDENCE.md`, `ReplayTests.java`) are covered only by a general sentence.
12. **Placing an order prints no message saying what the customer chose;** the dish only appears in the next state printout.

Fix 1–3 first (Process 20 and Sequence 20 marks). Then do 4–7 before Stage 3, because Happy Hour and ComboMeal build on exactly that code.

---

## Prompt 2  <sub>2026-11-14</sub>

ok create a commit and push this evidence to victor's branch, and put it as chat 03.md

### AI reply 2

Fast-forwarded the local `Victor` worktree to `origin/Victor`, wrote this record as `ai-history/stage-2/chat-03.md`, linked it from `ai-history/README.md` under Stage 2, then committed and pushed to `origin/Victor`.
