package restaurantrush.control;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import restaurantrush.domain.Customer;
import restaurantrush.domain.CustomerStatus;
import restaurantrush.domain.GameResult;
import restaurantrush.domain.MenuItem;
import restaurantrush.domain.Money;
import restaurantrush.domain.Order;
import restaurantrush.domain.OrderStatus;
import restaurantrush.domain.RegularCustomer;
import restaurantrush.domain.Restaurant;
import restaurantrush.domain.Table;

public final class GameController {
    public static final int MAX_TURNS = 18;

    private final Restaurant restaurant;
    private final GameIO io;

    public GameController(Restaurant restaurant, GameIO io) {
        this.restaurant = Objects.requireNonNull(restaurant, "restaurant");
        this.io = Objects.requireNonNull(io, "io");
    }

    public void run() {
        int turn = 1;
        String role = "Waiter";
        try {
            for (; turn <= MAX_TURNS; turn++) {
                if ((turn - 1) % 3 == 0) {
                    restaurant.registerArrival(new RegularCustomer((turn - 1) / 3 + 1));
                }
                io.showTurnStart(state(turn, true, true, "available; payments follow staff choices"));

                role = "Waiter";
                chooseWaiter(turn);
                io.showActionState(state(turn, false, true, "available; payments follow staff choices"));

                role = "Chef";
                chooseChef(turn);
                restaurant.advanceEating(turn);
                Customer payer = restaurant.oldestEligiblePayer(turn);
                String cashierStatus = "no eligible payment this turn";
                if (payer != null) {
                    String refusal = restaurant.collectPayment(payer, turn);
                    if (refusal != null) {
                        throw new IllegalStateException("Eligible automatic payment failed: " + refusal);
                    }
                    cashierStatus = "collected " + Money.format(payer.getOrder().getBillCents())
                            + " from C" + payer.getId() + "; customer and order PAID, table released";
                }
                restaurant.applyWaitingLoss();
                io.showActionState(state(turn, false, false, cashierStatus));
                io.showTurnEnd(summary(turn));
            }
            io.showFinalResult(finalReport());
        } catch (InputEndedException ended) {
            io.showInterrupted("Input ended during turn " + turn + ", " + role + " choice. Game interrupted.");
        }
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

    private GameView.TurnState state(int turn, boolean waiterAvailable, boolean chefAvailable,
                                    String cashierStatus) {
        List<GameView.CustomerLine> waiting = new ArrayList<>();
        List<GameView.TableLine> tables = new ArrayList<>();
        List<GameView.OrderLine> orders = new ArrayList<>();
        List<Integer> seatable = new ArrayList<>();
        List<Integer> orderable = new ArrayList<>();
        List<Integer> servable = new ArrayList<>();
        List<Integer> preparable = new ArrayList<>();
        for (Customer customer : restaurant.getWaitingQueue()) {
            waiting.add(customerLine(customer));
            if (waiterAvailable && restaurant.findFreeTable() != null) {
                seatable.add(customer.getId());
            }
        }
        for (Table table : restaurant.getTables()) {
            Customer occupant = table.getOccupant();
            tables.add(new GameView.TableLine(table.getId(), occupant == null ? null : customerLine(occupant)));
            if (waiterAvailable && occupant != null && occupant.getStatus() == CustomerStatus.SEATED) {
                orderable.add(occupant.getId());
            }
        }
        for (Customer customer : restaurant.getCustomers()) {
            Order order = customer.getOrder();
            if (order == null) {
                continue;
            }
            orders.add(new GameView.OrderLine(order.getId(), order.getItem().getName(),
                    order.getCompletedPreparationUnits(), order.getItem().getPreparationUnits(),
                    order.getStatus().name(), order.getBillCents()));
            if (waiterAvailable && order.canBeServedAt(turn)) {
                servable.add(order.getId());
            }
            if (chefAvailable && (order.getStatus() == OrderStatus.PLACED
                    || order.getStatus() == OrderStatus.PREPARING)) {
                preparable.add(order.getId());
            }
        }
        return new GameView.TurnState(1, turn, MAX_TURNS, restaurant.getRevenueCents(),
                restaurant.getPaidCount(), restaurant.getAveragePaidSatisfaction(), waiting, tables, orders,
                seatable, orderable, servable, preparable, cashierStatus);
    }

    private static GameView.CustomerLine customerLine(Customer customer) {
        return new GameView.CustomerLine(customer.getId(), customer.getTypeLabel(),
                customer.getSatisfaction(), customer.getStatus().name());
    }

    private GameView.TurnSummary summary(int turn) {
        return new GameView.TurnSummary(turn, MAX_TURNS, restaurant.getServedCount(), restaurant.getPaidCount(),
                restaurant.getUnhappyDepartureCount(), restaurant.getRevenueCents(),
                restaurant.getAveragePaidSatisfaction());
    }

    private GameView.FinalReport finalReport() {
        int paid = restaurant.getPaidCount();
        String average = restaurant.getAveragePaidSatisfaction().isPresent()
                ? String.format(Locale.ROOT, "%.1f", restaurant.getAveragePaidSatisfaction().getAsDouble()) : "N/A";
        return new GameView.FinalReport(restaurant.evaluateResult() == GameResult.VICTORY, summary(MAX_TURNS),
                List.of(new GameView.TargetLine("Paid customers", "4", Integer.toString(paid), paid >= 4),
                        new GameView.TargetLine("Revenue", Money.format(4500),
                                Money.format(restaurant.getRevenueCents()), restaurant.getRevenueCents() >= 4500),
                        new GameView.TargetLine("Average paid satisfaction", "60", average,
                                paid > 0 && restaurant.getPaidSatisfactionTotal() >= 60L * paid)));
    }
}
