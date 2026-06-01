package com.ticketing.ticketapp.Domain.Order;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "active_orders")
public class ActiveOrder {
    @Id
    @Column(name = "order_id")
    private String orderId;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "user_id", referencedColumnName = "user_id")
    private OrderUserRef user;

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "event_id", referencedColumnName = "event_id")
    private OrderEventRef event;

    @Column(name = "company_id")
    private String companyId;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "active_order_tickets",
            joinColumns = @JoinColumn(name = "order_id"),
            inverseJoinColumns = @JoinColumn(name = "ticket_id", referencedColumnName = "ticket_id")
    )
    private List<OrderTicketRef> tickets = new ArrayList<>();

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "expiration_time")
    private Date expirationTime;

    @Version
    private int version;

    protected ActiveOrder() {
    }

    public ActiveOrder(String company, String event, List<String> ticketsId, String buyerID, String orderId, Date expiryDate) {
        this.companyId = company;
        this.event = event != null ? new OrderEventRef(event) : null;
        this.tickets = ticketsId == null ? new ArrayList<>() :
                ticketsId.stream().map(OrderTicketRef::new).collect(Collectors.toCollection(ArrayList::new));
        this.user = buyerID != null ? new OrderUserRef(buyerID) : null;
        this.version = 1;
        this.orderId = orderId;
        this.expirationTime = expiryDate;
    }
    public ActiveOrder(ActiveOrder order) {
        this.companyId = order.getCompanyId();
        this.event = order.getEventId() != null ? new OrderEventRef(order.getEventId()) : null;
        this.tickets = order.getTicketIds() == null ? new ArrayList<>() :
                order.getTicketIds().stream().map(OrderTicketRef::new).collect(Collectors.toCollection(ArrayList::new));
        this.user = order.getUserId() != null ? new OrderUserRef(order.getUserId()) : null;
        this.version = order.getVersion();
        this.orderId = order.getOrderId();
        this.expirationTime = order.getExpirationTime();
    }

//    public boolean isExpired() {
//        return LocalDateTime.now().isAfter(expirationTime);
//    }

    // getters
    public String getOrderId() { return orderId; }
    public String getUserId() { return user != null ? user.getUserId() : null; }
    public String getEventId() { return event != null ? event.getEventId() : null; }
    public List<String> getTicketIds() {
        return tickets.stream().map(OrderTicketRef::getTicketId).toList();
    }
    public Date getExpirationTime() { return expirationTime; }
    public int getVersion() { return version; }
    public void SetVersion(int version) { this.version = version; }
    public String getCompanyId() { return companyId; }
}
