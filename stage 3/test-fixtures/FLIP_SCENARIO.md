# Flip scenario: Happy Hour decides Victory vs Defeat

Two Stage 3 inputs for the real `Main`/`ConsoleUI`. They are identical on every Host, Waiter and Chef line. Only the Happy Hour decision differs.

| File | Happy Hour choice | Lines | Result |
| --- | --- | ---: | --- |
| [`flip-happy-hour-on-input.txt`](flip-happy-hour-on-input.txt) | `yes` at the start of turn 13 (active turns 13 and 14) | 67 | **VICTORY**: 4 paid, $49.50, average 67.3 |
| [`flip-happy-hour-never-input.txt`](flip-happy-hour-never-input.txt) | `no` every turn, never activated | 72 | **DEFEAT**: 4 paid, $56.10, average 57.8 |

Run (JDK 17):

```sh
JAVA_HOME=/opt/homebrew/opt/openjdk@17 sh "stage 3/run.sh" < "stage 3/test-fixtures/flip-happy-hour-on-input.txt"
JAVA_HOME=/opt/homebrew/opt/openjdk@17 sh "stage 3/run.sh" < "stage 3/test-fixtures/flip-happy-hour-never-input.txt"
```

## The single differing choice

`diff` of the two files: line 49 is `yes` / `no` (the turn-13 Happy Hour prompt). Lines 53, 57, 61, 65 and 69 are the extra `no` answers the "never" run must give on turns 14–18, because the CLI keeps asking while Happy Hour has never been activated (after activation it never asks again). Nothing else differs, so there is no retry line and no rejected action.

## Real output (copied from the CLI, final block)

Happy Hour on turn 13:

```text
===== VICTORY =====
Served: 4
Paid: 4
Unhappy departures: 0
Revenue: $49.50
Average satisfaction of paid customers: 67.3
Turns used: 18 / 18
[PASS] Paid customers: required 4, actual 4
[PASS] Revenue: required $45.00, actual $49.50
[PASS] Average paid satisfaction: required 60, actual 67.3
```

Never activated:

```text
===== DEFEAT =====
Served: 4
Paid: 4
Unhappy departures: 0
Revenue: $56.10
Average satisfaction of paid customers: 57.8
Turns used: 18 / 18
[PASS] Paid customers: required 4, actual 4
[PASS] Revenue: required $45.00, actual $56.10
[FAIL] Average paid satisfaction: required 60, actual 57.8
```

## Why it flips: turn by turn

Shared manager script (same lines in both files). Customers: C1 Regular (Burger $15.00), C2 VIP (Salad $9.00 → $8.10), C3 Critic (Pasta $18.00, loses 12 per waiting turn), C4 Regular (ignored the whole game, never paid, never left), C5 Regular (Burger), C6 Regular (arrives turn 16, never served).

| Turn | Shared actions | Effect (both runs unless noted) |
| --- | --- | --- |
| 1–2 | Wait | C1 waits in the queue |
| 3 | Host seats C1, Waiter orders C1, Chef cooks O1 | O1 READY |
| 4 | Waiter serves O1 | C1 is SERVED with satisfaction 76 |
| 6 | (Cashier) | C1 pays $15.00; 76 recorded |
| 8 | Host seats C2, Waiter orders C2, Chef cooks O2 | VIP bill $9.00 → $8.10, locked |
| 9 | Waiter serves O2 | C2 SERVED with satisfaction 75 |
| 11 | (Cashier) | C2 pays $8.10; 75 recorded |
| 7–12 | C3 left alone in the queue | C3 loses 12 per turn: 28 at the end of turn 12 |
| 12 | Host seats C3 | |
| 13 | **Run A: Happy Hour on.** Host seats C5, Waiter orders C3, Chef cooks O3 (unit 1) | A: C3 28 → 43 at turn start (+15), 31 at turn end. B: C3 28, 16 at turn end. A: Pasta $18.00 → $14.40 (20% off, order taken in Happy Hour). B: $18.00 |
| 14 | Waiter orders C5, Chef cooks O3 (unit 2, READY) | A: C3 recovers 31 → 46, ends turn 34; C5 92 → 100 (capped), ends 92; Burger $12.00. B: C3 ends 4; C5 92 → 84; Burger $15.00 |
| 15 | Waiter serves O3, Chef cooks O5 (READY) | C3 SERVED, so its satisfaction freezes: **A 34, B 4**. C3 survives in B only because 4 is above 0 |
| 16 | Waiter serves O5 | C5 SERVED; waiting loss stops. Recorded **A 84** (92 − 8 on turn 15), **B 76** |
| 17 | (Cashier) | C3 pays: $14.40 (A) / $18.00 (B) |
| 18 | (Cashier) | C5 pays: $12.00 (A) / $15.00 (B). C6 and C4 stay unpaid |

Totals against the three targets (4 paid, $45.00, average ≥ 60):

| | Paid | Revenue | Recorded satisfactions | Sum | Average | Verdict |
| --- | ---: | ---: | --- | ---: | ---: | --- |
| A, Happy Hour turn 13 | 4 | $15.00 + $8.10 + $14.40 + $12.00 = $49.50 | 76 + 75 + 34 + 84 | 269 | 67.25 → 67.3 | Victory |
| B, never | 4 | $15.00 + $8.10 + $18.00 + $15.00 = $56.10 | 76 + 75 + 4 + 76 | 231 | 57.75 → 57.8 | Defeat (satisfaction target fails) |

So Happy Hour **lowers** revenue (about $6.60) but both runs clear $45.00. What flips the result is the 15-point recovery for customers still waiting for food (+30 for the Critic, +8 net for C5), which is frozen into the average when each pays. The run with less revenue wins because revenue was not the binding target.

## Honest limits

- The shared script is deliberately slow with C1, C2 and C3 to put the average near 60; a keen manager would serve C3 immediately and win both runs. The scenario shows that the choice can decide the verdict under the rules, not that the script is optimal play.
- It uses the Critic and Happy Hour recovery. VIP pricing appears only as C2's $8.10 (before any Happy Hour). A VIP-then-Happy-Hour bill ($6.48) is in [`vip-priority-input.txt`](vip-priority-input.txt) but does not flip there.
- Revenue cannot be the flipping target with Happy Hour (arithmetic, not a run): the cheapest set of four payers among C1–C5 is $56.10 without Happy Hour, and a two-turn window discounts at most two orders (at most $4.32 + $3.60), so revenue stays above $45.00.
