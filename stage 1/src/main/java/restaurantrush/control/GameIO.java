package restaurantrush.control;

public interface GameIO {
    /** @throws InputEndedException if input runs out */
    WaiterAction chooseWaiterAction();

    /** @return order ID to cook, or 0 to wait
     * @throws InputEndedException if input runs out */
    int chooseChefOrder();

    /** Says why a move was rejected. */
    void showInvalidAction(String message);

    default void showTurnStart(GameView.TurnState state) { }

    /** Redraws after the waiter acts and at the end of the turn. */
    default void showActionState(GameView.TurnState state) { }

    default void showTurnEnd(GameView.TurnSummary summary) { }

    default void showFinalResult(GameView.FinalReport report) { }

    default void showInterrupted(String message) { }
}
