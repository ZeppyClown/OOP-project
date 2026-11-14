# Talon: Stage 3 responsibility and code handoff

Mapped to the supplied Talon Member Pack. This records technical changes and checks; it does not supply personal reflection, approval or authorship claims.

ConsoleUI implements every GameIO default method, including activation, Host and status displays. Each turn asks yes/no only while Happy Hour has never been used; then Host ID/wait, Waiter command and Chef ID/wait. Once activated, later turns have no activation input, including after expiry. Remaining duration displays 2,1,0.

Malformed syntax re-prompts inside ConsoleUI. Correct syntax with an illegal game target returns to the controller's same-role retry loop. EOF throws InputEndedException; the controller reports Happy Hour/Host/Waiter/Chef as appropriate without a victory/defeat verdict.

The scripts compile from source into temporary directories with an installed JDK 17+. They preserve quoted javac argument files and support spaces, quotes and backslashes. check-build checks copied trees from another working directory, seven CLI fixtures, redirected stdin, prompt/duration counts, four EOF roles, PATH JDK discovery and invalid JAVA_HOME.

Report PDF, measured video timestamps, member reflections/declarations and publication are separate human/submission steps. No release/tag/commit is created by these local changes. New fixture grammar must accompany the controller/UI changes.

## Files to review / send

- [src/main/java/restaurantrush/boundary/ConsoleUI.java](../src/main/java/restaurantrush/boundary/ConsoleUI.java)
- [src/test/java/restaurantrush/AllTests.java](../src/test/java/restaurantrush/AllTests.java)
- [src/test/java/restaurantrush/ConsoleTests.java](../src/test/java/restaurantrush/ConsoleTests.java)
- [src/test/java/restaurantrush/ReplayTests.java](../src/test/java/restaurantrush/ReplayTests.java)
- [src/test/java/restaurantrush/ScriptedGameIO.java](../src/test/java/restaurantrush/ScriptedGameIO.java)
- [src/test/java/restaurantrush/ScriptedGameIOTests.java](../src/test/java/restaurantrush/ScriptedGameIOTests.java)
- [run.sh](../run.sh)
- [run-tests.sh](../run-tests.sh)
- [check-build.sh](../check-build.sh)
- [java-env.sh](../java-env.sh)
- [compile-sources.sh](../compile-sources.sh)
- [test-fixtures/FLIP_SCENARIO.md](../test-fixtures/FLIP_SCENARIO.md)
- [test-fixtures/README.md](../test-fixtures/README.md)
- [test-fixtures/base3/defeat-input.txt](../test-fixtures/base3/defeat-input.txt)
- [test-fixtures/base3/victory-input.txt](../test-fixtures/base3/victory-input.txt)
- [test-fixtures/defeat-input.txt](../test-fixtures/defeat-input.txt)
- [test-fixtures/flip-happy-hour-never-input.txt](../test-fixtures/flip-happy-hour-never-input.txt)
- [test-fixtures/flip-happy-hour-on-input.txt](../test-fixtures/flip-happy-hour-on-input.txt)
- [test-fixtures/regular-priority-input.txt](../test-fixtures/regular-priority-input.txt)
- [test-fixtures/victory-input.txt](../test-fixtures/victory-input.txt)
- [test-fixtures/victory-invalid-input.txt](../test-fixtures/victory-invalid-input.txt)
- [test-fixtures/vip-priority-input.txt](../test-fixtures/vip-priority-input.txt)

## Verification

Run from repository root:

```sh
JAVA_HOME=/opt/homebrew/opt/openjdk@17 sh "stage 3/run-tests.sh"
JAVA_HOME=/opt/homebrew/opt/openjdk@17 sh "stage 3/check-build.sh"
```

See [EVIDENCE](../EVIDENCE.md) for actual local results. Coordinate shared signatures and test registration before combining individual file sets. All four sets are needed for a complete Stage 3 build.

Diagram files use editable draw.io sources and matching PNG pages. See the [Stage 3 diagram index](../../diagrams/stage%203/README.md).
