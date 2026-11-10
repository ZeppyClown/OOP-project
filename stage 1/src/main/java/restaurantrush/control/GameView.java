package restaurantrush.control;

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
}
