package com.ticketing.ticketapp.Domain.PurchasedOrderAggregate;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "purchased_orders")
public class PurchaseOrder {

    @Id
    @Column(name = "order_id")
    private String orderId;

    @Column(name = "company", nullable = false)
    private String company;

    @Column(name = "event", nullable = false)
    private String event;

    @Column(name = "buyer_id")
    private String buyerID;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "purchased_order_tickets", joinColumns = @JoinColumn(name = "order_id"))
    @Column(name = "ticket_id")
    private List<String> ticketsId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "purchased_order_external_tickets", joinColumns = @JoinColumn(name = "order_id"))
    @Column(name = "external_ticket_code")
    private List<String> externalTicketIds = new ArrayList<>();

    protected PurchaseOrder() {}

    public PurchaseOrder(String company, String event, List<String> ticketsId, String buyerID, String orderId) {
        this.company = company;
        this.event = event;
        this.ticketsId = ticketsId;
        this.buyerID = buyerID;
        this.orderId = orderId;
    }

    public String getCompany() {
        return company;
    }
    public String getEvent() {
        return event;
    }
    public List<String> getTicketsId() {
        return ticketsId;
    }
    public String getBuyerID() {
        return buyerID;
    }
    public String getOrderId() {
        return orderId;
    }

    public List<String> getExternalTicketIds() {
        return externalTicketIds;
    }

    public void setExternalTicketIds(List<String> externalTicketIds) {
        this.externalTicketIds = externalTicketIds != null ? externalTicketIds : new ArrayList<>();
    }

    @Override
    public String toString() {
        return "PurchaseOrder{" +
                "company='" + (company != null ? company : "") + '\'' +
                ", event='" + (event != null ? event : "") + '\'' +
                ", buyerID='" + (buyerID != null ? buyerID : "Unknown") + '\'' +
                '}';
    }

}
