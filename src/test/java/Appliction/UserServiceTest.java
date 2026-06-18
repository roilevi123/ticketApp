package Appliction;

import com.ticketing.ticketapp.Appliction.*;

import com.ticketing.ticketapp.Domain.Notification.INotificationRepository;
import com.ticketing.ticketapp.Domain.Notification.Notification;
import com.ticketing.ticketapp.Domain.OwnerManagerTree.Manager;
import com.ticketing.ticketapp.Domain.OwnerManagerTree.Owner;
import com.ticketing.ticketapp.Domain.OwnerManagerTree.iTreeOfRoleRepository;
import com.ticketing.ticketapp.Domain.User.IUserRepository;
import com.ticketing.ticketapp.Domain.User.User;
import com.ticketing.ticketapp.Infastructure.TokenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private IUserRepository userRepository;

    @Mock
    private IPasswordEncoder passwordEncoder;

    @Mock
    private TokenService tokenService;

    @Mock
    private INotificationRepository userNotificationRepository;
    @Mock
    private iTreeOfRoleRepository treeOfRoleRepository;
    @Mock
    private INotifier notifier;

    @InjectMocks
    private UserService userService;

    private final String USERNAME = "roy_user";
    private final String RAW_PASSWORD = "password123";
    private final String ENCODED_PASSWORD = "encoded_password123";
    private final String TOKEN = "mock-jwt-token";

    @Test
    void register_ShouldEncodePasswordAndStoreInRepo() {
        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(ENCODED_PASSWORD);

        Response<String> result = userService.register(TOKEN, USERNAME, RAW_PASSWORD, 10, "roy_user@test.com");

        assertTrue(result.isSuccess());
        verify(userRepository, times(1)).Store(USERNAME, ENCODED_PASSWORD, 10, "roy_user@test.com");
    }

    @Test
    void login_Success_ShouldReturnToken() {
        User mockUser = new User(USERNAME, ENCODED_PASSWORD, 10, "roy_user@test.com");

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(userRepository.usernameExists(USERNAME)).thenReturn(true);
        when(userRepository.getUserPassword(USERNAME)).thenReturn(ENCODED_PASSWORD);
        when(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
        when(userRepository.getUserByUsername(USERNAME)).thenReturn(mockUser);
        when(tokenService.generateMemberToken(mockUser.getID(), USERNAME)).thenReturn(TOKEN);

        Response<String> result = userService.login(TOKEN, USERNAME, RAW_PASSWORD);

        assertTrue(result.isSuccess());
        assertEquals(TOKEN, result.getData());
        verify(tokenService, times(1)).generateMemberToken(mockUser.getID(), USERNAME);
    }

    @Test
    void login_WrongPassword_ShouldReturnErrorMessage() {
        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(userRepository.usernameExists(USERNAME)).thenReturn(true);
        when(userRepository.getUserPassword(USERNAME)).thenReturn(ENCODED_PASSWORD);
        when(passwordEncoder.matches("wrong_pass", ENCODED_PASSWORD)).thenReturn(false);

        Response<String> result = userService.login(TOKEN, USERNAME, "wrong_pass");

        assertTrue(result.isError());
        verify(tokenService, never()).generateMemberToken(anyString(), anyString());
    }

    @Test
    void login_UserDoesNotExist_ShouldReturnErrorMessage() {
        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(userRepository.usernameExists("unknown_user")).thenReturn(false);

        Response<String> result = userService.login(TOKEN, "unknown_user", RAW_PASSWORD);

        assertTrue(result.isError());
        verify(tokenService, never()).generateMemberToken(anyString(), anyString());
    }

    @Test
    void logout_ValidToken_shouldPutUserInBlacklist() {
        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        Response<String> result = userService.logout(TOKEN);
        assertTrue(result.isSuccess());
        assertEquals("success", result.getData());
        verify(tokenService, times(1)).addBlacklistToken(TOKEN);
    }

    @Test
    void logout_InvalidToken_shouldReturnErrorMessage() {
        when(tokenService.validateToken(TOKEN)).thenReturn(false);
        Response<String> result = userService.logout(TOKEN);
        assertTrue(result.isError());
        verify(tokenService, never()).addBlacklistToken(anyString());
    }

    @Test
    void getUserProfile_ShouldReturnUserDTO() {
        User mockUser = new User(USERNAME, ENCODED_PASSWORD,10);
        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);
        when(userRepository.getUserByID(USERNAME)).thenReturn(mockUser);

        var result = userService.getUserProfile(TOKEN);

        assertTrue(result.isSuccess());
        assertEquals(USERNAME, result.getData().getName());
        assertEquals(10, result.getData().getAge());
    }

    @Test
    void getUserProfile_InvalidToken_ShouldReturnErrorMessage() {
        when(tokenService.validateToken(TOKEN)).thenReturn(false);
        var result = userService.getUserProfile(TOKEN);

        assertFalse(result.isSuccess());
        assertEquals("Invalid token", result.getMessage());
        assertNull(result.getData());
    }

    @Test
    void getUserProfile_UserNotFound_ShouldReturnErrorMessage() {
        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);
        when(userRepository.getUserByID(USERNAME)).thenReturn(null);

        var result = userService.getUserProfile(TOKEN);

        assertFalse(result.isSuccess());
        assertEquals("User not found", result.getMessage());
        assertNull(result.getData());
    }

    @Test
    void updateUserPassword_Success_ShouldUpdatePassword() {
        User mockUser = new User(USERNAME, ENCODED_PASSWORD, 10, "roy_user@test.com");
        mockUser.setVersion(0);

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);
        when(userRepository.getUserByID(USERNAME)).thenReturn(mockUser);
        when(passwordEncoder.encode("new_password")).thenReturn("encoded_new_password");

        Response<String> result = userService.updateUserPassword(TOKEN, "new_password");

        assertTrue(result.isSuccess());
        assertEquals("success", result.getData());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void updateUserPassword_InvalidToken_ShouldReturnErrorMessage() {
        when(tokenService.validateToken(TOKEN)).thenReturn(false);

        Response<String> result = userService.updateUserPassword(TOKEN, "new_password");

        assertTrue(result.isError());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUserPassword_UserNotFound_ShouldReturnErrorMessage() {
        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);
        when(userRepository.getUserByID(USERNAME)).thenReturn(null);

        Response<String> result = userService.updateUserPassword(TOKEN, "new_password");

        assertTrue(result.isError());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUserPassword_OptimisticLockFailure_ShouldReturnErrorMessage() {
        User mockUser = new User(USERNAME, ENCODED_PASSWORD, 10, "roy_user@test.com");
        mockUser.setVersion(0);

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);
        when(userRepository.getUserByID(USERNAME)).thenReturn(mockUser);
        when(passwordEncoder.encode("new_password")).thenReturn("encoded_new_password");
        doThrow(new RuntimeException("Optimistic Lock Failure")).when(userRepository).save(any(User.class));

        Response<String> result = userService.updateUserPassword(TOKEN, "new_password");

        assertTrue(result.isError());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void updateUserProfile_Success_ShouldUpdateProfile() {
        User mockUser = new User(USERNAME, ENCODED_PASSWORD, 25, "old@test.com");
        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);
        when(userRepository.getUserByID(USERNAME)).thenReturn(mockUser);

        com.ticketing.ticketapp.Domain.User.UserDTO dto = new com.ticketing.ticketapp.Domain.User.UserDTO();
        dto.setName("NewName");
        dto.setEmail("new@test.com");

        Response<String> result = userService.updateUserProfile(TOKEN, dto);

        assertTrue(result.isSuccess());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void updateUserProfile_InvalidToken_ShouldReturnError() {
        when(tokenService.validateToken(TOKEN)).thenReturn(false);

        Response<String> result = userService.updateUserProfile(TOKEN,
                new com.ticketing.ticketapp.Domain.User.UserDTO());

        assertTrue(result.isError());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUserProfile_UserNotFound_ShouldReturnError() {
        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);
        when(userRepository.getUserByID(USERNAME)).thenReturn(null);

        Response<String> result = userService.updateUserProfile(TOKEN,
                new com.ticketing.ticketapp.Domain.User.UserDTO());

        assertTrue(result.isError());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void submitUserComplaint_Success_ShouldSaveNotification() {
        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUsername(TOKEN)).thenReturn(USERNAME);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);

        Response<String> result = userService.submitUserComplaint(TOKEN, "Admin", "My complaint");

        assertTrue(result.isSuccess());
        verify(notifier, times(1)).notifyUserWithSender(eq("SYSTEM_ADMIN"), eq(USERNAME), contains("Complaint from"), any());
    }

    @Test
    void submitUserComplaint_InvalidToken_ShouldReturnError() {
        when(tokenService.validateToken(TOKEN)).thenReturn(false);

        Response<String> result = userService.submitUserComplaint(TOKEN, "Admin", "My complaint");

        assertTrue(result.isError());
        verify(notifier, never()).notifyUserWithSender(any(), any(), any(), any());
    }

    @Test
    void getUserNotifications_Success_ShouldReturnMessages() {
        String userId = "user-id-123";
        Notification n1 = new Notification("id1", userId, "msg1");
        Notification n2 = new Notification("id2", userId, "msg2");
        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(userId);
        when(userNotificationRepository.getAll(userId)).thenReturn(java.util.List.of(n1, n2));

        Response<java.util.List<String>> result = userService.getUserNotifications(TOKEN);

        assertTrue(result.isSuccess());
        assertEquals(2, result.getData().size());
    }

    @Test
    void getUserNotifications_InvalidToken_ShouldReturnError() {
        when(tokenService.validateToken(TOKEN)).thenReturn(false);

        Response<java.util.List<String>> result = userService.getUserNotifications(TOKEN);

        assertTrue(result.isError());
        verify(userNotificationRepository, never()).getAll(any());
    }


    @Test
    void updateUserProfile_UserIsSuspended_ShouldReturnErrorMessage() {
        User mockUser = new User(USERNAME, ENCODED_PASSWORD, 25, "old@test.com");
        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);
        when(userRepository.getUserByID(USERNAME)).thenReturn(mockUser);

        when(userRepository.isUserSuspendedNow(USERNAME)).thenReturn(true);

        com.ticketing.ticketapp.Domain.User.UserDTO dto = new com.ticketing.ticketapp.Domain.User.UserDTO();
        dto.setName("NewName");
        dto.setEmail("new@test.com");

        Response<String> result = userService.updateUserProfile(TOKEN, dto);

        assertTrue(result.isError());
        assertEquals("User is suspended", result.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUserPassword_UserIsSuspended_ShouldReturnErrorMessage() {
        User mockUser = new User(USERNAME, ENCODED_PASSWORD, 10, "roy_user@test.com");
        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);
        when(userRepository.getUserByID(USERNAME)).thenReturn(mockUser);

        when(userRepository.isUserSuspendedNow(USERNAME)).thenReturn(true);

        Response<String> result = userService.updateUserPassword(TOKEN, "new_password");

        assertTrue(result.isError());
        assertEquals("User is suspended", result.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void submitUserComplaint_UserIsSuspended_ShouldReturnErrorMessage() {
        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);

        when(userRepository.isUserSuspendedNow(USERNAME)).thenReturn(true);

        Response<String> result = userService.submitUserComplaint(TOKEN, "Admin", "My complaint");

        assertTrue(result.isError());
        assertEquals("User is suspended", result.getMessage());
        verify(userNotificationRepository, never()).save(any(), any());
    }
    @Test
    void submitProducerComplaint_Success_WithSubject_NotifiesOwnersManagersAndCompanyInbox() {
        Owner owner = new Owner("owner1", "company1", "founder");
        Manager manager = new Manager("manager1", "company1", Set.of(), "owner1");

        when(tokenService.validateToken("token")).thenReturn(true);
        when(tokenService.extractUsername("token")).thenReturn("buyerName");
        when(tokenService.extractUserId("token")).thenReturn("buyerId");
        when(userRepository.isUserSuspendedNow("buyerId")).thenReturn(false);

        when(treeOfRoleRepository.getAllOwnersByCompany("company1"))
                .thenReturn(List.of(owner));

        when(treeOfRoleRepository.getAllManagersByCompany("company1"))
                .thenReturn(List.of(manager));

        Response<String> result = userService.submitProducerComplaint(
                "token",
                "company1",
                "event1",
                "bad seats",
                "The seats were broken"
        );

        assertTrue(result.isSuccess());
        assertEquals("Complaint sent to the producer successfully", result.getData());

        verify(notifier).notifyUserWithSender(
                eq("owner1"),
                eq("buyerId"),
                eq("Complaint about event1"),
                eq("[bad seats] The seats were broken")
        );

        verify(notifier).notifyUserWithSender(
                eq("manager1"),
                eq("buyerId"),
                eq("Complaint about event1"),
                eq("[bad seats] The seats were broken")
        );

        verify(notifier).notifyUserWithSender(
                eq("COMPANY_COMPLAINT::company1"),
                eq("buyerId"),
                eq("Complaint about event1"),
                eq("[bad seats] The seats were broken")
        );
    }

    @Test
    void submitProducerComplaint_Success_BlankSubject_UsesOnlyContent() {
        when(tokenService.validateToken("token")).thenReturn(true);
        when(tokenService.extractUsername("token")).thenReturn("buyerName");
        when(tokenService.extractUserId("token")).thenReturn("buyerId");
        when(userRepository.isUserSuspendedNow("buyerId")).thenReturn(false);

        when(treeOfRoleRepository.getAllOwnersByCompany("company1"))
                .thenReturn(List.of());

        when(treeOfRoleRepository.getAllManagersByCompany("company1"))
                .thenReturn(List.of());

        Response<String> result = userService.submitProducerComplaint(
                "token",
                "company1",
                "event1",
                "   ",
                "Only content message"
        );

        assertTrue(result.isSuccess());

        verify(notifier).notifyUserWithSender(
                eq("COMPANY_COMPLAINT::company1"),
                eq("buyerId"),
                eq("Complaint about event1"),
                eq("Only content message")
        );
    }

    @Test
    void submitProducerComplaint_InvalidToken_ReturnsError() {
        when(tokenService.validateToken("bad_token")).thenReturn(false);

        Response<String> result = userService.submitProducerComplaint(
                "bad_token",
                "company1",
                "event1",
                "subject",
                "content"
        );

        assertTrue(result.isError());
        assertEquals("Invalid token", result.getMessage());

        verify(notifier, never()).notifyUserWithSender(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void submitProducerComplaint_SuspendedUser_ReturnsError() {
        when(tokenService.validateToken("token")).thenReturn(true);
        when(tokenService.extractUserId("token")).thenReturn("buyerId");
        when(userRepository.isUserSuspendedNow("buyerId")).thenReturn(true);

        Response<String> result = userService.submitProducerComplaint(
                "token",
                "company1",
                "event1",
                "subject",
                "content"
        );

        assertTrue(result.isError());
        assertEquals("User is suspended", result.getMessage());

        verify(notifier, never())
                .notifyUserWithSender(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void submitProducerComplaint_DatabaseFailure_ReturnsError() {
        when(tokenService.validateToken("token")).thenReturn(true);
        when(tokenService.extractUsername("token")).thenReturn("buyerName");
        when(tokenService.extractUserId("token")).thenReturn("buyerId");
        when(userRepository.isUserSuspendedNow("buyerId")).thenReturn(false);

        doThrow(new DataAccessResourceFailureException("DB down"))
                .when(treeOfRoleRepository)
                .getAllOwnersByCompany("company1");

        Response<String> result = userService.submitProducerComplaint(
                "token",
                "company1",
                "event1",
                "subject",
                "content"
        );

        assertTrue(result.isError());
        assertEquals("Database unavailable", result.getMessage());
    }

    @Test
    void deleteAll_CallsUserRepository() {
        userService.deleteAll();

        verify(userRepository).deleteAll();
    }

}
