# Individual Reflection: Abraham

*SC2002 Restaurant Rush · Area: menu, orders, staff, combos and pricing*

## What did I learn?

I learned how inheritance, object relationships and interfaces each solve a different problem, and that choosing between them is a real design decision rather than a matter of definitions. In Stage 3, `ComboMeal` extends `MenuItem`, so a combo can be used anywhere an ordinary dish is expected. At the same time, it holds references to two existing menu items, which is a has-a relationship. In UML, those shared component references are aggregation rather than exclusive composition. The two relationships answer different questions: inheritance says what a combo *is*, while the has-a link (shared aggregation in UML) says what it is *made of*.

I also learned that timing is part of a design. Storing money as integer cents and fixing the bill at the moment an order is placed removed a whole class of rounding and timing bugs. Finally, I learned that placing a rule in the class responsible for it, such as task capacity in `Staff` and preparation progress in `Order`, keeps each class simple and makes the program easier to extend.

## How did I apply it?

In Stage 1, I worked on the menu, order and staff classes. `Order` owns its own lifecycle (PLACED, PREPARING, READY, SERVED, PAID, or CANCELLED if the customer leaves) and its preparation progress. A Burger needs one preparation unit and a Pasta needs two, so a Pasta ordered on turn 2 receives its first unit that turn, becomes READY on turn 3, and can only be served on turn 4. An order that becomes READY cannot be served in the same turn, which keeps preparation and service as separate steps.

`Staff` centralises the one-task-per-turn rule, and `Waiter`, `Chef` and `Cashier` inherit it while defining their own tasks. Each role checks its target and records the task only after a successful action. Action methods return `null` on success or a `String` reason on refusal, so an invalid action does not use up the staff member's turn and the player can retry. `StaffTests` checks this contract.

In Stage 2, the pricing extension added `Money.applyPercent` for the VIP discount without changing the preparation and payment sequence. The quoted bill is stored on the order, so the Cashier collects that amount instead of recalculating it at the end.

In Stage 3, we had to add a Host, combos and Happy Hour. `Host` also extends `Staff`, but has its own capacity, so it can seat a customer in the same turn that the Waiter takes an order. For combos, because `ComboMeal` is a `MenuItem`, `Order`, `Chef` and the Waiter's serve logic needed no changes. The combo simply reports the sum of its components' preparation units and 90% of their summed base price. For our default Salad + Burger combo, that is 2 units and 900 + 1500 = 2400 cents × 0.9 = 2160 cents ($21.60).

For pricing, I applied the interface idea through `PricingPolicy`, with two implementations: `StandardPricingPolicy` and `HappyHourPricingPolicy`. The controller picks a policy for the turn the order is placed, and `Restaurant.takeOrder` applies it. The controller does not branch on customer subtype. It does check Happy Hour state to select a policy; the policy performs the quotation. A new promotion could use another implementation, although wiring and selection may still need changes.

## What challenged me?

The hardest part was deciding when discounts should be applied and making the results consistent. A VIP ordering during Happy Hour gets two discounts, and the order of operations and rounding at each step change the final cents. On top of that, Happy Hour only lasts two turns, while a Pasta order takes several turns before it is paid. If the bill were recalculated at payment, the same order could cost different amounts depending on when the Cashier collected it.

This was difficult because the pricing rules touched other members' work. The customer discount belongs to Danielle's Customer subtypes, the bill lives in the Order lifecycle, and the policy choice happens in Victor's controller turn sequence. A change in one place could silently break assumptions in another.

A second challenge was the combo design. Making `ComboMeal` extend `MenuItem` required `MenuItem` to be non-final, and it raised the question of whether a combo really "is" a menu item. I also had to check that the UML showed both relationships correctly. `Menu` composes the items it creates, but that does not mean no other object can refer to them, and a combo does exactly that.

A third, smaller problem was a duplicate `Menu` accessor. A mismatch in our shared contract had left two methods doing the same job, and different callers used different ones.

## How did I solve it?

For pricing, I worked through a VIP Burger by hand in cents: 1500 → 1350 after the 10% VIP discount → 1080 after the 20% Happy Hour discount, rounding at each step, and compared that with `PricingTests`. We then fixed the rule: quote the price once in `Restaurant.takeOrder`, apply the customer discount first and Happy Hour second, and store the result as the order's `billCents`. The Cashier collects that locked amount even after Happy Hour expires, so an old bill can never change.

For combos, I considered two options. A separate combo-order hierarchy would model two dishes more explicitly, but it would duplicate the order status, preparation and service flow and add a second path through the code. Extending `MenuItem` reuses the existing pipeline, so I accepted that trade-off and the cost of making `MenuItem` non-final.

For the duplicate accessor, we migrated every caller to `Menu.getItems()` and removed the other method. This showed me that changing a shared API needs coordination with teammates, not just a local fix.

I also recognised the limits of our evidence. The `null`/`String` refusal convention makes retries simple, but a caller can ignore a failure, so it relies on discipline rather than the compiler. The UML signature checker confirms that drawn members match the code, but not whether a composition arrow or multiplicity is conceptually right, so I compared those against the source by hand.

This experience taught me that a worked example checked against a test is often the fastest way to settle a design question, and that the most important design decisions are often about *when* something happens and *who* owns it, not just how it is calculated.
