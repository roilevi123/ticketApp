package com.ticketing.ticketapp.Domain.Discount;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("COUPON")
public class CouponDiscount extends DiscountComponent {

    @Column(name = "coupon_code")
    private String code;

    @Column(name = "percentage")
    private double percentage;

    protected CouponDiscount() {
        super();
    }

    public CouponDiscount(String id, String code, double percentage) {
        super(id);
        this.code = code;
        this.percentage = percentage;
    }

    @Override
    public double calculateDiscount(double price, PurchaseContext context) {
        // מונע קריסה אם היוזר לא הזין קופון בכלל
        if (context.getUserCoupon() == null) {
            return 0;
        }

        boolean isCodeMatch = code.equalsIgnoreCase(context.getUserCoupon());

        if (isCodeMatch) {
            return price * (percentage / 100.0);
        }
        return 0;
    }

    // תוקנה שגיאת ההקלדה כאן
    public double getPercentage() {
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