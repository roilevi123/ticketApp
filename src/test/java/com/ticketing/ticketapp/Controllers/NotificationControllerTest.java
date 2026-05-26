package com.ticketing.ticketapp.Controllers;

import com.ticketing.ticketapp.Domain.Notification.INotificationRepository;
import com.ticketing.ticketapp.Domain.Notification.Notification;
import com.ticketing.ticketapp.Infastructure.Broadcaster;
import com.ticketing.ticketapp.Infastructure.TokenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock private INotificationRepository notificationRepository;
    @Mock private Broadcaster broadcaster;
    @Mock private TokenService tokenService;
    @InjectMocks private NotificationController notificationController;

    private static final String TOKEN = "test-token";
    private static final String USER_ID = "user-uuid";
    private static final String NOTIFICATION_ID = "notif-1";

    @Test
    void stream_RegistersWithBroadcasterAndReturnsSseEmitter() {
        when(tokenService.extractUserId(TOKEN)).thenReturn(USER_ID);

        SseEmitter emitter = notificationController.stream(TOKEN);

        assertNotNull(emitter);
        verify(broadcaster).register(eq(USER_ID), any());
    }

    @Test
    void getAllNotifications_Returns200WithAllNotifications() {
        Notification n1 = new Notification(NOTIFICATION_ID, USER_ID, "msg1");
        Notification n2 = new Notification("notif-2", USER_ID, "msg2");
        n2.setRead(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USER_ID);
        when(notificationRepository.getAll(USER_ID)).thenReturn(List.of(n1, n2));

        ResponseEntity<?> response = notificationController.getAllNotifications(TOKEN);

        assertEquals(200, response.getStatusCode().value());
        List<?> body = (List<?>) response.getBody();
        assertEquals(2, body.size());
        verify(notificationRepository, never()).markAsRead(any(), any());
    }

    @Test
    void getAllNotifications_ReturnsEmptyListWhenNone() {
        when(tokenService.extractUserId(TOKEN)).thenReturn(USER_ID);
        when(notificationRepository.getAll(USER_ID)).thenReturn(List.of());

        ResponseEntity<?> response = notificationController.getAllNotifications(TOKEN);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(List.of(), response.getBody());
    }

    @Test
    void markAsRead_Returns200AndDelegatesWithUserId() {
        when(tokenService.extractUserId(TOKEN)).thenReturn(USER_ID);

        ResponseEntity<?> response = notificationController.markAsRead(TOKEN, NOTIFICATION_ID);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Notification marked as read", ((Map<?, ?>) response.getBody()).get("message"));
        verify(notificationRepository).markAsRead(USER_ID, NOTIFICATION_ID);
    }

    @Test
    void markAsRead_MarksOnlyTheSpecifiedNotification() {
        when(tokenService.extractUserId(TOKEN)).thenReturn(USER_ID);

        notificationController.markAsRead(TOKEN, NOTIFICATION_ID);

        verify(notificationRepository, times(1)).markAsRead(USER_ID, NOTIFICATION_ID);
        verifyNoMoreInteractions(notificationRepository);
    }
}
