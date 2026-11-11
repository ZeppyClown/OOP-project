package restaurantrush;

import java.io.InputStream;

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
        play(System.in);
    }

    private static void play(InputStream input) {
        Restaurant restaurant = new Restaurant(new Menu(), new Waiter(), new Chef(), new Cashier());
        new GameController(restaurant, new ConsoleUI(input, System.out)).run();
    }
}
