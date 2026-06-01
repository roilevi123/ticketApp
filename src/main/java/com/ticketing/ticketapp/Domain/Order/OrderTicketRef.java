package com.ticketing.ticketapp.Domain.Order;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "order_tickets")
public class OrderTicketRef {
    @Id
    @Column(name = "ticket_id")
    private String ticketId;

    protected OrderTicketRef() {
    }

    public OrderTicketRef(String ticketId) {
        this.ticketId = ticketId;
    }

    public String getTicketId() {
        return ticketId;
    }
}
