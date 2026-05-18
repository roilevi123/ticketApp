package com.ticketing.ticketapp.Controllers;

import com.ticketing.ticketapp.Domain.User.UserDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    // In-memory mock profile (survives for the lifetime of the Spring context)
    private String name  = "Alexander Hamilton";
    private String id    = "U88102934";
    private String email = "a.hamilton@university.edu";

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(
            @RequestHeader("Authorization") String token) {

        UserDTO profile = new UserDTO();
        profile.setName(name);
        profile.setID(id);
        profile.setEmail(email);
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
            @RequestHeader("Authorization") String token,
            @RequestBody UserDTO request) {

        this.name  = request.getName();
        this.id    = request.getID();
        this.email = request.getEmail();

        return ResponseEntity.ok(Map.of("message", "Profile updated successfully."));
    }
}
