package Infastructure;

import com.ticketing.ticketapp.Infastructure.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TokenServiceTest {

    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new TokenService();
    }

    @Test
    void generateMemberToken_CreatesValidToken() {
        String token = tokenService.generateMemberToken("user-id-1", "alice");
        assertNotNull(token);
        assertTrue(tokenService.validateToken(token));
    }

    @Test
    void generateGuestToken_CreatesValidToken() {
        String token = tokenService.generateGuestToken();
        assertNotNull(token);
        assertTrue(tokenService.validateToken(token));
    }

    @Test
    void validateToken_InvalidString_ReturnsFalse() {
        assertFalse(tokenService.validateToken("not.a.jwt"));
    }

    @Test
    void validateToken_BlacklistedToken_ReturnsFalse() {
        String token = tokenService.generateMemberToken("uid", "bob");
        tokenService.addBlacklistToken(token);
        assertFalse(tokenService.validateToken(token));
    }

    @Test
    void validateToken_BannedUser_ReturnsFalse() {
        String userId = "banned-user";
        String token = tokenService.generateMemberToken(userId, "evil");
        tokenService.banUser(userId);
        assertFalse(tokenService.validateToken(token));
    }

    @Test
    void unbanUser_AllowsValidationAfterBan() {
        String userId = "user42";
        String token = tokenService.generateMemberToken(userId, "user42");
        tokenService.banUser(userId);
        assertFalse(tokenService.validateToken(token));
        tokenService.unbanUser(userId);
        assertTrue(tokenService.validateToken(token));
    }

    @Test
    void extractUserId_ReturnsCorrectSubject() {
        String token = tokenService.generateMemberToken("user-id-99", "charlie");
        assertEquals("user-id-99", tokenService.extractUserId(token));
    }

    @Test
    void extractUsername_ReturnsCorrectClaim() {
        String token = tokenService.generateMemberToken("uid", "diana");
        assertEquals("diana", tokenService.extractUsername(token));
    }

    @Test
    void clearAllData_RemovesBlacklistAndBans() {
        String userId = "u1";
        tokenService.banUser(userId);
        tokenService.clearAllData();
        String newToken = tokenService.generateMemberToken(userId, "u1");
        assertTrue(tokenService.validateToken(newToken));
    }

    @Test
    void validateToken_EmptyString_ReturnsFalse() {
        assertFalse(tokenService.validateToken(""));
    }

    @Test
    void generateGuestToken_ExtractUserId_ReturnsGuestSubject() {
        String token = tokenService.generateGuestToken();
        String subject = tokenService.extractUserId(token);
        assertNotNull(subject);
        assertFalse(subject.isBlank());
    }
}
