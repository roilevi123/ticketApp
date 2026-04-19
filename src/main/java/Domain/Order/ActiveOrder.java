package Domain.Order;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ActiveOrder {
    private String orderId;
    private String userId;
    private String eventId;
    private String companyId;
    private List<String> ticketIds;
    private Date expirationTime;
    private int version;
    public ActiveOrder(String company, String event, List<String> ticketsId, String buyer, String orderId, Date expiryDate) {
        this.companyId = company;
        this.eventId = event;
        this.ticketIds = ticketsId;
        this.userId = buyer;
        this.version = 1;
        this.orderId = orderId;
        this.expirationTime = expiryDate;
    }
    public ActiveOrder(ActiveOrder order) {
        this.companyId = order.getCompanyId();
        this.eventId = order.getEventId();
        this.ticketIds = order.getTicketIds();
        this.userId = order.getUserId();
        this.version = order.getVersion();
        this.orderId = order.getOrderId();
        this.expirationTime = order.getExpirationTime();
    }

    public void addTicket(String ticketId) {
        this.ticketIds.add(ticketId);
    }
//    public boolean isExpired() {
//        return LocalDateTime.now().isAfter(expirationTime);
//    }

    // getters
    public String getOrderId() { return orderId; }
    public String getUserId() { return userId; }
    public String getEventId() { return eventId; }
    public List<String> getTicketIds() { return ticketIds; }
    public Date getExpirationTime() { return expirationTime; }
    public int getVersion() { return version; }
    public void SetVersion(int version) { this.version = version; }
    public String getCompanyId() { return companyId; }
}