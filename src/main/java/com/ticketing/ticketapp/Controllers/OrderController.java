package com.ticketing.ticketapp.Controllers;

import com.ticketing.ticketapp.Appliction.OrderService;
import com.ticketing.ticketapp.Appliction.Response;
import com.ticketing.ticketapp.Domain.Ticket.TicketDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/reserve")
    public ResponseEntity<?> reserveTickets( 
            @RequestHeader("Authorization") String token,
            @RequestBody ReserveRequestDTO request) {
        try {
            Response<String> result = orderService.reserveTickets(
                    token, 
                    request.getCompany(), 
                    request.getEvent(), 
                    request.getRequests()
            );

            if (result.isSuccess()) {
                return ResponseEntity.ok(result.getData());
            } else {
                return ResponseEntity.badRequest().body(result.getMessage());
            }
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
        }
    }

    @GetMapping("/active")
    public ResponseEntity<?> getActiveOrderTickets( 
        @RequestHeader("Authorization") String token,
        @RequestParam(required = false) String orderId) {
        try {
            Response<List<TicketDTO>> result = orderService.getActiveOrderTickets(token, orderId);
            if (result.isSuccess()) {
                return ResponseEntity.ok(result.getData());
            } else {
                return ResponseEntity.badRequest().body(result.getMessage());
            }
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
        }
    }
}

class ReserveRequestDTO {
    private String company;
    private String event;
    private List<int[]> requests;

    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }
    
    public String getEvent() { return event; }
    public void setEvent(String event) { this.event = event; }
    
    public List<int[]> getRequests() { return requests; }
    public void setRequests(List<int[]> requests) { this.requests = requests; }
}