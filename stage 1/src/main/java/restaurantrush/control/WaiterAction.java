package restaurantrush.control;

/** The waiter's move for a turn. SEAT and ORDER take a customer ID,
 * SERVE takes an order ID (same number), WAIT takes 0. */
public record WaiterAction(Type type, int targetId) {
    public enum Type { SEAT, ORDER, SERVE, WAIT }

    public WaiterAction {
        if (type == null) {
            throw new IllegalArgumentException("Action type is required");
        }
        if (type == Type.WAIT && targetId != 0) {
            throw new IllegalArgumentException("WAIT must have target 0");
        }
        if (type != Type.WAIT && targetId <= 0) {
            throw new IllegalArgumentException(type + " needs a positive target ID");
        }
    }

    public static WaiterAction seat(int customerId) {
        return new WaiterAction(Type.SEAT, customerId);
    }

    public static WaiterAction order(int customerId) {
        return new WaiterAction(Type.ORDER, customerId);
    }

    public static WaiterAction serve(int orderId) {
        return new WaiterAction(Type.SERVE, orderId);
    }

    public static WaiterAction waitAction() {
        return new WaiterAction(Type.WAIT, 0);
    }
}
