package com.ticketing.ticketapp.Domain.PurchasePolicy;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("QUANTITY_LIMIT") // <-- הורדנו את ההערה!
public class QuantityLimitCondition extends PurchaseComponent {

    @Column(name = "min_quantity")
    private int minQuantity;

    @Column(name = "max_quantity")
    private int maxQuantity;

    protected QuantityLimitCondition() {
        super();
    } // חובה עבור JPA

    public QuantityLimitCondition(int min, int max) {
        super();
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
    public Integer getMaxSeats() {
        return maxQuantity;
    }

    @Override
    public String getDescription() {
        return String.format("Quantity between %d and %d", minQuantity, maxQuantity);
    }
}