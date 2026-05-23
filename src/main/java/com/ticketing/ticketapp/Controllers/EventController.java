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
            @RequestAttribute("cleanToken") String token,
            @RequestParam(required = false, value = "query") String query,
            @RequestParam(required = false, value = "company") String company,
            @RequestParam(required = false, value = "type") EventType type,
            @RequestParam(required = false, value = "minPrice") Double minPrice,
            @RequestParam(required = false, value = "maxPrice") Double maxPrice,
            @RequestParam(required = false, value = "startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date startDate,
            @RequestParam(required = false, value = "endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date endDate,
            @RequestParam(required = false, value = "location") String location,
            @RequestParam(required = false, value = "minRating") Double minRating) {

        Response<?> response = eventService.searchEvents(
                token, query, company, type, minPrice, maxPrice, startDate, endDate, location, minRating);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response.getData());
        }
        return ResponseEntity.badRequest().body(response.getMessage());
    }

    @GetMapping("/companies/{companyName}/events/{eventName}")
    public ResponseEntity<?> getEventDetails(
            @RequestAttribute("cleanToken") String token,
            @PathVariable(value = "companyName") String companyName,
            @PathVariable(value = "eventName") String eventName) {

        Response<?> response = eventService.getEvent(token, companyName, eventName);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response.getData());
        }
        return ResponseEntity.status(404).body(response.getMessage());
    }

    @GetMapping("/companies/{companyName}/events/{eventName}/map")
    public ResponseEntity<?> getEventMap(
            @RequestAttribute("cleanToken") String token,
            @PathVariable(value = "companyName") String companyName,
            @PathVariable(value = "eventName") String eventName) {

        Response<?> response = eventService.getMapArea(token, companyName, eventName);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response.getData());
        }
        return ResponseEntity.status(404).body(response.getMessage());
    }

    @GetMapping("/companies/{companyName}/events")
    public ResponseEntity<?> getCompanyEvents(
            @RequestAttribute("cleanToken") String token,
            @PathVariable(value = "companyName") String companyName) {

        Response<?> response = eventService.getCompanyEvents(token, companyName);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response.getData());
        }
        return ResponseEntity.badRequest().body(response.getMessage());
    }

    @GetMapping("/companies")
    public ResponseEntity<?> getActiveCompanies(
            @RequestAttribute("cleanToken") String token) {

        Response<?> response = companyService.getActiveCompanies(token);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response.getData());
        }
        return ResponseEntity.badRequest().body(response.getMessage());
    }
}
