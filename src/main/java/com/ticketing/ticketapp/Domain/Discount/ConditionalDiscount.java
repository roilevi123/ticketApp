package com.ticketing.ticketapp.Domain.Discount;

import java.util.function.Predicate;

public class ConditionalDiscount implements DiscountComponent {
    private final double percentage;
    private final Predicate<PurchaseContext> condition;
    private  final String conditionDescription;
    public ConditionalDiscount(double percentage, Predicate<PurchaseContext> condition,String conditionDescription) {
        this.percentage = percentage;
        this.condition = (condition != null) ? condition : ctx -> true;
        this.conditionDescription = conditionDescription;
    }

    @Override
    public double calculateDiscount(double price, PurchaseContext context) {
        if (condition.test(context)) {
            return price * (percentage / 100.0);
        }
        return 0;
    }
    public double getPercentage() {
        return percentage;
    }
    @Override
    public String getDescription() {
        return String.format("%.1f%% discount (condition: %s)", percentage, conditionDescription);
    }
}
