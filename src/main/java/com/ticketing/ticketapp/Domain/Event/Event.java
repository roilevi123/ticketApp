package com.ticketing.ticketapp.Domain.Event;

import jakarta.persistence.*;
import java.util.Arrays;
import java.util.Date;

@Entity
@Table(name = "events")
public class Event {

    @Id
    @Column(name = "event_id")
    private String eventId;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(name = "queue_id")
    private String queueId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "rating")
    private double rating;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private EventType type;

    @Column(name = "location")
    private String location;

    @Column(name = "artist_name")
    private String artistName;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "date")
    private Date date;

    @Column(name = "price")
    private double price;

    @Column(name = "total_tickets")
    private int totalTickets;

    @Column(name = "available_tickets")
    private int availableTickets;

    @Version
    @Column(name = "version")
    private int version;

    // MapArea[][] is serialised to a pipe-and-comma-delimited TEXT column via MapAreaConverter
    @Convert(converter = MapAreaConverter.class)
    @Column(name = "map_data", columnDefinition = "TEXT")
    private MapArea[][] map;

    @Column(name = "high_demand")
    private boolean highDemand;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "lottery_end_date")
    private Date lotteryEndDate;

    @Column(name = "lottery_max_winners")
    private int lotteryMaxWinners;

    protected Event() {}

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
        this.highDemand = false;
        this.lotteryEndDate = null;
        this.lotteryMaxWinners = 0;
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
        this.highDemand = event.isHighDemand();
        this.lotteryEndDate = event.getLotteryEndDate();
        this.lotteryMaxWinners = event.getLotteryMaxWinners();
    }

    // getters
    public String getId()                   { return eventId; }
    public String getCompany()              { return companyName; }
    public String getQueueId()              { return queueId; }
    public String getName()                 { return name; }
    public Date getDate()                   { return date; }
    public double getPrice()                { return price; }
    public MapArea[][] getMap()             { return map; }
    public int getTotalTickets()            { return totalTickets; }
    public int getAvailableTickets()        { return availableTickets; }
    public int getVersion()                 { return version; }
    public String getDescription()          { return description; }
    public EventType getType()              { return type; }
    public String getLocation()             { return location; }
    public String getArtistName()           { return artistName; }
    public double getRating()               { return rating; }

    // getters – lottery
    public boolean isHighDemand()           { return highDemand; }
    public Date getLotteryEndDate()         { return lotteryEndDate; }
    public int getLotteryMaxWinners()       { return lotteryMaxWinners; }

    // setters
    public void setName(String name)                        { this.name = name; }
    public void setType(EventType type)                     { this.type = type; }
    public void setLocation(String location)                { this.location = location; }
    public void setArtistName(String artistName)            { this.artistName = artistName; }
    public void setMap(MapArea[][] map)                     { this.map = map; }
    public void setPrice(double price)                      { this.price = price; }
    public void setDate(Date date)                          { this.date = date; }
    public void setVersion(int version)                     { this.version = version; }
    public void setRating(double rating)                    { this.rating = rating; }
    public void setCompany(String companyName)              { this.companyName = companyName; }
    public void setDescription(String description)          { this.description = description; }
    public void setAvailableTickets(int availableTickets)   { this.availableTickets = availableTickets; }
    public void setQueueId(String queueId)                  { this.queueId = queueId; }

    // setters – lottery
    public void setHighDemand(boolean highDemand)               { this.highDemand = highDemand; }
    public void setLotteryEndDate(Date lotteryEndDate)          { this.lotteryEndDate = lotteryEndDate; }
    public void setLotteryMaxWinners(int lotteryMaxWinners)     { this.lotteryMaxWinners = lotteryMaxWinners; }

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
