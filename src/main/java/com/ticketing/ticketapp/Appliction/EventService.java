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
import com.ticketing.ticketapp.Domain.QueueAggregates.iQueueRepository;
import com.ticketing.ticketapp.Domain.Ticket.iTicketRepository;
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
    private static final Logger logger = LoggerFactory.getLogger(EventService.class);


    public EventService(iCompanyRepository companyRepository, iEventRepository eventRepository, TokenService tokenService, iTreeOfRoleRepository treeOfRoleRepository, iTicketRepository ticketRepository, iQueueRepository iQueueRepository) {
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

    public String createEvent(String token,String eventName, String artistName, EventType eventType, double price, Date date, String location, String company, MapArea[][] map) {
        String username = tokenService.extractUsername(token);
        if (username == null || !isAuthorized(company, username)) {
            logger.info("Unauthorized attempt to create event '{}' for company '{}'", eventName, company);
            return "Unauthorized";
        }
        if(!tokenService.validateToken(token)) {
            throw new RuntimeException("Invalid token");
        }

        try {
            Event event = eventRepository.store(eventName, artistName, eventType, price, date, location, company,map);
            ticketRepository.makeMapToTicket(event.getCompany(), event.getName(), map, event.getDate(), event.getPrice());
            iQueueRepository.initQueue(eventName+company);
            logger.info("Event '{}' created successfully for company '{}'", eventName, company);
            return "success";
        } catch (Exception e) {
            logger.error("Failed to create event '{}' for company '{}': {}", eventName, company, e.getMessage());
            return "Failed to create event: " + e.getMessage();
        }
    }

    public String deleteEvent(String eventId, String companyName, String token) {
        String username = tokenService.extractUsername(token);

        if (username == null || !isAuthorized(companyName, username)) {
            logger.info("Unauthorized attempt to delete event '{}' for company '{}'", eventId, companyName);
            return "Unauthorized";
        }

        try {
            if (!tokenService.validateToken(token)) {
                return "Invalid token";
            }

            Event event = eventRepository.getEventById(eventId, companyName);
            if (event == null) {
                logger.info("Attempt to delete non-existent event '{}' for company '{}'", eventId, companyName);
                return "Event not found";
            }

            eventRepository.deleteEvent(eventId, companyName);
            logger.info("Event '{}' deleted successfully for company '{}'", eventId, companyName);
            return "success";

        } catch (Exception e) {
            logger.error("Failed to delete event '{}' for company '{}': {}", eventId, companyName, e.getMessage());
            return "Failed to delete event: " + e.getMessage();
        }
    }

    public String UpdateEvent(String token, String eventName, String artistName, EventType eventType, double price, Date date, String location, String company, MapArea[][] map, double rating) {
        try {
            logger.info("trying update  event: " + eventName);
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
            return e.getMessage();
        }
    }


    public String getCompanyInfo(String token, String company) {
        try {
            if (!tokenService.validateToken(token)) {
                throw new RuntimeException("Invalid token");
            }
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

    public List<EventDTO> getCompanyEvents(String token, String company) {
        try {
            if (!tokenService.validateToken(token)) {
                throw new RuntimeException("Invalid token");
            }
            logger.info("trying Getting company events: " + company);
            boolean c=companyRepository.isCompanyActive(company);
            if(!c){
                throw new RuntimeException("the company is not active");
            }
            List<Event> events = eventRepository.getEventsByCompany(company);
            List<String> eventsString = new ArrayList<>();
            List<EventDTO> eventDTOs = new ArrayList<>();
            for (Event event : events) {
                eventsString.add(event.toString());
                eventDTOs.add(EventDTO.fromEntity(event));
            }
            logger.info("Successfully Getting company events: " + company);
            return eventDTOs;
        }
        catch (Exception e) {
            logger.error(e.getMessage());
            return null;
        }
    }

    public MapArea[][] getMapArea(String token, String company, String eventName) {
        try {
            if (!tokenService.validateToken(token)) {
                throw new RuntimeException("Invalid token");
            }
            logger.info("trying Getting map area: " + eventName);
            MapArea[][] map=eventRepository.getMapArea(company,eventName);
            MapArea[][] mapArea=ticketRepository.getMapAreas(company,eventName,map);
            logger.info("Successfully Getting map area: " + eventName);
            return mapArea;
        }catch (Exception e) {
            logger.error(e.getMessage());
            return null;
        }
    }

    public List<EventDTO> searchEvents(String token, String query, String company, EventType type,
                                     Double minPrice, Double maxPrice,
                                     Date startDate, Date endDate,
                                     String location, Double minRating) {
        try {
            if (!tokenService.validateToken(token)) {
                throw new RuntimeException("Invalid token");
            }
            logger.info("Initiating search with parameters - Query: {}, Company: {}", query, company);
            List<EventDTO> results = eventRepository.searchEvents(
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
