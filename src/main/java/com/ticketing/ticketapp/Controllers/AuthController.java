package com.ticketing.ticketapp.Controllers;

import com.ticketing.ticketapp.Appliction.IAuth;
import com.ticketing.ticketapp.Appliction.Response;
import com.ticketing.ticketapp.Infastructure.TokenService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final IAuth authService;
    private final TokenService tokenService;

    public AuthController(IAuth authService, TokenService tokenService) {
        this.authService = authService;
        this.tokenService = tokenService;
    }

    private String getValidToken(String authHeader) {
        String token = authHeader;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }

        // Convert the hardcoded frontend token to a real backend JWT so UserService
        // validation passes
        if ("guest-temporary-token".equals(token) || token == null || token.isEmpty()) {
            return tokenService.generateGuestToken();
        }
        return token;
    }

    @PostMapping("/login")
    public ResponseEntity<Response<String>> login(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody LoginRequest request) {

        String token = getValidToken(authHeader);
        Response<String> response = authService.login(token, request.getUsername(), request.getPassword());

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

    @PostMapping("/register")
    public ResponseEntity<Response<String>> register(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody RegisterRequest request) {

        String token = getValidToken(authHeader);
        Response<String> response = authService.register(
                token,
                request.getUsername(),
                request.getPassword(),
                request.getAge(),
                request.getEmail());

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    // DTOs for the requests
    public static class LoginRequest {
        private String username;
        private String password;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static class RegisterRequest {
        private String username;
        private String password;
        private String email;
        private int age;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }
    }
}