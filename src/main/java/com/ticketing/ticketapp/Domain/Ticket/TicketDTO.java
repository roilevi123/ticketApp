package com.ticketing.ticketapp.Domain.Ticket;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Date;

public record TicketDTO(
        String id,
        int row,
        int col,
        String event,
        String company,
        double price,
        Date date,
        @JsonProperty("isPurchased") boolean isPurchased,
        @JsonProperty("isGA") boolean isGA
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
                ticket.isPurchased(),
                ticket.isGA()
        );
    }
}
