# Stage 3 design discussion

| Abstraction | Responsibility |
| --- | --- |
| GameIO / ConsoleUI | Typed choices and immutable snapshots / console parsing and rendering |
| GameController / HappyHour | Phase sequencing and retries / once-only activation timestamp |
| Restaurant | Registration, identity checks, queue/table consistency, staff wrappers, totals |
| Customer and subtypes | Guarded lifecycle, recovery eligibility, polymorphic item/discount/loss |
| Staff and roles | One successful task per role instance per turn |
| PricingPolicy and two implementations | Quote at order placement, customer discount before Happy Hour |
| MenuItem / ComboMeal | Immutable dish values / two component references with combined preparation |
| Order / Table | Locked bill and preparation/service guards / occupancy |

SRP is visible in parsing versus sequencing versus lifecycle state. Restaurant still has broad coordination/reporting duties. OCP applies to customer and pricing hooks, but adding Stage 3 phases required controller edits; adding a selectable new policy may require assembly/selection changes. LSP works for supplied customer types on the default menu; Critic's Pasta requirement is a stronger precondition on custom menus. ComboMeal works through ordinary MenuItem/Order/Chef operations. PricingPolicy is a focused interface (ISP); GameIO mixes input/display and default methods can hide a missing override, so production override tests matter. DIP applies to injected GameIO and customer factory and Restaurant's supplied PricingPolicy. Controller still creates concrete HappyHour and policy implementations.

Composition: Restaurant creates two tables and Customer creates one order. Menu constructs its offered items. ComboMeal holds shared actual MenuItem component references, so component lifetime is not exclusively owned by the combo. Injected staff references alone do not prove composition.

Alternatives: mutable active/remaining counters save a class but can drift; a timestamp derives both values. A separate combo-order hierarchy models two dishes explicitly but duplicates the required summed-unit path. Direct controller→staff calls keep Restaurant smaller but bypass its ownership checks; Stage 3 uses wrappers. An ActionResult type would express success/refusal more explicitly than null/String, at the cost of changes across action APIs.

The selected manager choice flips the final verdict: activating on turn 13 gives four paid, $49.50 and average 67.3; never activating gives four paid, $56.10 and 57.8. Both clear revenue and paid-count targets. Satisfaction recovery makes the lower-revenue run win. The fixtures deliberately delay service to approach the boundary; they demonstrate a possible consequence rather than optimal play.
