package com.ticketing.ticketapp.Domain.PurchasePolicy;

public interface PurchaseComponent {
    boolean isSatisfied(PurchaseValidationData data);
    String getDescription();
}
