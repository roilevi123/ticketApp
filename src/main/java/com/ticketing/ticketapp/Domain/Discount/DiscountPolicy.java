package com.ticketing.ticketapp.Domain.Discount;

import jakarta.persistence.*;

@Entity
@Table(name = "discount_policies")
public class DiscountPolicy {

    @Id
    @Column(name = "policy_id")
    private String policyId;

    @Column(name = "target_id")
    private String targetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type")
    private DiscountTargetType targetType;

    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "root_discount_id")
    private DiscountComponent root;

    protected DiscountPolicy() {}

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

    public String getPolicyId() { return policyId; }
    public String getTargetId() { return targetId; }
    public DiscountTargetType getTargetType() { return targetType; }
    public DiscountComponent getRoot() { return root; }
}