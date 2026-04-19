package Domain.Event;

import java.util.Arrays;
import java.util.Date;

public class Event {
    private String eventId;
    private String companyName; 
    private String queueId; 
    private String name;
    private String description;
    private double rating;
    private EventType type;
    private String location;
    private String artistName;
    private Date date;
    private double price;
    private int totalTickets;
    private int availableTickets;
    private int version; 
    private MapArea[][] map;

    public Event(String eventId, String companyName, String queueId, String name, String location, String artistName, Date date, double price, int totalTickets, EventType type, MapArea[][] mapArea) {
        this.eventId = eventId;
        this.companyName = companyName;
        this.queueId = queueId;
        this.name = name;
        this.location = location;
        this.artistName = artistName;
        this.date = date;
        this.price = price;
        this.totalTickets = totalTickets;
        this.availableTickets = totalTickets;
        this.version = 0;
        this.type = type;
        this.map = mapArea;
    }

    public Event(Event event) {
        this.eventId = event.getId();
        this.companyName = event.getCompany();
        this.queueId = event.getQueueId();
        this.name = event.getName();
        this.date = event.getDate();
        this.price = event.getPrice();
        this.totalTickets = event.getTotalTickets();
        this.availableTickets = event.getAvailableTickets();
        this.version = event.getVersion();
        this.description = event.getDescription();
        this.type = event.getType();
        this.location = event.getLocation();
        this.artistName = event.getArtistName();
        this.map = event.getMap();
        this.rating = event.getRating();
    }


    // getters
    public String getId() { return eventId; }
    public String getCompany() { return companyName; }
    public String getQueueId() { return queueId; }
    public String getName() { return name; }
    public Date getDate() { return date; }
    public double getPrice() { return price; }
    public MapArea[][] getMap() { return map; }
    public int getTotalTickets() { return totalTickets; }
    public int getAvailableTickets() { return availableTickets; }
    public int getVersion() { return version; }
    public String getDescription() { return description; }
    public EventType getType() { return type; }
    public String getLocation() { return location; }
    public String getArtistName() { return artistName; }
    public double getRating() { return rating; }

    // setters
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setType(EventType type) { this.type = type; }
    public void setLocation(String location) { this.location = location; }
    public void setArtistName(String artistName) { this.artistName = artistName; }
    public void setMap(MapArea[][] map) { this.map = map; }
    public void setPrice(double price) { this.price = price; }
    public void setDate(Date date) { this.date = date; }
    public void setAvailableTickets(int availableTickets) { this.availableTickets = availableTickets; }
    public void setVersion(int version) { this.version = version; }
    public void setRating(double rating) { this.rating = rating; }
    public void setCompany(String companyName) { this.companyName = companyName; }
    

    @Override
    public String toString() {
        return "Event{" +
                "eventName='" + name + '\'' +
                ", artistName='" + artistName + '\'' +
                ", eventType=" + type +
                ", price=" + price +
                ", date=" + date +
                ", location='" + location + '\'' +
                ", rating=" + rating +
                ", company='" + companyName + '\'' +
                ", version=" + version +
                ", map=" + Arrays.deepToString(map) +
                '}';
    }
}

