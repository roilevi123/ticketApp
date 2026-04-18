package Domain.Event;

import java.time.LocalDateTime;

public class Event {
    private String eventId;
    private String companyId; 
    private String queueId; 
    private String name;
    private LocalDateTime date;
    private double price;
    private int totalTickets;
    private int availableTickets;
    private int version; 

    public Event(String eventId, String companyId, String queueId, String name, LocalDateTime date, double price, int totalTickets) {
        this.eventId = eventId;
        this.companyId = companyId;
        this.queueId = queueId;
        this.name = name;
        this.date = date;
        this.price = price;
        this.totalTickets = totalTickets;
        this.availableTickets = totalTickets;
        this.version = 0;
    }

    public boolean reserveTickets(int amount) {
        if (amount <= 0 || availableTickets < amount) {
            return false;
        }
        availableTickets -= amount;
        return true;
    }

    // getters
    public String getEventId() { return eventId; }
    public String getCompanyId() { return companyId; }
    public String getQueueId() { return queueId; }
    public String getName() { return name; }
    public LocalDateTime getDate() { return date; }
    public double getPrice() { return price; }
    public int getTotalTickets() { return totalTickets; }
    public int getAvailableTickets() { return availableTickets; }
    public int getVersion() { return version; }

}
