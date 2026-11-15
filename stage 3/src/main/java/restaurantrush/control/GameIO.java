package restaurantrush.control;

/**
 * The controller's typed interaction contract.
 * ConsoleUI and ScriptedGameIO implement it and handle syntax only;
 * game rules stay in the controller.
 *
 * Displays receive immutable snapshots, never mutable domain objects.
 */
public interface GameIO {
    /** Older test doubles decline activation unless explicitly scripted. May throw on EOF. */
    default boolean chooseHappyHourActivation() { return false; }

    /** Customer ID for the Host to seat, or 0 to Wait. May throw on EOF. */
    default int chooseHostCustomer() { return 0; }

    /** Remaining duration is 2, then 1, then 0. Used stays true after expiry. */
    default void showHappyHour(int turn, int remaining, boolean used) { }

    /** @throws InputEndedException if input ends before a choice is read */
    WaiterAction chooseWaiterAction();

    /**
     * @return order ID for the Chef to advance by one unit, or 0 to Wait
     *         (ConsoleUI parses the command {@code wait} into this sentinel)
     * @throws InputEndedException if input ends before a choice is read
     */
    int chooseChefOrder();

    /** Explains why the last choice was rejected, before the controller asks again. */
    void showInvalidAction(String message);

    default void showTurnStart(GameView.TurnState state) { }

    /** Refreshed after the Host, after the Waiter and after automatic changes. */
    default void showActionState(GameView.TurnState state) { }

    default void showTurnEnd(GameView.TurnSummary summary) { }

    default void showFinalResult(GameView.FinalReport report) { }

    default void showInterrupted(String message) { }
}
