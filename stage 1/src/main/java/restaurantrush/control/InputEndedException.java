package restaurantrush.control;

/** Input ended before the player made a choice. */
public class InputEndedException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public InputEndedException(String message) {
        super(message);
    }
}
