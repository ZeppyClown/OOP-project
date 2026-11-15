# Restaurant Rush — Group report

**Members:** Victor Chua, Danielle, Abraham and Talon  
**Module:** SC2002 Object-Oriented Design and Programming  
**Report snapshot:** 15 November 2026, `main` at [`699b317`](https://github.com/ZeppyClown/OOP-project/commit/699b3173444e7efae4b324101130d73ce1252577)

This report explains how Restaurant Rush developed from a basic restaurant simulation into a game with different customer behaviours, an independent seating role and a timed pricing decision. We focus on the responsibilities that stayed stable, the changes that were needed, and the evidence behind those choices.



## 1. The game and the decisions it creates

Restaurant Rush is an offline Java command-line game played over eighteen turns. Six customers arrive on turns 1, 4, 7, 10, 13 and 16. There are two tables and limited staff capacity, so the manager cannot give every customer immediate attention. The manager decides who to seat, whose order to take, what the Chef prepares and which ready order to serve. Customers choose their own dishes using a documented deterministic rule.

Winning requires all three conditions at the end of turn 18: at least four paid customers, revenue of at least $45 and average satisfaction of at least 60 among paying customers. The game does not end early when a target is reached. Revenue includes paid orders only; the satisfaction average uses the value recorded at payment. With no paid customers, the display shows N/A.

The main pressure comes from the relationship between capacity and waiting. Each staff instance can complete one successful task per turn. A refusal leaves that task available, while an unserved customer continues losing satisfaction. Reaching zero causes departure, cancellation of an unfinished order and table release. A table otherwise remains occupied until payment. Serving a customer can therefore protect their satisfaction without immediately freeing their table.

## 2. Responsibilities and object relationships

The boundary, control and domain layers give us a way to keep decisions in one place without making the console responsible for the rules.

| Abstraction | Responsibility | Concrete example |
| --- | --- | --- |
| `Main` | Launch arguments, input streams and object assembly | Supplies the same Restaurant and GameIO references to the controller |
| `GameIO` / `ConsoleUI` | Typed interaction contract / text parsing and output | Syntax errors are retried before returning a typed action |
| `WaiterAction`, `GameView` | Action values and immutable display snapshots | UI receives snapshots rather than a mutable customer collection |
| `GameController` | Scheduling, phase order and semantic retries | Keeps a refused Host choice inside the Host phase |
| `HappyHour` | Once-only activation and derived active window | Activation on turn 13 is active on turns 13 and 14 and expired on turn 15 |
| `Restaurant` | Ownership, queue/table consistency, staff wrappers and totals | Rejects a foreign customer before taking an order |
| `Customer` and its subtypes | Shared lifecycle with variable choice, discount and waiting loss | VIP overrides discount; Critic chooses Pasta |
| `Staff` and role subclasses | Common task-availability rule and role actions | Host seating does not consume the Waiter's task |
| `MenuItem`, `ComboMeal`, `Menu` | Dish values, shared component references and offered menu | Salad + Burger costs 2160 cents and needs 2 units |
| `PricingPolicy` implementations | Quote an order using the selected policy | Customer discount precedes Happy Hour |
| `Order` | Locked bill, preparation progress and service/payment guards | A READY order cannot be served on its completion turn |
| `Table` | Occupancy and validated seating/release | Holds zero or one customer |
| `Money` | Integer-cent percentage rounding and display | VIP Salad 900 → 810; under Happy Hour → 648 |

The important relationships are more specific than “these classes use each other.” `ConsoleUI` realises `GameIO`; the customer variants inherit from `Customer`; and Host, Waiter, Chef and Cashier inherit from `Staff`. `ComboMeal` extends `MenuItem`, which lets the existing order and preparation code process it.

Restaurant creates its two tables, so they are a clear composition example. A customer has zero or one order and creates it after successful placement. An order references exactly one customer and one menu item. A table has zero or one occupant. Restaurant retains a history of zero or more registered customers. Main injects Waiter, Chef and Cashier; merely storing those references does not establish composition. Restaurant's four-argument constructor creates a default Host, while a five-argument overload supports supplying one.

A combo stores two actual menu-item references. They are shared with the offered menu, so the UML should show shared aggregation rather than exclusive ownership by the combo. The everyday phrase “made of two items” does not, by itself, justify a filled composition diamond.

### Class-diagram evidence

Figure 1 shows the original control separation. Figure 2 shows the customer subtype extension; Figure 3 shows the factory and later timing/policy dependencies. The final domain views supplement the control view; no one figure is expected to carry every readable detail.

![Figure 1. Stage 1 focused control view, matched to the published Stage 1 tag.](../diagrams/stage%201/report-print/png/class-stage1-control-report.png)

![Figure 2. Stage 2 customer extension; factory injection is described in Section 4.](../diagrams/stage%202/report-print/png/class-stage2-customer-report.png)

![Figure 3. Stage 3 control collaboration, selected members.](../diagrams/stage%203/report-print/class/png/class-control-report-1.png)

![Figure 3b. Stage 3 interaction contract and its implementations.](../diagrams/stage%203/report-print/class/png/class-control-report-2.png)

![Figure 4. Stage 3 entity view: customer, order and table relationships.](../diagrams/stage%203/report-print/class/png/class-entities-report-1.png)

![Figure 5. Stage 3 staff view: Host has independent capacity under Staff.](../diagrams/stage%203/report-print/class/png/class-staff-report.png)

![Figure 6. Stage 3 menu view: ComboMeal and the pricing abstraction.](../diagrams/stage%203/report-print/class/png/class-menu-report-1.png)

Editable sources and the complete navigation view are in the [Stage 3 diagram index](../diagrams/stage%203/README.md). The complete overview is not a substitute for these focused views. 

## 3. Stage 1: establishing the lifecycle

The first stage contains Regular customers and the three base dishes: Burger at 1500 cents with one preparation unit, Salad at 900 cents with one unit, and Pasta at 1800 cents with two. The Waiter chooses one of seating, ordering or serving. The Chef advances one order by one preparation unit. The Cashier automatically collects one oldest-eligible payment after the staff choices.

A Burger example makes the delays explicit:

| Turn | Calls and effect |
| ---: | --- |
| 1 | Register C1; `Restaurant.seatCustomer` delegates through Waiter, Table and Customer |
| 2 | Waiter takes C1's order; Chef advances its one unit; order becomes READY |
| 3 | Waiter serves; order and customer become SERVED |
| 4 | Automatic eating completes; customer becomes READY_TO_PAY |
| 5 | Cashier collects; customer/order become PAID; Restaurant releases the table |

Three guards protect these separate delays: `readyTurn < turn`, `servedTurn < turn`, and `readyToPayTurn < turn`. The controller's phase order supports them, but the domain validates them as well. This matters because the domain classes may also be called directly by tests.

The console rejects malformed syntax. The controller/domain reject an action whose syntax is valid but whose target or state is wrong. A refusal is a String reason; success is null. The controller displays the reason and repeats the same phase. Staff call `markTaskUsed` only after success. EOF is different: it interrupts the game, preserves earlier changes and produces no final victory/defeat report.

The published Stage 1 evidence records 29 registered checks, a victory with five paid customers, $66.00 and 79.2 average satisfaction, and an all-wait defeat with no paid customers. 

## 4. Stage 1 → Stage 2: customer behaviour without a new turn system

Stage 2 introduces `VIPCustomer` and `CriticCustomer`. VIP waiting loss is 5 per unserved turn and the discount is 10%; Critic waiting loss is 12 and its preference is Pasta. Regular waiting loss remains 8. All three use the same guarded customer lifecycle.

Main supplies `Main::createCustomer` as an `IntFunction<Customer>`. Scheduling remains `(turn - 1) % 3 == 0`, with ID `(turn - 1) / 3 + 1`. C2 is VIP, C3 is Critic and the other arrivals are Regular. The controller rejects a null or wrong-ID factory result before registration. During Stage 2 ordering it invokes the customer hooks, allowing dynamic dispatch to select behaviour.

| Added class | Why it was added |
| --- | --- |
| `VIPCustomer` | Encapsulates its waiting loss and discount |
| `CriticCustomer` | Encapsulates its waiting loss and Pasta preference |

| Existing class modified | Reason |
| --- | --- |
| `Main` | Supplies subtype construction through a factory |
| `GameController` | Receives/validates factory results and calls the discount hook |
| `Customer` | Adds the default discount operation |
| `Money` | Adds half-up percentage calculation using widened integer arithmetic |

The 13 unchanged classes are ConsoleUI, GameView, InputEndedException, Cashier, Chef, Menu, MenuItem, Order, RegularCustomer, Restaurant, Staff, Table and Waiter. No production class is deleted. The [published comparison](https://github.com/ZeppyClown/OOP-project/compare/stage-1...stage-2) and [historical transition inventory](https://github.com/ZeppyClown/OOP-project/blob/stage-2/stage%202/TRANSITION.md) show the actual source transition, including folder changes.

The Stage 2 priority pair gives a small, useful consequence: prioritising C2 yields payment on turn 8 with satisfaction 90; delaying C2 yields payment on turn 9 with satisfaction 85. Both finish in defeat with one paid customer and $8.10. This demonstrates a timing/satisfaction change, rather than a verdict flip. The Stage 2 victory fixture records five paid, $65.10 and 78.0. Its local suite records 40 registered checks.

## 5. Stage 2 → Stage 3: changing the workflow where necessary

Stage 3 adds independent seating, a composed menu offering and the manager's once-only Happy Hour decision. These are different kinds of extension, so they use different mechanisms.

| Added class | Responsibility and reuse |
| --- | --- |
| `Host` | Reuses Staff availability but has its own task timestamp |
| `ComboMeal` | Extends MenuItem and supplies combined values to the existing Order/Chef flow |
| `HappyHour` | Stores one activation turn and derives the two-turn window |
| `StandardPricingPolicy` | Quotes the customer-discounted price |
| `HappyHourPricingPolicy` | Applies the customer discount followed by 80%, rounding at each step |

Stage 3 also adds the `PricingPolicy` interface. It is disclosed separately because an interface is not a class.

| Existing class modified on current main | Reason |
| --- | --- |
| `Main` | Passes Stage 3 metadata |
| `ConsoleUI` | Supports activation, Host choices and status output |
| `GameController` | Adds activation/recovery/Host phases and selects a policy; uses Restaurant wrappers |
| `Customer` | Adds capped waiting recovery; uses `% choices.size()` for the expanded menu |
| `Restaurant` | Coordinates Host seating, recovery and ownership-checked order/serve/prepare actions |
| `Menu` | Offers the combo; keeps `Menu(false)` for base-menu regression scenarios |
| `MenuItem` | Becomes extensible while retaining final value getters |
| `GameView` | Documentation only |
| `Money` | Formatting/comment changes in current main; calculation is unchanged |

The 10 byte-unchanged classes are InputEndedException, Cashier, Chef, CriticCustomer, Order, RegularCustomer, Staff, Table, VIPCustomer and Waiter. Money is behaviourally unchanged but is counted as text-modified above. This distinction avoids presenting a tidier count than the published source supports.

### Change-count convention

The counts below compare exact file bytes at the published Stage 1/2 tags and current main, using logical package paths across the folder rename. They count top-level production classes, including abstract and exception classes, and exclude interfaces, enums, records, nested types and tests.

| Transition | Before | Added | Existing modified | Existing unchanged | After |
| --- | ---: | ---: | ---: | ---: | ---: |
| Stage 1 → Stage 2 | 17 | 2 | 4 | 13 | 19 |
| Stage 2 → current Stage 3 main | 19 | 5 | 9 | 10 | 24 |

For 2 → 3, seven existing classes have substantive code changes; GameView is documentation-only and Money has presentation changes. The earlier prepared handoff comparison gave 8 modified/11 unchanged because Money had not yet been reformatted. The current main comparison is 9/10. Neither count should be used without identifying its source snapshot.

Counting all top-level production types instead gives 1 → 2: 2 added, 4 modified, 18 unchanged, 24 total; and 2 → 3: 6 added, 11 modified, 13 unchanged, 30 total. GameIO is a changed interface and CustomerStatus has comment/whitespace changes. The exact inventory is retained in [the comparison CSV](evidence/production-type-change-map.csv).

### Timing, quotation and encapsulation

A Stage 3 turn runs arrival → activation choice → eligible recovery → Host → Waiter → Chef → eating → at most one payment → waiting loss → summary. Snapshot callbacks occur around the choices and automatic phases. The Host can seat a customer whom the Waiter orders for in the same turn. A Cashier-released table cannot retroactively enable an earlier seating action.

Happy Hour is active on the activation turn and the following turn. A turn 13 activation displays 2, 1, 0 remaining on turns 13, 14, 15. Activation is allowed once. Activation on turn 18 does not extend the game; the displayed window still says 2 although only one game turn remains, which is a presentation limitation.

Restaurant first checks membership and seated state when taking an order. It then calls the customer for an item, calls the selected policy for a quote, and calls the Waiter to place it. The Waiter calls Customer, which creates the Order with its final bill. Serving and preparation also pass through Restaurant wrappers. The controller no longer obtains Waiter/Chef objects to perform those actions directly.

Integer cents make quotation reproducible. A VIP Salad is 900 → 810 cents, or 900 → 810 → 648 under Happy Hour. The default Salad+Burger combo is 90% of 2400 cents: 2160, with 2 preparation units. A VIP combo would be 2160 → 1944 → 1555 under Happy Hour; that is a component-test example, not a dish selected by the default VIP C2. Each percentage step rounds half-up, and later activation/expiry cannot change an existing bill.

## 6. Selected sequence: one manager choice changes the verdict

The chosen use case is **activate Happy Hour on turn 13 rather than never activating it**. Both scripts keep the same Host, Waiter and Chef choices. The never-active script needs additional `no` lines after turn 13 because it continues receiving activation prompts; these are not different staff decisions.

After turn 12, C1 and C2 have paid. C3, a Critic, is seated with satisfaction 28. C4 is still queued and is never seated. C5 arrives on turn 13 and C6 on turn 16; C4 and C6 never pay.

| Turn | Interaction and consequence |
| ---: | --- |
| 13 | Factory creates/registers C5. Optional activation precedes recovery. Host seats C5, bypassing the earlier queued C4. Waiter orders C3's Pasta; bill locks at 1440 cents with Happy Hour or 1800 without. Chef completes unit 1. |
| 14 | Second active recovery occurs only on the active path. Waiter orders C5's Burger at 1200 or 1500 cents. Chef completes Pasta unit 2, making O3 READY. |
| 15 | Waiter serves O3 through Restaurant and Customer; the READY timestamp is earlier. Chef prepares O5 to READY. |
| 16 | C6 arrives. Waiter serves O5. Automatic eating makes C3 READY_TO_PAY, but payment is not yet allowed. |
| 17 | C5 finishes eating. Cashier collects C3's payment; Restaurant releases its table. |
| 18 | Cashier collects C5's payment; Restaurant releases its table. After the completed turn, the controller requests the result. |

C3's active-path satisfaction is28 → 43 after recovery → 31 after loss → 46 after the next recovery → 34 after loss. It is served on turn 15 at 34. Without activation, it falls28 → 16 → 4 and is served at 4. Service does not add15 satisfaction; the bonus is start-of-turn recovery.

| Path | Paid | Revenue | Paid satisfaction total | Average | Result |
| --- | ---: | ---: | ---: | ---: | --- |
| Activate on turn 13 | 4 | $49.50 | 269 | 67.3 | VICTORY |
| Never activate | 4 | $56.10 | 231 | 57.8 | DEFEAT |

Both satisfy the revenue and paid-count conditions. Satisfaction decides the outcome, so the lower-revenue run wins. The scenario intentionally delays service to approach the threshold; it demonstrates a possible manager-choice consequence, not an optimal strategy.

The focused sequence includes the real CLI, controller, factory, timing, Restaurant, staff and domain participants. Calls and returns should follow the real callers: Restaurant asks the Host or Waiter to seat, and that staff member calls Table, which calls Customer. Restaurant calls Table directly to release it after payment or departure. PricingPolicy calls the customer’s discount hook, and Waiter calls Customer to place the order. Relevant notation includes the 18-turn loop, active/inactive alternatives, successful/refused action branches and same-phase retries. Optional invalid-action branches illustrate possible controller paths, not extra events in the valid fixtures.

The selected scenario is split into readable panels with no more than eight lifelines. Each panel repeats the enclosing turn-loop context; the final result is explicitly outside that loop. All panels and editable sources are included in [the figure appendix](FIGURE_APPENDIX.md).

## 7. Design choices, alternatives and limits

| Principle | Where it helps | Limit we acknowledge |
| --- | --- | --- |
| SRP | Parsing in ConsoleUI; sequencing in GameController; preparation in Order; quoting in policies | Restaurant combines coordination, ownership and reporting |
| OCP | Customer hooks and policy implementations support behavioural variation | New phases still require controller/IO edits; new policy selection may require wiring |
| LSP | ComboMeal uses ordinary MenuItem/Order/Chef operations; customer lifecycle is shared | Critic assumes Pasta exists; a custom menu without it exposes a stronger precondition |
| ISP | PricingPolicy has one focused quote operation | GameIO combines input and display; defaults can hide missing implementations |
| DIP | Injected GameIO and customer factory; Restaurant accepts PricingPolicy | Controller still constructs concrete timing/policy objects and its legacy constructor references RegularCustomer |

We considered mutable active/remaining counters for Happy Hour. They are straightforward but can drift if one is updated in the wrong phase. One activation timestamp derives both active state and remaining duration, leaving fewer independent values to keep consistent. It still needs validation and an explicit once-only guard.

For combos, a separate combo-order hierarchy would model component dishes more independently. The required rules only need summed preparation and a combined price, so that hierarchy would duplicate preparation, service and payment paths. Extending MenuItem reuses those paths. MenuItem remains non-final, but its value getters are final to keep those values stable.

The Stage 2 direct controller-to-staff calls kept Restaurant's API smaller. Stage3 wrappers trade a larger aggregate API for one consistent place to check membership. This does not create absolute encapsulation: Customer's recovery method is still public, and Restaurant remains broad. Holders of injected Waiter/Chef references can also call their public action methods directly; getCashier/getHost are public even though the underlying payment/seating mutations are package-private. The controller uses the wrappers, but these APIs do not enforce an absolute aggregate boundary. Those are reasonable candidates for later refinement rather than evidence that every SOLID principle is completely satisfied.

A typed `ActionResult` could replace null/String refusals. It would make success and failure more explicit and reduce the chance of ignoring a returned reason, but changing it now would affect action APIs throughout the game. Likewise, making every new GameIO method abstract would catch missing implementations at compilation, but would require updating older test doubles. Defaults preserve compatibility; the console override test addresses part of the resulting risk.

### Method-level evidence for the design discussion

The claims above refer to specific operations, not just class names. `ConsoleUI.chooseWaiterAction()` parses the command before `GameController.performWaiter()` validates its meaning (SRP). `Customer.loseWaitingSatisfaction()` calls the subtype hook `waitingLossPerTurn()` (OCP and dynamic dispatch). `ComboMeal` supplies values through final MenuItem getters used by `Order.advancePreparation()` (the supported substitution example). `PricingPolicy.priceFor()` is the small focused contract (ISP). The GameController constructor accepts GameIO and IntFunction<Customer>, and Restaurant.takeOrder accepts PricingPolicy (DIP), while concrete assembly dependencies remain.

A later refactor could extract a scoreboard/result evaluator from Restaurant. That would improve cohesion, but it is a proposed alternative rather than a change needed to satisfy the current rules.

## 8. Functional and integration evidence

The final restored tree was checked before the last push. The actual logs are retained at [run-tests.sh.log](evidence/run-tests.sh.log) and [check-build.sh.log](evidence/check-build.sh.log). 

| Evidence | Result and scope |
| --- | --- |
| Full registered suite | 55 passed, 0 failed; registered cases contain additional nested assertions/scenarios |
| Portable copied-source build | Passes in plain and complex paths containing spaces, quotes and a backslash |
| Replay/console checks | Seven main fixtures, redirected stdin, duration/prompt counts and EOF in four roles |
| JDK discovery | Finds installed JDK on PATH without JAVA_HOME; invalid JAVA_HOME produces a useful error |
| Normal Stage3 victory | 5 paid,$73.38, 89.4 average,VICTORY |
| Happy Hour pair | 4/$49.50/67.3/VICTORY versus4/$56.10/57.8/DEFEAT |

To reproduce from current repository root with JDK 17 or newer:

```sh
sh "stage 3/run-tests.sh"
sh "stage 3/check-build.sh"
sh "stage 3/run.sh" --script "stage 3/test-fixtures/victory-input.txt"
sh "stage 3/run.sh" --script "stage 3/test-fixtures/flip-happy-hour-on-input.txt"
sh "stage 3/run.sh" --script "stage 3/test-fixtures/flip-happy-hour-never-input.txt"
```

On the reviewed Mac, JAVA_HOME was `/opt/homebrew/opt/openjdk@17`; other machines should use their own installed JDK. No network download is required to run the Java game.

The integration audit found that merges had reintroduced `%3` menu selection, removed a test import, widened staff getters, dropped final menu getters and replaced Stage3 test expectations. The saved four handoffs restored the intended behaviour. Independent review tested the combined files, and the final restoration was published as focused repair commits ending at 699b317. 

## Console excerpts from the integrated source

### victory-invalid-input

```text
Turns used: 18 / 18

===== VICTORY =====
Served: 6
Paid: 5
Unhappy departures: 0
Revenue: $73.38
Average satisfaction of paid customers: 89.4
Turns used: 18 / 18
[PASS] Paid customers: required 4, actual 5
[PASS] Revenue: required $45.00, actual $73.38
[PASS] Average paid satisfaction: required 60, actual 89.4
```

Refusal-and-retry messages from the same completed run:

```text
Host (<customer ID> or wait)> Not allowed: Customer 99 has not arrived; use 'wait' to Wait - choose again.
Waiter (seat <C>, order <C>, serve <O>, wait)> Not allowed: Order 99 does not exist - choose again.
Chef (<order ID> or wait)> Not allowed: Order 99 does not exist; use 'wait' to Wait - choose again.
```

### flip-happy-hour-on-input

```text
Turns used: 18 / 18

===== VICTORY =====
Served: 4
Paid: 4
Unhappy departures: 0
Revenue: $49.50
Average satisfaction of paid customers: 67.3
Turns used: 18 / 18
[PASS] Paid customers: required 4, actual 4
[PASS] Revenue: required $45.00, actual $49.50
[PASS] Average paid satisfaction: required 60, actual 67.3
```

### flip-happy-hour-never-input

```text
Turns used: 18 / 18

===== DEFEAT =====
Served: 4
Paid: 4
Unhappy departures: 0
Revenue: $56.10
Average satisfaction of paid customers: 57.8
Turns used: 18 / 18
[PASS] Paid customers: required 4, actual 4
[PASS] Revenue: required $45.00, actual $56.10
[FAIL] Average paid satisfaction: required 60, actual 57.8
```

## 9. Repository evidence and submission status

### Source lineage

The implementation and diagram material carry forward Restaurant Rush work from the team’s earlier repository, [SC2002MACS2G2](https://github.com/abra0006-glitch/SC2002MACS2G2), which is the repository checked out locally as `Documents/OOP`. That material was integrated into [OOP-project](https://github.com/ZeppyClown/OOP-project), with local controller, testing, build-portability, documentation and diagram changes.

### Runnable references

| Stage | Runnable source reference | Evidence status |
| --- | --- | --- |
| 1 | [`stage-1`](https://github.com/ZeppyClown/OOP-project/tree/stage-1), 9197f24 | Published tag/release; historical 29-check evidence |
| 2 | [`stage-2`](https://github.com/ZeppyClown/OOP-project/tree/stage-2), 4711c31 | Published tag/release; historical 40-check evidence |
| 3 | [current integrated commit 699b317](https://github.com/ZeppyClown/OOP-project/tree/699b3173444e7efae4b324101130d73ce1252577) | 55-check/build evidence; stage-3 tag/release not yet created |

The [1 → 2 comparison](https://github.com/ZeppyClown/OOP-project/compare/stage-1...stage-2) and [2 → current 3 comparison](https://github.com/ZeppyClown/OOP-project/compare/stage-2...699b3173444e7efae4b324101130d73ce1252577) retain the real source history. The Stage 2 folder becomes Stage 3 in current main; earlier code remains runnable at its tag. Folder renaming is not counted as a new logical Java class. New/modified production files are named in Sections 4–5; the complete type inventory, including unchanged types and the absence of logical deletions, is in the CSV.



### Recorded presentation

The supplied recording is **Video Project.mp4**, 16 minutes 25 seconds at 1920×1080 with an audio track. Its terminal demonstration uses the earlier repository layout. The submitted source uses the commands in Section 8. A separate clarified copy adds captions distinguishing the current 55-check suite and 9/10 text-change count, and qualifying the OCP/LSP/aggregation claims; it does not alter the demonstrated CLI outcomes. [Video inspection notes](VIDEO_REVIEW.md) record the observed segments and remaining evidence limitations.

## 10. Individual reflections

Each member discusses their assigned area, the decisions they worked through and the limits they identified.


### Victor

My area of the project was startup, turn control, the IO contract, controller tests and the selected sequence diagram. Across the three stages, the question I kept coming back to was whether the objects were doing the right things in the right order. A calculation could be correct on its own and still produce the wrong game if it happened in the wrong phase.

#### Stage 1: learning to follow a whole turn

##### What did I learn?

Stage 1 helped me understand the difference between coordinating an action and owning the rule behind it. `GameController` decides when the Waiter and Chef act, but it should not directly set an order to READY or a customer to PAID. Those changes belong to the domain objects, which check whether the transition is legal. Separating those responsibilities made the flow easier to explain because I could follow a call from the controller down to the object that actually changed state.

I also learned why phase order alone is not enough. The Waiter runs before the Chef, which prevents the usual same-turn preparation-and-service shortcut. However, the domain still needs timestamp guards. Otherwise, a different caller could bypass the intended delay. The same reasoning applies to eating and payment.

##### How did I apply it?

A useful example is C1's Burger order. The Waiter seats C1 on turn 1, takes the order on turn 2, and the Chef completes the Burger that turn. Service follows on turn 3, eating finishes on turn 4, and payment happens on turn 5. Following that small example made the separate guards meaningful: `readyTurn < turn`, `servedTurn < turn`, and `readyToPayTurn < turn` each protect a different boundary.

The IO interface gave the controller a way to ask for choices without parsing console text itself. `Main` supplies the Restaurant and ConsoleUI references, and the controller uses the latter through `GameIO`. This is where dependency inversion became a practical decision for me: the interaction can be replaced by a test double while the same controller continues to run the game.

##### What challenged me?

The less obvious part was handling actions that look like valid commands but cannot be carried out. `seat 99` is understandable input, but customer 99 has not arrived. Treating that as a completed action would waste a staff task. It would also make the output misleading because the game would advance even though nothing useful happened.

EOF raised a related question. An unfinished input stream is not the same as choosing Wait for the rest of the game. Producing a final defeat after silently filling in missing decisions would describe a game the manager never played.

##### How did I solve it?

The useful approach was to trace both a successful action and a refusal through the same phase. For `seat 99`, the customer lookup fails, the controller displays the reason, and it asks the Waiter again without advancing the turn. Staff record a task only after success. Syntax errors are handled earlier by the console, before a typed action is returned.

For EOF, the controller catches `InputEndedException`, reports the current role and stops without a victory or defeat verdict. Earlier changes are retained; the game does not pretend that an interrupted turn completed. The lesson I took from this was that the refusal and interruption paths deserve the same attention as the successful lifecycle.

**Supporting examples:** the [Stage 1 controller](https://github.com/ZeppyClown/OOP-project/blob/stage-1/stage%201/src/main/java/restaurantrush/control/GameController.java) and [turn tests](https://github.com/ZeppyClown/OOP-project/blob/stage-1/stage%201/src/test/java/restaurantrush/TurnTests.java).

#### Stage 2: separating customer construction from customer behaviour

##### What did I learn?

Stage 2 made the purpose of polymorphism clearer. The controller needs to know when a customer arrives, but it does not need to decide how every customer type loses satisfaction or chooses food. Those are different responsibilities. Once the controller holds a `Customer` reference, calls can dispatch to the actual Regular, VIP or Critic object.

This also helped me be more careful with claims about extensibility. Adding a customer subtype can avoid changes to the turn loop, but the program still needs a way to construct that subtype. There is still configuration work; the benefit is that it does not spread through every phase of the game.

##### How did I apply it?

The factory supplied as `Main::createCustomer` separates arrival scheduling from subtype selection. On turn 4, the controller calculates ID 2 and asks the factory for that customer. Main's factory returns a VIP, while the controller validates that the returned object is non-null and has the expected ID.

During ordering, the Stage 2 controller calls `chooseItem()` and `applyDiscount()` through `Customer`. It does not ask whether the object is a VIP or Critic. The VIP discount turns a 900-cent Salad into an 810-cent bill, while the Critic's override selects Pasta. The shared lifecycle stays in the parent class.

##### What challenged me?

The challenge was checking the extension without concentrating only on its most visible feature. A correct VIP price would not prove that serving, retries or payment still worked. It was also easy to overstate what a demonstration showed. The priority scenario changes C2's waiting time and satisfaction, but both alternatives still lose the game.

##### How did I solve it?

I used the Stage 1 lifecycle as the baseline and looked for the specific points where Stage 2 changed it. The factory changes the arriving object; dispatch changes its behaviour; neither should introduce a new turn system. The controller tests provide examples of factory validation, scheduling and continued timing rules.

The priority comparison gave me a concrete explanation: serving C2 earlier leads to payment on turn 8 with satisfaction 90, while delaying C2 gives payment on turn 9 with satisfaction 85. I describe that as a timing and satisfaction consequence, rather than calling it a victory/defeat flip. This stage taught me to connect a design claim to exactly what the example proves.

**Supporting examples:** [Stage 2 Main](https://github.com/ZeppyClown/OOP-project/blob/stage-2/stage%202/src/main/java/restaurantrush/Main.java), [Stage2ControllerTests](https://github.com/ZeppyClown/OOP-project/blob/stage-2/stage%202/src/test/java/restaurantrush/Stage2ControllerTests.java), and the [1 → 2 comparison](https://github.com/ZeppyClown/OOP-project/compare/stage-1...stage-2).

#### Stage 3: extending the workflow and checking integration

##### What did I learn?

Stage 3 showed me that a reusable design still needs changes when the workflow changes. Host seating and Happy Hour cannot be added only by creating subclasses; they introduce new decisions and phases. The important question is which existing responsibilities must change and which rules can remain stable.

I also learned to distinguish a correct set of member files from a correct merged application. The integration problems near the end were a useful example. The tested handoffs contained the intended implementation, but merging had brought back older menu selection and test code. A file being present on GitHub did not mean it was the version that had passed the checks.

##### How did I apply it?

In the controller area, Happy Hour is represented by one activation timestamp. That lets the active window be derived instead of maintaining separate flags and counters. Activation on turn 13 means turns 13 and 14 are active, and turn 15 is outside the window. The controller coordinates recovery before Host, Waiter and Chef choices, and selects the pricing policy when an order is taken.

I used the Restaurant wrappers when explaining the final call flow. The controller coordinates the action, while Restaurant checks ownership and delegates to the appropriate staff and domain objects. The policies, customers and menu classes are shared dependencies from the team's other areas; they are not all my individual contribution.

##### What challenged me?

Keeping the timing understandable became harder as the turn acquired more phases. A customer seated by the Host can order in the same turn, but a table released by the Cashier cannot be used for seating earlier in that turn. Recovery also happens before staff choices, so eligibility must be understood at that point in the sequence.

The selected sequence needed to explain a real consequence of a manager choice, rather than simply show more arrows. The Happy Hour pair was useful because the staff choices are held constant and satisfaction decides the final result.

##### How did I solve it?

I followed the scenario from turn 13 through payment on turns 17 and 18. Activating Happy Hour gives four paid customers, $49.50 revenue and average satisfaction 67.3, producing victory. Never activating gives four paid customers, $56.10 and 57.8, producing defeat. Both runs clear the paid-count and revenue targets. Explaining why the lower-revenue run wins helped me connect pricing, recovery and the result calculation.

For the integration issue, I requested a comparison of the saved handoffs with the branches and main, followed by an independent Claude review. After that review confirmed the restored version, I authorised focused repair commits and verification of the completed tree. The automated checks passed 55 registered checks and all four build checks before the restoration reached main. Those repairs restored the team's existing work; they do not establish sole authorship of that code.

The main lesson I take from Stage 3 is to test the version that will actually be submitted, not just the individual pieces. Clear interfaces help, but they do not remove the need to review merge results and rerun complete scenarios.

**Supporting examples:** [HappyHour](../stage%203/src/main/java/restaurantrush/control/HappyHour.java), [Stage3ControllerTests](../stage%203/src/test/java/restaurantrush/Stage3ControllerTests.java), [FLIP_SCENARIO](../stage%203/test-fixtures/FLIP_SCENARIO.md), and [the completed restoration](https://github.com/ZeppyClown/OOP-project/commit/699b3173444e7efae4b324101130d73ce1252577).


### Danielle

*SC2002 Restaurant Rush · Area: customers, customer lifecycle and subtypes*

#### What did I learn?

I learned how polymorphism and encapsulation allow behaviour to be placed within the object responsible for it, making customer behaviour easier to extend without adding subtype branches to the turn loop. In `Customer.java`, the shared lifecycle (waiting, seated, waiting for food, served (eating), ready to pay, paid or left) is implemented once through guarded methods that prevent invalid state transitions. The differences between customer types are then handled through a few overridable methods. For example, the VIP loses 5 satisfaction per waiting turn and receives a 10% discount, the Regular loses 8, while the Critic loses 12 and always chooses Pasta. More importantly, I learned that inheritance does not automatically guarantee a sound design, as a subtype can introduce behaviour that violates the expectations of its parent.

#### How did I apply it?

One example was the Stage 2 decision on how to implement the three customer types. This was a design decision documented in my member pack, where I extended and tested the customer behaviour together with my team. I initially considered handling the differences through controller conditionals, such as checking whether a customer was a VIP whenever a discount or waiting penalty needed to be applied. However, we decided to use `Customer` as the parent class and implement the differences through separate subclasses. Methods such as `getTypeLabel()` and `waitingLossPerTurn()` are abstract, requiring each subtype to provide its own implementation. Meanwhile, `applyDiscount()` and `chooseItem()` act as hooks with default implementations that can be overridden by `VIPCustomer` and `CriticCustomer`.

This approach uses inheritance to establish the shared customer lifecycle, polymorphism to handle differing behaviour and encapsulation to keep each customer's rules within its respective class. It also follows the Open/Closed Principle, as additional customer types can be introduced through extension rather than repeatedly modifying existing controller logic.

In `Restaurant.applyWaitingLoss()`, the program checks the customers and calls `loseWaitingSatisfaction()` for those still waiting. The parent method then subtracts the value returned by `waitingLossPerTurn()`, allowing each customer type to have a different waiting penalty without requiring separate conditions.

In Stage 2, `GameController` calls `chooseItem()` and `applyDiscount()` in the same way for every customer. In Stage 3, Restaurant coordinates item selection and the pricing policy calls the discount hook. The Critic consequently selects Pasta while the VIP receives a discounted bill, without the controller needing to identify their respective types.

I examined the method overrides and traced how calls were dispatched through `Customer` references. I also reviewed `CustomerSubtypeTests.behaviourDispatch`, which creates all three customer types, invokes the same methods and checks whether each produces the expected behaviour. For Stage 2, I ran both replay fixtures (VIP-first and C1-first). Both ended in DEFEAT with one paid customer, $8.10 in revenue and average satisfaction values of 90.0 and 85.0 respectively, matching the diagram notes.

#### What challenged me?

The main challenge was recognising that polymorphism also introduces certain design responsibilities. A subtype should be usable wherever its parent is expected, but `CriticCustomer.chooseItem()` introduces a stricter requirement than the base implementation. While the parent accepts any non-null menu, the Critic throws an `IllegalStateException` if Pasta is unavailable. This is a substitutability limitation if the parent contract is interpreted as accepting every non-null menu. The shipped menu always includes Pasta, so the supported game does not encounter that case.

What made this particularly difficult to identify was that the existing tests still passed because the shipped menu always contains Pasta. This showed me that a green test suite does not necessarily mean that the design is fully sound. I had to compare the overridden method against the original contract to identify the issue.

Another challenge was verifying the UML diagram. Although the signature checker reported no mismatches, it could not determine whether the inheritance relationships or multiplicities were conceptually correct. I therefore had to compare the relationships and arrows against the actual source code manually.

#### How did I solve it?

I considered both controller conditionals and subtype overrides. Although conditionals would have been straightforward initially, they would introduce repeated type checks throughout the controller. Every additional customer type would also require changes to existing logic. Subtype overrides provided a cleaner alternative by keeping each behaviour within the class responsible for it.

We proceeded with the subtype approach and added `behaviourDispatch` to verify that the same method calls produced the expected results across all three customer types. For the Critic, I chose not to overlook this LSP limitation. Although the shipped game remains functional because Pasta is always available, I documented this assumption as a known limitation rather than describing the design as fully LSP-compliant.

To address this limitation, the Critic's preference could be implemented as a menu-independent rule or include a fallback to the default selection when Pasta is unavailable. This would allow `chooseItem()` to preserve the parent's original contract regardless of the available menu.

This experience taught me that passing tests and having a sound design are not necessarily the same thing. Testing verifies expected behaviour under specific conditions, but evaluating the design also requires checking whether the implementation adheres to the principles it is intended to follow.

I also recognised that `Restaurant` has gradually become quite large because it handles the queue, table management and ownership checks in one place. While this keeps state consistent, it also reduces cohesion.


### Abraham

*SC2002 Restaurant Rush · Area: menu, orders, staff, combos and pricing*

#### What did I learn?

I learned how inheritance, object relationships and interfaces each solve a different problem, and that choosing between them is a real design decision rather than a matter of definitions. In Stage 3, `ComboMeal` extends `MenuItem`, so a combo can be used anywhere an ordinary dish is expected. At the same time, it holds references to two existing menu items, which is a has-a relationship. In UML, those shared component references are aggregation rather than exclusive composition. The two relationships answer different questions: inheritance says what a combo *is*, while the has-a link (shared aggregation in UML) says what it is *made of*.

I also learned that timing is part of a design. Storing money as integer cents and fixing the bill at the moment an order is placed removed a whole class of rounding and timing bugs. Finally, I learned that placing a rule in the class responsible for it, such as task capacity in `Staff` and preparation progress in `Order`, keeps each class simple and makes the program easier to extend.

#### How did I apply it?

In Stage 1, I worked on the menu, order and staff classes. `Order` owns its own lifecycle (PLACED, PREPARING, READY, SERVED, PAID, or CANCELLED if the customer leaves) and its preparation progress. A Burger needs one preparation unit and a Pasta needs two, so a Pasta ordered on turn 2 receives its first unit that turn, becomes READY on turn 3, and can only be served on turn 4. An order that becomes READY cannot be served in the same turn, which keeps preparation and service as separate steps.

`Staff` centralises the one-task-per-turn rule, and `Waiter`, `Chef` and `Cashier` inherit it while defining their own tasks. Each role checks its target and records the task only after a successful action. Action methods return `null` on success or a `String` reason on refusal, so an invalid action does not use up the staff member's turn and the player can retry. `StaffTests` checks this contract.

In Stage 2, the pricing extension added `Money.applyPercent` for the VIP discount without changing the preparation and payment sequence. The quoted bill is stored on the order, so the Cashier collects that amount instead of recalculating it at the end.

In Stage 3, we had to add a Host, combos and Happy Hour. `Host` also extends `Staff`, but has its own capacity, so it can seat a customer in the same turn that the Waiter takes an order. For combos, because `ComboMeal` is a `MenuItem`, `Order`, `Chef` and the Waiter's serve logic needed no changes. The combo simply reports the sum of its components' preparation units and 90% of their summed base price. For our default Salad + Burger combo, that is 2 units and 900 + 1500 = 2400 cents × 0.9 = 2160 cents ($21.60).

For pricing, I applied the interface idea through `PricingPolicy`, with two implementations: `StandardPricingPolicy` and `HappyHourPricingPolicy`. The controller picks a policy for the turn the order is placed, and `Restaurant.takeOrder` applies it. The controller does not branch on customer subtype. It does check Happy Hour state to select a policy; the policy performs the quotation. A new promotion could use another implementation, although wiring and selection may still need changes.

#### What challenged me?

The hardest part was deciding when discounts should be applied and making the results consistent. A VIP ordering during Happy Hour gets two discounts, and the order of operations and rounding at each step change the final cents. On top of that, Happy Hour only lasts two turns, while a Pasta order takes several turns before it is paid. If the bill were recalculated at payment, the same order could cost different amounts depending on when the Cashier collected it.

This was difficult because the pricing rules touched other members' work. The customer discount belongs to Danielle's Customer subtypes, the bill lives in the Order lifecycle, and the policy choice happens in Victor's controller turn sequence. A change in one place could silently break assumptions in another.

A second challenge was the combo design. Making `ComboMeal` extend `MenuItem` required `MenuItem` to be non-final, and it raised the question of whether a combo really "is" a menu item. I also had to check that the UML showed both relationships correctly. `Menu` composes the items it creates, but that does not mean no other object can refer to them, and a combo does exactly that.

A third, smaller problem was a duplicate `Menu` accessor. A mismatch in our shared contract had left two methods doing the same job, and different callers used different ones.

#### How did I solve it?

For pricing, I worked through a VIP Burger by hand in cents: 1500 → 1350 after the 10% VIP discount → 1080 after the 20% Happy Hour discount, rounding at each step, and compared that with `PricingTests`. We then fixed the rule: quote the price once in `Restaurant.takeOrder`, apply the customer discount first and Happy Hour second, and store the result as the order's `billCents`. The Cashier collects that locked amount even after Happy Hour expires, so an old bill can never change.

For combos, I considered two options. A separate combo-order hierarchy would model two dishes more explicitly, but it would duplicate the order status, preparation and service flow and add a second path through the code. Extending `MenuItem` reuses the existing pipeline, so I accepted that trade-off and the cost of making `MenuItem` non-final.

For the duplicate accessor, we migrated every caller to `Menu.getItems()` and removed the other method. This showed me that changing a shared API needs coordination with teammates, not just a local fix.

I also recognised the limits of our evidence. The `null`/`String` refusal convention makes retries simple, but a caller can ignore a failure, so it relies on discipline rather than the compiler. The UML signature checker confirms that drawn members match the code, but not whether a composition arrow or multiplicity is conceptually right, so I compared those against the source by hand.

This experience taught me that a worked example checked against a test is often the fastest way to settle a design question, and that the most important design decisions are often about *when* something happens and *who* owns it, not just how it is calculated.


### Talon

**Area:** console boundary (`ConsoleUI`, implementing `GameIO`), scripted replay harness (`ScriptedGameIO`, `AllTests`), reproducible fixtures, final integration, figures and video.

#### What did I learn?

I learned that a program's boundary should translate input, not decide what it means. In `ConsoleUI.java`, the parser turns text such as `seat 2` or `serve 3` into a typed `WaiterAction` or an order ID, and asks again when the syntax is wrong. It never decides whether the move is legal. That decision belongs to `GameController` and the domain objects, which know whether a table is free or whether order 3 is actually READY. When they refuse, the console prints `Not allowed: <reason> - choose again.` and the same staff member gets another try in the same phase, so a typo or an illegal target never costs a turn. More importantly, I learned that how input *ends* is part of the contract too. When input runs out, `ConsoleUI` throws `InputEndedException` instead of quietly choosing Wait, because a fabricated Wait would let an unfinished game be reported as a DEFEAT that never happened.

#### How did I apply it?

One example was how to make gameplay testable without a human at the keyboard. This was a design decision recorded in my member pack, where I built and tested the replay harness together with my team. I first considered the simple approach of storing the Waiter's and Chef's answers in two separate queues, which is how an earlier test double worked. However, we decided that `ScriptedGameIO` should hold one ordered, role-tagged script. Every request from the controller is checked against the next step in the script. If the controller asks the Chef when the script expects the Waiter, or asks the Host when the script expects the Chef, the harness throws an `IllegalStateException` that names the request number. Separate queues could never detect a role asked out of order, so they could not prove the turn order (Host, then Waiter, then Chef, then automatic Cashier) that the whole game depends on.

This approach relies on dependency inversion. `GameController` depends on the `GameIO` interface for interaction, so `ConsoleUI`, `ScriptedGameIO` and `TestGameIO` can be substituted for one another without the controller knowing which one it is talking to. The same controller code that runs in the terminal also runs in the tests, so a passing replay is evidence about the real game, not about a copy of it.

In Stage 3, the Host and Happy Hour prompts added three new methods to `GameIO`. We gave them default implementations (`chooseHappyHourActivation()` returns `false`, `chooseHostCustomer()` returns `0`), so the Stage 1 and Stage 2 test doubles kept compiling without changes. The trade-off is a weaker contract. If `ConsoleUI` forgot to override one of them, the real game would silently never offer Happy Hour or never let the Host act, and older test doubles could still compile without detecting the missing behaviour.

To cover that risk, I added `ConsoleTests.overridesEveryGameIoDefault`. It uses reflection to list every default method on `GameIO` and fails if `ConsoleUI` does not declare its own version. I also reviewed `invalidFixtureRecovers`, which feeds a fixture full of typos and illegal targets through the real parser and checks that it reaches exactly the same choices as the clean victory script. The suites for the three stages record 29, 40 and 55 registered checks, and the integrated Stage 3 automated verification exercised both Happy Hour replays through the real `Main`. With Happy Hour switched on at turn 13, 4 customers paid, revenue was $49.50 and average satisfaction was 67.3, a VICTORY. Without it, 4 paid, revenue was $56.10 and satisfaction was 57.8, a DEFEAT. These match the selected sequence scenario. The final recorded video remains separate evidence to check.

#### What challenged me?

The main challenge was understanding what each kind of evidence can and cannot prove. A replay that produces the right verdict does not show that every internal rule is correct, and a registered check count is not a count of assertions. When I compared the two flip fixtures, I also found that they are not identical apart from one answer. The "on" script contains `yes` on turn 13 and then nothing more, because the prompt stops once Happy Hour has been used. The "never" script has to answer `no` on every remaining turn, so it is five lines longer. The staff choices are the same in both runs, but anyone reading the files would think more than one decision had changed unless this was explained. I therefore documented it in `FLIP_SCENARIO.md` rather than assuming readers would work it out.

Another challenge was integration. When each member's branch was compiled on its own, it often failed because it referred to classes that lived in another member's branch, so a branch that looked finished was not actually buildable. Tests were also sensitive to where they were run from: the fixture path was originally relative, so the same compiled classes passed from the project root and failed from another directory. A release check run from the wrong folder would have looked like a regression. Git caused a similar problem on macOS. Because the file system ignores case, `git tag stage-1` was refused while an older `Stage-1` tag existed.

#### How did I solve it?

For the fixture path, I considered both hard-coding the path and passing it in. Hard-coding is simpler, but it ties the result to the directory the command happens to start in. We moved to a `stage1.fixtures` system property, which the run scripts set from their own location, so the suites give the same result wherever they are launched. For the `GameIO` defaults, I considered making every new method abstract. That would give compile-time enforcement, but it would also force changes to every older test double in other members' areas. I kept the defaults and relied on the reflection test, and I recorded the trade-off as a known limitation rather than claiming `GameIO` is a clean Interface Segregation example. It still combines input and output in one interface.

To address this properly, `GameIO` could be split into an input interface and a display interface, and the Stage 3 methods could live in a separate `Stage3Input` interface that only the Stage 3 console implements. The controller would then require that interface where it needs Host and Happy Hour choices, and a missing implementation would be a compile error instead of a runtime surprise.

This experience taught me that a passing test suite and a correct program are not the same thing, and that the gap is widest at the boundary between components. Unit tests check each part under the conditions someone thought of, but only a full replay through the real entry point shows that the parts still agree with each other after integration.

I also recognised that assembling the final submission is a design task in itself. A large diagram that is correct but unreadable on an A4 page, or a video timestamp that no longer matches the code that was tagged, weakens evidence that is otherwise sound. The selected sequence is split across focused pages. Checking those figures at their final PDF page size remains important before submission; native-size images alone do not prove report readability.
