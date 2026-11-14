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

README paths/results and EVIDENCE.md are updated. Stage 2 figures are in `diagrams/stage 2`; copied Stage 1 figures inside the Stage 2 source folder are removed. Draw.io sources and PNG exports cover the full class model, focused views and a three-page priority sequence. The sequence changes VIP payment timing and satisfaction; both alternatives end in DEFEAT.

## Integration provenance

The integrated code commit is `15f731f55bae5309fed977f98e0369c36d84ea6e`. It records the published main, Talon, Victor and Danielle heads as parents, preserving their branch histories. Abraham's pricing commits were already ancestors of main. The final `stage-2` tag includes subsequent build-script fixes and current documentation. This release does not manufacture earlier development commits or establish human review/approval.
