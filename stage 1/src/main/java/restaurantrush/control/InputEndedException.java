package restaurantrush.control;

/** Thrown by GameIO when input ends (EOF) before a choice could be read. */
public class InputEndedException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public InputEndedException(String message) {
        super(message);
    }
}
