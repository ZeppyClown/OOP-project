# Stage 1 to Stage 2 transition

[Actual Git comparison](https://github.com/ZeppyClown/OOP-project/compare/stage-1...stage-2).

Stage 2 replaces the working `stage 1/` directory with `stage 2/`. It extends the same game rather than keeping two source copies. The lowercase `stage-1` tag preserves the runnable earlier snapshot.

## Production changes

| File | Reason |
|---|---|
| `Main.java` | Supplies `Main::createCustomer` and stage=2; C2 VIP, C3 Critic, others Regular. |
| `control/GameController.java` | Injects and validates the customer factory; dispatches choice/discount through Customer; retains original phase order and Regular-only regression constructor. |
| `domain/Customer.java` | Adds the full-price default discount hook, shared by Regular and Critic. |
| `domain/Money.java` | Adds bounded, overflow-safe, HALF_UP percentage arithmetic in integer cents. |
| `domain/VIPCustomer.java` | New subtype: label VIP, waiting loss 5, price 90%. |
| `domain/CriticCustomer.java` | New subtype: label Critic, waiting loss 12, Pasta preference. |

The other production files are byte-identical to the Stage 1 tag. No production Java file was deleted. Customer/Order guards, staff capacity, table cleanup and paid-only accounting remain in their existing classes. Stage 2 still calls Waiter and Chef directly through Restaurant getters; it does not introduce Host, Happy Hour or pricing policies. Critic assumes Pasta exists in the supplied menu; that is a documented limitation of its choice rule.

## Test changes

### Added

- `src/test/java/restaurantrush/Stage2ControllerTests.java`
- `src/test/java/restaurantrush/domain/CustomerSubtypeTests.java`
- `src/test/java/restaurantrush/domain/PricingTests.java`

### Changed

- `src/test/java/restaurantrush/AllTests.java`
- `src/test/java/restaurantrush/ConsoleTests.java`
- `src/test/java/restaurantrush/LauncherTests.java`
- `src/test/java/restaurantrush/ReplayTests.java`

New tests cover exact pricing, subtype lifecycle/dispatch and factory/controller behaviour. Console, launcher and replay checks now verify real Stage 2 defaults and retain Regular-only regression totals. Existing ControlTests remain registered. PricingTests lives only in test sources and is registered once.

## Inputs, scripts and documentation

The original victory, defeat and invalid-input fixture files are unchanged. VIP-priority and Regular-priority inputs are added. The same original victory commands now produce $65.10 and average satisfaction 78.0 instead of $66.00 and 79.2 because the customer objects dispatch different prices and waiting losses.

`run.sh` and `run-tests.sh` now use `java-env.sh` and `compile-sources.sh` for installed-JDK selection and escaped compiler argument files. `check-build.sh` verifies copied-source runs from another working directory, five replays, stdin, EOF, JDK discovery and invalid JAVA_HOME. No JDK is downloaded.

README paths/results and EVIDENCE.md are updated. The old tracked `stage 1/diagrams/` files are deleted from the current working source tree; their historical versions remain at `stage-1`. New Stage 2 models are added under `diagrams/stage 2/`. This is a directory replacement, not deletion from a pre-existing tracked `stage 2/diagrams/` directory. The local untracked `diagrams/stage 1/` copy is separate and is not part of the release. Draw.io sources/PNGs cover the full class model, focused views and a priority sequence. The local review repair splits that sequence into five pages; both alternatives still end in DEFEAT.

## Explicit changed, moved and deleted path inventory

The conceptual comparison above maps each `stage 1/` file to its `stage 2/` counterpart. The actual Git diff also records path-level deletions/additions where similarity is below rename detection. No gameplay class was dropped.

| Old tracked path(s) | Current counterpart/status | Reason |
|---|---|---|
| `stage 1/src/main/java/**` | Moved to `stage 2/src/main/java/**`; add VIP/Critic and modify the four listed production files | Extend the Stage 1 game in one current-stage directory. |
| `stage 1/src/test/java/**` | Moved to `stage 2/src/test/java/**`; three suites added, four updated | Preserve regressions while testing subtype pricing/dispatch and real Stage 2 startup. |
| `stage 1/run.sh`, `stage 1/run-tests.sh` | Deleted at old paths; rewritten as `stage 2/run.sh`, `stage 2/run-tests.sh` | Use the current source root, installed JDK selection and correctly escaped javac argument lists. |
| `stage 1/EVIDENCE.md` | Deleted at old path; new `stage 2/EVIDENCE.md` | Record actual Stage 2 checks/results instead of relabelling old Stage 1 evidence. |
| `stage 1/test-fixtures/README.md` | Deleted at old path; rewritten `stage 2/test-fixtures/README.md` | Explain current commands, discounted totals, departure timing and priority inputs. |
| Three original `stage 1/test-fixtures/*.txt` inputs | Renamed byte-for-byte to the corresponding `stage 2/test-fixtures/` paths | Compare stage behaviour under identical manager commands. |
| `stage 1/diagrams/*.drawio` (7 files) and `stage 1/diagrams/png/*.png` (7 files) | Deleted from the current tree; retained in the historical tag | Replace old-stage figures with Stage 2 models; the starter diagram remains historical. |
| No old equivalent | Add `diagrams/stage 2/` draw.io sources and PNGs | Show current factory, customer subtypes, pricing dependencies and actual sequence calls. |
| No old equivalent | Add `stage 2/check-build.sh`, `compile-sources.sh`, `java-env.sh` | Verify copied-source CLI behaviour, difficult paths, installed JDK selection and diagnostics. |
| No old equivalent | Add `stage 2/TRANSITION.md` and two priority input files | Explain the real transition and reproduce manager-choice timing/satisfaction changes. |
| Root `README.md` | Modified | Update paths, commands, tag/compare links, diagrams, design discussion and results. |

At the published `stage-1...stage-2` comparison, Git reports `Money.java` and `ReplayTests.java` as old-path deletions plus new-path additions, despite their conceptual continuity. The same applies to the substantially rewritten runner/evidence/fixture README files. Rename detection is heuristic; it does not mean those responsibilities vanished. To reproduce the actual status list:

```sh
git diff --name-status --find-renames stage-1 stage-2
```

The review follow-up corrects diagram sources/exports, expands the design discussion and path inventory, and clarifies the discount-hook Javadoc. The five-page sequence makes the turn loop, lookups, lifetime continuation, same-role retries and EOF exit explicit. These changes do not alter gameplay rules. The recreated stage-2 release includes this follow-up; the Git history preserves the original integration checkpoint.

## Integration provenance

The integrated code commit is `15f731f55bae5309fed977f98e0369c36d84ea6e`. It records the published main, Talon, Victor and Danielle heads as parents, preserving their branch histories. Abraham's pricing commits were already ancestors of main. The final `stage-2` tag includes subsequent build-script fixes and current documentation. This release does not manufacture earlier development commits or establish human review/approval.
