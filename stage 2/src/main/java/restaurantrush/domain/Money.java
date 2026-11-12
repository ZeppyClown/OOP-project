package restaurantrush.domain;

public final class Money{
    private Money(){}
    public static String format(int cents){
        long magnitude=Math.abs((long) cents);
        long fraction=magnitude%100;
        return (cents<0 ? "-$" : "$")+magnitude/100+"."+(fraction<10 ? "0" : "")+fraction;
    }
}
