package com.ticketing.ticketapp.Domain.Order;

public class ActiveOrderDomainException extends RuntimeException {
    public ActiveOrderDomainException(String message) {
        super(message);
    }
}
