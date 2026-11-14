package restaurantrush.control;

/** Once-only two-turn window, measured from its activation timestamp. */
public final class HappyHour {
    private int activationTurn;

    public boolean hasBeenActivated() {
        return activationTurn != 0;
    }

    /** The controller invokes this only at turn start. Null means success. */
    public String activate(int turn) {
        if (turn < 1 || turn > GameController.MAX_TURNS) {
            return "Happy Hour activation turn must be between 1 and 18";
        }
        if (hasBeenActivated()) {
            return "Happy Hour has already been activated";
        }
        activationTurn = turn;
        return null;
    }

    public boolean isActiveAt(int turn) {
        return hasBeenActivated() && turn >= activationTurn && (long) turn < (long) activationTurn + 2;
    }

    public int remainingAt(int turn) {
        return isActiveAt(turn) ? (int) ((long) activationTurn + 2 - turn) : 0;
    }
}
