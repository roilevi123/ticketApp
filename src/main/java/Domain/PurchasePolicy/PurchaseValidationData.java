package Domain.PurchasePolicy;

public class PurchaseValidationData {
    private final int userAge;
    private final int quantity;

    public PurchaseValidationData(int userAge, int quantity) {
        this.userAge = userAge;
        this.quantity = quantity;
    }

    public int getUserAge() { return userAge; }
    public int getQuantity() { return quantity; }
}