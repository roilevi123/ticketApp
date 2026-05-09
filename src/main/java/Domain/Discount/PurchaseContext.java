package Domain.Discount;

import java.util.Date;

public class PurchaseContext {
    private final int quantity;
    private final String userCoupon;
    private final Date purchaseDate;
    public PurchaseContext(int quantity, String userCoupon, Date purchaseDate) {
        this.quantity = quantity;
        this.userCoupon = userCoupon;
        this.purchaseDate = purchaseDate;
    }

    public int getQuantity() { return quantity; }
    public String getUserCoupon() { return userCoupon; }
    public Date getPurchaseDate() { return purchaseDate; }
}