package Domain.Discount;

public interface DiscountComponent {
    double calculateDiscount(double originalPrice, PurchaseContext context);
}