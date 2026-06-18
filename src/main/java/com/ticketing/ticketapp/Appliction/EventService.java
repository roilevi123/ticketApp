package com.ticketing.ticketapp.Appliction;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ticketing.ticketapp.Domain.Discount.DiscountPolicy;
import com.ticketing.ticketapp.Domain.Discount.MaxDiscountComposite;
import com.ticketing.ticketapp.Domain.Discount.PurchaseContext;
import com.ticketing.ticketapp.Domain.Discount.iDiscountPolicyRepository;
import com.ticketing.ticketapp.Domain.Company.iCompanyRepository;
import com.ticketing.ticketapp.Domain.Event.Event;
import com.ticketing.ticketapp.Domain.Event.EventDTO;
import com.ticketing.ticketapp.Domain.Event.EventDomainException;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.cache.annotation.CacheEvict;

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
    private iDiscountPolicyRepository discountRepo;
    private INotifier notifier;
    private static final Logger logger = LoggerFactory.getLogger(EventService.class);

    public EventService(iCompanyRepository companyRepository, iEventRepository eventRepository,
            TokenService tokenService, iTreeOfRoleRepository treeOfRoleRepository, iTicketRepository ticketRepository,
            iQueueRepository iQueueRepository, iPurchasedOrderRepository purchasedOrderRepository,
            IUserRepository userRepository, INotifier notifier, iDiscountPolicyRepository discountRepo) {
        this.companyRepository = companyRepository;
        this.eventRepository = eventRepository;
        this.tokenService = tokenService;
        this.treeOfRoleRepository = treeOfRoleRepository;
        this.ticketRepository = ticketRepository;
        this.iQueueRepository = iQueueRepository;
        this.purchasedOrderRepository = purchasedOrderRepository;
        this.userRepository = userRepository;
        this.notifier = notifier;
        this.discountRepo = discountRepo;
    }

    public boolean isAuthorized(String company, String userId) {
        boolean o = treeOfRoleRepository.exitsOwner(userId, company);
        boolean m = treeOfRoleRepository.ManagerPermitedToCreateUpdateDelete(userId, company);
        return m || (o);
    }

    @Transactional
    @CacheEvict(value = { "companyEvents", "searchedEvents" }, allEntries = true)
    public Response<String> createEvent(String token, String eventName, String artistName, EventType eventType,
            double price, Date date, String location, String company, MapArea[][] map) {
        try {
            logger.info("User of token {} is attempting to create an event for the company: {}", token, company);
            if (!tokenService.validateToken(token)) {
                throw new RuntimeException("Invalid token");
            }
            String userId = tokenService.extractUserId(token);
            if (userId == null || !isAuthorized(company, userId)) {
                logger.info("Unauthorized attempt to create event '{}' for company '{}'", eventName, company);
                return Response.error("Unauthorized");
            }
            String userID = tokenService.extractUserId(token);
            if (userRepository.isUserSuspendedNow(userID))
                throw new EventDomainException("User is suspended");

            Event event = eventRepository.store(eventName, artistName, eventType, price, date, location, company, map);
            ticketRepository.makeMapToTicket(event.getCompany(), event.getName(), map, null,
                    event.getPrice());
            iQueueRepository.initQueue(eventName + company);
            logger.info("Event '{}' created successfully for company '{}'", eventName, company);
            return Response.success("success");
        } catch (Exception e) {
            logger.error("Failed to create event '{}' for company '{}': {}", eventName, company, e.getMessage());
            return Response.error("Failed to create event: " + e.getMessage());
        }
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "events", allEntries = true),
            @CacheEvict(value = "eventMaps", allEntries = true),
            @CacheEvict(value = { "companyEvents", "searchedEvents" }, allEntries = true)
    })
    public Response<String> deleteEvent(String eventId, String companyName, String token) {
        try {
            logger.info("User of token {} is attempting to delete the event {} of the company: {}", token, eventId,
                    companyName);
            if (!tokenService.validateToken(token)) {
                throw new RuntimeException("Invalid token");
            }
            String userId = tokenService.extractUserId(token);

            if (userId == null || !isAuthorized(companyName, userId)) {
                logger.info("Unauthorized attempt to delete event '{}' for company '{}'", eventId, companyName);
                return Response.error("Unauthorized");
            }
            String userID = tokenService.extractUserId(token);
            if (userRepository.isUserSuspendedNow(userID))
                throw new EventDomainException("User is suspended");

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

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "events", key = "#company + '-' + #eventName"),
            @CacheEvict(value = "eventMaps", key = "#company + '-' + #eventName"),
            @CacheEvict(value = { "companyEvents", "searchedEvents" }, allEntries = true)
    })
    public Response<String> UpdateEvent(String token, String eventName, String artistName, EventType eventType,
            double price, Date date, String location, String company, MapArea[][] map, double rating) {
        try {
            logger.info("User of token {} is attempting to update the event: {}", token, eventName);
            String userId = tokenService.extractUserId(token);
            if (!tokenService.validateToken(token)) {
                throw new RuntimeException("Invalid token");
            }
            if (userId == null || !isAuthorized(company, userId)) {
                throw new RuntimeException("Unauthorized: User is not an owner or authorized manager");
            }
            String userID = tokenService.extractUserId(token);
            if (userRepository.isUserSuspendedNow(userID))
                throw new EventDomainException("User is suspended");

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
            logger.info("User {} successfully update event: ", userID, eventName);
            return Response.success("success");
        } catch (Exception e) {
            logger.error(e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "companyInfo", key = "#company")
    public Response<String> getCompanyInfo(String token, String company) {
        try {
            logger.info("User of token {} is attempting to get company info: {}", token, company);

            boolean isGuest = token == null || token.contains("guest-temporary-token");
            if (!isGuest && !tokenService.validateToken(token)) {
                throw new RuntimeException("Invalid token");
            }
            boolean c = companyRepository.isCompanyActive(company);
            if (!c) {
                throw new RuntimeException("the company is not active");
            }
            logger.info("User {} successfully got company info: ", token, company);
            return Response.success(companyRepository.getCompanyDescription(company));
        } catch (Exception e) {
            logger.error(e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "companyEvents", key = "#company")
    public Response<List<EventDTO>> getCompanyEvents(String token, String company) {
        try {
            logger.info("User of token {} is attempting to get company events: {}", token, company);
            boolean isGuest = token == null || token.contains("guest-temporary-token");
            if (!isGuest && !tokenService.validateToken(token)) {
                throw new RuntimeException("Invalid token");
            }
            boolean c = companyRepository.isCompanyActive(company);
            if (!c) {
                throw new RuntimeException("the company is not active");
            }
            List<Event> events = eventRepository.getEventsByCompany(company);
            List<EventDTO> eventDTOs = new ArrayList<>();
            for (Event event : events) {
                eventDTOs.add(getEventWithDiscount(event));
            }
            logger.info("User of token {} successfully got company events: {}", token, company);
            return Response.success(eventDTOs);
        } catch (Exception e) {
            logger.error(e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "eventMaps", key = "#company + '-' + #eventName")
    public Response<MapArea[][]> getMapArea(String token, String company, String eventName) {
        try {
            logger.info("User of token {} is attempting to get area map for event {} of the company {} ", token,
                    eventName, company);
            boolean isGuest = token == null || token.contains("guest-temporary-token");
            if (!isGuest && !tokenService.validateToken(token)) {
                throw new RuntimeException("Invalid token");
            }
            MapArea[][] map = eventRepository.getMapArea(company, eventName);
            MapArea[][] mapArea = ticketRepository.getMapAreas(company, eventName, map);
            logger.info("User of token {} successfully got area map for the event: {} of the compant {}", token,
                    eventName, company);
            return Response.success(mapArea);
        } catch (Exception e) {
            logger.error(e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "events", key = "#company + '-' + #eventName")
    public Response<EventDTO> getEvent(String token, String company, String eventName) {
        try {
            logger.info("User of token {} is attempting to get event {} of the company {} ", token, eventName, company);
            boolean isGuest = token == null || token.contains("guest-temporary-token");
            if (!isGuest && !tokenService.validateToken(token)) {
                throw new RuntimeException("Invalid token");
            }
            Event event = eventRepository.getEvent(eventName, company);
            if (event == null) {
                return Response.error("Event not found");
            }
            logger.info("User of token {} successfully got event: {} of the company {}", token, event, company);
            return Response.success(getEventWithDiscount(event));
        } catch (Exception e) {
            logger.error("Failed to retrieve event '{}': {}", eventName, e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "searchedEvents", key = "{#query, #company, #type, #minPrice, #maxPrice, #startDate, #endDate, #location, #minRating}")
    public Response<List<EventDTO>> searchEvents(String token, String query, String company, EventType type,
            Double minPrice, Double maxPrice,
            Date startDate, Date endDate,
            String location, Double minRating) {
        try {
            logger.info("User of token {} is attempting to search company events: ", token, company);

            if (token != null && !token.trim().isEmpty()) {
                boolean isGuest = token.contains("guest-temporary-token");
                if (!isGuest && !tokenService.validateToken(token)) {
                    throw new RuntimeException("Invalid token");
                }
            }

            logger.info("Initiating search with parameters - Query: {}, Company: {}", query, company);
            List<Event> rawEvents = eventRepository.searchEvents(
                    query, company, type, minPrice, maxPrice, startDate, endDate, location, minRating);
            List<EventDTO> results = new ArrayList<>();
            for (Event event : rawEvents) {
                results.add(getEventWithDiscount(event));
            }
            logger.info("Search completed successfully. Found {} events", results.size());
            return Response.success(results);
        } catch (Exception e) {
            logger.error("Error occurred during event search: {}", e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    private EventDTO getEventWithDiscount(Event event) {
        Double discountedPrice = null;
        try {
            DiscountPolicy eventPolicy = discountRepo.findByEvent(event.getName());
            DiscountPolicy companyPolicy = discountRepo.findByCompany(event.getCompany());
            String policyId = UUID.randomUUID().toString();

            MaxDiscountComposite combinedRoot = new MaxDiscountComposite(policyId);
            if (eventPolicy != null)
                combinedRoot.add(eventPolicy.getRoot());
            if (companyPolicy != null)
                combinedRoot.add(companyPolicy.getRoot());

            PurchaseContext context = new PurchaseContext(1, null, new Date());
            double discountAmount = combinedRoot.calculateDiscount(event.getPrice(), context);

            if (discountAmount > 0) {
                discountedPrice = event.getPrice() - discountAmount;
            }
        } catch (Exception e) {
            logger.error("Error calculating discount for event '{}': {}", event.getName(), e.getMessage());
        }

        return EventDTO.fromEntity(event, discountedPrice);
    }
    public void deleteAllEvents() {
        eventRepository.deleteAllEvents();

    }
}
