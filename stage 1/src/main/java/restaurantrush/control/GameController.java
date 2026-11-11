package restaurantrush.control;

import java.util.Objects;

import restaurantrush.domain.Customer;
import restaurantrush.domain.MenuItem;
import restaurantrush.domain.Order;
import restaurantrush.domain.Restaurant;

public final class GameController {
    public static final int MAX_TURNS = 18;

    private final Restaurant restaurant;
    private final GameIO io;

    public GameController(Restaurant restaurant, GameIO io) {
        this.restaurant = Objects.requireNonNull(restaurant, "restaurant");
        this.io = Objects.requireNonNull(io, "io");
    }

    private void chooseWaiter(int turn) {
        while (true) {
            WaiterAction action = io.chooseWaiterAction();
            String refusal = performWaiter(action, turn);
            if (refusal == null) {
                return;
            }
            io.showInvalidAction(refusal);
        }
    }

    private String performWaiter(WaiterAction action, int turn) {
        if (action == null) {
            return "Choose a Waiter action or Wait";
        }
        if (action.type() == WaiterAction.Type.WAIT) {
            return null;
        }
        if (action.type() == WaiterAction.Type.SERVE) {
            Order order = restaurant.findOrder(action.targetId());
            if (order == null) {
                return "Order " + action.targetId() + " does not exist";
            }
            return restaurant.getWaiter().serve(order.getCustomer(), turn);
        }
        Customer customer = restaurant.findCustomer(action.targetId());
        if (customer == null) {
            return "Customer " + action.targetId() + " has not arrived";
        }
        if (action.type() == WaiterAction.Type.SEAT) {
            return restaurant.seatCustomer(customer, turn);
        }
        MenuItem item = customer.chooseItem(restaurant.getMenu());
        return restaurant.getWaiter().takeOrder(customer, item, item.getPriceCents(), turn);
    }

    private void chooseChef(int turn) {
        while (true) {
            int orderId = io.chooseChefOrder();
            if (orderId == 0) {
                return;
            }
            Order order = restaurant.findOrder(orderId);
            String refusal = order == null ? "Order " + orderId + " does not exist; use 'wait' to Wait"
                    : restaurant.getChef().prepare(order, turn);
            if (refusal == null) {
                return;
            }
            io.showInvalidAction(refusal);
        }
    }
}
