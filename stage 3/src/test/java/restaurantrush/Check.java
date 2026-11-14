package restaurantrush;

import java.util.Objects;

/** Assertions that work even without -ea. */
public final class Check {

    private Check() {
    }

    public static void isTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static void equal(Object expected, Object actual, String message) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(message + ": expected <" + expected + "> but was <" + actual + ">");
        }
    }

    public static void contains(String haystack, String needle, String message) {
        if (haystack == null || !haystack.contains(needle)) {
            throw new AssertionError(message + ": missing <" + needle + "> in:\n" + haystack);
        }
    }

    public static <T extends Throwable> T throwsType(Class<T> type, Runnable body, String message) {
        try {
            body.run();
        } catch (Throwable thrown) {
            if (type.isInstance(thrown)) {
                return type.cast(thrown);
            }
            throw new AssertionError(message + ": expected " + type.getSimpleName()
                    + " but got " + thrown, thrown);
        }
        throw new AssertionError(message + ": expected " + type.getSimpleName() + " but nothing was thrown");
    }
}
