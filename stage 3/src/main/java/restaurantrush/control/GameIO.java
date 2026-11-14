package restaurantrush.control;

public interface GameIO {

    default boolean chooseHappyHourActivation() { return false; }
    default int chooseHostCustomer() { return 0; }
    default void showHappyHour(int turn, int remaining, boolean used) { }

    WaiterAction chooseWaiterAction();

    int chooseChefOrder();

    void showInvalidAction(String message);

    default void showTurnStart(GameView.TurnState state) { }
    default void showActionState(GameView.TurnState state) { }
    default void showTurnEnd(GameView.TurnSummary summary) { }
    default void showFinalResult(GameView.FinalReport report) { }
    default void showInterrupted(String message) { }
}
