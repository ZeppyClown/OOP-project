# Danielle: Stage 3 responsibility and code handoff

Mapped to the supplied Danielle Member Pack. This records technical changes and checks; it does not supply personal reflection, approval or authorship claims.

Customer owns lifecycle and satisfaction; Restaurant owns registration/queue coordination and its two tables. Seating removes a queue entry only after successful staff/table/customer transitions. Payment releases the occupied table after successful collection. Departures cancel unserved orders and remove queue/table occupancy.

`recoverWaitingSatisfaction(15)` applies only to WAITING, SEATED and WAITING_FOR_FOOD customers, including orders already READY but awaiting service. It excludes SERVED, READY_TO_PAY, PAID and LEFT. Long addition prevents overflow and the result is capped at 100. Negative amounts are rejected.

Restaurant adds `seatCustomerWithHost`, `takeOrder`, `serveOrder`, `prepareOrder` and aggregate recovery. Action wrappers reject foreign customers/orders by identity. Ordering verifies SEATED before customer item selection and policy quotation. A bill is frozen in the new Order. Existing customer subclasses and Table remain inherited from Stage 2.

Class ownership is not inferred from every field: Restaurant creates its tables, but staff/menu can be injected. Combo components are shared menu references. Critic still requires Pasta, so custom menus without it are a documented limitation.

## Files to review / send

- [src/main/java/restaurantrush/domain/Customer.java](../src/main/java/restaurantrush/domain/Customer.java)
- [src/main/java/restaurantrush/domain/CustomerStatus.java](../src/main/java/restaurantrush/domain/CustomerStatus.java)
- [src/main/java/restaurantrush/domain/Restaurant.java](../src/main/java/restaurantrush/domain/Restaurant.java)
- [src/test/java/restaurantrush/domain/CustomerTests.java](../src/test/java/restaurantrush/domain/CustomerTests.java)
- [src/test/java/restaurantrush/domain/CustomerSubtypeTests.java](../src/test/java/restaurantrush/domain/CustomerSubtypeTests.java)
- [src/test/java/restaurantrush/domain/RestaurantTests.java](../src/test/java/restaurantrush/domain/RestaurantTests.java)
- [src/test/java/restaurantrush/domain/RecoveryTests.java](../src/test/java/restaurantrush/domain/RecoveryTests.java)
- [src/test/java/restaurantrush/domain/RestaurantOrderingTests.java](../src/test/java/restaurantrush/domain/RestaurantOrderingTests.java)

## Verification

Run from repository root:

```sh
JAVA_HOME=/opt/homebrew/opt/openjdk@17 sh "stage 3/run-tests.sh"
JAVA_HOME=/opt/homebrew/opt/openjdk@17 sh "stage 3/check-build.sh"
```

See [EVIDENCE](../EVIDENCE.md) for actual local results. Coordinate shared signatures and test registration before combining individual file sets. All four sets are needed for a complete Stage 3 build.

Diagram files use editable draw.io sources and matching PNG pages. See the [Stage 3 diagram index](../../diagrams/stage%203/README.md).
