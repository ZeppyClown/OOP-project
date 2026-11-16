# Victor Chua — Individual reflection

My area of the project was startup, turn control, the IO contract, controller tests and the selected sequence diagram. Across the three stages, the question I kept coming back to was whether the objects were doing the right things in the right order. A calculation could be correct on its own and still produce the wrong game if it happened in the wrong phase.

## Stage 1: learning to follow a whole turn

### What did I learn?

Stage 1 helped me understand the difference between coordinating an action and owning the rule behind it. `GameController` decides when the Waiter and Chef act, but it should not directly set an order to READY or a customer to PAID. Those changes belong to the domain objects, which check whether the transition is legal. Separating those responsibilities made the flow easier to explain because I could follow a call from the controller down to the object that actually changed state.

I also learned why phase order alone is not enough. The Waiter runs before the Chef, which prevents the usual same-turn preparation-and-service shortcut. However, the domain still needs timestamp guards. Otherwise, a different caller could bypass the intended delay. The same reasoning applies to eating and payment.

### How did I apply it?

A useful example is C1's Burger order. The Waiter seats C1 on turn 1, takes the order on turn 2, and the Chef completes the Burger that turn. Service follows on turn 3, eating finishes on turn 4, and payment happens on turn 5. Following that small example made the separate guards meaningful: `readyTurn < turn`, `servedTurn < turn`, and `readyToPayTurn < turn` each protect a different boundary.

The IO interface gave the controller a way to ask for choices without parsing console text itself. `Main` supplies the Restaurant and ConsoleUI references, and the controller uses the latter through `GameIO`. This is where dependency inversion became a practical decision for me: the interaction can be replaced by a test double while the same controller continues to run the game.

### What challenged me?

The less obvious part was handling actions that look like valid commands but cannot be carried out. `seat 99` is understandable input, but customer 99 has not arrived. Treating that as a completed action would waste a staff task. It would also make the output misleading because the game would advance even though nothing useful happened.

EOF raised a related question. An unfinished input stream is not the same as choosing Wait for the rest of the game. Producing a final defeat after silently filling in missing decisions would describe a game the manager never played.

### How did I solve it?

The useful approach was to trace both a successful action and a refusal through the same phase. For `seat 99`, the customer lookup fails, the controller displays the reason, and it asks the Waiter again without advancing the turn. Staff record a task only after success. Syntax errors are handled earlier by the console, before a typed action is returned.

For EOF, the controller catches `InputEndedException`, reports the current role and stops without a victory or defeat verdict. Earlier changes are retained; the game does not pretend that an interrupted turn completed. The lesson I took from this was that the refusal and interruption paths deserve the same attention as the successful lifecycle.


## Stage 2: separating customer construction from customer behaviour

### What did I learn?

Stage 2 made the purpose of polymorphism clearer. The controller needs to know when a customer arrives, but it does not need to decide how every customer type loses satisfaction or chooses food. Those are different responsibilities. Once the controller holds a `Customer` reference, calls can dispatch to the actual Regular, VIP or Critic object.

This also helped me be more careful with claims about extensibility. Adding a customer subtype can avoid changes to the turn loop, but the program still needs a way to construct that subtype. There is still configuration work; the benefit is that it does not spread through every phase of the game.

### How did I apply it?

The factory supplied as `Main::createCustomer` separates arrival scheduling from subtype selection. On turn 4, the controller calculates ID 2 and asks the factory for that customer. Main's factory returns a VIP, while the controller validates that the returned object is non-null and has the expected ID.

During ordering, the Stage 2 controller calls `chooseItem()` and `applyDiscount()` through `Customer`. It does not ask whether the object is a VIP or Critic. The VIP discount turns a 900-cent Salad into an 810-cent bill, while the Critic's override selects Pasta. The shared lifecycle stays in the parent class.

### What challenged me?

The challenge was checking the extension without concentrating only on its most visible feature. A correct VIP price would not prove that serving, retries or payment still worked. It was also easy to overstate what a demonstration showed. The priority scenario changes C2's waiting time and satisfaction, but both alternatives still lose the game.

### How did I solve it?

I used the Stage 1 lifecycle as the baseline and looked for the specific points where Stage 2 changed it. The factory changes the arriving object; dispatch changes its behaviour; neither should introduce a new turn system. The controller tests provide examples of factory validation, scheduling and continued timing rules.

The priority comparison gave me a concrete explanation: serving C2 earlier leads to payment on turn 8 with satisfaction 90, while delaying C2 gives payment on turn 9 with satisfaction 85. I describe that as a timing and satisfaction consequence, rather than calling it a victory/defeat flip. This stage taught me to connect a design claim to exactly what the example proves.

## Stage 3: extending the workflow and checking integration

### What did I learn?

Stage 3 showed me that a reusable design still needs changes when the workflow changes. Host seating and Happy Hour cannot be added only by creating subclasses; they introduce new decisions and phases. The important question is which existing responsibilities must change and which rules can remain stable.

I also learned to distinguish a correct set of member files from a correct merged application. The integration problems near the end were a useful example. The tested handoffs contained the intended implementation, but merging had brought back older menu selection and test code. A file being present on GitHub did not mean it was the version that had passed the checks.

### How did I apply it?

In the controller area, Happy Hour is represented by one activation timestamp. That lets the active window be derived instead of maintaining separate flags and counters. Activation on turn 13 means turns 13 and 14 are active, and turn 15 is outside the window. The controller coordinates recovery before Host, Waiter and Chef choices, and selects the pricing policy when an order is taken.

I used the Restaurant wrappers when explaining the final call flow. The controller coordinates the action, while Restaurant checks ownership and delegates to the appropriate staff and domain objects. The policies, customers and menu classes are shared dependencies from the team's other areas; these are the team’s shared contributions.

### What challenged me?

Keeping the timing understandable became harder as the turn acquired more phases. A customer seated by the Host can order in the same turn, but a table released by the Cashier cannot be used for seating earlier in that turn. Recovery also happens before staff choices, so eligibility must be understood at that point in the sequence.

The selected sequence needed to explain a real consequence of a manager choice, rather than simply show more arrows. The Happy Hour pair was useful because the staff choices are held constant and satisfaction decides the final result.

### How did I solve it?

I followed the scenario from turn 13 through payment on turns 17 and 18. Activating Happy Hour gives four paid customers, $49.50 revenue and average satisfaction 67.3, producing victory. Never activating gives four paid customers, $56.10 and 57.8, producing defeat. Both runs clear the paid-count and revenue targets. Explaining why the lower-revenue run wins helped me connect pricing, recovery and the result calculation.

For the integration issue, I requested a comparison of the saved handoffs with the branches and main, followed by an independent Claude review. After that review confirmed the restored version, I authorised focused repair commits and verification of the completed tree. The automated checks passed 55 registered checks and all four build checks before the restoration reached main.

The main lesson I take from Stage 3 is to test the version that will actually be submitted, not just the individual pieces. Clear interfaces help, but they do not remove the need to review merge results and rerun complete scenarios.
