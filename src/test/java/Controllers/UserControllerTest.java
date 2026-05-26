package Controllers;

import com.ticketing.ticketapp.Appliction.*;
import com.ticketing.ticketapp.Controllers.UserController;
import com.ticketing.ticketapp.Domain.PurchasedOrderAggregate.PurchaseOrderDTO;
import com.ticketing.ticketapp.Domain.User.UserDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private PurchasedService purchasedService;

    @Mock
    private QueueService queueService;

    @InjectMocks
    private UserController userController;

    private static final String TOKEN = "test-token";

    @Test
    void getProfile_Success_Returns200() {
        UserDTO dto = new UserDTO();
        dto.setName("alice");
        dto.setAge(25);
        when(userService.getUserProfile(TOKEN)).thenReturn(Response.success(dto));

        ResponseEntity<?> response = userController.getProfile(TOKEN);

        assertEquals(200, response.getStatusCode().value());
        assertSame(dto, response.getBody());
    }

    @Test
    void getProfile_Failure_Returns400() {
        when(userService.getUserProfile(TOKEN)).thenReturn(Response.error("Invalid token"));

        ResponseEntity<?> response = userController.getProfile(TOKEN);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Invalid token", ((Map<?, ?>) response.getBody()).get("error"));
    }

    @Test
    void updateProfile_Success_Returns200() {
        UserDTO dto = new UserDTO();
        when(userService.updateUserProfile(TOKEN, dto)).thenReturn(Response.success("ok"));

        ResponseEntity<?> response = userController.updateProfile(TOKEN, dto);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Profile updated successfully.", ((Map<?, ?>) response.getBody()).get("message"));
    }

    @Test
    void updateProfile_Failure_Returns400() {
        UserDTO dto = new UserDTO();
        when(userService.updateUserProfile(TOKEN, dto)).thenReturn(Response.error("Update failed"));

        ResponseEntity<?> response = userController.updateProfile(TOKEN, dto);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Update failed", ((Map<?, ?>) response.getBody()).get("error"));
    }

    @Test
    void getPurchaseHistory_Success_Returns200() {
        PurchaseOrderDTO order = new PurchaseOrderDTO("o1", "alice", "CompanyA", "EventX", List.of());
        List<PurchaseOrderDTO> orders = List.of(order);
        when(purchasedService.getUserTransaction(TOKEN)).thenReturn(Response.success(orders));

        ResponseEntity<?> response = userController.getPurchaseHistory(TOKEN);

        assertEquals(200, response.getStatusCode().value());
        assertSame(orders, response.getBody());
    }

    @Test
    void getPurchaseHistory_Failure_Returns400() {
        when(purchasedService.getUserTransaction(TOKEN)).thenReturn(Response.error("No history found"));

        ResponseEntity<?> response = userController.getPurchaseHistory(TOKEN);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("No history found", ((Map<?, ?>) response.getBody()).get("error"));
    }

    @Test
    void getNotifications_Success_Returns200() {
        List<String> notes = List.of("Note 1", "Note 2");
        when(userService.getUserNotifications(TOKEN)).thenReturn(Response.success(notes));

        ResponseEntity<?> response = userController.getMyNotifications(TOKEN);

        assertEquals(200, response.getStatusCode().value());
        assertSame(notes, response.getBody());
    }

    @Test
    void getNotifications_Failure_Returns400() {
        when(userService.getUserNotifications(TOKEN)).thenReturn(Response.error("Not logged in"));

        ResponseEntity<?> response = userController.getMyNotifications(TOKEN);

        assertEquals(400, response.getStatusCode().value());
    }
}
