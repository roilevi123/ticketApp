package com.ticketing.ticketapp.Domain.PurchasedOrderAggregate;

public class PurchaseOrderException extends RuntimeException {
    public PurchaseOrderException(String message) {
        super(message);
    }

    public PurchaseOrderException(String message, Throwable cause) {
        super(message, cause);
    }
}
