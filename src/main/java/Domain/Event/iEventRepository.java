package Domain.Event;

import java.awt.geom.Area;
import java.util.Date;
import java.util.List;
import java.util.Map;

public interface iEventRepository {
    public Event store(String eventName, String artistName, EventType eventType, double price, Date date, String location, String company, int totalTickets, MapArea[][] mapArea   );

    public Event getEvent(String eventName, String company);
    public Event getEventById(String eventId, String company);
    public List<Event> getAllEvents();
    public Event save(Event eventToUpdate);
    public List<String> searchEvents(String query, String company, EventType type, Double minPrice, Double maxPrice, Date startDate, Date endDate, String location, Double minRating);
    public void deleteEvent(String eventId, String company);
    public void deleteAllEvents();
    public List<Event> getEventsByCompany(String company);
    public List<Event> getEventsByDateRange(Date from, Date to);
    public List<Event> getEventsByType(EventType eventType);
    public List<Event> getEventsByLocation(String location);
    public List<Event> getEventsByArtist(String artistName);
    public List<Event> getEventsByPriceRange(double minPrice, double maxPrice);
    public List<Event> getEventsByName(String name);
    public MapArea[][] getMapArea(String company, String eventName);
}
