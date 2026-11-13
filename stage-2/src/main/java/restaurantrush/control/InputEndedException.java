package restaurantrush.control;

/** Input ran out before the player chose. */
public class InputEndedException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public InputEndedException(String message) {
        super(message);
    }
}
