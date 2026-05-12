package com.ticketing.ticketapp.Domain.PurchasePolicy;

public class QuantityLimitCondition implements PurchaseComponent {
    private final int minQuantity;
    private final int maxQuantity;

    public QuantityLimitCondition(int min, int max) {
        this.minQuantity = min;
        this.maxQuantity = max;
    }

    @Override
    public boolean isSatisfied(PurchaseValidationData data) {
        return data.getQuantity() >= minQuantity && data.getQuantity() <= maxQuantity;
    }
    public int getMinQuantity() {
        return minQuantity;

    }
    public int getMaxQuantity() {
        return maxQuantity;
    }
    @Override
    public String getDescription() {
        return String.format("Quantity between %d and %d", minQuantity, maxQuantity);
    }
}
