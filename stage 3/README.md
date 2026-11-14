# Restaurant Rush — Stage 3

Stage 3 builds on the published Stage 2 implementation. The current source folder is `stage 3`; earlier source versions remain at the `stage-1` and `stage-2` tags.

```sh
sh "stage 3/run.sh"
sh "stage 3/run.sh" --script "stage 3/test-fixtures/victory-input.txt"
sh "stage 3/run-tests.sh"
sh "stage 3/check-build.sh"
```

Run from repository root with an installed JDK 17 or newer. Set JAVA_HOME if needed. No dependencies are downloaded. Build output is temporary.

Happy Hour may be activated once for two turns, recovering 15 satisfaction at each active turn start for still-waiting customers and discounting newly placed orders by 20% after customer discounts. A Host adds independent seating capacity. The default menu now includes a composed Salad + Burger combo.

| Responsibility | Member code guide |
| --- | --- |
| Startup, turn phases, typed IO, Happy Hour, sequence | [Victor](members/victor.md) |
| Customers, recovery, Restaurant ownership/wrappers, class model | [Danielle](members/danielle.md) |
| Host, menu/combo, pricing policies, staff/order checks | [Abraham](members/abraham.md) |
| Console, fixture grammar, test registration, portable execution | [Talon](members/talon.md) |

[Transition](TRANSITION.md) · [Evidence](EVIDENCE.md) · [Fixture grammar](test-fixtures/README.md) · [Verdict-flip scenario](test-fixtures/FLIP_SCENARIO.md) · [Stage 3 diagrams](../diagrams/stage%203/README.md) · [Design discussion](DESIGN.md)
