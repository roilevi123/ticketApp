package com.ticketing.ticketapp.Domain.Event;

import java.util.Date;
import java.util.List;

public interface iEventRepository {

    public Event store(String eventName, String artistName, EventType eventType, double price, Date date, String location, String company, MapArea[][] mapArea   );

    public Event getEvent(String eventName, String company);
    public Event getEventById(String eventId, String company);
    public Event save(Event eventToUpdate);
    public List<Event> searchEvents(String query, String company, EventType type, Double minPrice, Double maxPrice, Date startDate, Date endDate, String location, Double minRating);
    public void deleteEvent(String eventId, String company);
    public void deleteAllEvents();
    public void deleteCompanyEvent(String company);
    public List<Event> getEventsByCompany(String company);


    public MapArea[][] getMapArea(String company, String eventName);
}
