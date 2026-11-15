package restaurantrush.domain;

/** Seats one customer per turn using capacity independent of the Waiter. */
public final class Host extends Staff {
    public Host() { super("Host"); }

    /** Restaurant couples this guarded primitive with queue removal. */
    String seat(Customer customer, Table table, int turn) {
        String reason = availabilityReason(turn);
        if (reason != null) { return reason; }
        if (customer == null || table == null) { return "Customer and table are required"; }
        reason = table.seat(customer);
        if (reason == null) { markTaskUsed(turn); }
        return reason;
    }
}
