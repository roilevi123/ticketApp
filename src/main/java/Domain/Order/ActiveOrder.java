package Domain.Order;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ActiveOrder {
    private String orderId;
    private String userId;
    private String eventId;
    private List<String> ticketIds;
    private LocalDateTime expirationTime;

    public ActiveOrder(String orderId, String userId, String eventId, int expirationMinutes) {
        this.orderId = orderId;
        this.userId = userId;
        this.eventId = eventId;
        this.ticketIds = new ArrayList<>();
        this.expirationTime = LocalDateTime.now().plusMinutes(expirationMinutes); 
    }

    public void addTicket(String ticketId) {
        this.ticketIds.add(ticketId);
    }
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expirationTime);
    }

    // getters
    public String getOrderId() { return orderId; }
    public String getUserId() { return userId; }
    public String getEventId() { return eventId; }
    public List<String> getTicketIds() { return ticketIds; }
    public LocalDateTime getExpirationTime() { return expirationTime; }
}