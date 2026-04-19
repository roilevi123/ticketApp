package Domain.PurchasedOrderAggregate;

import Domain.Order.ActiveOrder;

import java.util.List;

public class PurchaseOrder {
    private String company;
    private String event;
    private List<String> ticketsId;
    private String buyer;
    private String orderId;
    public PurchaseOrder(String company, String event, List<String> ticketsId, String buyer, String orderId) {
        this.company = company;
        this.event = event;
        this.ticketsId = ticketsId;
        this.buyer = buyer;
        this.orderId = orderId;

    }
    public PurchaseOrder(ActiveOrder order) {
        this.company = order.getOrderId();
        this.event = order.getEventId();
        this.ticketsId = order.getTicketIds();
        this.buyer = order.getUserId();
        this.orderId = order.getOrderId();
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
    public String getBuyer() {
        return buyer;
    }
    public String getOrderId() {
        return orderId;
    }
    @Override
    public String toString() {
        return "PurchaseOrder{" +
                "company='" + (company != null ? company : "") + '\'' +
                ", event='" + (event != null ? event : "") + '\'' +
                ", buyer='" + (buyer != null ? buyer : "Unknown") + '\'' +
                '}';
    }

}
