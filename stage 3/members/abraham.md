# Abraham: Stage 3 responsibility and code handoff

Mapped to the supplied Abraham Member Pack. This records technical changes and checks; it does not supply personal reflection, approval or authorship claims.

Host extends Staff and has an independent one-successful-task-per-turn timestamp. Host can seat C1 and Waiter can order C1 in the same turn. A refusal consumes neither role's capacity. Staff still delegates state transitions to Table, Customer and Order.

Default Menu offers Burger 1500 cents/one unit, Salad 900/one, Pasta 1800/two and Salad + Burger Combo 2160/two. ComboMeal extends immutable MenuItem and stores its actual two component references. Its price is 90% of summed cents and preparation is summed units. Null components and sum overflow are rejected. `Menu(false)` supplies the original three choices for inherited regression tests.

StandardPricingPolicy calls Customer.applyDiscount. HappyHourPricingPolicy applies that customer quote first, then 80%, rounding at each step through Money.applyPercent. VIP Salad is 900 → 810 → 648 cents. VIP combo is 2160 → 1944 → 1555 cents. These are distinct from multiplying the original price by 72% in one rounding step.

Order.billCents is final: activation/expiry after placement cannot change payment revenue. A separate combo-order hierarchy would duplicate preparation/lifecycle handling without adding required behaviour, so the ordinary Order/Chef path is retained.

## Files to review / send

- [src/main/java/restaurantrush/domain/ComboMeal.java](../src/main/java/restaurantrush/domain/ComboMeal.java)
- [src/main/java/restaurantrush/domain/Host.java](../src/main/java/restaurantrush/domain/Host.java)
- [src/main/java/restaurantrush/domain/Menu.java](../src/main/java/restaurantrush/domain/Menu.java)
- [src/main/java/restaurantrush/domain/MenuItem.java](../src/main/java/restaurantrush/domain/MenuItem.java)
- [src/main/java/restaurantrush/domain/PricingPolicy.java](../src/main/java/restaurantrush/domain/PricingPolicy.java)
- [src/main/java/restaurantrush/domain/StandardPricingPolicy.java](../src/main/java/restaurantrush/domain/StandardPricingPolicy.java)
- [src/main/java/restaurantrush/domain/HappyHourPricingPolicy.java](../src/main/java/restaurantrush/domain/HappyHourPricingPolicy.java)
- [src/test/java/restaurantrush/domain/OrderTests.java](../src/test/java/restaurantrush/domain/OrderTests.java)
- [src/test/java/restaurantrush/domain/StaffTests.java](../src/test/java/restaurantrush/domain/StaffTests.java)
- [src/test/java/restaurantrush/domain/PricingTests.java](../src/test/java/restaurantrush/domain/PricingTests.java)
- [src/test/java/restaurantrush/domain/Stage3ComponentTests.java](../src/test/java/restaurantrush/domain/Stage3ComponentTests.java)

## Verification

Run from repository root:

```sh
JAVA_HOME=/opt/homebrew/opt/openjdk@17 sh "stage 3/run-tests.sh"
JAVA_HOME=/opt/homebrew/opt/openjdk@17 sh "stage 3/check-build.sh"
```

See [EVIDENCE](../EVIDENCE.md) for actual local results. Coordinate shared signatures and test registration before combining individual file sets. All four sets are needed for a complete Stage 3 build.

Diagram files use editable draw.io sources and matching PNG pages. See the [Stage 3 diagram index](../../diagrams/stage%203/README.md).
