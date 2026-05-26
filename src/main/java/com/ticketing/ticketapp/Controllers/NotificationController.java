package com.ticketing.ticketapp.Controllers;

import com.ticketing.ticketapp.Domain.Notification.INotificationRepository;
import com.ticketing.ticketapp.Domain.Notification.Notification;
import com.ticketing.ticketapp.Infastructure.Broadcaster;
import com.ticketing.ticketapp.Infastructure.TokenService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final INotificationRepository notificationRepository;
    private final Broadcaster broadcaster;
    private final TokenService tokenService;

    public NotificationController(
            INotificationRepository notificationRepository,
            Broadcaster broadcaster,
            TokenService tokenService) {
        this.notificationRepository = notificationRepository;
        this.broadcaster = broadcaster;
        this.tokenService = tokenService;
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestAttribute("cleanToken") String token) {
        String userId = tokenService.extractUserId(token);
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        broadcaster.register(userId, message -> {
            try {
                emitter.send(SseEmitter.event().data(message));
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
        });

        emitter.onCompletion(() -> broadcaster.unregister(userId));
        emitter.onTimeout(() -> broadcaster.unregister(userId));
        emitter.onError(e -> broadcaster.unregister(userId));

        return emitter;
    }

    @GetMapping
    public ResponseEntity<?> getAllNotifications(
            @RequestAttribute("cleanToken") String token) {

        String userId = tokenService.extractUserId(token);
        List<Notification> notifications = notificationRepository.getAll(userId);
        return ResponseEntity.ok(notifications);
    }

    @PutMapping("/{notificationId}/read")
    public ResponseEntity<?> markAsRead(
            @RequestAttribute("cleanToken") String token,
            @PathVariable String notificationId) {

        String userId = tokenService.extractUserId(token);
        notificationRepository.markAsRead(userId, notificationId);
        return ResponseEntity.ok(Map.of("message", "Notification marked as read"));
    }
}
