package Infastructure;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import Domain.Event.*;

public class EventRepositoryImpl implements iEventRepository {
    private Map<String, Event> events=new HashMap<>();
    private AtomicInteger idCounter = new AtomicInteger(1);
    
    @Override
    public Event store(String eventName, String artistName, EventType eventType, double price, Date date, String location, String company, int totalTickets) {
        String key = eventName + company;
        if (events.containsKey(key)) {
            throw new RuntimeException("Event already exists: " + eventName + " for company: " + company);
        }
        Event newEvent = new Event(String.valueOf(idCounter.getAndIncrement()), company, null, eventName, location, artistName, date, price, totalTickets, eventType);
        events.put(key, newEvent);
        return newEvent;
    }

    @Override
    public Event save(Event eventToUpdate) {
        String key = eventToUpdate.getName() + eventToUpdate.getCompany();
            Event currentInDb = events.get(key);
            if (currentInDb == null) {
                throw new RuntimeException("Event not found for update: " + eventToUpdate.getName());
            }
            Event updatedEvent = new Event(eventToUpdate);
            updatedEvent.setVersion(eventToUpdate.getVersion() + 1);
            boolean success = events.replace(key, currentInDb, updatedEvent);
            if (!success) {
                throw new RuntimeException("Optimistic Lock Failure: Event '" + eventToUpdate.getName() +
                        "' was updated by another thread/user.");
            }
        return updatedEvent;

    }

    @Override
    public Event getEvent(String eventName, String company) {
        return events.get(eventName + company);
    }

    @Override
    public List<Event> getAllEvents() {
        return new ArrayList<>(events.values());
    }

    @Override
    public List<Event> getEventsByCompany(String company) {
        List<Event> companyEvents = new ArrayList<>();
        for (Event event : events.values()) {
            if (event.getCompany().equals(company)) {
                companyEvents.add(event);
            }
        }
        return companyEvents;
    }

    @Override
    public List<Event> getEventsByDateRange(Date from, Date to, String company) {
        List<Event> dateRangeEvents = new ArrayList<>();
        for (Event event : events.values()) {
            if (event.getCompany().equals(company) && !event.getDate().before(from) && !event.getDate().after(to)) {
                dateRangeEvents.add(event);
            }
        }
        return dateRangeEvents;
    }
    
    @Override
    public List<Event> getEventsByType(EventType eventType, String company) {
        List<Event> typeEvents = new ArrayList<>();
        for (Event event : events.values()) {
            if (event.getCompany().equals(company) && event.getType() == eventType) {
                typeEvents.add(event);
            }
        }
        return typeEvents;
    }
    
    @Override
    public List<Event> getEventsByLocation(String location, String company) {
        List<Event> locationEvents = new ArrayList<>();
        for (Event event : events.values()) {
            if (event.getCompany().equals(company) && event.getLocation().equals(location)) {
                locationEvents.add(event);
            }
        }
        return locationEvents;
    }
    
    @Override
    public List<Event> getEventsByArtist(String artistName, String company) {
        List<Event> artistEvents = new ArrayList<>();
        for (Event event : events.values()) {
            if (event.getCompany().equals(company) && event.getArtistName().equals(artistName)) {
                artistEvents.add(event);
            }
        }
        return artistEvents;
    }
    
    @Override
    public List<Event> getEventsByPriceRange(double minPrice, double maxPrice, String company) {
        List<Event> priceRangeEvents = new ArrayList<>();
        for (Event event : events.values()) {
            if (event.getCompany().equals(company) && event.getPrice() >= minPrice && event.getPrice() <= maxPrice) {
                priceRangeEvents.add(event);
            }
        }
        return priceRangeEvents;
    }

    @Override
    public List<String> searchEvents(String query, String company, EventType type, Double minPrice, Double maxPrice, Date startDate, Date endDate, String location, Double minRating) {
        return events.values().stream()
                .filter(e -> company == null || e.getCompany().equalsIgnoreCase(company))
                .filter(e -> query == null || query.isEmpty() ||
                        e.getName().contains(query) ||
                        e.getArtistName().contains(query))
                .filter(e -> type == null || e.getType() == type)
                .filter(e -> (minPrice == null || e.getPrice() >= minPrice) &&
                        (maxPrice == null || e.getPrice() <= maxPrice))
                .filter(e -> (startDate == null || !e.getDate().before(startDate)) &&
                        (endDate == null || !e.getDate().after(endDate)))
                .filter(e -> location == null || e.getLocation().equalsIgnoreCase(location))
                .filter(e -> {
                    if (company != null) return true;
                    return minRating == null || e.getRating() >= minRating;
                })
                .map(Event::toString)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteAllEvents() {
        events.clear();
        idCounter.set(1);
    }

}
