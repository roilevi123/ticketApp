package com.ticketing.ticketapp.Domain.Discount;

import java.util.Date;

import java.util.Date;

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
}
