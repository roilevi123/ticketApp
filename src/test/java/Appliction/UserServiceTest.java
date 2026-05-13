package Appliction;

import com.ticketing.ticketapp.Appliction.*;

import com.ticketing.ticketapp.Domain.Order.IActiveOrderRepository;
import com.ticketing.ticketapp.Domain.User.IUserRepository;
import com.ticketing.ticketapp.Domain.User.User;
import com.ticketing.ticketapp.Infastructure.TokenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    private IActiveOrderRepository activeOrderRepository;

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

        Response<String> result = userService.register(TOKEN, USERNAME, RAW_PASSWORD, 10);

        assertTrue(result.isSuccess());
        verify(userRepository, times(1)).Store(USERNAME, ENCODED_PASSWORD, 10);
    }

    @Test
    void login_Success_ShouldReturnToken() {
        User mockUser = new User(USERNAME, ENCODED_PASSWORD, 10);

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
    void getUserInfo_ShouldReturnUserInfoString() {
        User mockUser = new User(USERNAME, ENCODED_PASSWORD, 10);
        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);
        when(userRepository.getUserByID(USERNAME)).thenReturn(mockUser);

        Response<String> result = userService.getUserInfo(TOKEN);

        assertTrue(result.isSuccess());
        assertEquals("name=" + USERNAME, result.getData());
    }

    @Test
    void getUserInfo_InvalidToken_ShouldReturnErrorMessage() {
        when(tokenService.validateToken(TOKEN)).thenReturn(false);
        Response<String> result = userService.getUserInfo(TOKEN);

        assertTrue(result.isError());
        assertEquals("Invalid token", result.getMessage());
    }

    @Test
    void getUserInfo_UserNotFound_ShouldReturnErrorMessage() {
        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);
        when(userRepository.getUserByID(USERNAME)).thenReturn(null);

        Response<String> result = userService.getUserInfo(TOKEN);

        assertTrue(result.isError());
    }

    @Test
    void updateUserPassword_Success_ShouldUpdatePassword() {
        User mockUser = new User(USERNAME, ENCODED_PASSWORD, 10);
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
        User mockUser = new User(USERNAME, ENCODED_PASSWORD, 10);
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
}
