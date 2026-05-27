package Infastructure;

import com.ticketing.ticketapp.Infastructure.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TokenServiceExtendedTest {

    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new TokenService();
    }

    // ── generateCompanyToken (4-arg, no permissions) ──────────────────────────

    @Test
    void generateCompanyToken_NoPermissions_CreatesValidToken() {
        String token = tokenService.generateCompanyToken("uid-1", "alice", "OWNER", "CorpX");
        assertNotNull(token);
        assertTrue(tokenService.validateToken(token));
        assertEquals("uid-1", tokenService.extractUserId(token));
        assertEquals("alice", tokenService.extractUsername(token));
    }

    // ── generateCompanyToken (5-arg, with permissions list) ───────────────────

    @Test
    void generateCompanyToken_WithPermissions_CreatesValidToken() {
        List<String> perms = List.of("VIEW_PURCHASE_HISTORY", "GENERATE_SALES_REPORTS");
        String token = tokenService.generateCompanyToken("uid-2", "bob", "MANAGER", "CorpY", perms);
        assertNotNull(token);
        assertTrue(tokenService.validateToken(token));
        assertEquals("uid-2", tokenService.extractUserId(token));
    }

    @Test
    void generateCompanyToken_WithEmptyPermissions_CreatesValidToken() {
        String token = tokenService.generateCompanyToken("uid-3", "carol", "MANAGER", "CorpZ", List.of());
        assertNotNull(token);
        assertTrue(tokenService.validateToken(token));
    }

    // ── isBannedToken ─────────────────────────────────────────────────────────

    @Test
    void isBannedToken_UnbannedUser_ReturnsFalse() {
        String token = tokenService.generateMemberToken("user-ok", "dave");
        assertFalse(tokenService.isBannedToken(token));
    }

    @Test
    void isBannedToken_BannedUser_ReturnsTrue() {
        String userId = "user-banned";
        String token = tokenService.generateMemberToken(userId, "eve");
        tokenService.banUser(userId);
        assertTrue(tokenService.isBannedToken(token));
    }

    @Test
    void isBannedToken_InvalidToken_ReturnsFalse() {
        assertFalse(tokenService.isBannedToken("not.a.valid.jwt"));
    }

    @Test
    void isBannedToken_AfterUnban_ReturnsFalse() {
        String userId = "user-rebanned";
        String token = tokenService.generateMemberToken(userId, "frank");
        tokenService.banUser(userId);
        assertTrue(tokenService.isBannedToken(token));
        tokenService.unbanUser(userId);
        assertFalse(tokenService.isBannedToken(token));
    }

    // ── validateToken: banned user in company token ───────────────────────────

    @Test
    void validateToken_BannedUser_CompanyToken_ReturnsFalse() {
        String userId = "banned-owner";
        String token = tokenService.generateCompanyToken(userId, "grace", "OWNER", "AcmeCorp");
        tokenService.banUser(userId);
        assertFalse(tokenService.validateToken(token));
    }
}
