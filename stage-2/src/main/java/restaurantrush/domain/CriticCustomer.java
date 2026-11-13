package restaurantrush.domain;

public final class CriticCustomer extends Customer {
    public CriticCustomer(int id) {
        super(id);
    }

    @Override
    public String getTypeLabel() {
        return "Critic";
    }

    @Override
    protected int waitingLossPerTurn() {
        return 12;
    }

    @Override
    public MenuItem chooseItem(Menu menu) {
        if (menu == null) {
            throw new IllegalArgumentException("Menu cannot be null");
        }
        for (MenuItem item : menu.getItems()) {
            if (item.getName().equals("Pasta")) {
                return item;
            }
        }
        throw new IllegalStateException("Critic preference requires Pasta on the menu");
    }
}
