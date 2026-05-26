package com.ticketing.ticketapp.Controllers;

import com.ticketing.ticketapp.Appliction.UserService;
import com.ticketing.ticketapp.Appliction.Response;
import com.ticketing.ticketapp.Infastructure.TokenService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final TokenService tokenService;

    public AuthController(UserService userService, TokenService tokenService) {
        this.userService = userService;
        this.tokenService = tokenService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(
            @RequestAttribute("cleanToken") String token,
            @RequestBody RegisterRequest request) {
        
        Response<String> response = userService.register(
                token, 
                request.getUsername(), 
                request.getPassword(), 
                request.getAge(), 
                request.getEmail()
        );

        if (response.isSuccess()) {
            return ResponseEntity.ok(Map.of("message", response.getData()));
        }
        return ResponseEntity.status(400).body(Map.of("error", response.getMessage()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestAttribute("cleanToken") String token,
            @RequestBody LoginRequest request) {
        
        Response<String> response = userService.login(
                token, 
                request.getUsername(), 
                request.getPassword()
        );

        if (response.isSuccess()) {
            return ResponseEntity.ok(Map.of("token", response.getData()));
        }
        return ResponseEntity.status(401).body(Map.of("error", response.getMessage()));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @RequestAttribute("cleanToken") String token) {
        
        Response<String> response = userService.logout(token);

        if (response.isSuccess()) {
            return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
        }
        return ResponseEntity.status(400).body(Map.of("error", response.getMessage()));
    }

    @GetMapping("/guest")
    public ResponseEntity<?> getGuestToken() {
        String guestToken = tokenService.generateGuestToken(); 
        
        return ResponseEntity.ok(Map.of(
            "message", "Guest token generated",
            "token", guestToken
        ));
    }

    @GetMapping("/my-companies")
    public ResponseEntity<?> getMyCompanies(@RequestHeader("Authorization") String token) {
        String cleanToken = token.replace("Bearer ", "");
        Response<List<String>> response = userService.getUserCompanies(cleanToken);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response.getData());
        }
        return ResponseEntity.status(400).body(Map.of("error", response.getMessage()));
    }

    @PostMapping("/switch-company")
    public ResponseEntity<?> switchCompanyContext(
            @RequestHeader("Authorization") String token, 
            @RequestBody Map<String, String> body) {
        
        String cleanToken = token.replace("Bearer ", "");
        String companyName = body.get("companyName");
        
        Response<String> response = userService.switchCompanyContext(cleanToken, companyName);
        if (response.isSuccess()) {
            return ResponseEntity.ok(Map.of("token", response.getData())); // מחזיר את הטוקן המשודרג
        }
        return ResponseEntity.status(401).body(Map.of("error", response.getMessage()));
    }

    
    public static class RegisterRequest {
        private String username;
        private String password;
        private int age;
        private String email;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        
        public int getAge() { return age; }
        public void setAge(int age) { this.age = age; }
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    public static class LoginRequest {
        private String username;
        private String password;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
}