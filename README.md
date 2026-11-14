# Restaurant Rush

Plain Java console restaurant game. The current source is **Stage 2**, built upon Stage 1 in one `stage 2/` folder. Stage 1 is preserved by its Git tag.

## Runnable stages

| Stage | Source snapshot | Release |
|---|---|---|
| 1 | [stage-1](https://github.com/ZeppyClown/OOP-project/tree/stage-1) | Existing Stage 1 tag |
| 2 | [stage-2](https://github.com/ZeppyClown/OOP-project/tree/stage-2) | [Stage 2](https://github.com/ZeppyClown/OOP-project/releases/tag/stage-2) |

[Compare Stage 1 to Stage 2](https://github.com/ZeppyClown/OOP-project/compare/stage-1...stage-2). New files, changed files and design reasons are recorded in [the transition inventory](stage%202/TRANSITION.md). Stage 3 is not part of this release.

## Requirements and commands

Use an installed **JDK 17 or newer** and a POSIX shell. Set `JAVA_HOME` if `java` and `javac` are unavailable on PATH. For this Mac:

```sh
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
sh "stage 2/run.sh"
sh "stage 2/run-tests.sh"
sh "stage 2/check-build.sh"
sh "stage 2/run.sh" --script "stage 2/test-fixtures/victory-input.txt"
sh "stage 2/run.sh" --script "stage 2/test-fixtures/defeat-input.txt"
```

The scripts compile with `--release 17` into temporary directories. Tests enable assertions and supply the fixture directory. The expected integrated result is **40 registered checks passed**, with nested suites reporting their own scenario counts; the total is not a count of individual assertions.

## Gameplay

Play 18 turns. Customers arrive on turns 1, 4, 7, 10, 13 and 16. C2 is VIP, C3 is Critic and the remaining customers are Regular. Two tables are available.

Each turn asks for a Waiter action (`seat <customer ID>`, `order <customer ID>`, `serve <order ID>` or `wait`), then a Chef order ID or `wait`. Eating, at most one oldest eligible payment, and waiting-satisfaction loss follow automatically. Invalid syntax and refused actions retry without advancing the role's phase. EOF reports an interruption without a final verdict.

Food completed this turn can be served only on a later turn. Eating finishes after the serving turn; payment occurs after the turn eating finishes. The bill is locked when ordered. Only paid bills contribute revenue.

| Customer | Waiting loss per unserved turn | Food choice | Price |
|---|---:|---|---|
| Regular | 8 | ID rotation: Burger, Salad, Pasta | Full price |
| VIP | 5 | Inherited ID rotation | 90%, rounded HALF_UP to cents |
| Critic | 12 | Pasta | Full price |

Victory requires at least 4 paid customers, revenue of at least $45.00, and average paid satisfaction of at least 60. All three conditions must pass after turn 18.

## Reproducible outcomes

| Fixture in `stage 2/test-fixtures/` | Paid | Revenue | Average | Verdict |
|---|---:|---:|---:|---|
| `victory-input.txt` | 5 | $65.10 | 78.0 | VICTORY |
| `victory-invalid-input.txt` | 5 | $65.10 | 78.0 | VICTORY |
| `defeat-input.txt` | 0 | $0.00 | N/A | DEFEAT |
| `vip-priority-input.txt` | 1 | $8.10 | 90.0 | DEFEAT |
| `regular-priority-input.txt` | 1 | $8.10 | 85.0 | DEFEAT |

The priority pair changes VIP payment timing (turn 8 versus 9) and satisfaction, while both verdicts remain DEFEAT. It does not demonstrate a victory/defeat flip. See [fixture schedules](stage%202/test-fixtures/README.md) and [execution evidence](stage%202/EVIDENCE.md).

## Design and diagrams

`Main` supplies a customer factory. `GameController` coordinates the existing phases through `GameIO`, calls customer hooks for item choice/discount, and reaches Waiter/Chef through Restaurant getters. Customer and Order enforce lifecycle guards; Restaurant coordinates membership, tables and payment. Stage 2 has no Host, Happy Hour, ComboMeal or pricing-policy layer.

Editable draw.io sources and PNG exports are in [diagrams/stage 2](diagrams/stage%202):

| View | Editable source | PNG |
|---|---|---|
| Full class model | [draw.io](diagrams/stage%202/class-diagram.drawio) | [PNG](diagrams/stage%202/png/class-diagram.png) |
| Entities | [draw.io](diagrams/stage%202/class-entities.drawio) | [PNG](diagrams/stage%202/png/class-entities.png) |
| Menu/pricing | [draw.io](diagrams/stage%202/class-menu.drawio) | [PNG](diagrams/stage%202/png/class-menu.png) |
| Staff | [draw.io](diagrams/stage%202/class-staff.drawio) | [PNG](diagrams/stage%202/png/class-staff.png) |
| Control | [draw.io](diagrams/stage%202/class-control.drawio) | [PNG](diagrams/stage%202/png/class-control.png) |
| Customer subtypes | [draw.io](diagrams/stage%202/customer-subtypes.drawio) | [PNG](diagrams/stage%202/png/customer-subtypes.png) |
| VIP-priority sequence | [three-page draw.io](diagrams/stage%202/stage2-sequence.drawio) | [1](diagrams/stage%202/png/stage2-sequence-1.png), [2](diagrams/stage%202/png/stage2-sequence-2.png), [3](diagrams/stage%202/png/stage2-sequence-3.png) |

The full class model is a zoomable reference. Use the focused views in a report and check readability at final page size. Diagrams do not certify report/video or member approval.

## Team branches and AI records

| Member | Branch | AI records |
|---|---|---|
| Abraham | [Abraham](https://github.com/ZeppyClown/OOP-project/tree/Abraham) | [Records](https://github.com/ZeppyClown/OOP-project/tree/Abraham/ai-history) |
| Danielle | [Danielle](https://github.com/ZeppyClown/OOP-project/tree/Danielle) | [Records](https://github.com/ZeppyClown/OOP-project/tree/Danielle/ai-history) |
| Talon | [Talon](https://github.com/ZeppyClown/OOP-project/tree/Talon) | [Records](https://github.com/ZeppyClown/OOP-project/tree/Talon/ai-history) |
| Victor | [Victor](https://github.com/ZeppyClown/OOP-project/tree/Victor) | [Records](https://github.com/ZeppyClown/OOP-project/tree/Victor/ai-history) |

Member records retain their stated scope. This technical release does not establish completion of individual reflections, declarations, the group report or video.
