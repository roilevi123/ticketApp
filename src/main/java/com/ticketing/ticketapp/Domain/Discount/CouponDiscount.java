package com.ticketing.ticketapp.Domain.Discount;

public class CouponDiscount implements DiscountComponent {
    private final String code;
    private final double percentage;

    public CouponDiscount(String code, double percentage) {
        this.code = code;
        this.percentage = percentage;
    }

    @Override
    public double calculateDiscount(double price, PurchaseContext context) {
        boolean isCodeMatch = code.equalsIgnoreCase(context.getUserCoupon());

        if (isCodeMatch ) {
            return price * (percentage / 100.0);
        }
        return 0;
    }
    public double gePercentage() {
        return percentage;
    }
    public String getCode() {
        return code;
    }
    @Override
    public String getDescription() {
        return String.format("%.1f%% discount with coupon code: %s", percentage, code);
    }
}
