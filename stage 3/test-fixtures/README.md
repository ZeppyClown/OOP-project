# Stage 2 CLI replay fixtures

Each retry-free file has 36 raw console lines: Waiter then Chef for each of all 18 turns. They drive production Main/ConsoleUI and the role-ordered `ScriptedGameIO.alternating(...)` harness. All runs use actual Java customer dispatch.

Run a fixture from the repository root (JDK 17; see the root [README](../../README.md)):

```sh
sh "stage 2/run.sh" --script "stage 2/test-fixtures/victory-input.txt"
sh "stage 2/run.sh" < "stage 2/test-fixtures/defeat-input.txt"
```

| File | Lines | Stage 2 expected and observed result |
| --- | ---: | --- |
| `victory-input.txt` | 36 | Victory: 5 paid/served, $65.10, paid satisfactions [84,90,64,76,76] = 390, average 78.0, zero departures |
| `victory-invalid-input.txt` | 45 | Same result as victory, with syntax retries and two semantic refusals |
| `defeat-input.txt` | 36, all `wait` | Defeat: 0 paid/served, $0.00, N/A, two departures (Regular C1 on 13, Critic C3 on 15); VIP C2 remains WAITING at 25 |
| `vip-priority-input.txt` | 36 | Defeat: only VIP C2 pays, turn 8, $8.10, satisfaction/average 90; C1/C3 depart |
| `regular-priority-input.txt` | 36 | Defeat: only VIP C2 pays, turn 9, $8.10, satisfaction/average 85; C1's unserved order cancels and C1/C3 depart |

The three inherited input files are byte-identical to Stage 1: the manager's actions remain valid. The changed default customer objects cause the new outcomes. `ReplayTests.regularRegression` also runs the inherited victory/defeat through the explicitly Regular-only constructor, retaining the original $66.00/79.2 and C1/C2 departure turns 13/16.

## Victory schedule

| Turn | Waiter | Chef |
| ---: | --- | --- |
| 1 | seat 1 | wait |
| 2 | order 1 | 1 |
| 3 | serve 1 | wait |
| 4 | seat 2 | wait |
| 5 | order 2 | 2 |
| 6 | serve 2 | wait |
| 7 | seat 3 | wait |
| 8 | order 3 | 3 |
| 9 | wait | 3 |
| 10 | serve 3 | wait |
| 11 | seat 4 | wait |
| 12 | order 4 | 4 |
| 13 | serve 4 | wait |
| 14 | seat 5 | wait |
| 15 | order 5 | 5 |
| 16 | serve 5 | wait |
| 17 | seat 6 | wait |
| 18 | order 6 | 6 |

## Focused manager alternatives

Both fixtures seat C1 on turn 1 and Wait on 2–3. C2 VIP arrives on 4. Every omitted action is `wait` through turn 18.

| Turn | VIP first: Waiter / Chef | Regular first: Waiter / Chef |
| ---: | --- | --- |
| 4 | seat 2 / wait | order 1 / 1 |
| 5 | order 2 / 2 | seat 2 / wait |
| 6 | serve 2 / wait | order 2 / 2 |
| 7 | wait / wait; C2 READY_TO_PAY | serve 2 / wait |
| 8 | wait / wait; C2 pays $8.10 at 90 | wait / wait; C2 READY_TO_PAY |
| 9 | wait / wait | wait / wait; C2 pays $8.10 at 85 |

Taking/preparing C1's order first delays C2 by one turn. The real order snapshots retain 810 cents in both alternatives, while loss before service changes satisfaction by 5. Both games still complete all 18 turns and fail the paid-count/revenue targets. No Python gameplay model supplies these results.

## Invalid input

Only turns 1–3 differ from clean victory:

| Lines | Turn / role | Input | Handling |
| --- | --- | --- | --- |
| 1–6 | 1 Waiter | empty, `dance`, `seat`, `seat x`, `seat -1`, `seat 1 2` | ConsoleUI syntax retry |
| 7 | 1 Waiter | `  SEAT 1  ` | Accepted after trim/case normalization |
| 8 | 1 Chef | `two` | ConsoleUI syntax retry |
| 10 | 2 Waiter | `serve 1` | Controller refusal: O1 does not exist yet |
| 14 | 3 Chef | `1` | Controller refusal: the Waiter already served O1 this turn |

ConsoleTests retains the inherited grammar/recovery checks. ReplayTests checks the actual controller retries, locked subtype prices, paid records, departure turns and EOF. Run them all with `sh "stage 2/run-tests.sh"`. See [EVIDENCE.md](../EVIDENCE.md) for the actual recorded results.
