package Appliction;

import com.ticketing.ticketapp.Appliction.IPaymentService;
import com.ticketing.ticketapp.Appliction.ISupplyService;
import com.ticketing.ticketapp.Appliction.Response;
import com.ticketing.ticketapp.Appliction.SystemService;
import com.ticketing.ticketapp.Appliction.UserService;
import com.ticketing.ticketapp.Domain.AdminAggregate.iAdminRepository;
import com.ticketing.ticketapp.Infastructure.TokenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SystemServiceTest {

    @Mock private UserService userService;
    @Mock private TokenService tokenService;
    @Mock private iAdminRepository adminRepository;
    @Mock private IPaymentService paymentService;
    @Mock private ISupplyService supplyService;

    @InjectMocks private SystemService systemService;

    private static final String GUEST_TOKEN     = "guest-token";
    private static final String LOGIN_TOKEN     = "login-guest-token";
    private static final String ADMIN_JWT       = "admin-jwt";
    private static final String ADMIN_TOKEN     = "admin-token";
    private static final String ADMIN_USER_ID   = "admin-uuid-123";

    // ── helpers ──────────────────────────────────────────────────────────────

    private void stubSuccessfulInit() {
        when(paymentService.processPayment(anyString(), anyDouble())).thenReturn(true);
        when(supplyService.supplyToEmail(anyString(), anyString())).thenReturn(true);
        when(userService.register(eq(GUEST_TOKEN), anyString(), anyString(), anyInt(), anyString()))
                .thenReturn(Response.success("registered"));
        when(tokenService.generateGuestToken()).thenReturn(LOGIN_TOKEN);
        when(userService.login(eq(LOGIN_TOKEN), anyString(), anyString()))
                .thenReturn(Response.success(ADMIN_JWT));
        when(tokenService.extractUserId(ADMIN_JWT)).thenReturn(ADMIN_USER_ID);
    }

    private void performInit() {
        stubSuccessfulInit();
        systemService.initSystem(GUEST_TOKEN, "admin", "pass123", 30, "admin@test.com");
    }

    private void performOpen() {
        when(tokenService.extractUserId(ADMIN_TOKEN)).thenReturn(ADMIN_USER_ID);
        when(adminRepository.isAdmin(ADMIN_USER_ID)).thenReturn(true);
        systemService.openSystem(ADMIN_TOKEN);
    }

    // ── initSystem ───────────────────────────────────────────────────────────

    @Test
    void initSystem_Success_SetsInitializedAndRegistersAdmin() {
        stubSuccessfulInit();

        Response<String> result = systemService.initSystem(GUEST_TOKEN, "admin", "pass123", 30, "admin@test.com");

        assertTrue(result.isSuccess());
        assertTrue(result.getData().contains("admin"));
        assertTrue(systemService.isInitialized());
        verify(adminRepository).addAdmin(ADMIN_USER_ID);
    }

    @Test
    void initSystem_AlreadyInitialized_ReturnsError() {
        performInit();

        Response<String> result = systemService.initSystem(GUEST_TOKEN, "admin2", "pass", 25, "a@b.com");

        assertFalse(result.isSuccess());
        assertEquals("System already initialized", result.getMessage());
        // admin repo must not be called a second time
        verify(adminRepository, times(1)).addAdmin(any());
    }

    @Test
    void initSystem_PaymentServiceUnavailable_ReturnsError() {
        when(paymentService.processPayment(anyString(), anyDouble())).thenReturn(false);
        when(supplyService.supplyToEmail(anyString(), anyString())).thenReturn(true);

        Response<String> result = systemService.initSystem(GUEST_TOKEN, "admin", "pass", 30, "a@b.com");

        assertFalse(result.isSuccess());
        assertEquals("External services unavailable", result.getMessage());
        assertFalse(systemService.isInitialized());
        verify(adminRepository, never()).addAdmin(any());
    }

    @Test
    void initSystem_SupplyServiceUnavailable_ReturnsError() {
        when(paymentService.processPayment(anyString(), anyDouble())).thenReturn(true);
        when(supplyService.supplyToEmail(anyString(), anyString())).thenReturn(false);

        Response<String> result = systemService.initSystem(GUEST_TOKEN, "admin", "pass", 30, "a@b.com");

        assertFalse(result.isSuccess());
        assertEquals("External services unavailable", result.getMessage());
        assertFalse(systemService.isInitialized());
        verify(adminRepository, never()).addAdmin(any());
    }

    @Test
    void initSystem_AdminRegistrationFails_ReturnsError() {
        when(paymentService.processPayment(anyString(), anyDouble())).thenReturn(true);
        when(supplyService.supplyToEmail(anyString(), anyString())).thenReturn(true);
        when(userService.register(anyString(), anyString(), anyString(), anyInt(), anyString()))
                .thenReturn(Response.error("Username already taken"));

        Response<String> result = systemService.initSystem(GUEST_TOKEN, "admin", "pass", 30, "a@b.com");

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Failed to create admin"));
        assertFalse(systemService.isInitialized());
        verify(adminRepository, never()).addAdmin(any());
    }

    @Test
    void initSystem_AdminLoginFails_ReturnsError() {
        when(paymentService.processPayment(anyString(), anyDouble())).thenReturn(true);
        when(supplyService.supplyToEmail(anyString(), anyString())).thenReturn(true);
        when(userService.register(eq(GUEST_TOKEN), anyString(), anyString(), anyInt(), anyString()))
                .thenReturn(Response.success("registered"));
        when(tokenService.generateGuestToken()).thenReturn(LOGIN_TOKEN);
        when(userService.login(eq(LOGIN_TOKEN), anyString(), anyString()))
                .thenReturn(Response.error("Login failed"));

        Response<String> result = systemService.initSystem(GUEST_TOKEN, "admin", "pass", 30, "a@b.com");

        assertFalse(result.isSuccess());
        assertEquals("Failed to authenticate admin after creation", result.getMessage());
        assertFalse(systemService.isInitialized());
        verify(adminRepository, never()).addAdmin(any());
    }

    // ── openSystem ───────────────────────────────────────────────────────────

    @Test
    void openSystem_NotInitialized_ReturnsError() {
        Response<String> result = systemService.openSystem(ADMIN_TOKEN);

        assertFalse(result.isSuccess());
        assertEquals("System must be initialized before opening", result.getMessage());
        assertFalse(systemService.isOpen());
    }

    @Test
    void openSystem_NotAdmin_ReturnsError() {
        performInit();
        when(tokenService.extractUserId(ADMIN_TOKEN)).thenReturn("other-user-id");
        when(adminRepository.isAdmin("other-user-id")).thenReturn(false);

        Response<String> result = systemService.openSystem(ADMIN_TOKEN);

        assertFalse(result.isSuccess());
        assertEquals("Unauthorized: admin access required to open the system", result.getMessage());
        assertFalse(systemService.isOpen());
    }

    @Test
    void openSystem_Success_SetsOpenState() {
        performInit();
        when(tokenService.extractUserId(ADMIN_TOKEN)).thenReturn(ADMIN_USER_ID);
        when(adminRepository.isAdmin(ADMIN_USER_ID)).thenReturn(true);

        Response<String> result = systemService.openSystem(ADMIN_TOKEN);

        assertTrue(result.isSuccess());
        assertEquals("System is now open", result.getData());
        assertTrue(systemService.isOpen());
    }

    @Test
    void openSystem_AlreadyOpen_ReturnsError() {
        performInit();
        performOpen();

        Response<String> result = systemService.openSystem(ADMIN_TOKEN);

        assertFalse(result.isSuccess());
        assertEquals("System is already open", result.getMessage());
    }

    // ── getters ──────────────────────────────────────────────────────────────

    @Test
    void isInitialized_DefaultsFalse() {
        assertFalse(systemService.isInitialized());
    }

    @Test
    void isOpen_DefaultsFalse() {
        assertFalse(systemService.isOpen());
    }
}
