# Stage 1 replay fixtures

Raw console input, one line per prompt, in the order the controller asks: **Waiter, then Chef, every turn** (including when only Wait is useful). The same files drive both the real CLI and, for retry-free files, `ScriptedGameIO.alternating(...)`.

```sh
scripts/run.sh < stage-1/test-fixtures/victory-input.txt
```

| File | Lines | Expected result (from [Stage 1 plan](../PLAN.md)) |
|---|---:|---|
| `victory-input.txt` | 36 (18 × 2) | Victory after 18: 5 paid, 5 served, $66.00, paid satisfaction [84,84,76,76,76] = 396, average 79.2, 0 departures |
| `defeat-input.txt` | 36, all `wait` | Defeat at 18: 0 paid, 0 served, $0.00, average N/A, 2 departures (C1 leaves on 13, C2 on 16), C3 ends at 4 |
| `victory-invalid-input.txt` | 45 | Same result as victory; every invalid line is retried without using the role's task |

## Victory schedule

| Turn | Waiter | Chef |
|---:|---|---|
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

## Invalid lines in `victory-invalid-input.txt`

Turns 1–3 only; turns 4–18 are identical to the victory file.

| Lines | Turn / role | Input | Rejected by |
|---|---|---|---|
| 1–6 | 1 Waiter | empty, `dance`, `seat`, `seat x`, `seat -1`, `seat 1 2` | ConsoleUI syntax (re-prompt) |
| 7 | 1 Waiter | `  SEAT 1  ` | Accepted: trimmed, case-insensitive |
| 8 | 1 Chef | `two` | ConsoleUI syntax |
| 10 | 2 Waiter | `serve 1` (O1 does not exist yet) | Controller semantic retry |
| 14 | 3 Chef | `1` (O1 was served by the Waiter earlier this turn) | Controller semantic retry |

ConsoleTests checks syntax recovery; ReplayTests runs all three files through the real controller and checks both semantic retries and final totals. See [EVIDENCE.md](../EVIDENCE.md) for actual results.
