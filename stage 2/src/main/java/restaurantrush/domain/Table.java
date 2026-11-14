package restaurantrush.domain;

public class Table {
    private final int id;
    private Customer occupant;

    public Table(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("Table ID must be positive: " + id);
        }
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public boolean isFree() {
        return occupant == null;
    }

    public Customer getOccupant() {
        return occupant;
    }

    String seat(Customer customer) {
        if (!isFree()) {
            return "Table " + id + " is already occupied by Customer " + occupant.getId();
        }
        if (customer == null) {
            return "Customer cannot be null";
        }
        String custErr = customer.seat();
        if (custErr != null) {
            return custErr;
        }
        this.occupant = customer;
        return null;
    }

    String release() {
        if (occupant == null) {
            return null;
        }
        if (occupant.getStatus() != CustomerStatus.PAID && occupant.getStatus() != CustomerStatus.LEFT) {
            return "Cannot release Table " + id + " while Customer " + occupant.getId()
                    + " has status " + occupant.getStatus();
        }
        this.occupant = null;
        return null;
    }
}