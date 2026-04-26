package Appliction;

import Domain.Domains.EventDomain;
import Domain.Event.Event;
import Domain.Event.EventType;
import Domain.Event.MapArea;
import Domain.Ticket.Ticket;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class EventService {
    private EventDomain domain;
    public EventService(EventDomain domain) {
        this.domain = domain;
    }

    public Response<String> createEvent(String token, String eventName, String artistName, EventType eventType, double price, Date date, String location, String company, MapArea[][] map) {
        return domain.createEvent(token,eventName,artistName,eventType,price,date,location,company,map);
    }

    public Response<Void> deleteEvent(String eventId, String companyName, String token) {
        return domain.deleteEvent(eventId,companyName,token);
    }

    public String UpdateEvent(String token, String eventName, String artistName, EventType eventType, double price, Date date, String location, String company, MapArea[][] map, double rating) {
        return domain.UpdateEvent(token, eventName, artistName, eventType, price, date, location, company, map, rating);
    }

    public Response<Void> updateEventDate(String eventId, String companyName, Date newDate, String token) {
        return domain.updateEventDate(eventId,companyName,newDate,token);
    }

    public Response<Void> updateEventName(String eventId, String companyName, String newName, String token) {
        return domain.updateEventName(eventId,companyName,newName,token);
    }

    public Response<Void> updateEventLocation(String eventId, String companyName, String newLocation, String token) {
        return domain.updateEventLocation(eventId,companyName,newLocation,token);
    }

    public Response<String> getEventInfo(String eventId, String companyName) {
        return domain.getEventInfo(eventId,companyName);
    }

    public Response<String> getAllEvents() {
        return domain.getAllEvents();
    }

    public Response<String> searchEventsByDate(String date) {
        return domain.searchEventsByDate(date);
    }

    public Response<String> searchEventsByCategory(EventType eventType) {
        return domain.searchEventsByCategory(eventType);
    }


    public Response<String> searchEventsByName(String name) {
        return domain.searchEventsByName(name);
    }

    public Response<String> searchEventsByLocation(String location) {
        return domain.searchEventsByLocation(location);
    }

    public Response<Integer> getAvailableTickets(String eventId, String companyName) {
        return domain.getAvailableTickets(eventId,companyName);
    }

    public String getCompanyInfo(String company) {
        return domain.getCompanyInfo(company);
    }
    public List<String> getCompanyEvents(String company) {
        return domain.getCompanyEvents(company);
    }
    public MapArea[][] getMapArea(String company,String eventName) {
        return domain.getMapArea(company,eventName);
    }
    public List<String> searchEvents(String query, String company, EventType type,
                                     Double minPrice, Double maxPrice,
                                     Date startDate, Date endDate,
                                     String location, Double minRating) {
        return domain.searchEvents(query, company, type, minPrice, maxPrice, startDate, endDate, location, minRating);
    }
}
