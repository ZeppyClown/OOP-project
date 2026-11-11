package restaurantrush.domain;

public abstract class Staff {
    private final String name;
    private int taskTurn=-1;
    protected Staff(String name){this.name=name;}
    public final String getName(){return name;}
    public final int getTaskTurn(){return taskTurn;}
    public final boolean canAct(int turn){return turn>0 && turn>taskTurn;}
    protected final String availabilityReason(int turn) {
        if (turn<=0)return "Turn must be positive";
        if (turn<taskTurn)return name+" cannot act on a backwards turn";
        if (turn==taskTurn)return name+" has already acted this turn";
        return null;
    }
    protected final void markTaskUsed(int turn){taskTurn=turn;}
}
