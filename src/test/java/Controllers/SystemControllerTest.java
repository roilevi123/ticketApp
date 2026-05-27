package Controllers;

import com.ticketing.ticketapp.Appliction.IPaymentService;
import com.ticketing.ticketapp.Appliction.ISupplyService;
import com.ticketing.ticketapp.Appliction.Response;
import com.ticketing.ticketapp.Appliction.SystemService;
import com.ticketing.ticketapp.Controllers.SystemController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SystemControllerTest {

    @Mock
    private SystemService systemService;

    @Mock
    private IPaymentService paymentService;

    @Mock
    private ISupplyService supplyService;

    @InjectMocks
    private SystemController systemController;

    private static final String TOKEN = "test-token";

    // ── /init ────────────────────────────────────────────────────────────────

    @Test
    void init_Success_Returns200() {
        when(systemService.initSystem(TOKEN, "admin", "pass123", 30, "admin@test.com"))
                .thenReturn(Response.success("System initialized successfully. Admin 'admin' created."));

        SystemController.InitRequest request = new SystemController.InitRequest();
        request.setAdminUsername("admin");
        request.setAdminPassword("pass123");
        request.setAdminAge(30);
        request.setAdminEmail("admin@test.com");

        ResponseEntity<?> response = systemController.initSystem(TOKEN, request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("System initialized successfully. Admin 'admin' created.",
                ((Map<?, ?>) response.getBody()).get("message"));
    }

    @Test
    void init_AlreadyInitialized_Returns400() {
        when(systemService.initSystem(anyString(), anyString(), anyString(), anyInt(), anyString()))
                .thenReturn(Response.error("System already initialized"));

        SystemController.InitRequest request = new SystemController.InitRequest();
        request.setAdminUsername("admin");
        request.setAdminPassword("pass123");
        request.setAdminAge(30);
        request.setAdminEmail("admin@test.com");

        ResponseEntity<?> response = systemController.initSystem(TOKEN, request);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("System already initialized", ((Map<?, ?>) response.getBody()).get("error"));
    }

    @Test
    void init_ExternalServiceUnavailable_Returns400() {
        when(systemService.initSystem(anyString(), anyString(), anyString(), anyInt(), anyString()))
                .thenReturn(Response.error("External services unavailable"));

        SystemController.InitRequest request = new SystemController.InitRequest();
        request.setAdminUsername("admin");
        request.setAdminPassword("pass123");
        request.setAdminAge(30);
        request.setAdminEmail("admin@test.com");

        ResponseEntity<?> response = systemController.initSystem(TOKEN, request);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("External services unavailable", ((Map<?, ?>) response.getBody()).get("error"));
    }

    // ── /open ────────────────────────────────────────────────────────────────

    @Test
    void open_Success_Returns200() {
        when(systemService.openSystem(TOKEN)).thenReturn(Response.success("System is now open"));

        ResponseEntity<?> response = systemController.openSystem(TOKEN);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("System is now open", ((Map<?, ?>) response.getBody()).get("message"));
    }

    @Test
    void open_NotInitialized_Returns400() {
        when(systemService.openSystem(TOKEN))
                .thenReturn(Response.error("System must be initialized before opening"));

        ResponseEntity<?> response = systemController.openSystem(TOKEN);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("System must be initialized before opening",
                ((Map<?, ?>) response.getBody()).get("error"));
    }

    @Test
    void open_NotAdmin_Returns400() {
        when(systemService.openSystem(TOKEN))
                .thenReturn(Response.error("Unauthorized: admin access required to open the system"));

        ResponseEntity<?> response = systemController.openSystem(TOKEN);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Unauthorized: admin access required to open the system",
                ((Map<?, ?>) response.getBody()).get("error"));
    }

    @Test
    void open_AlreadyOpen_Returns400() {
        when(systemService.openSystem(TOKEN)).thenReturn(Response.error("System is already open"));

        ResponseEntity<?> response = systemController.openSystem(TOKEN);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("System is already open", ((Map<?, ?>) response.getBody()).get("error"));
    }

    // ── /external/payment ────────────────────────────────────────────────────

    @Test
    void processPayment_Approved_Returns200WithTransactionId() {
        when(paymentService.processPayment("4111111111111111", 99.99)).thenReturn(true);

        SystemController.PaymentRequest request = new SystemController.PaymentRequest();
        request.setCreditCardDetails("4111111111111111");
        request.setAmount(99.99);

        ResponseEntity<?> response = systemController.processPayment(TOKEN, request);

        assertEquals(200, response.getStatusCode().value());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals(true, body.get("success"));
        assertNotNull(body.get("transactionId"));
        assertTrue(body.get("transactionId").toString().startsWith("TXN-"));
    }

    @Test
    void processPayment_Declined_Returns400() {
        when(paymentService.processPayment(anyString(), anyDouble())).thenReturn(false);

        SystemController.PaymentRequest request = new SystemController.PaymentRequest();
        request.setCreditCardDetails("0000000000000000");
        request.setAmount(50.0);

        ResponseEntity<?> response = systemController.processPayment(TOKEN, request);

        assertEquals(400, response.getStatusCode().value());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals(false, body.get("success"));
        assertEquals("Payment declined", body.get("error"));
    }

    // ── /external/supply ─────────────────────────────────────────────────────

    @Test
    void supplyTicket_Success_Returns200() {
        when(supplyService.supplyToEmail("user@test.com", "Your ticket")).thenReturn(true);

        SystemController.SupplyRequest request = new SystemController.SupplyRequest();
        request.setEmailAddress("user@test.com");
        request.setContent("Your ticket");

        ResponseEntity<?> response = systemController.supplyTicket(TOKEN, request);

        assertEquals(200, response.getStatusCode().value());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals(true, body.get("success"));
        assertEquals("Ticket delivered to user@test.com", body.get("message"));
    }

    @Test
    void supplyTicket_Failure_Returns400() {
        when(supplyService.supplyToEmail(anyString(), anyString())).thenReturn(false);

        SystemController.SupplyRequest request = new SystemController.SupplyRequest();
        request.setEmailAddress("user@test.com");
        request.setContent("Your ticket");

        ResponseEntity<?> response = systemController.supplyTicket(TOKEN, request);

        assertEquals(400, response.getStatusCode().value());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals(false, body.get("success"));
        assertEquals("Supply failed", body.get("error"));
    }
}
