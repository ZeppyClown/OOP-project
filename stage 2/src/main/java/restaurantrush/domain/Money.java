package restaurantrush.domain;

public final class Money{
    private Money(){}
    public static int applyPercent(int cents,int percent){
        if (cents<0||percent<0||percent>100)throw new IllegalARguementException("Cents mus be nonnegative and percent must be in 0-100");
        return (int)(((long)cents*percent+50)/100);
    }
    public static String format(int cents){
        long magnitude=Math.abs((long) cents);
        long fraction=magnitude%100;
        return (cents<0 ? "-$" : "$")+magnitude/100+"."+(fraction<10 ? "0" : "")+fraction;
    }
}
