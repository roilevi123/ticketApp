package com.ticketing.ticketapp.Domain.PurchasePolicy;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("AGE_LIMIT")
public class AgeLimitCondition extends PurchaseComponent {

    @Column(name = "min_age")
    private int minAge;

    protected AgeLimitCondition() {
        super();
    }

    public AgeLimitCondition(int minAge) {
        super();
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