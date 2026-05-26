package com.ticketing.ticketapp.Controllers;

import com.ticketing.ticketapp.Appliction.CompanyService;
import com.ticketing.ticketapp.Appliction.EventService;
import com.ticketing.ticketapp.Appliction.PurchasePolicyService;
import com.ticketing.ticketapp.Appliction.Response;
import com.ticketing.ticketapp.Domain.Event.EventType;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

import java.util.Date;

@RestController
@RequestMapping("/api/discovery")
public class EventController {

    private final EventService eventService;
    private final CompanyService companyService;
    private final PurchasePolicyService purchasePolicyService;

    public EventController(EventService eventService, CompanyService companyService,
                           PurchasePolicyService purchasePolicyService) {
        this.eventService = eventService;
        this.companyService = companyService;
        this.purchasePolicyService = purchasePolicyService;
    }

    @GetMapping("/events/search")
    public ResponseEntity<?> searchEvents(
            @RequestAttribute(value = "cleanToken", required = false) String token,
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
            @RequestAttribute(value = "cleanToken", required = false) String token,
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
            @RequestAttribute(value = "cleanToken", required = false) String token,
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
            @RequestAttribute(value = "cleanToken", required = false) String token,
            @PathVariable(value = "companyName") String companyName) {

        Response<?> response = eventService.getCompanyEvents(token, companyName);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response.getData());
        }
        return ResponseEntity.badRequest().body(response.getMessage());
    }

    /**
     * Returns the maximum number of seats a user may select for this event,
     * as dictated by the event-level and/or company-level purchase policies.
     * {@code maxSeats: null} means no limit is defined.
     * This endpoint is public (excluded from token-interceptor via /api/discovery/**).
     */
    @GetMapping("/companies/{companyName}/events/{eventName}/seat-limit")
    public ResponseEntity<?> getEventSeatLimit(
            @RequestAttribute(value = "cleanToken", required = false) String token,
            @PathVariable("companyName") String companyName,
            @PathVariable("eventName") String eventName) {

        Response<Integer> response = purchasePolicyService.getMaxSeatsForEvent(eventName, companyName);

        if (response.isSuccess()) {
            Map<String, Object> body = new HashMap<>();
            body.put("maxSeats", response.getData()); // null == unrestricted
            return ResponseEntity.ok(body);
        }
        return ResponseEntity.badRequest().body(response.getMessage());
    }

    @GetMapping("/companies")
    public ResponseEntity<?> getActiveCompanies(
            @RequestAttribute(value = "cleanToken", required = false) String token) {

        Response<?> response = companyService.getActiveCompanies(token);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response.getData());
        }
        return ResponseEntity.badRequest().body(response.getMessage());
    }
}
