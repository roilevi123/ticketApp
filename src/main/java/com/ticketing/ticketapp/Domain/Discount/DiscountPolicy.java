package com.ticketing.ticketapp.Domain.Discount;

public class DiscountPolicy {
    private final String policyId;
    private final String targetId;
    private final DiscountTargetType targetType;
    private final DiscountComponent root;

    public DiscountPolicy(String policyId, String targetId, DiscountTargetType targetType, DiscountComponent root) {
        this.policyId = policyId;
        this.targetId = targetId;
        this.targetType = targetType;
        this.root = root;
    }

    public double calculateFinalPrice(double originalPrice, PurchaseContext context) {
        double discountAmount = root.calculateDiscount(originalPrice, context);
        return Math.max(0, originalPrice - discountAmount);
    }

    // Getters
    public String getPolicyId() { return policyId; }
    public String getTargetId() { return targetId; }
    public DiscountTargetType getTargetType() { return targetType; }
    public DiscountComponent getRoot() { return root; }
}
