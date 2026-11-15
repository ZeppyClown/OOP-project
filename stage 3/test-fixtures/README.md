# Stage 3 input fixtures

These are raw input lines for the real Stage 3 `Main` and `ConsoleUI`, with no comments or blank separators. The grammar for each turn is:

1. `yes` or `no` for Happy Hour **only while it has never been activated**.
2. Host: positive customer ID, or `wait`.
3. Waiter: `seat <customer ID>`, `order <customer ID>`, `serve <order ID>`, or `wait`.
4. Chef: positive order ID, or `wait`.

IDs are numbers without `C`/`O` prefixes. Input is case insensitive and trims surrounding whitespace. Invalid syntax re-prompts the same role; valid but illegal choices are rejected by the controller without consuming a staff task. EOF interrupts explicitly without a final verdict. After activation there are no further yes/no lines, including after Happy Hour expires. Status is still displayed each turn: remaining 2 on activation, 1 next turn, then 0.

| File | Activation | Lines | Actual final result |
| --- | --- | ---: | --- |
| `victory-input.txt` | Turn 10 | 64 | Victory; 5 paid, 6 served, $73.38, average 89.4, no departures |
| `victory-invalid-input.txt` | Turn 10 | 71 | Same victory after four syntax retries and three semantic retries |
| `defeat-input.txt` | Never | 72 | Defeat; 0 paid/served, $0.00, N/A, two departures |
| `vip-priority-input.txt` | Turn 4 | 58 | Defeat; C2 pays turn 7, $6.48, satisfaction 100 |
| `regular-priority-input.txt` | Turn 4 | 58 | Defeat; C2 pays turn 8, $6.48, satisfaction 95 |

The full victory follows [the recorded victory evidence](../EVIDENCE.md). Host seats each arrival on 1/4/7/10/13/16; Waiter orders that customer immediately. Chef prepares the order then, continuing Pasta on 8 and combo on 11. Waiter serves on 2/5/9/12/14/17. C4 chooses the composed Salad + Burger item: 2,160 cents, 2 units; the turn-10 bill locks at 1,728 cents. Turn-11 recovery brings C4 to 100 before ordinary loss returns it to 92. C6 reaches READY_TO_PAY on 18 and remains unpaid.

The invalid fixture adds `maybe` at activation, `seat 1` at Host, `dance` at Waiter and `two` at Chef. On turn 1, valid numeric Host/Chef target `99` and Waiter `serve 99` are semantic errors, each followed by the correct same-role action. All subsequent successful actions match the clean victory.

The focused files share Host seating C1 on 1, waiting on 2–3, activating on 4 and Host seating C2 on 4. VIP first orders/prepares C2 on 4 and serves on 5. Regular first orders/prepares C1 on 4, C2 on 5, and serves C2 on 6. All other actions wait. VIP discount then Happy Hour gives 900 → 810 → 648 cents in both files. Only C2 pays. These are the concrete sequence/counterfactual fixtures for the controller sequence; [ReplayTests](../src/test/java/restaurantrush/ReplayTests.java) checks READY, service, eating, payment and frozen satisfaction.

`base3/victory-input.txt` and `base3/defeat-input.txt` preserve the earlier two-line Waiter/Chef grammar for explicit `Menu(false)` / Regular-only test doubles with No-Happy-Hour / Host-Wait defaults. They are not inputs for production Stage 3 Main. `ScriptedGameIO.alternating` loads those regressions; `ScriptedGameIO.stage3` loads the retry-free production grammar and enforces all roles, conditional activation and complete 18 turns.

```sh
JAVA_HOME=/opt/homebrew/opt/openjdk@17 sh "stage 3/run.sh" --script "stage 3/test-fixtures/victory-input.txt"
JAVA_HOME=/opt/homebrew/opt/openjdk@17 sh "stage 3/run.sh" < "stage 3/test-fixtures/victory-input.txt"
```

**Manager-choice comparison.** The two VIP-priority files differ only in whose food is handled on turn 4. VIP first: C2 pays turn 7, satisfaction 100. C1 first: C2 pays turn 8, satisfaction 95. Both end in defeat with $6.48 (1 paid). This pair changes timing and recorded satisfaction. A separate [Happy Hour pair](FLIP_SCENARIO.md) changes the final verdict: activation on turn 13 wins (4 paid, $49.50, 67.3), while never activating loses (4 paid, $56.10, 57.8). Both comparisons run through the real CLI and are registered in `ReplayTests`.

**Fix pass, 28 Oct 2026.** All five production fixtures were replayed again through the real CLI after the controller started calling `Restaurant.takeOrder/serveOrder/prepareOrder`; the outputs are byte-identical to the earlier logs (totals in the table above). No fixture file was edited.

See [EVIDENCE](../EVIDENCE.md) for actual verification commands and remaining integration/release gates.

**Result-flipping pair, 28 Oct 2026 (second fix pass).** `flip-happy-hour-on-input.txt` (Happy Hour `yes` on turn 13) ends in Victory ($49.50, 4 paid, average 67.3). `flip-happy-hour-never-input.txt` (same Host/Waiter/Chef lines, Happy Hour never activated) ends in Defeat ($56.10, 4 paid, average 57.8; satisfaction target fails). Both were executed through the real CLI. Turn-by-turn explanation and real output: [FLIP_SCENARIO](FLIP_SCENARIO.md). Both outcomes are registered in ReplayTests.
