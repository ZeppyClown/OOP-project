package restaurantrush.control;

import java.util.List;
import java.util.OptionalDouble;

/**
 * Immutable snapshots the controller builds from Restaurant/Order queries and
 * hands to GameIO. The UI formats these values; it never receives mutable
 * domain objects and never decides eligibility itself.
 *
 * The same state shape is refreshed between Host, Waiter and Chef choices.
 */
public final class GameView {

    private GameView() {
    }

    /** A waiting or seated customer. {@code status} is the CustomerStatus name. */
    public record CustomerLine(int id, String type, int satisfaction, String status) {
    }

    /** {@code occupant} is null when the table is free. */
    public record TableLine(int tableId, CustomerLine occupant) {
    }

    public record OrderLine(int orderId, String item, int completedUnits, int totalUnits,
                            String status, int billCents) {
    }

    /**
     * Full start-of-turn state. The ID lists are the controller's eligible
     * targets for this turn; an empty list means only Wait is useful.
     */
    public record TurnState(int stage, int turn, int maxTurns,
                            int revenueCents, int paidCount, OptionalDouble averagePaidSatisfaction,
                            List<CustomerLine> waiting, List<TableLine> tables, List<OrderLine> orders,
                            List<Integer> seatableCustomerIds, List<Integer> orderableCustomerIds,
                            List<Integer> servableOrderIds, List<Integer> preparableOrderIds,
                            String cashierStatus) {
        public TurnState {
            waiting = List.copyOf(waiting);
            tables = List.copyOf(tables);
            orders = List.copyOf(orders);
            seatableCustomerIds = List.copyOf(seatableCustomerIds);
            orderableCustomerIds = List.copyOf(orderableCustomerIds);
            servableOrderIds = List.copyOf(servableOrderIds);
            preparableOrderIds = List.copyOf(preparableOrderIds);
        }
    }

    /** End-of-turn statistics; counts are cumulative for the run. */
    public record TurnSummary(int turn, int maxTurns, int servedCount, int paidCount, int departures,
                              int revenueCents, OptionalDouble averagePaidSatisfaction) {
    }

    public record TargetLine(String name, String required, String actual, boolean met) {
    }

    public record FinalReport(boolean victory, TurnSummary totals, List<TargetLine> targets) {
        public FinalReport {
            targets = List.copyOf(targets);
        }
    }
}
