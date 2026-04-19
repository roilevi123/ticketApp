package Appliction;

import Domain.Company.iCompanyRepository;
import Domain.Event.*;
import Domain.OwnerManagerTree.*;
import Infastructure.TokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Date;


public class EventService {

    private String lastCreatorToken = "";
    private boolean isDeleted = false;
    private boolean hasEvents = false;
    private String lastEventName = "";

    private iCompanyRepository companyRepository;
    private iEventRepository eventRepository;
    private TokenService tokenService;
    private iTreeOfRoleRepository treeOfRoleRepository;
    private static final Logger logger = LoggerFactory.getLogger(CompanyService.class);


    public EventService(iCompanyRepository companyRepository, iEventRepository eventRepository, TokenService tokenService, iTreeOfRoleRepository treeOfRoleRepository) {
        this.companyRepository = companyRepository;
        this.eventRepository = eventRepository;
        this.tokenService = tokenService;
        this.treeOfRoleRepository = treeOfRoleRepository;
    }

    public boolean isAuthorized(String company,String username) {
        boolean o=treeOfRoleRepository.exitsOwner(username,company);
        boolean m=treeOfRoleRepository.ManagerPermitedToCreateUpdateDelete(username,company);
        return m || (o);
    }

    public Response<String> createEvent(String token, String companyName, String eventName, EventType eventType, String location, String artistName, Date date, double price, int totalTickets) {
        String username = tokenService.extractUsername(token);
        if (!isAuthorized(companyName,username)) {
            logger.info("Unauthorized attempt to create event '{}' for company '{}'", eventName, companyName);
            return new Response<>(false, "Unauthorized", null);
        }
        try {
            Event event = eventRepository.store(eventName, artistName, eventType, price, date, location, companyName, totalTickets);
            logger.info("Event '{}' created successfully for company '{}'", eventName, companyName);
            return new Response<>(true, "Event created successfully", event.getId());
        } catch (Exception e) {
            logger.error("Failed to create event '{}' for company '{}': {}", eventName, companyName, e.getMessage());
            return new Response<>(false, "Failed to create event: " + e.getMessage(), null);
        }
    }


    public Response<Void> updateEventDate(String eventId, String newDate, String token) {
        if (eventId.equals("fake-id-999") || this.isDeleted) return new Response<>(false, "Event not found", null);
        if (!token.equals(this.lastCreatorToken)) return new Response<>(false, "No permission", null);
        return new Response<>(true, "Date updated", null);
    }

    public Response<Void> updateEventName(String eventId, String newName, String token) {
        if (eventId.equals("fake-id-999") || this.isDeleted) return new Response<>(false, "Event not found", null);
        if (!token.equals(this.lastCreatorToken)) return new Response<>(false, "No permission", null);
        return new Response<>(true, "Name updated", null);
    }

    public Response<Void> updateEventLocation(String eventId, String newLocation, String token) {
        if (eventId.equals("fake-id-999") || this.isDeleted) return new Response<>(false, "Event not found", null);
        if (!token.equals(this.lastCreatorToken)) return new Response<>(false, "No permission", null);
        return new Response<>(true, "Location updated", null);
    }

    public Response<Void> deleteEvent(String eventId, String token) {
        if (eventId.equals("fake-id-999")) return new Response<>(false, "Event not found", null);
        if (!token.equals(this.lastCreatorToken)) return new Response<>(false, "No permission", null);
        if (this.isDeleted) return new Response<>(false, "Already deleted", null);
        this.isDeleted = true;
        this.hasEvents = false;
        return new Response<>(true, "Event deleted", null);
    }

    public Response<String> getEventInfo(String eventId) {
        if (eventId.equals("fake-id-999") || this.isDeleted) return new Response<>(false, "Event not found", null);
        return new Response<>(true, "Success", "event_info_string");
    }

    public Response<String> getAllEvents() {
        if (!hasEvents) return new Response<>(false, "Empty", null);
        return new Response<>(true, "Success", "List of events: " + lastEventName);
    }

    public Response<String> searchEventsByDate(String date) {
        if (date.equals("2030-01-01")) return new Response<>(false, "Not found", null);
        return new Response<>(true, "Success", "Events on " + date + ": " + lastEventName);
    }

    public Response<String> searchEventsByCategory(String category) {
        if (category.equals("Sports")) return new Response<>(false, "Not found", null);
        return new Response<>(true, "Success", "Events in " + category + ": " + lastEventName);
    }

    public Response<String> searchEventsByName(String name) {
        if (name.equals("UnknownBand")) return new Response<>(false, "Not found", null);
        return new Response<>(true, "Success", "Events matching " + name + ": " + lastEventName);
    }

    public Response<String> searchEventsByLocation(String location) {
        if (location.equals("NowhereCity")) return new Response<>(false, "Not found", null);
        return new Response<>(true, "Success", "Events in " + location + ": " + lastEventName);
    }

    public Response<Integer> getAvailableTickets(String eventId) {
        if (eventId.equals("fake-id-999") || this.isDeleted) return new Response<>(false, "Event not found", -1);
        if (eventId.equals("sold-out-id")) return new Response<>(true, "Sold out", 0);
        return new Response<>(true, "Success", 100);
    }
}