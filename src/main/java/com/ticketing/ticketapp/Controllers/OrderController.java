package com.ticketing.ticketapp.Controllers;

import com.ticketing.ticketapp.Appliction.OrderService;
import com.ticketing.ticketapp.Appliction.PurchasedService;
import com.ticketing.ticketapp.Appliction.Response;
import com.ticketing.ticketapp.Domain.Ticket.TicketDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final PurchasedService purchasedService;

    public OrderController(OrderService orderService, PurchasedService purchasedService) {
        this.orderService = orderService;
        this.purchasedService = purchasedService;
    }

    @PostMapping("/reserve")
    public ResponseEntity<?> reserveTickets( 
            @RequestAttribute("cleanToken") String token,
            @RequestBody ReserveRequestDTO request) {
        try {
            Response<String> result = orderService.reserveTickets(
                    token,
                    request.getCompany(),
                    request.getEvent(),
                    request.getRequests(),
                    request.getLotteryCode()
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
        @RequestAttribute("cleanToken") String token,
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

    @DeleteMapping("/active")
    public ResponseEntity<?> cancelActiveOrder(
            @RequestAttribute("cleanToken") String token) {
        try {
            Response<String> result = orderService.cancelActiveOrder(token);
            if (result.isSuccess()) {
                return ResponseEntity.ok(Map.of("message", "Order cancelled"));
            }
            return ResponseEntity.badRequest().body(Map.of("error", result.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/purchase")
    public ResponseEntity<?> purchaseTicket(
            @RequestAttribute("cleanToken") String token,
            @RequestBody PurchaseRequestDTO request) {
        try {
            String coupon = (request.getCoupon() != null && !request.getCoupon().isBlank())
                    ? request.getCoupon() : "none";
            Response<String> result = purchasedService.PurchaseTicket(
                    request.getEmail(), null, token, coupon);
            if (result.isSuccess()) {
                return ResponseEntity.ok(Map.of("message", "Purchase successful"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", result.getMessage()));
            }
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", ex.getMessage()));
        }
    }
}

class PurchaseRequestDTO {
    private String email;
    private String coupon;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getCoupon() { return coupon; }
    public void setCoupon(String coupon) { this.coupon = coupon; }
}

class ReserveRequestDTO {
    private String company;
    private String event;
    private List<int[]> requests;
    /** Optional lottery purchase code – required for high-demand events. */
    private String lotteryCode;

    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }

    public String getEvent() { return event; }
    public void setEvent(String event) { this.event = event; }

    public List<int[]> getRequests() { return requests; }
    public void setRequests(List<int[]> requests) { this.requests = requests; }

    public String getLotteryCode() { return lotteryCode; }
    public void setLotteryCode(String lotteryCode) { this.lotteryCode = lotteryCode; }
}