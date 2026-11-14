package restaurantrush.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Exact pricing checks use the real domain without depending on customer subtypes. */
public final class PricingTests {
    private PricingTests() { }

    public static void main(String[] args) {
        runAll();
        System.out.println("PricingTests: 8 scenarios passed (20,907 decimal-oracle comparisons)");
    }

    public static void runAll() {
        menuQuotes();
        halfUpRounding();
        validBoundaries();
        invalidInputs();
        integerExtremes();
        decimalOracle();
        discountedBillsStayLockedThroughPayment();
        cancelledDiscountedBillAddsNoRevenue();
    }

    private static void menuQuotes() {
        int[] expected = {1350, 810, 1620};
        String[] names = {"Burger", "Salad", "Pasta"};
        Menu menu = new Menu();
        for (int i = 0; i < expected.length; i++) {
            MenuItem item = menu.getItems().get(i);
            equal(names[i], item.getName(), "quote belongs to documented dish");
            equal(expected[i], Money.applyPercent(item.getPriceCents(), 90), "90% " + names[i]);
            equal(item.getPriceCents(), Money.applyPercent(item.getPriceCents(), 100), "full price");
        }
    }

    private static void halfUpRounding() {
        equal(0, Money.applyPercent(1, 49), "below half a cent rounds down");
        equal(1, Money.applyPercent(1, 50), "half a cent rounds up");
        equal(1, Money.applyPercent(1, 51), "above half a cent rounds up");
        equal(1, Money.applyPercent(3, 49), "1.47 cents rounds down");
        equal(2, Money.applyPercent(3, 50), "1.50 cents rounds up, rather than to even");
        equal(5, Money.applyPercent(5, 90), "4.50 cents rounds up");
        equal(14, Money.applyPercent(15, 90), "13.50 cents rounds up");
        equal(11, Money.applyPercent(12, 90), "10.80 cents rounds up");
        equal(12, Money.applyPercent(13, 90), "11.70 cents rounds up");
        equal(9, Money.applyPercent(10, 90), "whole cent stays exact");
    }

    private static void validBoundaries() {
        for (int percent = 0; percent <= 100; percent++) {
            equal(0, Money.applyPercent(0, percent), "zero cents at " + percent + "%");
        }
        for (int cents : new int[] {0, 1, 99, 100, 1500, Integer.MAX_VALUE}) {
            equal(0, Money.applyPercent(cents, 0), "0% is free");
            equal(cents, Money.applyPercent(cents, 100), "100% is identity");
        }
    }

    private static void invalidInputs() {
        for (int cents : new int[] {-1, Integer.MIN_VALUE}) {
            for (int percent : new int[] {0, 90, 100}) {
                illegal(() -> Money.applyPercent(cents, percent));
            }
        }
        for (int percent : new int[] {Integer.MIN_VALUE, -1, 101, Integer.MAX_VALUE}) {
            for (int cents : new int[] {0, 1, Integer.MAX_VALUE}) {
                illegal(() -> Money.applyPercent(cents, percent));
            }
        }
    }

    private static void integerExtremes() {
        equal(21474836, Money.applyPercent(Integer.MAX_VALUE, 1), "maximum cents at 1%");
        equal(1073741824, Money.applyPercent(Integer.MAX_VALUE, 50), "maximum cents half rounds up");
        equal(1932735282, Money.applyPercent(Integer.MAX_VALUE, 90), "maximum cents at 90%");
        equal(2126008811, Money.applyPercent(Integer.MAX_VALUE, 99), "maximum cents at 99%");
        equal(Integer.MAX_VALUE, Money.applyPercent(Integer.MAX_VALUE, 100), "product and rounding cannot overflow");
        equal(1073741823, Money.applyPercent(Integer.MAX_VALUE - 1, 50), "large exact half");
    }

    private static void decimalOracle() {
        // Exercise every percentage and every product remainder, plus values near int overflow.
        for (int cents = 0; cents <= 200; cents++) {
            compareEveryPercentage(cents);
        }
        for (int cents : new int[] {21474836, 21474837, 1073741823,
                1073741824, Integer.MAX_VALUE - 1, Integer.MAX_VALUE}) {
            compareEveryPercentage(cents);
        }
    }

    private static void compareEveryPercentage(int cents) {
        int previous = -1;
        for (int percent = 0; percent <= 100; percent++) {
            int expected = BigDecimal.valueOf(cents).multiply(BigDecimal.valueOf(percent))
                    .movePointLeft(2).setScale(0, RoundingMode.HALF_UP).intValueExact();
            int actual = Money.applyPercent(cents, percent);
            equal(expected, actual, cents + " cents at " + percent + "% matches decimal oracle");
            check(actual >= previous && actual <= cents, "percentage is monotone and bounded by base cents");
            previous = actual;
        }
    }

    private static void discountedBillsStayLockedThroughPayment() {
        for (MenuItem item : new Menu().getItems()) {
            Restaurant restaurant = new Restaurant(new Menu(), new Waiter(), new Chef(), new Cashier());
            Customer customer = new RegularCustomer(1);
            restaurant.registerArrival(customer);
            ok(restaurant.seatCustomer(customer, 1));
            int baseCents = item.getPriceCents();
            int quote = Money.applyPercent(baseCents, 90);
            ok(restaurant.getWaiter().takeOrder(customer, item, quote, 2));
            Order order = customer.getOrder();
            equal(quote, order.getBillCents(), "quote locked at placement");
            equal(0, restaurant.getRevenueCents(), "unpaid quote is not revenue");
            int readyTurn = 1 + item.getPreparationUnits();
            for (int turn = 2; turn <= readyTurn; turn++) {
                ok(restaurant.getChef().prepare(order, turn));
                equal(quote, order.getBillCents(), "preparation retains quote");
            }
            ok(restaurant.getWaiter().serve(customer, readyTurn + 1));
            restaurant.advanceEating(readyTurn + 2);
            equal(quote, order.getBillCents(), "serving and eating retain quote");
            equal(0, restaurant.getRevenueCents(), "ready payer still adds no revenue");
            ok(restaurant.collectPayment(customer, readyTurn + 3));
            equal(OrderStatus.PAID, order.getStatus(), "discounted order reaches paid");
            equal(quote, order.getBillCents(), "payment retains quote");
            equal(quote, restaurant.getRevenueCents(), "payment credits exact locked quote");
            equal(1, restaurant.getPaidCount(), "one payer recorded");
            check(restaurant.getTables().stream().allMatch(Table::isFree), "paid table released");
            equal(baseCents, item.getPriceCents(), "menu price stays full");
        }
    }

    private static void cancelledDiscountedBillAddsNoRevenue() {
        Restaurant restaurant = new Restaurant(new Menu(), new Waiter(), new Chef(), new Cashier());
        Customer customer = new RegularCustomer(1);
        restaurant.registerArrival(customer);
        ok(restaurant.seatCustomer(customer, 1));
        MenuItem pasta = restaurant.getMenu().getItems().get(2);
        int quote = Money.applyPercent(pasta.getPriceCents(), 90);
        ok(restaurant.getWaiter().takeOrder(customer, pasta, quote, 2));
        ok(restaurant.getChef().prepare(customer.getOrder(), 2));
        for (int i = 0; i < 13; i++) { restaurant.applyWaitingLoss(); }
        equal(OrderStatus.CANCELLED, customer.getOrder().getStatus(), "abandonment cancels discounted order");
        equal(quote, customer.getOrder().getBillCents(), "cancelled bill remains locked");
        String refusal = restaurant.collectPayment(customer, 15);
        check(refusal != null && !refusal.isBlank(), "cancelled quote cannot be collected");
        equal(0, restaurant.getRevenueCents(), "cancelled quote is never revenue");
        equal(0, restaurant.getPaidCount(), "cancelled customer is not a payer");
        check(restaurant.getTables().stream().allMatch(Table::isFree), "abandonment releases table");
        check(restaurant.getCashier().canAct(15), "rejected collection preserves Cashier task");
    }

    private static void illegal(Runnable action) {
        try {
            action.run();
            throw new AssertionError("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) { /* pricing bounds */ }
    }

    private static void ok(String reason) { check(reason == null, "Expected success: " + reason); }
    private static void check(boolean value, String message) { if (!value) { throw new AssertionError(message); } }
    private static void equal(Object expected, Object actual, String message) {
        check(java.util.Objects.equals(expected, actual), message + ": expected " + expected + ", got " + actual);
    }
}
