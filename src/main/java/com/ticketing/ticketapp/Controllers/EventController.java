package com.ticketing.ticketapp.Controllers;

import com.ticketing.ticketapp.Appliction.EventService;
import com.ticketing.ticketapp.Domain.Event.EventDTO;
import com.ticketing.ticketapp.Domain.Event.EventType;
import com.ticketing.ticketapp.Domain.Event.MapArea;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/discovery")
@CrossOrigin(origins = "*") 
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    // Global event search (משתמש במתודה המקורית שלכם!)
    @GetMapping("/events/search")
    public ResponseEntity<?> searchEvents(
            @RequestHeader("Authorization") String token, // חובה! יהיה טוקן אורח או מחובר
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String company,
            @RequestParam(required = false) EventType type,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date endDate,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) Double minRating) {
        
        try {
            // קריאה לשירות המקורי שלכם!
            List<EventDTO> results = eventService.searchEvents(token, query, company, type, minPrice, maxPrice, startDate, endDate, location, minRating);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // השלמות קטנות שתצטרך להוסיף ב-EventService (כי חסרות מתודות ספציפיות):
    // 1. fetch specific event details (כרגע חסר ב-Service)
    // 2. get ALL active companies (כרגע חסר ב-Service)
    
    // שליפת מפה (משתמש במתודה המקורית שלכם!)
    @GetMapping("/companies/{companyName}/events/{eventName}/map")
    public ResponseEntity<?> getEventMap(
            @RequestHeader("Authorization") String token, // חובה!
            @PathVariable String companyName,
            @PathVariable String eventName) {
        try {
            MapArea[][] map = eventService.getMapArea(token, companyName, eventName);
            if (map == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Map not found");
            }
            return ResponseEntity.ok(map);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // שליפת קטלוג חברה (משתמש במתודה המקורית שלכם!)
    @GetMapping("/companies/{companyName}/events")
    public ResponseEntity<?> getCompanyCatalog(
            @RequestHeader("Authorization") String token, // חובה!
            @PathVariable String companyName) {
        try {
            List<EventDTO> events = eventService.getCompanyEvents(token, companyName);
            return ResponseEntity.ok(events);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}