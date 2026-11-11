package restaurantrush.control;

import java.util.List;
import java.util.OptionalDouble;

public final class GameView {

    private GameView() {
    }

    /* A waiting or seated customer. */
    public record CustomerLine(int id, String type, int satisfaction, String status) {
    }

    /* A free table has a null occupant. */
    public record TableLine(int tableId, CustomerLine occupant) {
    }

    public record OrderLine(int orderId, String item, int completedUnits, int totalUnits,
                            String status, int billCents) {
    }

    /* Current game state and valid target IDs. Empty lists leave only Wait. */
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

    /* Running totals at the end of a turn. */
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
