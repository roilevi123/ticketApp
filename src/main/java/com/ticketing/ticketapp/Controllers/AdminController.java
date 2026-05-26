package com.ticketing.ticketapp.Controllers;

import com.ticketing.ticketapp.Appliction.AdminService;
import com.ticketing.ticketapp.Appliction.QueueService;
import com.ticketing.ticketapp.Appliction.Response;
import com.ticketing.ticketapp.Domain.Notification.INotificationRepository;
import com.ticketing.ticketapp.Domain.Notification.Notification;
import com.ticketing.ticketapp.Domain.PurchasedOrderAggregate.PurchaseOrderDTO;
import com.ticketing.ticketapp.Domain.QueueAggregates.QueueEntry;
import com.ticketing.ticketapp.Domain.User.Suspension;
import com.ticketing.ticketapp.Infastructure.TokenService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final QueueService queueService;
    private final TokenService tokenService;
    private final INotificationRepository notificationRepository;

    public AdminController(AdminService adminService, QueueService queueService, TokenService tokenService, INotificationRepository notificationRepository) {
        this.adminService = adminService;
        this.queueService = queueService;
        this.tokenService = tokenService;
        this.notificationRepository = notificationRepository;
    }

    @PutMapping("/users/{userId}/suspend")
    public ResponseEntity<?> suspendUser(
            @RequestAttribute("cleanToken") String token,
            @PathVariable String userId,
            @RequestBody SuspendRequest request) {

        String adminId = tokenService.extractUserId(token);
        Response<String> response = adminService.suspendUser(userId, adminId, request.getDurationInDays());
        if (response.isSuccess()) {
            return ResponseEntity.ok(Map.of("message", "User suspended successfully"));
        }
        return ResponseEntity.badRequest().body(Map.of("error", response.getMessage()));
    }

    @PutMapping("/users/{userId}/suspend/cancel")
    public ResponseEntity<?> cancelSuspension(
            @RequestAttribute("cleanToken") String token,
            @PathVariable String userId) {

        String adminId = tokenService.extractUserId(token);
        Response<String> response = adminService.cancelSuspension(userId, adminId);
        if (response.isSuccess()) {
            return ResponseEntity.ok(Map.of("message", "Suspension cancelled successfully"));
        }
        return ResponseEntity.badRequest().body(Map.of("error", response.getMessage()));
    }

    @GetMapping("/suspensions")
    public ResponseEntity<?> getSuspensionHistory(
            @RequestAttribute("cleanToken") String token) {

        String adminId = tokenService.extractUserId(token);
        Response<List<Suspension>> response = adminService.getAllSuspensions(adminId);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response.getData());
        }
        return ResponseEntity.badRequest().body(Map.of("error", response.getMessage()));
    }

    @DeleteMapping("/companies/{companyName}")
    public ResponseEntity<?> closeCompany(
            @RequestAttribute("cleanToken") String token,
            @PathVariable String companyName) {

        String adminId = tokenService.extractUserId(token);
        Response<String> response = adminService.CloseCompany(companyName, adminId);
        if (response.isSuccess()) {
            return ResponseEntity.ok(Map.of("message", "Company closed successfully"));
        }
        return ResponseEntity.badRequest().body(Map.of("error", response.getMessage()));
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<?> removeUser(
            @RequestAttribute("cleanToken") String token,
            @PathVariable String userId) {

        String adminId = tokenService.extractUserId(token);
        Response<String> response = adminService.removeUser(userId, adminId);
        if (response.isSuccess()) {
            return ResponseEntity.ok(Map.of("message", "User removed successfully"));
        }
        return ResponseEntity.badRequest().body(Map.of("error", response.getMessage()));
    }

    @GetMapping("/queues/{eventId}")
    public ResponseEntity<?> getQueue(
            @RequestAttribute("cleanToken") String token,
            @PathVariable String eventId) {

        String adminId = tokenService.extractUserId(token);
        Response<List<QueueEntry>> response = queueService.getQueueForAdmin(adminId, eventId);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response.getData());
        }
        return ResponseEntity.badRequest().body(Map.of("error", response.getMessage()));
    }

    @PutMapping("/queues/{eventId}")
    public ResponseEntity<?> adjustQueue(
            @RequestAttribute("cleanToken") String token,
            @PathVariable String eventId,
            @RequestBody QueueAdjustRequest request) {

        String adminId = tokenService.extractUserId(token);
        Response<String> response;

        if (Boolean.TRUE.equals(request.getClear())) {
            response = queueService.clearQueueForAdmin(adminId, eventId);
        } else if (request.getMaxActiveUsers() != null) {
            response = queueService.setFlowRate(adminId, eventId, request.getMaxActiveUsers());
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "Must specify 'clear' or 'maxActiveUsers'"));
        }

        if (response.isSuccess()) {
            return ResponseEntity.ok(Map.of("message", response.getData()));
        }
        return ResponseEntity.badRequest().body(Map.of("error", response.getMessage()));
    }

    @GetMapping("/purchases")
    public ResponseEntity<?> getPurchaseHistory(
            @RequestAttribute("cleanToken") String token,
            @RequestParam(required = false) String buyerId,
            @RequestParam(required = false) String company,
            @RequestParam(required = false) String eventId) {

        String adminId = tokenService.extractUserId(token);
        Response<List<PurchaseOrderDTO>> response = adminService.getPurchaseHistory(adminId, buyerId, company, eventId);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response.getData());
        }
        return ResponseEntity.badRequest().body(Map.of("error", response.getMessage()));
    }

    @GetMapping("/analytics")
    public ResponseEntity<?> getSystemAnalytics(
            @RequestAttribute("cleanToken") String token) {

        String adminId = tokenService.extractUserId(token);
        Response<Map<String, Long>> response = adminService.getSystemAnalytics(adminId);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response.getData());
        }
        return ResponseEntity.badRequest().body(Map.of("error", response.getMessage()));
    }

    @GetMapping("/complaints")
    public ResponseEntity<?> getComplaints(
            @RequestAttribute("cleanToken") String token) {

        List<Notification> complaints = notificationRepository.getAll("SYSTEM_ADMIN");
        return ResponseEntity.ok(complaints);
    }

    @PostMapping("/users/{userId}/message")
    public ResponseEntity<?> sendMessageToUser(
            @RequestAttribute("cleanToken") String token,
            @PathVariable String userId,
            @RequestBody AdminMessageRequest request) {

        String adminId = tokenService.extractUserId(token);
        Response<String> response = adminService.sendMessageToUser(adminId, userId, request.getMessage());
        if (response.isSuccess()) {
            return ResponseEntity.ok(Map.of("message", "Message sent successfully"));
        }
        return ResponseEntity.badRequest().body(Map.of("error", response.getMessage()));
    }
}

class SuspendRequest {
    private int durationInDays;

    public int getDurationInDays() { return durationInDays; }
    public void setDurationInDays(int durationInDays) { this.durationInDays = durationInDays; }
}

class QueueAdjustRequest {
    private Boolean clear;
    private Integer maxActiveUsers;

    public Boolean getClear() { return clear; }
    public void setClear(Boolean clear) { this.clear = clear; }
    public Integer getMaxActiveUsers() { return maxActiveUsers; }
    public void setMaxActiveUsers(Integer maxActiveUsers) { this.maxActiveUsers = maxActiveUsers; }
}

class AdminMessageRequest {
    private String message;

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
