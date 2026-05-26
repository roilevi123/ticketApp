package com.ticketing.ticketapp.Appliction;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ticketing.ticketapp.Domain.Company.iCompanyRepository;
import com.ticketing.ticketapp.Domain.Event.Event;
import com.ticketing.ticketapp.Domain.Event.EventDTO;
import com.ticketing.ticketapp.Domain.Event.EventType;
import com.ticketing.ticketapp.Domain.Event.MapArea;
import com.ticketing.ticketapp.Domain.Event.iEventRepository;
import com.ticketing.ticketapp.Domain.OwnerManagerTree.iTreeOfRoleRepository;
import com.ticketing.ticketapp.Domain.PurchasedOrderAggregate.iPurchasedOrderRepository;
import com.ticketing.ticketapp.Domain.QueueAggregates.iQueueRepository;
import com.ticketing.ticketapp.Domain.Ticket.iTicketRepository;
import com.ticketing.ticketapp.Domain.User.IUserRepository;
import com.ticketing.ticketapp.Infastructure.TokenService;
import org.springframework.stereotype.Service;

@Service
public class EventService {

    private iCompanyRepository companyRepository;
    private iEventRepository eventRepository;
    private TokenService tokenService;
    private iTreeOfRoleRepository treeOfRoleRepository;
    private iTicketRepository ticketRepository;
    private iQueueRepository iQueueRepository;
    private iPurchasedOrderRepository purchasedOrderRepository;
    private IUserRepository userRepository;
    private INotifier notifier;
    private static final Logger logger = LoggerFactory.getLogger(EventService.class);

    public EventService(iCompanyRepository companyRepository, iEventRepository eventRepository,
            TokenService tokenService, iTreeOfRoleRepository treeOfRoleRepository, iTicketRepository ticketRepository,
            iQueueRepository iQueueRepository, iPurchasedOrderRepository purchasedOrderRepository,
            IUserRepository userRepository, INotifier notifier) {
        this.companyRepository = companyRepository;
        this.eventRepository = eventRepository;
        this.tokenService = tokenService;
        this.treeOfRoleRepository = treeOfRoleRepository;
        this.ticketRepository = ticketRepository;
        this.iQueueRepository = iQueueRepository;
        this.purchasedOrderRepository = purchasedOrderRepository;
        this.userRepository = userRepository;
        this.notifier = notifier;
    }

    public boolean isAuthorized(String company, String userID) {
        boolean o = treeOfRoleRepository.exitsOwner(userID, company);
        boolean m = treeOfRoleRepository.ManagerPermitedToCreateUpdateDelete(userID, company);
        return m || o;
    }

    public Response<String> createEvent(String token, String eventName, String artistName, EventType eventType,
            double price, Date date, String location, String company, MapArea[][] map) {
        try {
            if (!tokenService.validateToken(token)) {
                throw new RuntimeException("Invalid token");
            }
            
            String userID = tokenService.extractUserId(token);
            if (userID == null || !isAuthorized(company, userID)) {
                logger.info("Unauthorized attempt to create event '{}' for company '{}' by user ID '{}'", eventName, company, userID);
                return Response.error("Unauthorized");
            }

            Event event = eventRepository.store(eventName, artistName, eventType, price, date, location, company, map);
            ticketRepository.makeMapToTicket(event.getCompany(), event.getName(), map, event.getDate(),
                    event.getPrice());
            iQueueRepository.initQueue(eventName + company);
            logger.info("Event '{}' created successfully for company '{}'", eventName, company);
            return Response.success("success");
        } catch (Exception e) {
            logger.error("Failed to create event '{}' for company '{}': {}", eventName, company, e.getMessage());
            return Response.error("Failed to create event: " + e.getMessage());
        }
    }

    public Response<String> deleteEvent(String eventId, String companyName, String token) {
        try {
            if (!tokenService.validateToken(token)) {
                throw new RuntimeException("Invalid token");
            }
            
            String userID = tokenService.extractUserId(token);
            if (userID == null || !isAuthorized(companyName, userID)) {
                logger.info("Unauthorized attempt to delete event '{}' for company '{}'", eventId, companyName);
                return Response.error("Unauthorized");
            }

            Event event = eventRepository.getEventById(eventId, companyName);
            if (event == null) {
                logger.info("Attempt to delete non-existent event '{}' for company '{}'", eventId, companyName);
                return Response.error("Event not found");
            }

            purchasedOrderRepository.getPurchasedOrdersForCompany(companyName).stream()
                    .filter(o -> o.getEvent().equals(event.getName()))
                    .forEach(o -> notifier.notifyUser(o.getBuyerID(), "Event Cancelled",
                            "The event '" + event.getName() + "' by " + companyName
                                    + " has been cancelled. Your tickets are no longer valid."));

            eventRepository.deleteEvent(eventId, companyName);
            logger.info("Event '{}' deleted successfully for company '{}'", eventId, companyName);
            return Response.success("success");

        } catch (Exception e) {
            logger.error("Failed to delete event '{}' for company '{}': {}", eventId, companyName, e.getMessage());
            return Response.error("Failed to delete event: " + e.getMessage());
        }
    }

    public Response<String> UpdateEvent(String token, String eventName, String artistName, EventType eventType,
            double price, Date date, String location, String company, MapArea[][] map, double rating) {
        try {
            logger.info("trying update event: " + eventName);
            
            if (!tokenService.validateToken(token)) {
                throw new RuntimeException("Invalid token");
            }
            
            String userID = tokenService.extractUserId(token);
            if (userID == null || !isAuthorized(company, userID)) {
                throw new RuntimeException("Unauthorized: User is not an owner or authorized manager");
            }
            
            Event event = eventRepository.getEvent(eventName, company);
            if (event == null) {
                throw new RuntimeException("Event not found: " + eventName);
            }
            
            Date oldDate = event.getDate();
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
            
            if (oldDate != null && !oldDate.equals(date)) {
                purchasedOrderRepository.getPurchasedOrdersForCompany(company).stream()
                        .filter(o -> o.getEvent().equals(eventName))
                        .forEach(o -> notifier.notifyUser(o.getBuyerID(), "Event Rescheduled",
                                "The event '" + eventName + "' has been rescheduled to " + date + "."));
            }
            logger.info("Successfully update event: " + eventName);
            return Response.success("success");
        } catch (Exception e) {
            logger.error(e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    public Response<String> getCompanyInfo(String token, String company) {
        try {
            boolean isGuest = token == null || token.contains("guest-temporary-token");
            if (!isGuest && !tokenService.validateToken(token)) {
                throw new RuntimeException("Invalid token");
            }
            logger.info("trying Getting company info: " + company);
            boolean c = companyRepository.isCompanyActive(company);
            if (!c) {
                throw new RuntimeException("the company is not active");
            }
            logger.info("Successfully Getting company info: " + company);
            return Response.success(companyRepository.getCompanyDescription(company));
        } catch (Exception e) {
            logger.error(e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    public Response<List<EventDTO>> getCompanyEvents(String token, String company) {
        try {
            boolean isGuest = token == null || token.contains("guest-temporary-token");
            if (!isGuest && !tokenService.validateToken(token)) {
                throw new RuntimeException("Invalid token");
            }
            logger.info("trying Getting company events: " + company);
            boolean c = companyRepository.isCompanyActive(company);
            if (!c) {
                throw new RuntimeException("the company is not active");
            }
            List<Event> events = eventRepository.getEventsByCompany(company);
            List<EventDTO> eventDTOs = new ArrayList<>();
            for (Event event : events) {
                eventDTOs.add(EventDTO.fromEntity(event));
            }
            logger.info("Successfully Getting company events: " + company);
            return Response.success(eventDTOs);
        } catch (Exception e) {
            logger.error(e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    public Response<MapArea[][]> getMapArea(String token, String company, String eventName) {
        try {
            boolean isGuest = token == null || token.contains("guest-temporary-token");
            if (!isGuest && !tokenService.validateToken(token)) {
                throw new RuntimeException("Invalid token");
            }
            logger.info("trying Getting map area: " + eventName);
            MapArea[][] map = eventRepository.getMapArea(company, eventName);
            MapArea[][] mapArea = ticketRepository.getMapAreas(company, eventName, map);
            logger.info("Successfully Getting map area: " + eventName);
            return Response.success(mapArea);
        } catch (Exception e) {
            logger.error(e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    public Response<EventDTO> getEvent(String token, String company, String eventName) {
        try {
            boolean isGuest = token == null || token.contains("guest-temporary-token");
            if (!isGuest && !tokenService.validateToken(token)) {
                throw new RuntimeException("Invalid token");
            }
            Event event = eventRepository.getEvent(eventName, company);
            if (event == null) {
                return Response.error("Event not found");
            }
            logger.info("Retrieved event '{}' for company '{}'", eventName, company);
            return Response.success(EventDTO.fromEntity(event));
        } catch (Exception e) {
            logger.error("Failed to retrieve event '{}': {}", eventName, e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    public Response<List<EventDTO>> searchEvents(String token, String query, String company, EventType type,
            Double minPrice, Double maxPrice,
            Date startDate, Date endDate,
            String location, Double minRating) {
        try {
            if (token != null && !token.trim().isEmpty()) {
                boolean isGuest = token.contains("guest-temporary-token");
                if (!isGuest && !tokenService.validateToken(token)) {
                    throw new RuntimeException("Invalid token");
                }
            }

            logger.info("Initiating search with parameters - Query: {}, Company: {}", query, company);
            List<EventDTO> results = eventRepository.searchEvents(
                    query, company, type, minPrice, maxPrice, startDate, endDate, location, minRating);
            logger.info("Search completed successfully. Found {} events", results.size());
            return Response.success(results);
        } catch (Exception e) {
            logger.error("Error occurred during event search: {}", e.getMessage());
            return Response.error(e.getMessage());
        }
    }
}


/*package com.ticketing.ticketapp.Appliction;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ticketing.ticketapp.Domain.Company.iCompanyRepository;
import com.ticketing.ticketapp.Domain.Event.Event;
import com.ticketing.ticketapp.Domain.Event.EventDTO;
import com.ticketing.ticketapp.Domain.Event.EventType;
import com.ticketing.ticketapp.Domain.Event.MapArea;
import com.ticketing.ticketapp.Domain.Event.iEventRepository;
import com.ticketing.ticketapp.Domain.OwnerManagerTree.iTreeOfRoleRepository;
import com.ticketing.ticketapp.Domain.PurchasedOrderAggregate.iPurchasedOrderRepository;
import com.ticketing.ticketapp.Domain.QueueAggregates.iQueueRepository;
import com.ticketing.ticketapp.Domain.Ticket.iTicketRepository;
import com.ticketing.ticketapp.Domain.User.IUserRepository;
import com.ticketing.ticketapp.Infastructure.TokenService;
import org.springframework.stereotype.Service;

@Service
public class EventService {

    private iCompanyRepository companyRepository;
    private iEventRepository eventRepository;
    private TokenService tokenService;
    private iTreeOfRoleRepository treeOfRoleRepository;
    private iTicketRepository ticketRepository;
    private iQueueRepository iQueueRepository;
    private iPurchasedOrderRepository purchasedOrderRepository;
    private IUserRepository userRepository;
    private INotifier notifier;
    private static final Logger logger = LoggerFactory.getLogger(EventService.class);

    public EventService(iCompanyRepository companyRepository, iEventRepository eventRepository,
            TokenService tokenService, iTreeOfRoleRepository treeOfRoleRepository, iTicketRepository ticketRepository,
            iQueueRepository iQueueRepository, iPurchasedOrderRepository purchasedOrderRepository,
            IUserRepository userRepository, INotifier notifier) {
        this.companyRepository = companyRepository;
        this.eventRepository = eventRepository;
        this.tokenService = tokenService;
        this.treeOfRoleRepository = treeOfRoleRepository;
        this.ticketRepository = ticketRepository;
        this.iQueueRepository = iQueueRepository;
        this.purchasedOrderRepository = purchasedOrderRepository;
        this.userRepository = userRepository;
        this.notifier = notifier;
    }

    public boolean isAuthorized(String company, String username) {
        boolean o = treeOfRoleRepository.exitsOwner(username, company);
        boolean m = treeOfRoleRepository.ManagerPermitedToCreateUpdateDelete(username, company);
        return m || (o);
    }

    public Response<String> createEvent(String token, String eventName, String artistName, EventType eventType,
            double price, Date date, String location, String company, MapArea[][] map) {
        try {
            if (!tokenService.validateToken(token)) {
                throw new RuntimeException("Invalid token");
            }
            String username = tokenService.extractUsername(token);
            if (username == null || !isAuthorized(company, username)) {
                logger.info("Unauthorized attempt to create event '{}' for company '{}'", eventName, company);
                return Response.error("Unauthorized");
            }

            Event event = eventRepository.store(eventName, artistName, eventType, price, date, location, company, map);
            ticketRepository.makeMapToTicket(event.getCompany(), event.getName(), map, event.getDate(),
                    event.getPrice());
            iQueueRepository.initQueue(eventName + company);
            logger.info("Event '{}' created successfully for company '{}'", eventName, company);
            return Response.success("success");
        } catch (Exception e) {
            logger.error("Failed to create event '{}' for company '{}': {}", eventName, company, e.getMessage());
            return Response.error("Failed to create event: " + e.getMessage());
        }
    }

    public Response<String> deleteEvent(String eventId, String companyName, String token) {
        try {
            if (!tokenService.validateToken(token)) {
                throw new RuntimeException("Invalid token");
            }
            String username = tokenService.extractUsername(token);

            if (username == null || !isAuthorized(companyName, username)) {
                logger.info("Unauthorized attempt to delete event '{}' for company '{}'", eventId, companyName);
                return Response.error("Unauthorized");
            }

            Event event = eventRepository.getEventById(eventId, companyName);
            if (event == null) {
                logger.info("Attempt to delete non-existent event '{}' for company '{}'", eventId, companyName);
                return Response.error("Event not found");
            }

            purchasedOrderRepository.getPurchasedOrdersForCompany(companyName).stream()
                    .filter(o -> o.getEvent().equals(event.getName()))
                    .forEach(o -> notifier.notifyUser(o.getBuyerID(), "Event Cancelled",
                            "The event '" + event.getName() + "' by " + companyName
                                    + " has been cancelled. Your tickets are no longer valid."));

            eventRepository.deleteEvent(eventId, companyName);
            logger.info("Event '{}' deleted successfully for company '{}'", eventId, companyName);
            return Response.success("success");

        } catch (Exception e) {
            logger.error("Failed to delete event '{}' for company '{}': {}", eventId, companyName, e.getMessage());
            return Response.error("Failed to delete event: " + e.getMessage());
        }
    }

    public Response<String> UpdateEvent(String token, String eventName, String artistName, EventType eventType,
            double price, Date date, String location, String company, MapArea[][] map, double rating) {
        try {
            logger.info("trying update event: " + eventName);
            String username = tokenService.extractUsername(token);
            if (!tokenService.validateToken(token)) {
                throw new RuntimeException("Invalid token");
            }
            if (username == null || !isAuthorized(company, username)) {
                throw new RuntimeException("Unauthorized: User is not an owner or authorized manager");
            }
            Event event = eventRepository.getEvent(eventName, company);
            if (event == null) {
                throw new RuntimeException("Event not found: " + eventName);
            }
            Date oldDate = event.getDate();
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
            if (oldDate != null && !oldDate.equals(date)) {
                purchasedOrderRepository.getPurchasedOrdersForCompany(company).stream()
                        .filter(o -> o.getEvent().equals(eventName))
                        .forEach(o -> notifier.notifyUser(o.getBuyerID(), "Event Rescheduled",
                                "The event '" + eventName + "' has been rescheduled to " + date + "."));
            }
            logger.info("Successfully update event: " + eventName);
            return Response.success("success");
        } catch (Exception e) {
            logger.error(e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    public Response<String> getCompanyInfo(String token, String company) {
        try {
            boolean isGuest = token == null || token.contains("guest-temporary-token");
            if (!isGuest && !tokenService.validateToken(token)) {
                throw new RuntimeException("Invalid token");
            }
            logger.info("trying Getting company info: " + company);
            boolean c = companyRepository.isCompanyActive(company);
            if (!c) {
                throw new RuntimeException("the company is not active");
            }
            logger.info("Successfully Getting company info: " + company);
            return Response.success(companyRepository.getCompanyDescription(company));
        } catch (Exception e) {
            logger.error(e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    public Response<List<EventDTO>> getCompanyEvents(String token, String company) {
        try {
            boolean isGuest = token == null || token.contains("guest-temporary-token");
            if (!isGuest && !tokenService.validateToken(token)) {
                throw new RuntimeException("Invalid token");
            }
            logger.info("trying Getting company events: " + company);
            boolean c = companyRepository.isCompanyActive(company);
            if (!c) {
                throw new RuntimeException("the company is not active");
            }
            List<Event> events = eventRepository.getEventsByCompany(company);
            List<EventDTO> eventDTOs = new ArrayList<>();
            for (Event event : events) {
                eventDTOs.add(EventDTO.fromEntity(event));
            }
            logger.info("Successfully Getting company events: " + company);
            return Response.success(eventDTOs);
        } catch (Exception e) {
            logger.error(e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    public Response<MapArea[][]> getMapArea(String token, String company, String eventName) {
        try {
            boolean isGuest = token == null || token.contains("guest-temporary-token");
            if (!isGuest && !tokenService.validateToken(token)) {
                throw new RuntimeException("Invalid token");
            }
            logger.info("trying Getting map area: " + eventName);
            MapArea[][] map = eventRepository.getMapArea(company, eventName);
            MapArea[][] mapArea = ticketRepository.getMapAreas(company, eventName, map);
            logger.info("Successfully Getting map area: " + eventName);
            return Response.success(mapArea);
        } catch (Exception e) {
            logger.error(e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    public Response<EventDTO> getEvent(String token, String company, String eventName) {
        try {
            boolean isGuest = token == null || token.contains("guest-temporary-token");
            if (!isGuest && !tokenService.validateToken(token)) {
                throw new RuntimeException("Invalid token");
            }
            Event event = eventRepository.getEvent(eventName, company);
            if (event == null) {
                return Response.error("Event not found");
            }
            logger.info("Retrieved event '{}' for company '{}'", eventName, company);
            return Response.success(EventDTO.fromEntity(event));
        } catch (Exception e) {
            logger.error("Failed to retrieve event '{}': {}", eventName, e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    public Response<List<EventDTO>> searchEvents(String token, String query, String company, EventType type,
            Double minPrice, Double maxPrice,
            Date startDate, Date endDate,
            String location, Double minRating) {
        try {
            // נבדוק את הטוקן רק אם הוא נשלח בבקשה. אם הוא null, נאפשר חיפוש חופשי ציבורי.
            if (token != null && !token.trim().isEmpty()) {
                boolean isGuest = token.contains("guest-temporary-token");
                if (!isGuest && !tokenService.validateToken(token)) {
                    throw new RuntimeException("Invalid token");
                }
            }

            logger.info("Initiating search with parameters - Query: {}, Company: {}", query, company);
            List<EventDTO> results = eventRepository.searchEvents(
                    query, company, type, minPrice, maxPrice, startDate, endDate, location, minRating);
            logger.info("Search completed successfully. Found {} events", results.size());
            return Response.success(results);
        } catch (Exception e) {
            logger.error("Error occurred during event search: {}", e.getMessage());
            return Response.error(e.getMessage());
        }
    }
}
*/