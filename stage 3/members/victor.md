# Victor: Stage 3 responsibility and code handoff

Mapped to the supplied Victor Member Pack. This records technical changes and checks; it does not supply personal reflection, approval or authorship claims.

The controller runs arrival → activation choice → eligible +15 recovery → Host → Waiter → Chef → eating → oldest eligible payment → waiting loss → summary. It evaluates the result after turn 18. HappyHour stores one activation timestamp: the activation turn and following turn are active; activation cannot be reused. Activation on turn 18 does not add turn 19.

Main injects `Main::createCustomer`: C2 VIP, C3 Critic, all other arrivals Regular. Arrivals are turns 1,4,7,10,13,16. The loop uses Customer and PricingPolicy abstractions. Restaurant wrappers handle all order/serve/prepare actions; controller no longer calls staff getters.

Refused Host/Waiter/Chef actions retry in the same phase. EOF reports the current role and skips remaining phases/final verdict, preserving earlier changes. For example, Waiter EOF can occur after successful Host seating.

Selected trace: turn 13 C5 arrives, optional activation/recovery, Host seats C5, Waiter orders C3 Pasta, Chef prepares its first unit. Turn 14 C3 gets unit two and C5 orders Burger. Turn 15 C3 is served and C5 becomes READY. Turn 16 C5 is served and C3 finishes eating. Turns 17 and 18 collect C3 and C5, releasing tables. Food READY, eating completion and payment each require a later turn.

Timestamp-based Happy Hour avoids counters drifting apart, but the controller still needs edits for new phases. Stage metadata is display metadata; the Stage 3 controller always runs Stage 3 phases even in inherited regression tests.

## Files to review / send

- [src/main/java/restaurantrush/Main.java](../src/main/java/restaurantrush/Main.java)
- [src/main/java/restaurantrush/control/GameController.java](../src/main/java/restaurantrush/control/GameController.java)
- [src/main/java/restaurantrush/control/GameIO.java](../src/main/java/restaurantrush/control/GameIO.java)
- [src/main/java/restaurantrush/control/GameView.java](../src/main/java/restaurantrush/control/GameView.java)
- [src/main/java/restaurantrush/control/HappyHour.java](../src/main/java/restaurantrush/control/HappyHour.java)
- [src/test/java/restaurantrush/TestGameIO.java](../src/test/java/restaurantrush/TestGameIO.java)
- [src/test/java/restaurantrush/TestGameIOTests.java](../src/test/java/restaurantrush/TestGameIOTests.java)
- [src/test/java/restaurantrush/TurnTests.java](../src/test/java/restaurantrush/TurnTests.java)
- [src/test/java/restaurantrush/Stage2ControllerTests.java](../src/test/java/restaurantrush/Stage2ControllerTests.java)
- [src/test/java/restaurantrush/Stage3ControllerTests.java](../src/test/java/restaurantrush/Stage3ControllerTests.java)
- [src/test/java/restaurantrush/LauncherTests.java](../src/test/java/restaurantrush/LauncherTests.java)

- [src/test/java/restaurantrush/control/ControlTests.java](../src/test/java/restaurantrush/control/ControlTests.java)

## Verification

Run from repository root:

```sh
JAVA_HOME=/opt/homebrew/opt/openjdk@17 sh "stage 3/run-tests.sh"
JAVA_HOME=/opt/homebrew/opt/openjdk@17 sh "stage 3/check-build.sh"
```

The integrated local tree passed 55 registered checks and all build/replay checks. The inherited EVIDENCE file on this partial branch describes Stage 2; final Stage 3 evidence must be supplied at team integration. Coordinate shared signatures and test registration before combining individual file sets. All four sets are needed for a complete Stage 3 build.

Diagram files use editable draw.io sources and matching PNG pages. See the [Stage 3 diagram index](../../diagrams/stage%203/README.md).
