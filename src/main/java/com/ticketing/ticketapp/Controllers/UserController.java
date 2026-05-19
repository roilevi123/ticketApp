package com.ticketing.ticketapp.Controllers;

import com.ticketing.ticketapp.Appliction.IAuth;
import com.ticketing.ticketapp.Appliction.Response;
import com.ticketing.ticketapp.Domain.User.UserDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final IAuth authService;

    // In-memory mock profile (survives for the lifetime of the Spring context)
    private String name = "Alexander Hamilton";
    private String id = "U88102934";
    private String email = "a.hamilton@university.edu";

    public UserController(IAuth authService) {
        this.authService = authService;
    }

    private String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return authHeader;
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(
            @RequestHeader("Authorization") String authHeader) {

        String token = extractToken(authHeader);
        Response<UserDTO> profileResponse = authService.getUserProfile(token);

        if (profileResponse.isSuccess()) {
            return ResponseEntity.ok(profileResponse.getData());
        }

        // If the user isn't found (e.g. backend was restarted and the memory DB got
        // wiped)
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", profileResponse.getMessage()));
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
            @RequestHeader("Authorization") String token,
            @RequestBody UserDTO request) {

        this.name = request.getName();
        this.id = request.getID();
        this.email = request.getEmail();

        return ResponseEntity.ok(Map.of("message", "Profile updated successfully."));
    }
}
