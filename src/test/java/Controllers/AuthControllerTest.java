package Controllers;

import com.ticketing.ticketapp.Appliction.Response;
import com.ticketing.ticketapp.Appliction.UserService;
import com.ticketing.ticketapp.Controllers.AuthController;
import com.ticketing.ticketapp.Infastructure.TokenService;
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
class AuthControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private TokenService tokenService;

    @InjectMocks
    private AuthController authController;

    private static final String TOKEN = "test-token";

    @Test
    void register_Success_Returns200() {
        when(userService.register(TOKEN, "alice", "pass123", 25, "alice@test.com"))
                .thenReturn(Response.success("User registered successfully"));

        AuthController.RegisterRequest request = new AuthController.RegisterRequest();
        request.setUsername("alice");
        request.setPassword("pass123");
        request.setAge(25);
        request.setEmail("alice@test.com");

        ResponseEntity<?> response = authController.register(TOKEN, request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("User registered successfully", ((Map<?, ?>) response.getBody()).get("message"));
    }

    @Test
    void register_ServiceFailure_Returns400() {
        when(userService.register(anyString(), anyString(), anyString(), anyInt(), anyString()))
                .thenReturn(Response.error("Username already taken"));

        AuthController.RegisterRequest request = new AuthController.RegisterRequest();
        request.setUsername("alice");
        request.setPassword("pass123");
        request.setAge(25);
        request.setEmail("alice@test.com");

        ResponseEntity<?> response = authController.register(TOKEN, request);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Username already taken", ((Map<?, ?>) response.getBody()).get("error"));
    }

    @Test
    void login_Success_Returns200WithToken() {
        when(userService.login(TOKEN, "alice", "pass123"))
                .thenReturn(Response.success("jwt-token-abc"));

        AuthController.LoginRequest request = new AuthController.LoginRequest();
        request.setUsername("alice");
        request.setPassword("pass123");

        ResponseEntity<?> response = authController.login(TOKEN, request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("jwt-token-abc", ((Map<?, ?>) response.getBody()).get("token"));
    }

    @Test
    void login_InvalidCredentials_Returns401() {
        when(userService.login(anyString(), anyString(), anyString()))
                .thenReturn(Response.error("Invalid credentials"));

        AuthController.LoginRequest request = new AuthController.LoginRequest();
        request.setUsername("alice");
        request.setPassword("wrong");

        ResponseEntity<?> response = authController.login(TOKEN, request);

        assertEquals(401, response.getStatusCode().value());
        assertEquals("Invalid credentials", ((Map<?, ?>) response.getBody()).get("error"));
    }

    @Test
    void logout_Success_Returns200() {
        when(userService.logout(TOKEN)).thenReturn(Response.success("ok"));

        ResponseEntity<?> response = authController.logout(TOKEN);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Logged out successfully", ((Map<?, ?>) response.getBody()).get("message"));
    }

    @Test
    void logout_Failure_Returns400() {
        when(userService.logout(TOKEN)).thenReturn(Response.error("Already logged out"));

        ResponseEntity<?> response = authController.logout(TOKEN);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Already logged out", ((Map<?, ?>) response.getBody()).get("error"));
    }

    @Test
    void getGuestToken_Returns200WithToken() {
        when(tokenService.generateGuestToken()).thenReturn("guest-xyz-token");

        ResponseEntity<?> response = authController.getGuestToken();

        assertEquals(200, response.getStatusCode().value());
        assertEquals("guest-xyz-token", ((Map<?, ?>) response.getBody()).get("token"));
        assertEquals("Guest token generated", ((Map<?, ?>) response.getBody()).get("message"));
    }
}
