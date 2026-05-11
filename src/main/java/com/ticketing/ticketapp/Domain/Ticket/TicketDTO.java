package com.ticketing.ticketapp.Domain.Ticket;

import java.util.Date;

public record TicketDTO(
        String id,
        int row,
        int col,
        String event,
        String company,
        double price,
        Date date,
        boolean isPurchased
) {
    public static TicketDTO fromEntity(Ticket ticket) {
        return new TicketDTO(
                ticket.getId(),
                ticket.getRow(),
                ticket.getCol(),
                ticket.getEvent(),
                ticket.getCompany(),
                ticket.getPrice(),
                ticket.getDate(),
                ticket.isPurchased()
        );
    }
}
