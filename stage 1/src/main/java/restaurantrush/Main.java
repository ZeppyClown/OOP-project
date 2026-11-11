package restaurantrush;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

import restaurantrush.boundary.ConsoleUI;
import restaurantrush.control.GameController;
import restaurantrush.domain.Cashier;
import restaurantrush.domain.Chef;
import restaurantrush.domain.Menu;
import restaurantrush.domain.Restaurant;
import restaurantrush.domain.Waiter;

public final class Main {
    private Main() { }

    public static void main(String[] args) {
        int status = launch(args);
        if (status != 0) {
            System.exit(status);
        }
    }

    private static int launch(String[] args) {
        if (args.length != 0 && !(args.length == 2 && args[0].equals("--script"))) {
            System.err.println("Usage: restaurantrush.Main [--script <path>]");
            return 2;
        }
        try {
            if (args.length == 0) {
                play(System.in);
            } else {
                Path script;
                try {
                    script = Path.of(args[1]);
                } catch (InvalidPathException invalid) {
                    System.err.println("Cannot open script: invalid path");
                    return 1;
                }
                try (InputStream input = Files.newInputStream(script)) {
                    play(input);
                }
            }
        } catch (IOException failure) {
            System.err.println("Script I/O error: " + failure.getMessage());
            return 1;
        } catch (UncheckedIOException failure) {
            System.err.println("Input I/O error: " + failure.getCause().getMessage());
            return 1;
        }
        return 0;
    }

    private static void play(InputStream input) {
        Restaurant restaurant = new Restaurant(new Menu(), new Waiter(), new Chef(), new Cashier());
        new GameController(restaurant, new ConsoleUI(input, System.out)).run();
    }
}
