package Appliction;

import com.ticketing.ticketapp.Appliction.*;
import com.ticketing.ticketapp.Domain.Notification.INotificationRepository;
import com.ticketing.ticketapp.Domain.OwnerManagerTree.Permission;
import com.ticketing.ticketapp.Domain.OwnerManagerTree.iTreeOfRoleRepository;
import com.ticketing.ticketapp.Domain.User.IUserRepository;
import com.ticketing.ticketapp.Domain.User.User;
import com.ticketing.ticketapp.Infastructure.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UserServiceExtendedTest {

    @Mock private IPasswordEncoder passwordEncoder;
    @Mock private IUserRepository userRepository;
    @Mock private TokenService tokenService;
    @Mock private INotificationRepository notificationRepository;
    @Mock private INotifier notifier;
    @Mock private iTreeOfRoleRepository roleRepository;

    private UserService userService;

    private final String TOKEN = "valid-token";
    private final String USER_ID = "uid-1";
    private final String USERNAME = "alice";
    private final String COMPANY = "Corp";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        userService = new UserService(passwordEncoder, userRepository, tokenService,
                notificationRepository, notifier, roleRepository);
        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USER_ID);
    }

    // ── register ──────────────────────────────────────────────────────────────

    @Test
    void register_InvalidToken_ReturnsError() {
        when(tokenService.validateToken("bad")).thenReturn(false);

        var result = userService.register("bad", USERNAME, "pass", 25, "a@b.com");

        assertTrue(result.isError());
        verify(userRepository, never()).Store(any(), any(), anyInt(), any());
    }

    // ── submitUserComplaint ───────────────────────────────────────────────────

    @Test
    void submitUserComplaint_NonAdminTarget_SendsToTargetRole() {
        when(tokenService.extractUsername(TOKEN)).thenReturn(USERNAME);

        var result = userService.submitUserComplaint(TOKEN, "someRole", "issue here");

        assertTrue(result.isSuccess());
        // target is not "Admin" so targetId == "someRole" (not "SYSTEM_ADMIN")
        verify(notifier).notifyUserWithSender(eq("someRole"), eq(USER_ID), anyString(), anyString());
    }

    // ── getUserCompanies ──────────────────────────────────────────────────────

    @Test
    void getUserCompanies_ValidToken_ReturnsCompanies() {
        when(roleRepository.getUserCompanies(USER_ID)).thenReturn(List.of("Corp", "OtherCorp"));

        var result = userService.getUserCompanies(TOKEN);

        assertTrue(result.isSuccess());
        assertEquals(2, result.getData().size());
    }

    @Test
    void getUserCompanies_InvalidToken_ReturnsError() {
        when(tokenService.validateToken("bad")).thenReturn(false);

        var result = userService.getUserCompanies("bad");

        assertTrue(result.isError());
        verify(roleRepository, never()).getUserCompanies(any());
    }

    // ── switchCompanyContext ──────────────────────────────────────────────────

    @Test
    void switchCompanyContext_MemberRole_ReturnsError() {
        User user = new User(USERNAME, "pass", 30);
        when(userRepository.getUserByID(USER_ID)).thenReturn(user);
        when(roleRepository.getRoleInCompany(USER_ID, COMPANY)).thenReturn("MEMBER");

        var result = userService.switchCompanyContext(TOKEN, COMPANY);

        assertTrue(result.isError());
        assertTrue(result.getMessage().contains("not authorized"));
        verify(tokenService, never()).generateCompanyToken(any(), any(), any(), any());
    }

    @Test
    void switchCompanyContext_ManagerRole_GeneratesTokenWithPermissions() {
        User user = new User(USERNAME, "pass", 30);
        when(userRepository.getUserByID(USER_ID)).thenReturn(user);
        when(roleRepository.getRoleInCompany(USER_ID, COMPANY)).thenReturn("MANAGER");
        when(roleRepository.getManagerPermissions(USER_ID, COMPANY))
                .thenReturn(Set.of(Permission.VIEW_PURCHASE_HISTORY));
        when(tokenService.generateCompanyToken(eq(USER_ID), eq(USERNAME), eq("MANAGER"),
                eq(COMPANY), anyList())).thenReturn("manager-token");

        var result = userService.switchCompanyContext(TOKEN, COMPANY);

        assertTrue(result.isSuccess());
        assertEquals("manager-token", result.getData());
        verify(tokenService).generateCompanyToken(eq(USER_ID), eq(USERNAME), eq("MANAGER"),
                eq(COMPANY), anyList());
    }

    @Test
    void switchCompanyContext_OwnerRole_GeneratesTokenWithoutPermissions() {
        User user = new User(USERNAME, "pass", 30);
        when(userRepository.getUserByID(USER_ID)).thenReturn(user);
        when(roleRepository.getRoleInCompany(USER_ID, COMPANY)).thenReturn("OWNER");
        when(tokenService.generateCompanyToken(USER_ID, USERNAME, "OWNER", COMPANY))
                .thenReturn("owner-token");

        var result = userService.switchCompanyContext(TOKEN, COMPANY);

        assertTrue(result.isSuccess());
        assertEquals("owner-token", result.getData());
        verify(tokenService).generateCompanyToken(USER_ID, USERNAME, "OWNER", COMPANY);
    }

    @Test
    void switchCompanyContext_InvalidToken_ReturnsError() {
        when(tokenService.validateToken("bad")).thenReturn(false);

        var result = userService.switchCompanyContext("bad", COMPANY);

        assertTrue(result.isError());
        verify(roleRepository, never()).getRoleInCompany(any(), any());
    }
}
