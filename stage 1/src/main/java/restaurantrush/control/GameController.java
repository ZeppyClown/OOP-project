package restaurantrush.control;

import java.util.Objects;

import restaurantrush.domain.Restaurant;

public final class GameController {
    public static final int MAX_TURNS = 18;

    private final Restaurant restaurant;
    private final GameIO io;

    public GameController(Restaurant restaurant, GameIO io) {
        this.restaurant = Objects.requireNonNull(restaurant, "restaurant");
        this.io = Objects.requireNonNull(io, "io");
    }
}
