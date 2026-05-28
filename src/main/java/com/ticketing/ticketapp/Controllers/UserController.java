package com.ticketing.ticketapp.Controllers;

import com.ticketing.ticketapp.Domain.PurchasedOrderAggregate.PurchaseOrderDTO;
import com.ticketing.ticketapp.Domain.User.UserDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.ticketing.ticketapp.Appliction.UserService;
import com.ticketing.ticketapp.Appliction.PurchasedService;
import com.ticketing.ticketapp.Appliction.QueueService;
import java.util.*;
import com.ticketing.ticketapp.Appliction.Response;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private QueueService queueService;
    private PurchasedService purchasedService;
    private UserService userService;

    public UserController(QueueService queueService, PurchasedService purchasedService, UserService userService) {
        this.queueService = queueService;
        this.purchasedService = purchasedService;
        this.userService = userService;
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(
            @RequestAttribute("cleanToken") String token) {

        Response<UserDTO> response = userService.getUserProfile(token);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response.getData());
        }
        return ResponseEntity.status(400).body(Map.of("error", response.getMessage()));
    }
    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
            @RequestAttribute("cleanToken") String token,
            @RequestBody UserDTO request) {

        Response<String> response = userService.updateUserProfile(token, request);

        if (response.isSuccess()) {
            return ResponseEntity.ok(Map.of("message", "Profile updated successfully."));
        }
        return ResponseEntity.status(400).body(Map.of("error", response.getMessage()));
    }

    @GetMapping("/history")
    public ResponseEntity<?> getPurchaseHistory(
            @RequestAttribute("cleanToken") String token) {

            Response<List<PurchaseOrderDTO>> response = purchasedService.getUserTransaction(token);
            
            if (response.isSuccess()) {
                return ResponseEntity.ok(response.getData());
            }
            return ResponseEntity.status(400).body(Map.of("error", response.getMessage()));

    }

    @PostMapping("/support/message")
    public ResponseEntity<?> sendMessage(
            @RequestAttribute("cleanToken") String token,
            @RequestBody MessageRequest request) {
        
        Response<String> response = userService.submitUserComplaint(
                token, 
                request.getRecipientRole(), 
                request.getContent()
        );

        if (response.isSuccess()) {
            return ResponseEntity.ok(Map.of("message", response.getData()));
        }
        return ResponseEntity.status(400).body(Map.of("error", response.getMessage()));
    }

    @GetMapping("/notifications")
    public ResponseEntity<?> getMyNotifications(@RequestAttribute("cleanToken") String token) {
        Response<List<String>> response = userService.getUserNotifications(token);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response.getData());
        }
        return ResponseEntity.status(400).body(Map.of("error", response.getMessage()));
    }

    @PostMapping("/lottery/register")
    public ResponseEntity<?> registerForLottery(
            @RequestAttribute("cleanToken") String token,
            @RequestBody LotteryRequest request) {
        
        Response<String> response = queueService.checkStatus(token, request.getEventId());
        if (response.isSuccess()) {
            return ResponseEntity.ok(Map.of(
                "status", response.getData(),
                "message", "Lottery status processed successfully."
            ));
        }
        return ResponseEntity.status(400).body(Map.of("error", response.getMessage()));
    }
}


class MessageRequest {
        private String recipientRole; 
        private String content;

        public String getRecipientRole() { return recipientRole; }
        public void setRecipientRole(String recipientRole) { this.recipientRole = recipientRole; }

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
}


class LotteryRequest {
    private String eventId;

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
}