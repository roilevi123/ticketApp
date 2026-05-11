package com.ticketing.ticketapp.Domain.Discount;

import java.util.function.Predicate;

public class ConditionalDiscount implements DiscountComponent {
    private final double percentage;
    private final Predicate<PurchaseContext> condition;

    public ConditionalDiscount(double percentage, Predicate<PurchaseContext> condition) {
        this.percentage = percentage;
        this.condition = (condition != null) ? condition : ctx -> true;
    }

    @Override
    public double calculateDiscount(double price, PurchaseContext context) {
        if (condition.test(context)) {
            return price * (percentage / 100.0);
        }
        return 0;
    }
}
