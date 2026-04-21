package Appliction;

import Domain.Order.IActiveOrderRepository;
import Domain.User.IUserRepository;
import Domain.User.User;
import Infastructure.TokenService;
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
        when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(ENCODED_PASSWORD);

        userService.register(USERNAME, RAW_PASSWORD);

        verify(userRepository, times(1)).Store(USERNAME, ENCODED_PASSWORD);
    }
    @Test
    void login_Success_ShouldReturnToken() {
        User mockUser = new User(USERNAME, ENCODED_PASSWORD);

        when(userRepository.userExists(USERNAME)).thenReturn(true);
        when(userRepository.getUserPassword(USERNAME)).thenReturn(ENCODED_PASSWORD);
        when(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
        when(tokenService.generateToken(USERNAME)).thenReturn("mock-jwt-token");

        String result = userService.login(USERNAME, RAW_PASSWORD);

        assertEquals("mock-jwt-token", result);
        verify(tokenService, times(1)).generateToken(USERNAME);
    }

    @Test
    void login_WrongPassword_ShouldReturnErrorMessage() {
        when(userRepository.userExists(USERNAME)).thenReturn(true);
        when(userRepository.getUserPassword(USERNAME)).thenReturn(ENCODED_PASSWORD);

        when(passwordEncoder.matches("wrong_pass", ENCODED_PASSWORD)).thenReturn(false);

        String result = userService.login(USERNAME, "wrong_pass");

        assertEquals(null, result);
        verify(tokenService, never()).generateToken(anyString());
    }
    @Test
    void login_UserDoesNotExist_ShouldReturnErrorMessage() {
        when(userRepository.userExists("unknown_user")).thenReturn(false);

        String result = userService.login("unknown_user", RAW_PASSWORD);

        assertNull(result);
        verify(tokenService, never()).generateToken(anyString());
    }

    @Test
    void logout_ValidToken_shouldPutUserInBlacklist() {
        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUsername(TOKEN)).thenReturn(USERNAME);
        String result = userService.logout(TOKEN);
        assertEquals("success", result);
        verify(tokenService, times(1)).addBlacklistToken(USERNAME);
    }

    @Test
    void logout_InvalidToken_shouldReturnErrorMessage() {
        when(tokenService.validateToken(TOKEN)).thenReturn(false);
        String result = userService.logout(TOKEN);
        assertEquals("failed", result);
        verify(tokenService, never()).addBlacklistToken(anyString());
    }

    @Test
    void getUserInfo_ShouldReturnUserInfoString() {
        User mockUser = new User(USERNAME, ENCODED_PASSWORD);
        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUsername(TOKEN)).thenReturn(USERNAME);
        when(userRepository.getUser(USERNAME)).thenReturn(mockUser);

        String result = userService.getUserInfo(TOKEN);

        assertEquals("name=" + USERNAME, result);
    }

    @Test
    void getUserInfo_InvalidToken_ShouldReturnErrorMessage() {
        when(tokenService.validateToken(TOKEN)).thenReturn(false);
        String result = userService.getUserInfo(TOKEN);

        assertEquals(null, result);
    }

    @Test
    void getUserInfo_UserNotFound_ShouldReturnErrorMessage() {
        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUsername(TOKEN)).thenReturn(USERNAME);
        when(userRepository.getUser(USERNAME)).thenReturn(null);

        String result = userService.getUserInfo(TOKEN);

        assertEquals(null, result);
    }
}