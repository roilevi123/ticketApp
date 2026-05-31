package com.ticketing.ticketapp.Domain.PurchasePolicy;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;

@Entity
public class AgeLimitCondition extends PurchaseComponent {

    @Column(name = "min_age")
    private int minAge;

    protected AgeLimitCondition() {} // חובה עבור JPA

    public AgeLimitCondition(int minAge) {
        this.minAge = minAge;
    }

    @Override
    public boolean isSatisfied(PurchaseValidationData data) {
        return data.getUserAge() >= minAge;
    }

    @Override
    public String getDescription() {
        return "Minimum age: " + minAge;
    }
}