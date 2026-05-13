package com.ticketing.ticketapp.Controllers;
import com.ticketing.ticketapp.Infastructure.TokenService;
import com.ticketing.ticketapp.Appliction.UserService;
import com.ticketing.ticketapp.Appliction.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {
    private final TokenService tokenService;
    private final UserService userService;

    public UserController(UserService userService, TokenService tokenService) {
        this.userService = userService;
        this.tokenService = tokenService;
    }

    // מימוש ה-Issue: Member Profile & Support
    @GetMapping("/{userId}/profile")
    public ResponseEntity<Response<Object>> getUserProfile(@PathVariable String userId, @RequestHeader("Authorization") String token) {
        // הערת מנטור: בגרסה 2 אנחנו צריכים אבטחה. ה-TokenInterceptor שלכם כבר קיים בקוד.
        // כאן ה-Service חייב לוודא שה-Token שייך ל-userId שמבקשים את הפרופיל שלו.
        // אסור ש-Guest ימשוך נתונים של Member אחר!
        
        Response<UserDTO> response = userService.getUserInfo(userId); // עליך לוודא שהמתודה הזו קיימת ב-UserService ומחזירה UserDTO מתאים
        
        if (response.isError()) {
            return ResponseEntity.status(403).body(response); // Forbidden אם אין הרשאה
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/guest")
    public ResponseEntity<Response<String>> generateGuestToken() {
        // אתה קורא ל-UserService או TokenService שייצר Token ייעודי לאורח
        // למשל, Token שה-Subject שלו הוא UUID שנוצר באותו רגע (לדוגמה: "guest-8f7d6a5")
        String guestToken = tokenService.generateGuestToken(); 
        
        return ResponseEntity.ok(new Response<>(guestToken)); // מחזיר את הטוקן ל-UI
    }
}