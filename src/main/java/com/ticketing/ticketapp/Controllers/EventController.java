package com.ticketing.ticketapp.Controllers;

import com.ticketing.ticketapp.Appliction.CompanyService;
import com.ticketing.ticketapp.Appliction.EventService;
import com.ticketing.ticketapp.Appliction.Response;
import com.ticketing.ticketapp.Domain.Event.EventType;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

@RestController
@RequestMapping("/api/discovery")
public class EventController {

    private final EventService eventService;
    private final CompanyService companyService;

    public EventController(EventService eventService, CompanyService companyService) {
        this.eventService = eventService;
        this.companyService = companyService;
    }

    @GetMapping("/events/search")
    public ResponseEntity<?> searchEvents(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String company,
            @RequestParam(required = false) EventType type,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date endDate,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) Double minRating) {

        Response<?> response = eventService.searchEvents(
                token, query, company, type, minPrice, maxPrice, startDate, endDate, location, minRating);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response.getData());
        }
        return ResponseEntity.badRequest().body(response.getMessage());
    }

    @GetMapping("/companies/{companyName}/events/{eventName}")
    public ResponseEntity<?> getEventDetails(
            @RequestHeader("Authorization") String token,
            @PathVariable String companyName,
            @PathVariable String eventName) {

        Response<?> response = eventService.getEvent(token, companyName, eventName);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response.getData());
        }
        return ResponseEntity.status(404).body(response.getMessage());
    }

    @GetMapping("/companies/{companyName}/events/{eventName}/map")
    public ResponseEntity<?> getEventMap(
            @RequestHeader("Authorization") String token,
            @PathVariable String companyName,
            @PathVariable String eventName) {

        Response<?> response = eventService.getMapArea(token, companyName, eventName);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response.getData());
        }
        return ResponseEntity.status(404).body(response.getMessage());
    }

    @GetMapping("/companies/{companyName}/events")
    public ResponseEntity<?> getCompanyEvents(
            @RequestHeader("Authorization") String token,
            @PathVariable String companyName) {

        Response<?> response = eventService.getCompanyEvents(token, companyName);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response.getData());
        }
        return ResponseEntity.badRequest().body(response.getMessage());
    }

    @GetMapping("/companies")
    public ResponseEntity<?> getActiveCompanies(
            @RequestHeader("Authorization") String token) {

        Response<?> response = companyService.getActiveCompanies(token);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response.getData());
        }
        return ResponseEntity.badRequest().body(response.getMessage());
    }
}
