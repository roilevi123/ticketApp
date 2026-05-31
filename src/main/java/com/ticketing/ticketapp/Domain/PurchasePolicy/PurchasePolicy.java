package com.ticketing.ticketapp.Domain.PurchasePolicy;

import jakarta.persistence.*;

@Entity
@Table(name = "purchase_policies")
public class PurchasePolicy {

    @Id
    @Column(name = "policy_id")
    private String policyId;

    @Column(name = "target_id")
    private String targetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type")
    private PurchaseTargetType targetType;

    @OneToOne(cascade = CascadeType.ALL, targetEntity = PurchaseComponent.class)
    @JoinColumn(name = "root_condition_id")
    private PurchaseComponent rootCondition;

    protected PurchasePolicy() {
    }

    public PurchasePolicy(String policyId, String targetId, PurchaseTargetType targetType, PurchaseComponent rootCondition) {
        this.policyId = policyId;
        this.targetId = targetId;
        this.targetType = targetType;
        this.rootCondition = rootCondition;
    }

    public boolean validate(PurchaseValidationData data) {
        return rootCondition.isSatisfied(data);
    }

    public String getPolicyId() { return policyId; }
    public String getTargetId() { return targetId; }
    public PurchaseComponent getRoot() { return rootCondition; }
    public PurchaseTargetType getTargetType() { return targetType; }
}