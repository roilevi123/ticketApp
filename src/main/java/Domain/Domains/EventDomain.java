package Domain.Domains;

import Appliction.Response;
import Domain.Company.iCompanyRepository;
import Domain.Event.*;
import Domain.Ticket.Ticket;
import Domain.OwnerManagerTree.*;
import Domain.QueueAggregates.iQueueRepository;
import Domain.Ticket.iTicketRepository;
import Infastructure.TokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.*;


public class EventDomain {

    private String lastCreatorToken = "";
    private boolean isDeleted = false;
    private boolean hasEvents = false;
    private String lastEventName = "";

    private iCompanyRepository companyRepository;
    private iEventRepository eventRepository;
    private TokenService tokenService;
    private iTreeOfRoleRepository treeOfRoleRepository;
    private iTicketRepository ticketRepository;
    private iQueueRepository iQueueRepository;
    private static final Logger logger = LoggerFactory.getLogger(EventDomain.class);


    public EventDomain(iCompanyRepository companyRepository, iEventRepository eventRepository, TokenService tokenService, iTreeOfRoleRepository treeOfRoleRepository, iTicketRepository ticketRepository, iQueueRepository iQueueRepository) {
        this.companyRepository = companyRepository;
        this.eventRepository = eventRepository;
        this.tokenService = tokenService;
        this.treeOfRoleRepository = treeOfRoleRepository;
        this.ticketRepository = ticketRepository;
        this.iQueueRepository = iQueueRepository;
    }

    public boolean isAuthorized(String company,String username) {
        boolean o=treeOfRoleRepository.exitsOwner(username,company);
        boolean m=treeOfRoleRepository.ManagerPermitedToCreateUpdateDelete(username,company);
        return m || (o);
    }

    public Response<String> createEvent(String token, String eventName, String artistName, EventType eventType, double price, Date date, String location, String company, MapArea[][] map) {
        String username = tokenService.extractUsername(token);
        if (!isAuthorized(company,username)) {
            logger.info("Unauthorized attempt to create event '{}' for company '{}'", eventName, company);
            return new Response<>(false, "Unauthorized", null);
        }
        if(!tokenService.validateToken(token)) {
            throw new RuntimeException("Invalid token");
        }

        try {
            Event event = eventRepository.store(eventName, artistName, eventType, price, date, location, company,map);
            ticketRepository.makeMapToTicket(event.getCompany(), event.getName(), map, event.getDate(), event.getPrice());
            iQueueRepository.initQueue(eventName+company);
            logger.info("Event '{}' created successfully for company '{}'", eventName, company);
            return new Response<>(true, "Event created successfully", event.getId());
        } catch (Exception e) {
            logger.error("Failed to create event '{}' for company '{}': {}", eventName, company, e.getMessage());
            return new Response<>(false, "Failed to create event: " + e.getMessage(), null);
        }
    }

    public Response<Void> deleteEvent(String eventId, String companyName, String token) {
        String username = tokenService.extractUsername(token);
        if (!isAuthorized(companyName,username)) {
            logger.info("Unauthorized attempt to delete event '{}' for company '{}'", eventId, companyName);
            return new Response<>(false, "Unauthorized", null);
        }
        if(!tokenService.validateToken(token)) {
            throw new RuntimeException("Invalid token");
        }
        try {
            Event event = eventRepository.getEventById(eventId, companyName);
            if (event == null) {
                logger.info("Attempt to delete non-existent event '{}' for company '{}'", eventId, companyName);
                return new Response<>(false, "Event not found", null);
            }
            eventRepository.deleteEvent(eventId, companyName);
            logger.info("Event '{}' deleted successfully for company '{}'", eventId, companyName);
            return new Response<>(true, "Event deleted successfully", null);
        } catch (Exception e) {
            logger.error("Failed to delete event '{}' for company '{}': {}", eventId, companyName, e.getMessage());
            return new Response<>(false, "Failed to delete event: " + e.getMessage(), null);
        }
    }

    public String UpdateEvent(String token, String eventName, String artistName, EventType eventType, double price, Date date, String location, String company, MapArea[][] map, double rating) {
        try {
            logger.info("trying update  event: " + eventName);
            String username = tokenService.extractUsername(token);
            if (!tokenService.validateToken(token)) {
                throw new RuntimeException("Invalid token");
            }
            if (!isAuthorized(company, username)) {
                throw new RuntimeException("Unauthorized: User is not an owner or authorized manager");
            }
            Event event = eventRepository.getEvent(eventName, company);
            if (event == null) {
                throw new RuntimeException("Event not found: " + eventName);
            }
            event.setName(eventName);
            event.setArtistName(artistName);
            event.setType(eventType);
            event.setPrice(price);
            event.setDate(date);
            event.setLocation(location);
            eventRepository.save(event);
            event.setMap(map);
            event.setRating(rating);
            eventRepository.save(event);
            logger.info("Successfully update  event: " + eventName);
            return "success";
        } catch (Exception e) {
            logger.error(e.getMessage());
            return "failed";
        }
    }

    public Response<Void> updateEventDate(String eventId, String companyName, Date newDate, String token) {
        String username = tokenService.extractUsername(token);
        if (!isAuthorized(companyName,username)) {
            logger.info("Unauthorized attempt to delete event '{}' for company '{}'", eventId, companyName);
            return new Response<>(false, "Unauthorized", null);
        }
        if(!tokenService.validateToken(token)) {
            throw new RuntimeException("Invalid token");
        }

        try {
            Event event = eventRepository.getEventById(eventId, companyName);
            if (event == null) {
                logger.info("Attempt to update non-existent event '{}' for company '{}'", eventId, companyName);
                return new Response<>(false, "Event not found", null);
            }
            event.setDate(newDate);
            eventRepository.save(event);
            logger.info("Event '{}' date updated successfully for company '{}'", eventId, companyName);
            return new Response<>(true, "Event date updated successfully", null);
        } catch (Exception e) {
            logger.error("Failed to update event '{}' for company '{}': {}", eventId, companyName, e.getMessage());
            return new Response<>(false, "Failed to update event: " + e.getMessage(), null);
        }
        
    }

    public Response<Void> updateEventName(String eventId, String companyName, String newName, String token) {
        String username = tokenService.extractUsername(token);
        if (!isAuthorized(companyName,username)) {
            logger.info("Unauthorized attempt to delete event '{}' for company '{}'", eventId, companyName);
            return new Response<>(false, "Unauthorized", null);
        }
        if(!tokenService.validateToken(token)) {
            throw new RuntimeException("Invalid token");
        }

        try {
            Event event = eventRepository.getEventById(eventId, companyName);
            if (event == null) {
                logger.info("Attempt to update non-existent event '{}' for company '{}'", eventId, companyName);
                return new Response<>(false, "Event not found", null);
            }
            event.setName(newName);
            eventRepository.save(event);
            logger.info("Event '{}' name updated successfully for company '{}'", eventId, companyName);
            return new Response<>(true, "Event name updated successfully", null);
        } catch (Exception e) {
            logger.error("Failed to update event '{}' for company '{}': {}", eventId, companyName, e.getMessage());
            return new Response<>(false, "Failed to update event: " + e.getMessage(), null);
        }
    }

    public Response<Void> updateEventLocation(String eventId, String companyName, String newLocation, String token) {
        String username = tokenService.extractUsername(token);
        if (!isAuthorized(companyName,username)) {
            logger.info("Unauthorized attempt to delete event '{}' for company '{}'", eventId, companyName);
            return new Response<>(false, "Unauthorized", null);
        }
        if(!tokenService.validateToken(token)) {
            throw new RuntimeException("Invalid token");
        }

        try {
            Event event = eventRepository.getEventById(eventId, companyName);
            if (event == null) {
                logger.info("Attempt to update non-existent event '{}' for company '{}'", eventId, companyName);
                return new Response<>(false, "Event not found", null);
            }
            event.setLocation(newLocation);
            eventRepository.save(event);
            logger.info("Event '{}' location updated successfully for company '{}'", eventId, companyName);
            return new Response<>(true, "Event location updated successfully", null);
        } catch (Exception e) {
            logger.error("Failed to update event '{}' for company '{}': {}", eventId, companyName, e.getMessage());
            return new Response<>(false, "Failed to update event: " + e.getMessage(), null);
        }
    }

    public Response<String> getEventInfo(String eventId, String companyName) {
        Event event = eventRepository.getEventById(eventId, companyName);
        if (event == null) {
            return new Response<>(false, "Event not found", null);
        }
        String info = String.format("Event '%s' by %s at %s on %s. Price: %.2f. Location: %s.",
                event.getName(), event.getArtistName(), event.getType(), event.getDate(), event.getPrice(), event.getLocation());
        return new Response<>(true, "Success", info);
    }

    public Response<String> getAllEvents() {
        List<Event> events = eventRepository.getAllEvents();
        if (events.isEmpty()) {
            return new Response<>(false, "No events found", null);
        }
        String allEvents = "";
        for (Event event : events) {
            allEvents += event.toString() + "\n";
        }
        return new Response<>(true, "Success", allEvents);
    }

    public Response<String> searchEventsByDate(String date) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd"); 
        try {
            Date myDate = formatter.parse(date);
            List<Event> events = eventRepository.getEventsByDateRange(myDate, myDate);
            if (events.isEmpty()) {
                return new Response<>(false, "No events found on this date", null);
            }
            String allEvents = "";
            for (Event event : events) {
                allEvents += event.toString() + "\n";
            }
            return new Response<>(true, "Success", allEvents);
        } catch (java.text.ParseException e) {
            logger.error("Failed to parse date '{}': {}", date, e.getMessage());
            return new Response<>(false, "Invalid date format", null);
        }
    }

    public Response<String> searchEventsByCategory(EventType eventType) {
        List<Event> events = eventRepository.getEventsByType(eventType);
        if (events.isEmpty()) {
            return new Response<>(false, "No events found in this category", null);
        }
        String allEvents = "";
        for (Event event : events) {
            allEvents += event.toString() + "\n";
        }
        return new Response<>(true, "Success", allEvents);
    }
        

    public Response<String> searchEventsByName(String name) {
        List<Event> events = eventRepository.getEventsByName(name);
        if (events.isEmpty()) {
            return new Response<>(false, "No events found with this name", null);
        }
        String allEvents = "";
        for (Event event : events) {
            allEvents += event.toString() + "\n";
        }
        return new Response<>(true, "Success", allEvents);
    }

    public Response<String> searchEventsByLocation(String location) {
        List<Event> events = eventRepository.getEventsByLocation(location);
        if (events.isEmpty()) {
            return new Response<>(false, "No events found in this location", null);
        }
        String allEvents = "";
        for (Event event : events) {
            allEvents += event.toString() + "\n";
        }
        return new Response<>(true, "Success", allEvents);
    }

    public Response<Integer> getAvailableTickets(String eventId, String companyName) {
        Event event = eventRepository.getEventById(eventId, companyName);
        if (event == null) {
            return new Response<>(false, "Event not found", null);
        }
        List<Ticket> tickets = ticketRepository.getAllTicketsByEventAndCompany(event.getCompany(), event.getName());
        int availableTickets = tickets.size(); 
        return new Response<>(true, "Success", availableTickets);
    }

    public String getCompanyInfo(String company) {
        try {
            logger.info("trying Getting company info: " + company);
            boolean c=companyRepository.isCompanyActive(company);
            if(!c){
                throw new RuntimeException("the company is not active");
            }
            logger.info("Successfully Getting company info: " + company);
            return companyRepository.getCompanyDescription(company);
        }catch (Exception e) {
            logger.error(e.getMessage());
            return null;
        }
    }
    public List<String> getCompanyEvents(String company) {
        try {
            logger.info("trying Getting company events: " + company);
            boolean c=companyRepository.isCompanyActive(company);
            if(!c){
                throw new RuntimeException("the company is not active");
            }
            List<Event> events = eventRepository.getEventsByCompany(company);
            List<String> eventsString = new ArrayList<>();
            for (Event event : events) {
                eventsString.add(event.toString());

            }
            logger.info("Successfully Getting company events: " + company);
            return eventsString;
        }
        catch (Exception e) {
            logger.error(e.getMessage());
            return null;
        }
    }
    public MapArea[][] getMapArea(String company,String eventName) {
        try {
            logger.info("trying Getting map area: " + eventName);
            MapArea[][] map=eventRepository.getMapArea(company,eventName);
            logger.info("Successfully Getting map area: " + eventName);
            return map;
        }catch (Exception e) {
            logger.error(e.getMessage());
            return null;
        }
    }
    public List<String> searchEvents(String query, String company, EventType type,
                                     Double minPrice, Double maxPrice,
                                     Date startDate, Date endDate,
                                     String location, Double minRating) {
        try {
            logger.info("Initiating search with parameters - Query: {}, Company: {}", query, company);

            List<String> results = eventRepository.searchEvents(
                    query, company, type, minPrice, maxPrice, startDate, endDate, location, minRating
            );

            logger.info("Search completed successfully. Found {} events", results.size());
            return results;

        } catch (Exception e) {
            logger.error("Error occurred during event search: {}", e.getMessage());
            return null;
        }
    }
}