package com.ticketing.ticketapp.Domain.Ticket;

public class TicketDomainException extends RuntimeException {
    public TicketDomainException(String message) {
        super(message);
    }
}
