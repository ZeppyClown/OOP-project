# OOP-project: Restaurant Rush

SC2002 group project. Restaurant Rush is a turn-based console restaurant game written in plain Java with no external libraries. The manager plays 18 turns, assigning the Waiter and the Chef each turn while customers arrive, wait, eat and pay.

**Released stage:** Stage 1, tag [`stage-1`](https://github.com/ZeppyClown/OOP-project/releases/tag/stage-1).

## Team

| Member | Branch | AI history |
|---|---|---|
| Abraham | [`Abraham`](https://github.com/ZeppyClown/OOP-project/tree/Abraham) | [Abraham/ai-history](https://github.com/ZeppyClown/OOP-project/tree/Abraham/ai-history) |
| Danielle | [`Danielle`](https://github.com/ZeppyClown/OOP-project/tree/Danielle) | [Danielle/ai-history](https://github.com/ZeppyClown/OOP-project/tree/Danielle/ai-history) |
| Talon | [`Talon`](https://github.com/ZeppyClown/OOP-project/tree/Talon) | [Talon/ai-history](https://github.com/ZeppyClown/OOP-project/tree/Talon/ai-history) |
| Victor | [`Victor`](https://github.com/ZeppyClown/OOP-project/tree/Victor) | [Victor/ai-history](https://github.com/ZeppyClown/OOP-project/tree/Victor/ai-history) |

Each member keeps AI history for test-case work in an `ai-history/` folder on their own branch. Selected excerpts are labelled with their scope.

## Requirements

- **JDK 17 or newer.** The code is compiled with `--release 17`. Check with `javac -version` and `java -version`.
- A POSIX shell (macOS, Linux, or Git Bash on Windows) for the helper scripts. You can also use the plain `javac`/`java` commands below.
- If `java` is not on your `PATH`, set `JAVA_HOME` to the JDK folder. The scripts then use `$JAVA_HOME/bin`. For example, with Homebrew on macOS: `export JAVA_HOME=/opt/homebrew/opt/openjdk@17`.

## Layout

```
stage 1/
  src/main/java/restaurantrush/   Main, boundary (ConsoleUI), control (GameController, GameIO, …), domain
  src/test/java/restaurantrush/   plain-Java test suites, run together by AllTests
  test-fixtures/                  scripted console input: victory, defeat, victory with invalid lines
  diagrams/                       draw.io UML sources; PNG exports in diagrams/png/
  run.sh                          compile and play
  run-tests.sh                    compile and run every test
  EVIDENCE.md                     recorded test and replay results
```

## Stage 1: build and run

All commands are run from the repository root. Because the folder name `stage 1` contains a space, keep the quotes.

### With the scripts

```sh
sh "stage 1/run.sh"                                                         # play at the keyboard
sh "stage 1/run.sh" --script "stage 1/test-fixtures/victory-input.txt"      # replay a scripted game
sh "stage 1/run-tests.sh"                                                   # compile and run all tests
```

### By hand

```sh
# compile
javac --release 17 -d build/main $(find "stage 1/src/main/java" -name '*.java')
# play (interactive), or replay a script file
java -cp build/main restaurantrush.Main
java -cp build/main restaurantrush.Main --script "stage 1/test-fixtures/defeat-input.txt"
```

On bash/zsh, `$(find …)` splits the folder name `stage 1` at the space. If compiling by hand fails, use the scripts, or pass the file list through an argument file:

```sh
find "stage 1/src/main/java" -name '*.java' | sed 's/.*/"&"/' > build-main.txt
javac --release 17 -d build/main @build-main.txt
```

### Tests

The tests need **assertions enabled (`-ea`)** and the **fixture folder** passed as `-Dstage1.fixtures=<path>`. `run-tests.sh` does both. To run them by hand:

```sh
find "stage 1/src/main/java" -name '*.java' | sed 's/.*/"&"/' > build-main.txt
find "stage 1/src/test/java" -name '*.java' | sed 's/.*/"&"/' > build-test.txt
javac --release 17 -d build/main @build-main.txt
javac --release 17 -cp build/main -d build/test @build-test.txt
java -ea -Dstage1.fixtures="stage 1/test-fixtures" -cp build/main:build/test restaurantrush.AllTests
```

The expected result is `29 passed, 0 failed, 29 total`. On Windows, use `;` instead of `:` in the `-cp` list.

## How to play (Stage 1)

Each turn, the game shows the waiting queue, both tables, every order and the totals, then asks two questions:

- **Waiter:** `seat <C>`, `order <C>`, `serve <O>` or `wait`. `C` is a customer ID and `O` is an order ID; order IDs equal the customer's ID.
- **Chef:** an order ID to cook one preparation unit, or `wait`.

Eating, payment (oldest eligible customer first) and waiting-satisfaction loss then run automatically. An invalid command explains why and asks again without using up that staff member's turn. After turn 18 the game reports **VICTORY** if at least 4 customers paid, revenue is at least $45.00, and average paid satisfaction is at least 60. Otherwise it reports **DEFEAT**. If input ends early, the game stops as *interrupted* with no result.

## Reproducible results

| Script | Result |
|---|---|
| `stage 1/test-fixtures/victory-input.txt` | VICTORY: 5 paid, 5 served, $66.00, average paid satisfaction 79.2, 0 departures |
| `stage 1/test-fixtures/defeat-input.txt` | DEFEAT: 0 paid, 0 served, $0.00, average N/A, 2 unhappy departures |

The full recorded output and the commit it was produced from are in [`stage 1/EVIDENCE.md`](stage%201/EVIDENCE.md). The fixture schedules are explained in [`stage 1/test-fixtures/README.md`](stage%201/test-fixtures/README.md).

## UML (Stage 1)

Each diagram has a draw.io source in [`stage 1/diagrams/`](stage%201/diagrams) and a PNG in [`stage 1/diagrams/png/`](stage%201/diagrams/png).

| Diagram | Shows |
|---|---|
| [`class-diagram`](stage%201/diagrams/png/class-diagram.png) | **Full Stage 1 class diagram:** every class, member and relationship, grouped into the boundary, control and domain packages |
| [`class-entities`](stage%201/diagrams/png/class-entities.png) | Restaurant, tables, customer lifecycle and orders |
| [`class-menu`](stage%201/diagrams/png/class-menu.png) | Menu, menu items and price locking |
| [`class-staff`](stage%201/diagrams/png/class-staff.png) | Staff, Waiter, Chef and Cashier, and the staff each Restaurant is given |
| [`class-control`](stage%201/diagrams/png/class-control.png) | Main, GameController, GameIO / ConsoleUI and the controller's calls into Waiter and Chef |
| [`stage1-sequence`](stage%201/diagrams/png/stage1-sequence.png) | Sequence diagram: customer C3 through the victory replay (turns 7–18), with retries, guards and both Waiter-priority paths |
| [`current-starter`](stage%201/diagrams/png/current-starter.png) | The original starter code, before Stage 1 (for comparison) |
