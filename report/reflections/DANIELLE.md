# Individual Reflection: Danielle

*SC2002 Restaurant Rush · Area: customers, customer lifecycle and subtypes*

## What did I learn?

I learned how polymorphism and encapsulation allow behaviour to be placed within the object responsible for it, making customer behaviour easier to extend without adding subtype branches to the turn loop. In `Customer.java`, the shared lifecycle (waiting, seated, waiting for food, served (eating), ready to pay, paid or left) is implemented once through guarded methods that prevent invalid state transitions. The differences between customer types are then handled through a few overridable methods. For example, the VIP loses 5 satisfaction per waiting turn and receives a 10% discount, the Regular loses 8, while the Critic loses 12 and always chooses Pasta. More importantly, I learned that inheritance does not automatically guarantee a sound design, as a subtype can introduce behaviour that violates the expectations of its parent.

## How did I apply it?

One example was the Stage 2 decision on how to implement the three customer types. This was a design decision documented in my member pack, where I extended and tested the customer behaviour together with my team. I initially considered handling the differences through controller conditionals, such as checking whether a customer was a VIP whenever a discount or waiting penalty needed to be applied. However, we decided to use `Customer` as the parent class and implement the differences through separate subclasses. Methods such as `getTypeLabel()` and `waitingLossPerTurn()` are abstract, requiring each subtype to provide its own implementation. Meanwhile, `applyDiscount()` and `chooseItem()` act as hooks with default implementations that can be overridden by `VIPCustomer` and `CriticCustomer`.

This approach uses inheritance to establish the shared customer lifecycle, polymorphism to handle differing behaviour and encapsulation to keep each customer's rules within its respective class. It also follows the Open/Closed Principle, as additional customer types can be introduced through extension rather than repeatedly modifying existing controller logic.

In `Restaurant.applyWaitingLoss()`, the program checks the customers and calls `loseWaitingSatisfaction()` for those still waiting. The parent method then subtracts the value returned by `waitingLossPerTurn()`, allowing each customer type to have a different waiting penalty without requiring separate conditions.

In Stage 2, `GameController` calls `chooseItem()` and `applyDiscount()` in the same way for every customer. In Stage 3, Restaurant coordinates item selection and the pricing policy calls the discount hook. The Critic consequently selects Pasta while the VIP receives a discounted bill, without the controller needing to identify their respective types.

I examined the method overrides and traced how calls were dispatched through `Customer` references. I also reviewed `CustomerSubtypeTests.behaviourDispatch`, which creates all three customer types, invokes the same methods and checks whether each produces the expected behaviour. For Stage 2, I ran both replay fixtures (VIP-first and C1-first). Both ended in DEFEAT with one paid customer, $8.10 in revenue and average satisfaction values of 90.0 and 85.0 respectively, matching the diagram notes.

## What challenged me?

The main challenge was recognising that polymorphism also introduces certain design responsibilities. A subtype should be usable wherever its parent is expected, but `CriticCustomer.chooseItem()` introduces a stricter requirement than the base implementation. While the parent accepts any non-null menu, the Critic throws an `IllegalStateException` if Pasta is unavailable. This is a substitutability limitation if the parent contract is interpreted as accepting every non-null menu. The shipped menu always includes Pasta, so the supported game does not encounter that case.

What made this particularly difficult to identify was that the existing tests still passed because the shipped menu always contains Pasta. This showed me that a green test suite does not necessarily mean that the design is fully sound. I had to compare the overridden method against the original contract to identify the issue.

Another challenge was verifying the UML diagram. Although the signature checker reported no mismatches, it could not determine whether the inheritance relationships or multiplicities were conceptually correct. I therefore had to compare the relationships and arrows against the actual source code manually.

## How did I solve it?

I considered both controller conditionals and subtype overrides. Although conditionals would have been straightforward initially, they would introduce repeated type checks throughout the controller. Every additional customer type would also require changes to existing logic. Subtype overrides provided a cleaner alternative by keeping each behaviour within the class responsible for it.

We proceeded with the subtype approach and added `behaviourDispatch` to verify that the same method calls produced the expected results across all three customer types. For the Critic, I chose not to overlook this LSP limitation. Although the shipped game remains functional because Pasta is always available, I documented this assumption as a known limitation rather than describing the design as fully LSP-compliant.

To address this limitation, the Critic's preference could be implemented as a menu-independent rule or include a fallback to the default selection when Pasta is unavailable. This would allow `chooseItem()` to preserve the parent's original contract regardless of the available menu.

This experience taught me that passing tests and having a sound design are not necessarily the same thing. Testing verifies expected behaviour under specific conditions, but evaluating the design also requires checking whether the implementation adheres to the principles it is intended to follow.

I also recognised that `Restaurant` has gradually become quite large because it handles the queue, table management and ownership checks in one place. While this keeps state consistent, it also reduces cohesion.
