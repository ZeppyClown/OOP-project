# Stage 1 test notes and evidence

Actual results of the Stage 1 test suite and of one reproducible victory run and one reproducible defeat run. The expected values and turn-by-turn schedules are in [test-fixtures/README.md](test-fixtures/README.md).

## Environment

| Item | Value |
|---|---|
| Date (SGT) | 12 November 2026 |
| Tested commit | `bb96907` on branch `Victor`. Later commits change only documentation (this file), not code or tests. |
| Checkout | Fresh `git clone`, no local changes |
| JDK | `javac 17.0.21.0.2`, `openjdk version "17.0.21.0.2" 2026-10-22`, via `JAVA_HOME=/opt/homebrew/opt/openjdk@17` |
| OS | macOS (Darwin 25.6) |

To reproduce, run from the repository root (see the root [README](../README.md)):

```sh
sh "stage 1/run-tests.sh"
sh "stage 1/run.sh" --script "stage 1/test-fixtures/victory-input.txt"
sh "stage 1/run.sh" --script "stage 1/test-fixtures/defeat-input.txt"
sh "stage 1/run.sh" --script "stage 1/test-fixtures/victory-invalid-input.txt"
```

## 1. Test suite: 29 passed, 0 failed

`sh "stage 1/run-tests.sh"` compiles `src/main/java` and `src/test/java` with `--release 17`, then runs `java -ea -Dstage1.fixtures="stage 1/test-fixtures" restaurantrush.AllTests`. Exit code 0.

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
PASS Controller turns
LauncherTests: 9 passed (11 subprocesses; /tmp/oop-launcher-tests-…)
PASS Launcher arguments and script errors
PASS Replay: reference victory
PASS Replay: syntax and semantic retries preserve victory
PASS Replay: all-Wait defeat
PASS Replay: ordered scripted victory and departure timing
PASS Replay: EOF interrupts without final result
29 passed, 0 failed, 29 total
```

## 2. Reproducible victory run

Input: [`test-fixtures/victory-input.txt`](test-fixtures/victory-input.txt), 36 lines (Waiter then Chef for each of the 18 turns). Exit code 0. Final summary as printed:

```
===== VICTORY =====
Served: 5
Paid: 5
Unhappy departures: 0
Revenue: $66.00
Average satisfaction of paid customers: 79.2
Turns used: 18 / 18
[PASS] Paid customers: required 4, actual 5
[PASS] Revenue: required $45.00, actual $66.00
[PASS] Average paid satisfaction: required 60, actual 79.2
```

This matches the expected result: 5 paid, $66.00, paid satisfaction [84, 84, 76, 76, 76] = 396, and 396 / 5 = 79.2.

## 3. Reproducible defeat run

Input: [`test-fixtures/defeat-input.txt`](test-fixtures/defeat-input.txt), 36 lines, all `wait`. Exit code 0. Final summary as printed:

```
===== DEFEAT =====
Served: 0
Paid: 0
Unhappy departures: 2
Revenue: $0.00
Average satisfaction of paid customers: N/A
Turns used: 18 / 18
[FAIL] Paid customers: required 4, actual 0
[FAIL] Revenue: required $45.00, actual $0.00
[FAIL] Average paid satisfaction: required 60, actual N/A
```

The end-of-turn summaries show the two departures happening when expected: `Unhappy departures` goes from 0 to 1 at the end of turn 13 (C1 leaves) and to 2 at the end of turn 16 (C2 leaves). At the end of turn 18, C3 is still waiting at satisfaction 4.

## 4. Invalid input re-prompts without using the staff turn

Input: [`test-fixtures/victory-invalid-input.txt`](test-fixtures/victory-invalid-input.txt), 45 lines: the victory schedule plus nine invalid lines in turns 1–3. Exit code 0. Every invalid line was refused and the same role was asked again:

```
Invalid Waiter command: empty input
Invalid Waiter command: unknown command 'dance'; use seat, order, serve or wait
Invalid Waiter command: seat needs exactly one ID, e.g. 'seat 1'
Invalid Waiter command: 'x' is not a whole-number ID
Invalid Waiter command: IDs start at 1
Invalid Waiter command: seat needs exactly one ID, e.g. 'seat 1'
Invalid Chef command: 'two' is not a whole-number ID
Not allowed: Order 1 does not exist - choose again.
Not allowed: Order 1 is not awaiting preparation: SERVED - choose again.
```

The first seven are syntax errors caught by `ConsoleUI`. The last two are refused by the game rules (via `GameController`): serving O1 before it exists, and cooking O1 after the Waiter has already served it. The game still ends with exactly the victory result, which shows that no refused command used up a staff turn:

```
===== VICTORY =====
Served: 5
Paid: 5
Unhappy departures: 0
Revenue: $66.00
Average satisfaction of paid customers: 79.2
Turns used: 18 / 18
[PASS] Paid customers: required 4, actual 5
[PASS] Revenue: required $45.00, actual $66.00
[PASS] Average paid satisfaction: required 60, actual 79.2
```
