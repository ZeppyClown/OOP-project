package restaurantrush.domain;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.OptionalDouble;


public class Restaurant {
    private final List<Table> tables;
    private final Deque<Customer> waitingQueue;
    private final List<Customer> customers;
    private final Menu menu;
    private final Waiter waiter;
    private final Chef chef;
    private final Cashier cashier;
    private final Host host;

    public Restaurant(Menu menu, Waiter waiter, Chef chef, Cashier cashier) {
        this(menu, waiter, chef, cashier, new Host());
    }

    public Restaurant(Menu menu, Waiter waiter, Chef chef, Cashier cashier, Host host) {
        if (menu == null || waiter == null || chef == null || cashier == null || host == null) {
            throw new IllegalArgumentException("Dependencies cannot be null");
        }
        this.tables = List.of(new Table(1), new Table(2));
        this.waitingQueue = new ArrayDeque<>();
        this.customers = new ArrayList<>();
        this.menu = menu;
        this.waiter = waiter;
        this.chef = chef;
        this.cashier = cashier;
        this.host = host;
    }

    public List<Table> getTables() {
        return Collections.unmodifiableList(tables);
    }

    public List<Customer> getWaitingQueue() {
        return Collections.unmodifiableList(new ArrayList<>(waitingQueue));
    }

    public List<Customer> getCustomers() {
        return Collections.unmodifiableList(customers);
    }

    public Menu getMenu() {
        return menu;
    }

    Waiter getWaiter() {
        return waiter;
    }

    Chef getChef() {
        return chef;
    }

    public Cashier getCashier() {
        return cashier;
    }

    public Host getHost() {
        return host;
    }

    public void registerArrival(Customer customer) {
        if (customer == null) {
            throw new IllegalArgumentException("Arriving customer cannot be null");
        }
        for (Customer existing : customers) {
            if (existing.getId() == customer.getId()) {
                throw new IllegalStateException("Duplicate customer ID: " + customer.getId());
            }
        }
        if (customer.getStatus() != CustomerStatus.WAITING) {
            throw new IllegalArgumentException("Arriving customer must be WAITING");
        }
        // Claim exclusive ownership only after validation, before either collection changes.
        customer.bindTo(this);
        customers.add(customer);
        waitingQueue.addLast(customer);
    }

    public Customer findWaitingCustomer(int id) {
        for (Customer c : waitingQueue) {
            if (c.getId() == id && c.getStatus() == CustomerStatus.WAITING) {
                return c;
            }
        }
        return null;
    }

    public Customer findCustomer(int id) {
        for (Customer c : customers) {
            if (c.getId() == id) {
                return c;
            }
        }
        return null;
    }

    public Order findOrder(int orderId) {
        for (Customer c : customers) {
            if (c.getOrder() != null && c.getOrder().getId() == orderId) {
                return c.getOrder();
            }
        }
        return null;
    }

    public Table findFreeTable() {
        for (Table t : tables) {
            if (t.isFree()) {
                return t;
            }
        }
        return null;
    }

    public Table findTableFor(Customer customer) {
        if (customer == null) {
            return null;
        }
        for (Table t : tables) {
            if (t.getOccupant() == customer) {
                return t;
            }
        }
        return null;
    }

    public String seatCustomer(Customer customer, int turn) {
        return seatCustomer(customer, turn, false);
    }

    public String seatCustomerWithHost(Customer customer, int turn) {
        return seatCustomer(customer, turn, true);
    }

    private String seatCustomer(Customer customer, int turn, boolean useHost) {
        if (customer == null || findCustomer(customer.getId()) != customer) {
            return "Customer is not registered in this restaurant";
        }
        if (!waitingQueue.contains(customer)) { return "Customer is not in the waiting queue"; }
        Table table = findFreeTable();
        if (table == null) { return "No free table"; }
        String reason = useHost ? host.seat(customer, table, turn) : waiter.seat(customer, table, turn);
        if (reason == null) { waitingQueue.remove(customer); }
        return reason;
    }

    public String takeOrder(Customer customer, PricingPolicy pricing, int turn) {
        if (pricing == null) {
            throw new IllegalArgumentException("Pricing policy cannot be null");
        }
        if (customer == null || findCustomer(customer.getId()) != customer) {
            return "Customer is not registered in this restaurant";
        }
        if (customer.getStatus() != CustomerStatus.SEATED) {
            return "Customer " + customer.getId() + " must be SEATED to order; currently " + customer.getStatus();
        }
        MenuItem item = customer.chooseItem(menu);
        return waiter.takeOrder(customer, item, pricing.priceFor(customer, item), turn);
    }

    public String serveOrder(Order order, int turn) {
        if (order == null || findOrder(order.getId()) != order) {
            return "Order is not registered in this restaurant";
        }
        return waiter.serve(order.getCustomer(), turn);
    }

    public String prepareOrder(Order order, int turn) {
        if (order == null || findOrder(order.getId()) != order) {
            return "Order is not registered in this restaurant";
        }
        return chef.prepare(order, turn);
    }

    public String collectPayment(Customer customer, int turn) {
        if (customer == null || findCustomer(customer.getId()) != customer) {
            return "Customer is not registered in this restaurant";
        }
        Table table = findTableFor(customer);
        if (table == null) { return "Customer has no occupied table"; }
        String reason = cashier.collect(customer, turn);
        if (reason == null) { release(table); }
        return reason;
    }

    private static void release(Table table) {
        String refusal = table.release();
        if (refusal != null) {
            throw new IllegalStateException("Table release refused: " + refusal);
        }
    }

    public void advanceEating(int turn) {
        for (Customer c : customers) {
            if (c.getStatus() == CustomerStatus.SERVED && c.getServedTurn() < turn) {
                String refusal = c.finishEating(turn);
                if (refusal != null) {
                    throw new IllegalStateException("Finish eating refused: " + refusal);
                }
            }
        }
    }

    public Customer oldestEligiblePayer(int turn) {
        Customer oldest = null;
        for (Customer c : customers) {
            if (c.getStatus() == CustomerStatus.READY_TO_PAY && c.getReadyToPayTurn() < turn) {
                if (oldest == null) {
                    oldest = c;
                } else if (c.getReadyToPayTurn() < oldest.getReadyToPayTurn()) {
                    oldest = c;
                } else if (c.getReadyToPayTurn() == oldest.getReadyToPayTurn() && c.getId() < oldest.getId()) {
                    oldest = c;
                }
            }
        }
        return oldest;
    }

    public void applyWaitingLoss() {
        for (Customer c : customers) {
            if (c.isWaitingUnserved()) {
                c.loseWaitingSatisfaction();
                if (c.getStatus() == CustomerStatus.LEFT) {
                    waitingQueue.remove(c);
                    Table table = findTableFor(c);
                    if (table != null) {
                        release(table);
                    }
                }
            }
        }
    }

    public void recoverWaitingSatisfaction(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Recovery amount cannot be negative");
        }
        for (Customer customer : customers) {
            customer.recoverWaitingSatisfaction(amount);
        }
    }

    public int getRevenueCents() {
        int sum = 0;
        for (Customer c : customers) {
            if (c.getStatus() == CustomerStatus.PAID && c.getOrder() != null) {
                sum += c.getOrder().getBillCents();
            }
        }
        return sum;
    }

    public int getPaidCount() {
        int count = 0;
        for (Customer c : customers) {
            if (c.getStatus() == CustomerStatus.PAID) {
                count++;
            }
        }
        return count;
    }

    public int getServedCount() {
        int count = 0;
        for (Customer c : customers) {
            if (c.getServedTurn() >= 0) {
                count++;
            }
        }
        return count;
    }

    public int getUnhappyDepartureCount() {
        int count = 0;
        for (Customer c : customers) {
            if (c.getStatus() == CustomerStatus.LEFT) {
                count++;
            }
        }
        return count;
    }

    public int getPaidSatisfactionTotal() {
        int total = 0;
        for (Customer c : customers) {
            if (c.getStatus() == CustomerStatus.PAID) {
                total += c.getSatisfactionAtPayment();
            }
        }
        return total;
    }

    public OptionalDouble getAveragePaidSatisfaction() {
        int paidCount = getPaidCount();
        if (paidCount == 0) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of((double) getPaidSatisfactionTotal() / paidCount);
    }

    public GameResult evaluateResult() {
        int paid = getPaidCount();
        int revenue = getRevenueCents();
        int satTotal = getPaidSatisfactionTotal();
        boolean passPaid = paid >= 4;
        boolean passRevenue = revenue >= 4500;
        boolean passSatisfaction = paid > 0 && (long) satTotal >= 60L * paid;
        return (passPaid && passRevenue && passSatisfaction) ? GameResult.VICTORY : GameResult.DEFEAT;
    }
}
