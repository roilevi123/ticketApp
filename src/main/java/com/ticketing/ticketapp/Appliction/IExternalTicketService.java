package com.ticketing.ticketapp.Appliction;

public interface IExternalTicketService {
    String issueTicket(String customerId, String eventId, String zone, int row, int seat);
    boolean cancelTicket(String ticketId);
}
