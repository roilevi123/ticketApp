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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock private AdminService adminService;
    @Mock private QueueService queueService;
    @Mock private TokenService tokenService;
    @Mock private INotificationRepository notificationRepository;
    @InjectMocks private AdminController adminController;

    private static final String TOKEN = "test-token";
    private static final String ADMIN_ID = "admin-uuid";
    private static final String USER_ID = "user-uuid";
    private static final String EVENT_ID = "event-1";

    @Test
    void suspendUser_Success_Returns200() {
        when(tokenService.extractUserId(TOKEN)).thenReturn(ADMIN_ID);
        when(adminService.suspendUser(USER_ID, ADMIN_ID, 7)).thenReturn(Response.success("success"));

        SuspendRequest req = new SuspendRequest();
        req.setDurationInDays(7);
        ResponseEntity<?> response = adminController.suspendUser(TOKEN, USER_ID, req);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("User suspended successfully", ((Map<?, ?>) response.getBody()).get("message"));
    }

    @Test
    void suspendUser_Failure_Returns400() {
        when(tokenService.extractUserId(TOKEN)).thenReturn(ADMIN_ID);
        when(adminService.suspendUser(USER_ID, ADMIN_ID, 7)).thenReturn(Response.error("Admin does not exist"));

        SuspendRequest req = new SuspendRequest();
        req.setDurationInDays(7);
        ResponseEntity<?> response = adminController.suspendUser(TOKEN, USER_ID, req);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Admin does not exist", ((Map<?, ?>) response.getBody()).get("error"));
    }

    @Test
    void cancelSuspension_Success_Returns200() {
        when(tokenService.extractUserId(TOKEN)).thenReturn(ADMIN_ID);
        when(adminService.cancelSuspension(USER_ID, ADMIN_ID)).thenReturn(Response.success("success"));

        ResponseEntity<?> response = adminController.cancelSuspension(TOKEN, USER_ID);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Suspension cancelled successfully", ((Map<?, ?>) response.getBody()).get("message"));
    }

    @Test
    void cancelSuspension_Failure_Returns400() {
        when(tokenService.extractUserId(TOKEN)).thenReturn(ADMIN_ID);
        when(adminService.cancelSuspension(USER_ID, ADMIN_ID)).thenReturn(Response.error("User does not exist"));

        ResponseEntity<?> response = adminController.cancelSuspension(TOKEN, USER_ID);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("User does not exist", ((Map<?, ?>) response.getBody()).get("error"));
    }

    @Test
    void getSuspensionHistory_Success_Returns200() {
        List<Suspension> suspensions = List.of(
                new Suspension(USER_ID, LocalDateTime.now()),
                new Suspension("other-user", LocalDateTime.now())
        );
        when(tokenService.extractUserId(TOKEN)).thenReturn(ADMIN_ID);
        when(adminService.getAllSuspensions(ADMIN_ID)).thenReturn(Response.success(suspensions));

        ResponseEntity<?> response = adminController.getSuspensionHistory(TOKEN);

        assertEquals(200, response.getStatusCode().value());
        assertSame(suspensions, response.getBody());
    }

    @Test
    void getSuspensionHistory_Failure_Returns400() {
        when(tokenService.extractUserId(TOKEN)).thenReturn(ADMIN_ID);
        when(adminService.getAllSuspensions(ADMIN_ID)).thenReturn(Response.error("Admin does not exist"));

        ResponseEntity<?> response = adminController.getSuspensionHistory(TOKEN);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Admin does not exist", ((Map<?, ?>) response.getBody()).get("error"));
    }

    @Test
    void closeCompany_Success_Returns200() {
        when(tokenService.extractUserId(TOKEN)).thenReturn(ADMIN_ID);
        when(adminService.CloseCompany("Acme", ADMIN_ID)).thenReturn(Response.success("success"));

        ResponseEntity<?> response = adminController.closeCompany(TOKEN, "Acme");

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Company closed successfully", ((Map<?, ?>) response.getBody()).get("message"));
    }

    @Test
    void closeCompany_Failure_Returns400() {
        when(tokenService.extractUserId(TOKEN)).thenReturn(ADMIN_ID);
        when(adminService.CloseCompany("Acme", ADMIN_ID)).thenReturn(Response.error("Admin does not exist"));

        ResponseEntity<?> response = adminController.closeCompany(TOKEN, "Acme");

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Admin does not exist", ((Map<?, ?>) response.getBody()).get("error"));
    }

    @Test
    void removeUser_Success_Returns200() {
        when(tokenService.extractUserId(TOKEN)).thenReturn(ADMIN_ID);
        when(adminService.removeUser(USER_ID, ADMIN_ID)).thenReturn(Response.success("success"));

        ResponseEntity<?> response = adminController.removeUser(TOKEN, USER_ID);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("User removed successfully", ((Map<?, ?>) response.getBody()).get("message"));
    }

    @Test
    void removeUser_Failure_Returns400() {
        when(tokenService.extractUserId(TOKEN)).thenReturn(ADMIN_ID);
        when(adminService.removeUser(USER_ID, ADMIN_ID)).thenReturn(Response.error("Admin does not exist"));

        ResponseEntity<?> response = adminController.removeUser(TOKEN, USER_ID);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Admin does not exist", ((Map<?, ?>) response.getBody()).get("error"));
    }

    @Test
    void getQueue_Success_Returns200() {
        QueueEntry entry = new QueueEntry(USER_ID);
        when(tokenService.extractUserId(TOKEN)).thenReturn(ADMIN_ID);
        when(queueService.getQueueForAdmin(ADMIN_ID, EVENT_ID)).thenReturn(Response.success(List.of(entry)));

        ResponseEntity<?> response = adminController.getQueue(TOKEN, EVENT_ID);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(List.of(entry), response.getBody());
    }

    @Test
    void getQueue_Failure_Returns400() {
        when(tokenService.extractUserId(TOKEN)).thenReturn(ADMIN_ID);
        when(queueService.getQueueForAdmin(ADMIN_ID, EVENT_ID)).thenReturn(Response.error("Admin does not exist"));

        ResponseEntity<?> response = adminController.getQueue(TOKEN, EVENT_ID);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Admin does not exist", ((Map<?, ?>) response.getBody()).get("error"));
    }

    @Test
    void adjustQueue_ClearAction_Success_Returns200() {
        when(tokenService.extractUserId(TOKEN)).thenReturn(ADMIN_ID);
        when(queueService.clearQueueForAdmin(ADMIN_ID, EVENT_ID)).thenReturn(Response.success("Queue cleared for event: " + EVENT_ID));

        QueueAdjustRequest req = new QueueAdjustRequest();
        req.setClear(true);
        ResponseEntity<?> response = adminController.adjustQueue(TOKEN, EVENT_ID, req);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Queue cleared for event: " + EVENT_ID, ((Map<?, ?>) response.getBody()).get("message"));
    }

    @Test
    void adjustQueue_SetFlowRate_Success_Returns200() {
        when(tokenService.extractUserId(TOKEN)).thenReturn(ADMIN_ID);
        when(queueService.setFlowRate(ADMIN_ID, EVENT_ID, 50)).thenReturn(Response.success("Flow rate set to 50 for event: " + EVENT_ID));

        QueueAdjustRequest req = new QueueAdjustRequest();
        req.setMaxActiveUsers(50);
        ResponseEntity<?> response = adminController.adjustQueue(TOKEN, EVENT_ID, req);

        assertEquals(200, response.getStatusCode().value());
        assertTrue(((Map<?, ?>) response.getBody()).get("message").toString().contains("50"));
    }

    @Test
    void adjustQueue_NoAction_Returns400() {
        when(tokenService.extractUserId(TOKEN)).thenReturn(ADMIN_ID);

        QueueAdjustRequest req = new QueueAdjustRequest();
        ResponseEntity<?> response = adminController.adjustQueue(TOKEN, EVENT_ID, req);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Must specify 'clear' or 'maxActiveUsers'", ((Map<?, ?>) response.getBody()).get("error"));
    }

    @Test
    void adjustQueue_Failure_Returns400() {
        when(tokenService.extractUserId(TOKEN)).thenReturn(ADMIN_ID);
        when(queueService.clearQueueForAdmin(ADMIN_ID, EVENT_ID)).thenReturn(Response.error("Admin does not exist"));

        QueueAdjustRequest req = new QueueAdjustRequest();
        req.setClear(true);
        ResponseEntity<?> response = adminController.adjustQueue(TOKEN, EVENT_ID, req);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Admin does not exist", ((Map<?, ?>) response.getBody()).get("error"));
    }

    @Test
    void getPurchaseHistory_NoFilters_Returns200WithAllOrders() {
        List<PurchaseOrderDTO> orders = List.of(
                new PurchaseOrderDTO("o1", "buyer1", "Acme", "event-1", List.of()),
                new PurchaseOrderDTO("o2", "buyer2", "Acme", "event-2", List.of())
        );
        when(tokenService.extractUserId(TOKEN)).thenReturn(ADMIN_ID);
        when(adminService.getPurchaseHistory(ADMIN_ID, null, null, null)).thenReturn(Response.success(orders));

        ResponseEntity<?> response = adminController.getPurchaseHistory(TOKEN, null, null, null);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(orders, response.getBody());
    }

    @Test
    void getPurchaseHistory_Failure_Returns400() {
        when(tokenService.extractUserId(TOKEN)).thenReturn(ADMIN_ID);
        when(adminService.getPurchaseHistory(ADMIN_ID, null, null, null)).thenReturn(Response.error("Admin does not exist"));

        ResponseEntity<?> response = adminController.getPurchaseHistory(TOKEN, null, null, null);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Admin does not exist", ((Map<?, ?>) response.getBody()).get("error"));
    }

    @Test
    void getSystemAnalytics_Returns200WithCounts() {
        Map<String, Long> analytics = Map.of("totalPurchases", 10L, "activeOrders", 3L);
        when(tokenService.extractUserId(TOKEN)).thenReturn(ADMIN_ID);
        when(adminService.getSystemAnalytics(ADMIN_ID)).thenReturn(Response.success(analytics));

        ResponseEntity<?> response = adminController.getSystemAnalytics(TOKEN);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(analytics, response.getBody());
    }

    @Test
    void getComplaints_Returns200WithNotifications() {
        List<Notification> complaints = List.of(
                new Notification("c1", "SYSTEM_ADMIN", "Complaint from alice: bad event"),
                new Notification("c2", "SYSTEM_ADMIN", "Complaint from bob: wrong ticket")
        );
        when(notificationRepository.getAll("SYSTEM_ADMIN")).thenReturn(complaints);

        ResponseEntity<?> response = adminController.getComplaints(TOKEN);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(complaints, response.getBody());
    }

    @Test
    void sendMessageToUser_Success_Returns200() {
        when(tokenService.extractUserId(TOKEN)).thenReturn(ADMIN_ID);
        when(adminService.sendMessageToUser(ADMIN_ID, USER_ID, "Your account is fine")).thenReturn(Response.success("success"));

        AdminMessageRequest req = new AdminMessageRequest();
        req.setMessage("Your account is fine");
        ResponseEntity<?> response = adminController.sendMessageToUser(TOKEN, USER_ID, req);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Message sent successfully", ((Map<?, ?>) response.getBody()).get("message"));
    }

    @Test
    void sendMessageToUser_Failure_Returns400() {
        when(tokenService.extractUserId(TOKEN)).thenReturn(ADMIN_ID);
        when(adminService.sendMessageToUser(ADMIN_ID, USER_ID, "msg")).thenReturn(Response.error("Admin does not exist"));

        AdminMessageRequest req = new AdminMessageRequest();
        req.setMessage("msg");
        ResponseEntity<?> response = adminController.sendMessageToUser(TOKEN, USER_ID, req);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Admin does not exist", ((Map<?, ?>) response.getBody()).get("error"));
    }
}
