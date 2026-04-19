package Domain.Order;

import java.time.LocalDateTime;
import java.util.Map;

public class PurchasedOrder {
    private String orderId;
    private String userId;
    private String eventId;
    private String companyId;
    private Map<String, String> ticketBarcodes; // ticketId -> barcode
    private double totalPaid;
    private LocalDateTime purchaseTime;

    public PurchasedOrder(String orderId, String userId, String eventId, String companyId, Map<String, String> ticketBarcodes, double totalPaid) {
        this.orderId = orderId;
        this.userId = userId;
        this.eventId = eventId;
        this.companyId = companyId;
        this.ticketBarcodes = ticketBarcodes;
        this.totalPaid = totalPaid;
        this.purchaseTime = LocalDateTime.now();
    }

    // getters
    public String getOrderId() { return orderId; }
    public String getUserId() { return userId; }
    public String getEventId() { return eventId; }
    public String getCompanyId() { return companyId; }
    public Map<String, String> getTicketBarcodes() { return ticketBarcodes; }
    public double getTotalPaid() { return totalPaid; }
    public LocalDateTime getPurchaseTime() { return purchaseTime; }
}