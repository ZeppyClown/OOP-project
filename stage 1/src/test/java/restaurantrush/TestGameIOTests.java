package restaurantrush;

import java.util.List;

import restaurantrush.control.InputEndedException;
import restaurantrush.control.WaiterAction;

/** Checks the TurnTests test double itself. Run: java -cp <test-classes> restaurantrush.TestGameIOTests */
public final class TestGameIOTests {
    private static int passed;

    public static void main(String[] args) {
        returnsWaiterChoicesInOrder();
        returnsChefChoicesInOrderWithZeroAsWait();
        waiterRunOutThrowsInputEnded();
        chefRunOutThrowsInputEnded();
        recordsInvalidMessagesAndCallOrder();
        repeatedHelpersQueueEachChoice();
        allChoicesUsedOnlyWhenBothListsEmpty();
        System.out.println("TestGameIOTests: " + passed + " passed");
    }

    private static void returnsWaiterChoicesInOrder() {
        TestGameIO io = new TestGameIO().waiter(WaiterAction.seat(1), WaiterAction.order(1));
        check(io.chooseWaiterAction().equals(WaiterAction.seat(1)), "first Waiter choice is seat 1");
        check(io.chooseWaiterAction().equals(WaiterAction.order(1)), "second Waiter choice is order 1");
        check(io.waiterRequests() == 2, "two Waiter requests counted");
        passed++;
    }

    private static void returnsChefChoicesInOrderWithZeroAsWait() {
        TestGameIO io = new TestGameIO().chef(1, 0);
        check(io.chooseChefOrder() == 1, "first Chef choice is order 1");
        check(io.chooseChefOrder() == 0, "second Chef choice is Wait (0)");
        check(io.chefRequests() == 2, "two Chef requests counted");
        passed++;
    }

    private static void waiterRunOutThrowsInputEnded() {
        TestGameIO io = new TestGameIO();
        checkThrowsInputEnded(io::chooseWaiterAction, "empty Waiter script");
        checkThrowsInputEnded(io::chooseWaiterAction, "Waiter script stays ended");
        check(io.waiterRequests() == 2, "requests still counted after input ends");
        passed++;
    }

    private static void chefRunOutThrowsInputEnded() {
        TestGameIO io = new TestGameIO().chef(3);
        io.chooseChefOrder();
        checkThrowsInputEnded(io::chooseChefOrder, "Chef script used up");
        passed++;
    }

    private static void recordsInvalidMessagesAndCallOrder() {
        TestGameIO io = new TestGameIO().waiter(WaiterAction.seat(2), WaiterAction.seat(1)).chef(0);
        io.chooseWaiterAction();
        io.showInvalidAction("Customer 2 has not arrived");
        io.chooseWaiterAction();
        io.chooseChefOrder();
        check(io.invalidMessages().equals(List.of("Customer 2 has not arrived")), "invalid message recorded");
        check(io.calls().equals(List.of("chooseWaiterAction", "showInvalidAction",
                "chooseWaiterAction", "chooseChefOrder")), "calls recorded in order");
        passed++;
    }

    private static void repeatedHelpersQueueEachChoice() {
        TestGameIO io = new TestGameIO()
                .waiterRepeated(WaiterAction.waitAction(), 18)
                .chefRepeated(0, 18);
        for (int turn = 1; turn <= 18; turn++) {
            check(io.chooseWaiterAction().type() == WaiterAction.Type.WAIT, "Waiter Wait on turn " + turn);
            check(io.chooseChefOrder() == 0, "Chef Wait on turn " + turn);
        }
        check(io.allChoicesUsed(), "all 18 turns used exactly");
        checkThrowsInputEnded(io::chooseWaiterAction, "19th Waiter request");
        passed++;
    }

    private static void allChoicesUsedOnlyWhenBothListsEmpty() {
        TestGameIO io = new TestGameIO().waiter(WaiterAction.waitAction()).chef(0);
        check(!io.allChoicesUsed(), "not used before any request");
        io.chooseWaiterAction();
        check(!io.allChoicesUsed(), "Chef choice still pending");
        io.chooseChefOrder();
        check(io.allChoicesUsed(), "both lists used");
        passed++;
    }

    private static void checkThrowsInputEnded(Runnable call, String label) {
        try {
            call.run();
        } catch (InputEndedException expected) {
            return;
        }
        throw new AssertionError("Expected InputEndedException: " + label);
    }

    private static void check(boolean condition, String label) {
        if (!condition) {
            throw new AssertionError("Failed: " + label);
        }
    }
}
