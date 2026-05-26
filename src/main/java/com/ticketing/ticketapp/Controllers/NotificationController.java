package com.ticketing.ticketapp.Controllers;

import com.ticketing.ticketapp.Domain.AdminAggregate.iAdminRepository;
import com.ticketing.ticketapp.Domain.Notification.INotificationRepository;
import com.ticketing.ticketapp.Domain.Notification.Notification;
import com.ticketing.ticketapp.Infastructure.Broadcaster;
import com.ticketing.ticketapp.Infastructure.TokenService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final INotificationRepository notificationRepository;
    private final Broadcaster broadcaster;
    private final TokenService tokenService;
    private final iAdminRepository adminRepository;

    public NotificationController(
            INotificationRepository notificationRepository,
            Broadcaster broadcaster,
            TokenService tokenService,
            iAdminRepository adminRepository) {
        this.notificationRepository = notificationRepository;
        this.broadcaster = broadcaster;
        this.tokenService = tokenService;
        this.adminRepository = adminRepository;
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestAttribute("cleanToken") String token) {
        String userId = tokenService.extractUserId(token);
        boolean isAdmin = adminRepository.isAdmin(userId);
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        Consumer<String> callback = message -> {
            try {
                emitter.send(SseEmitter.event().data(message));
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
        };

        broadcaster.register(userId, callback);
        if (isAdmin) {
            broadcaster.register("SYSTEM_ADMIN", callback);
        }

        emitter.onCompletion(() -> {
            broadcaster.unregister(userId);
            if (isAdmin) broadcaster.unregister("SYSTEM_ADMIN");
        });
        emitter.onTimeout(() -> {
            broadcaster.unregister(userId);
            if (isAdmin) broadcaster.unregister("SYSTEM_ADMIN");
        });
        emitter.onError(e -> {
            broadcaster.unregister(userId);
            if (isAdmin) broadcaster.unregister("SYSTEM_ADMIN");
        });

        return emitter;
    }

    @GetMapping
    public ResponseEntity<?> getAllNotifications(
            @RequestAttribute("cleanToken") String token) {

        String userId = tokenService.extractUserId(token);
        List<Notification> notifications = new ArrayList<>(notificationRepository.getAll(userId));
        if (adminRepository.isAdmin(userId)) {
            notifications.addAll(notificationRepository.getAll("SYSTEM_ADMIN"));
        }
        notifications.sort(Comparator.comparing(Notification::getCreatedAt).reversed());
        return ResponseEntity.ok(notifications);
    }

    @PutMapping("/{notificationId}/read")
    public ResponseEntity<?> markAsRead(
            @RequestAttribute("cleanToken") String token,
            @PathVariable String notificationId) {

        String userId = tokenService.extractUserId(token);
        notificationRepository.markAsRead(userId, notificationId);
        if (adminRepository.isAdmin(userId)) {
            notificationRepository.markAsRead("SYSTEM_ADMIN", notificationId);
        }
        return ResponseEntity.ok(Map.of("message", "Notification marked as read"));
    }

    @PutMapping("/{notificationId}/unread")
    public ResponseEntity<?> markAsUnread(
            @RequestAttribute("cleanToken") String token,
            @PathVariable String notificationId) {

        String userId = tokenService.extractUserId(token);
        notificationRepository.markAsUnread(userId, notificationId);
        if (adminRepository.isAdmin(userId)) {
            notificationRepository.markAsUnread("SYSTEM_ADMIN", notificationId);
        }
        return ResponseEntity.ok(Map.of("message", "Notification marked as unread"));
    }

    @PutMapping("/read-all")
    public ResponseEntity<?> markAllAsRead(
            @RequestAttribute("cleanToken") String token) {

        String userId = tokenService.extractUserId(token);
        notificationRepository.markAllAsRead(userId);
        if (adminRepository.isAdmin(userId)) {
            notificationRepository.markAllAsRead("SYSTEM_ADMIN");
        }
        return ResponseEntity.ok(Map.of("message", "All notifications marked as read"));
    }
}
