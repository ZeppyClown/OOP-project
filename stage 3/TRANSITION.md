# Stage 2 → Stage 3

Baseline: annotated `stage-2`, commit `4711c313b455a0d88fb14553c936b0e25560b579`. Local changes only; there is no Stage 3 release/tag yet.

`stage 2/` is renamed to `stage 3/` for the current implementation. Earlier code remains at the Stage 2 tag. This is a logical inventory; Git may report renames or delete/add depending on similarity.

New features: HappyHour timing, Host independent seating, ComboMeal, standard/Happy Hour pricing policies, customer recovery, aggregate ordering/serving/preparation wrappers. Main uses Stage 3 metadata. GameIO defaults preserve older test doubles; ConsoleUI implements actual new prompts. Inherited tests explicitly use Menu(false) where they assert the original three-item rotation.

## Java changes

| Change | Path |
| --- | --- |
| Changed | `src/main/java/restaurantrush/Main.java` |
| Changed | `src/main/java/restaurantrush/boundary/ConsoleUI.java` |
| Changed | `src/main/java/restaurantrush/control/GameController.java` |
| Changed | `src/main/java/restaurantrush/control/GameIO.java` |
| Changed | `src/main/java/restaurantrush/control/GameView.java` |
| New | `src/main/java/restaurantrush/control/HappyHour.java` |
| New | `src/main/java/restaurantrush/domain/ComboMeal.java` |
| Changed | `src/main/java/restaurantrush/domain/Customer.java` |
| Changed | `src/main/java/restaurantrush/domain/CustomerStatus.java` |
| New | `src/main/java/restaurantrush/domain/HappyHourPricingPolicy.java` |
| New | `src/main/java/restaurantrush/domain/Host.java` |
| Changed | `src/main/java/restaurantrush/domain/Menu.java` |
| Changed | `src/main/java/restaurantrush/domain/MenuItem.java` |
| New | `src/main/java/restaurantrush/domain/PricingPolicy.java` |
| Changed | `src/main/java/restaurantrush/domain/Restaurant.java` |
| New | `src/main/java/restaurantrush/domain/StandardPricingPolicy.java` |
| Changed | `src/test/java/restaurantrush/AllTests.java` |
| Changed | `src/test/java/restaurantrush/ConsoleTests.java` |
| Changed | `src/test/java/restaurantrush/LauncherTests.java` |
| Changed | `src/test/java/restaurantrush/ReplayTests.java` |
| Changed | `src/test/java/restaurantrush/ScriptedGameIO.java` |
| Changed | `src/test/java/restaurantrush/ScriptedGameIOTests.java` |
| Changed | `src/test/java/restaurantrush/Stage2ControllerTests.java` |
| New | `src/test/java/restaurantrush/Stage3ControllerTests.java` |
| Changed | `src/test/java/restaurantrush/TestGameIO.java` |
| Changed | `src/test/java/restaurantrush/TestGameIOTests.java` |
| Changed | `src/test/java/restaurantrush/TurnTests.java` |
| Changed | `src/test/java/restaurantrush/domain/CustomerSubtypeTests.java` |
| Changed | `src/test/java/restaurantrush/domain/CustomerTests.java` |
| Changed | `src/test/java/restaurantrush/domain/OrderTests.java` |
| Changed | `src/test/java/restaurantrush/domain/PricingTests.java` |
| New | `src/test/java/restaurantrush/domain/RecoveryTests.java` |
| New | `src/test/java/restaurantrush/domain/RestaurantOrderingTests.java` |
| Changed | `src/test/java/restaurantrush/domain/RestaurantTests.java` |
| Changed | `src/test/java/restaurantrush/domain/StaffTests.java` |
| New | `src/test/java/restaurantrush/domain/Stage3ComponentTests.java` |

Other Java files retain the current Stage 2 implementation, including exact Money arithmetic, staff success-only capacity and timestamp guards. No production Java class is deleted logically.

## Scripts, fixtures and documents

run.sh/run-tests.sh identify Stage 3 and keep temporary builds/JDK discovery/escaped paths. check-build.sh now checks seven replays, conditional activation/duration and four EOF roles. java-env.sh and compile-sources.sh are carried forward unchanged. Five main fixtures adopt Stage 3 grammar; two Happy Hour flip fixtures and base3 regression fixtures are added. Fixture README and FLIP_SCENARIO explain the input contract.

Stage README/EVIDENCE/TRANSITION are replaced with current local facts. DESIGN, members/* and MEMBER_FILES.json are new. Root README navigates Stage 3. Stage 2 figures remain historical; Stage 3 editable draw.io figures and PNG exports are adapted from the local reference, with long sequences split across pages. No AI histories, personal branches or historical tags are changed.

Reference: `/Users/victorchua/Documents/OOP/stage-3`. Production differences from that reference retain Stage 2 formatting, diagnostics and small documentation fixes. The test suite also retains local ControlTests and four extra ConsoleTests, explaining 55 registered checks versus the reference's 50.
