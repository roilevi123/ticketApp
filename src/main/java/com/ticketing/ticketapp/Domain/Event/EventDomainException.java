package com.ticketing.ticketapp.Domain.Event;

public class EventDomainException extends RuntimeException {
    public EventDomainException(String message) {
        super(message);
    }
}
