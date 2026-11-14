# Stage 2 execution evidence

Checked on 14 November 2026 (SGT) during Stage 2 release integration. Source snapshot: the candidate published as the `stage-2` tag. Integrated gameplay base: `15f731f55bae5309fed977f98e0369c36d84ea6e`; the release also includes repaired runner argument escaping, JDK helpers and documentation. The exact release revision is obtained with `git rev-parse stage-2^{commit}`. Existing Stage 1 evidence remains at the Stage 1 tag.

## Environment and commands

```text
openjdk version "17.0.21.0.2" 2026-10-22
OpenJDK Runtime Environment Homebrew (build 17.0.21.0.2+0)
OpenJDK 64-Bit Server VM Homebrew (build 17.0.21.0.2+0, mixed mode, sharing)
```

```sh
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
sh "stage 2/run-tests.sh"
sh "stage 2/check-build.sh"
sh "stage 2/run.sh" --script "stage 2/test-fixtures/victory-input.txt"
sh "stage 2/run.sh" --script "stage 2/test-fixtures/defeat-input.txt"
```

The test/build commands were invoked from `/tmp` using absolute script paths to verify that they do not require the repository working directory. Each returned exit code 0. The source snapshot is also checked from a fresh Git export before publication.

## Registered suite output

```text
PASS Console: parses seat/order/serve/wait
PASS Console: trims and ignores case
PASS Console: rejects malformed Waiter commands
PASS Console: parses Chef ID or wait
PASS Console: rejects malformed Chef commands
PASS Console: re-prompts on syntax error in the same request
PASS Console: EOF throws instead of choosing Wait
PASS Console: start-of-turn display shows every required field
PASS Console: Stage 2 header and supplied subtype labels
PASS Console: no paid customers shows N/A and free tables
PASS Console: end-of-turn and final result lines
PASS Console: defeat verdict prints FAIL targets
PASS Console: lists every Waiter verb and Chef order with IDs
PASS Console: mid-turn state update, invalid action and interruption lines
PASS Console: accepts large IDs and rejects null Chef input
PASS Fixtures: four retry-free files are 18 valid Waiter+Chef turns
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
PASS Exact percentage pricing
PASS Customer subtype dispatch and shared guards
Stage2ControllerTests: 9 passed
PASS Stage 2 controller and VIP priorities
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
PASS Control records and GameIO
PASS Controller turns (Regular-only regression)
LauncherTests: 10 passed (12 subprocesses; /tmp/oop-launcher-tests-15366993593454853574)
PASS Launcher arguments and script errors
PASS Replay: reference victory
PASS Replay: syntax and semantic retries preserve victory
PASS Replay: all-Wait defeat
PASS Replay: ordered scripted victory and departure timing
PASS Replay: inherited Regular-only fixtures retain original totals
PASS Replay: VIP priority changes payment timing and satisfaction
PASS Replay: EOF interrupts without final result
40 passed, 0 failed, 40 total
```

The total is registered checks, not assertions. Nested suites report their own scenario counts. The additional five registered checks compared with the 35-check reference are four inherited Console tests and the ControlTests suite.

## Build/path/CLI check output

```text
PASS copied-source tests, five replays, stdin and EOF: plain-source
PASS copied-source tests, five replays, stdin and EOF: source with spaces and "quotes" \ backslash
PASS installed JDK selection on PATH without JAVA_HOME
PASS invalid JAVA_HOME gives an actionable error
```

This check copies sources into both a plain directory and a name containing spaces, double quotes and a backslash. In each copy it runs the suite and all five fixtures through Main, validates exactly 18 turn summaries and one verdict, then checks stdin victory and empty-input interruption. It also verifies a spaced executable PATH without JAVA_HOME and an actionable invalid-JAVA_HOME failure.

## Verified CLI outcomes

| Input | Paid | Revenue | Average paid satisfaction | Result |
|---|---:|---:|---:|---|
| victory-input.txt | 5 | $65.10 | 78.0 | VICTORY |
| victory-invalid-input.txt | 5 | $65.10 | 78.0 | VICTORY |
| defeat-input.txt | 0 | $0.00 | N/A | DEFEAT |
| vip-priority-input.txt | 1 | $8.10 | 90.0 | DEFEAT |
| regular-priority-input.txt | 1 | $8.10 | 85.0 | DEFEAT |

Victory runs serve five customers with no departures. The all-wait run has two departures. The priority pair pays C2 on turn 8 versus turn 9; both runs have two departures and one served/paid customer. EOF reports interruption without a final verdict. These results do not establish optimal play or a Stage 2 verdict-flip scenario.

## Remaining presentation/submission checks

Draw.io files were parsed/exported and focused images inspected during preparation. Final report PDF page-size readability, member sign-off, declarations, video and submission archive approval are separate human deliverables and are not certified by this release.

## Review follow-up

The follow-up changes documentation, UML and the discount-hook Javadoc without changing executable gameplay. Claude independently reran the suite (40 passing registered checks), all four build-check outcomes and both priority replays during review. After that review, small diagram routing/label/EOF-context issues were corrected locally and the PNGs re-exported and inspected. Those final visual adjustments were checked locally rather than claimed as a second Claude approval. The sequence now has five pages. Final report PDF sizing remains a separate check.
