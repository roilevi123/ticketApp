package com.ticketing.ticketapp.Domain.PurchasePolicy;

public interface PurchaseComponent {
    boolean isSatisfied(PurchaseValidationData data);
    String getDescription();

    /** Returns the maximum number of seats this condition permits, or {@code null} if unrestricted. */
    default Integer getMaxSeats() { return null; }
}
