package restaurantrush.control;

public interface GameIO {
    /** @throws InputEndedException if input ends before a choice */
    WaiterAction chooseWaiterAction();

    /** @return order ID to prepare one unit, or 0 to wait
     * @throws InputEndedException if input ends before a choice */
    int chooseChefOrder();

    /** Shows why a choice was rejected. */
    void showInvalidAction(String message);
}
