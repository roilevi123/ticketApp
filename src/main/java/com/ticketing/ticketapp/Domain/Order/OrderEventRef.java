package com.ticketing.ticketapp.Domain.Order;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "order_events")
public class OrderEventRef {
    @Id
    @Column(name = "event_id")
    private String eventId;

    protected OrderEventRef() {
    }

    public OrderEventRef(String eventId) {
        this.eventId = eventId;
    }

    public String getEventId() {
        return eventId;
    }
}
