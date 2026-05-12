package com.ticketing.ticketapp.Domain.PurchasedOrderAggregate;

import com.ticketing.ticketapp.Domain.Order.ActiveOrder;

import java.util.List;

public class PurchaseOrder {
    private String company;
    private String event;
    private List<String> ticketsId;
    private String buyerID;
    private String orderId;
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
    @Override
    public String toString() {
        return "PurchaseOrder{" +
                "company='" + (company != null ? company : "") + '\'' +
                ", event='" + (event != null ? event : "") + '\'' +
                ", buyerID='" + (buyerID != null ? buyerID : "Unknown") + '\'' +
                '}';
    }

}
