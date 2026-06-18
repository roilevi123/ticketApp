package com.ticketing.ticketapp.Infastructure.DataBaseInterface;

import com.ticketing.ticketapp.Domain.Event.Event;
import com.ticketing.ticketapp.Domain.Event.EventDomainException;
import com.ticketing.ticketapp.Domain.Event.EventType;
import com.ticketing.ticketapp.Domain.Event.MapArea;
import com.ticketing.ticketapp.Domain.Event.iEventRepository;
import com.ticketing.ticketapp.Infastructure.JpaEventRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
@Repository
@ConditionalOnProperty(name = "repository.type", havingValue = "DB")
public class EventRepositoryAdapter implements iEventRepository {

    private final JpaEventRepository jpaEventRepository;

    public EventRepositoryAdapter(JpaEventRepository jpaEventRepository) {
        this.jpaEventRepository = jpaEventRepository;
    }

    @Override
    @Transactional
    public Event store(String eventName, String artistName, EventType eventType, double price, Date date,
                       String location, String company, MapArea[][] mapArea) {
        if (jpaEventRepository.existsByNameAndCompanyName(eventName, company)) {
            throw new EventDomainException("Event already exists: " + eventName + " for company: " + company);
        }
        String eventId = UUID.randomUUID().toString();
        Event event = new Event(eventId, company, null, eventName, location, artistName, date, price, 100, eventType, mapArea);
        return jpaEventRepository.saveAndFlush(event);
    }

    @Override
    @Transactional
    public Event save(Event eventToUpdate) {
        // @Version on Event.version handles optimistic locking automatically
        return jpaEventRepository.save(eventToUpdate);
    }

    @Override
    public Event getEvent(String eventName, String company) {
        return jpaEventRepository.findByNameAndCompanyName(eventName, company).orElse(null);
    }

    @Override
    public Event getEventById(String eventId, String company) {
        return jpaEventRepository.findById(eventId)
                .filter(e -> e.getCompany().equals(company))
                .orElse(null);
    }

    @Override
    public List<Event> getEventsByCompany(String company) {
        return jpaEventRepository.findByCompanyName(company);
    }

    @Override
    public MapArea[][] getMapArea(String company, String eventName) {
        return jpaEventRepository.findByNameAndCompanyName(eventName, company)
                .map(Event::getMap)
                .orElseThrow(() -> new EventDomainException(
                        "Event not found: " + eventName + " for company: " + company));
    }

    @Override
    @Transactional
    public void deleteEvent(String eventId, String company) {
        Event event = getEventById(eventId, company);
        if (event == null) {
            throw new EventDomainException(
                    "Event not found for deletion: " + eventId + " for company: " + company);
        }
        jpaEventRepository.deleteById(eventId);
        jpaEventRepository.flush();
    }

    @Override
    @Transactional
    public void deleteAllEvents() {
        jpaEventRepository.deleteAll();
        jpaEventRepository.flush();
    }

    @Override
    @Transactional
    public void deleteCompanyEvent(String company) {
        jpaEventRepository.deleteByCompanyName(company);
        jpaEventRepository.flush();
    }

    @Override
    public List<Event> searchEvents(String query, String company, EventType type, Double minPrice, Double maxPrice,
                                    Date startDate, Date endDate, String location, Double minRating) {
        // Fetch by company at the DB level; remaining filters applied in-memory
        // to exactly replicate the original behaviour (e.g. rating filter is skipped
        // when a specific company is requested).
        List<Event> source = (company != null)
                ? jpaEventRepository.findByCompanyName(company)
                : jpaEventRepository.findAll();

        return source.stream()
                .filter(e -> query == null || query.isEmpty() ||
                        e.getName().toLowerCase().contains(query.toLowerCase()) ||
                        e.getArtistName().toLowerCase().contains(query.toLowerCase()))
                .filter(e -> type == null || e.getType() == type)
                .filter(e -> (minPrice == null || e.getPrice() >= minPrice) &&
                        (maxPrice == null || e.getPrice() <= maxPrice))
                .filter(e -> (startDate == null || !e.getDate().before(startDate)) &&
                        (endDate == null || !e.getDate().after(endDate)))
                .filter(e -> location == null || e.getLocation().equalsIgnoreCase(location))
                .filter(e -> company != null || minRating == null || e.getRating() >= minRating)
                .collect(Collectors.toList());
    }
}
