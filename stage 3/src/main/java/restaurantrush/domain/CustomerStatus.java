package restaurantrush.domain;

public enum CustomerStatus {
    WAITING, //Join queue
    SEATED,
    WAITING_FOR_FOOD,
    SERVED,
    READY_TO_PAY,
    PAID,
    LEFT
}
