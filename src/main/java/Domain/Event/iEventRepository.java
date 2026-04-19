package Domain.Event;

import java.util.Date;
import java.util.List;

public interface iEventRepository {
    public Event store(String eventName, String artistName, EventType eventType, double price, Date date, String location, String company, int totalTickets);
    public Event getEvent(String eventName, String company);
    public Event getEventById(String eventId, String company);
    public List<Event> getAllEvents();
    public Event save(Event eventToUpdate);
    public List<String> searchEvents(String query, String company, EventType type, Double minPrice, Double maxPrice, Date startDate, Date endDate, String location, Double minRating);
    public void deleteEvent(String eventId, String company);
    public void deleteAllEvents();
    public List<Event> getEventsByCompany(String company);
    public List<Event> getEventsByDateRange(Date from, Date to, String company);
    public List<Event> getEventsByType(EventType eventType, String company);
    public List<Event> getEventsByLocation(String location, String company);
    public List<Event> getEventsByArtist(String artistName, String company);
    public List<Event> getEventsByPriceRange(double minPrice, double maxPrice, String company);

}
