package AcceptanceTests;

import com.ticketing.ticketapp.Appliction.UserService;
import com.ticketing.ticketapp.Appliction.Response;
import com.ticketing.ticketapp.Domain.User.IUserRepository;
import com.ticketing.ticketapp.Domain.User.UserDTO;
import com.ticketing.ticketapp.Infastructure.PasswordEncoderImpl;
import com.ticketing.ticketapp.Infastructure.TokenService;
import com.ticketing.ticketapp.Infastructure.UserRepositoryImpl;
import com.ticketing.ticketapp.Appliction.IPasswordEncoder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("User Action Acceptance Tests")
public class UserActionInfoTest {

    private UserService userService;
    private IUserRepository userRepository;
    private IPasswordEncoder passwordEncoder;
    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        this.userRepository = new UserRepositoryImpl();
        this.passwordEncoder = new PasswordEncoderImpl();
        this.tokenService = new TokenService();
        this.userService = new UserService(passwordEncoder, userRepository, tokenService);

        userRepository.deleteAll();
        tokenService.clearAllData();
    }

    private String gt() {
        return tokenService.generateGuestToken();
    }

    @Test
    @DisplayName("1. Register Success")
    void registerSuccess1() {
        Response<String> result = userService.register(gt(), "roi", "roilevi", 10);
        assertTrue(result.isSuccess());
        assertEquals("success", result.getData());
    }

    @Test
    @DisplayName("2. Register Fail - Invalid Password")
    void registerFailInvalidPassword2() {
        Response<String> result = userService.register(gt(), "roi", null, 10);
        assertTrue(result.isError());
    }

    @Test
    @DisplayName("3. Register Fail - Already User In This UserName")
    void registerFailAlreadyUserInThisUserName3() {
        userService.register(gt(), "roi", "roilevi", 10);
        Response<String> result = userService.register(gt(), "roi", "roilevi", 10);
        assertTrue(result.isError());
    }

    @Test
    @DisplayName("4. Login Success")
    void loginSuccess4() {
        userService.register(gt(), "roi", "roilevi", 10);
        Response<String> result = userService.login(gt(), "roi", "roilevi");
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
    }

    @Test
    @DisplayName("5. Login Fail - Wrong Password")
    void loginFailWrongPassword5() {
        userService.register(gt(), "roi", "roilevi", 10);
        Response<String> result = userService.login(gt(), "roi", "wrong_pass");
        assertTrue(result.isError());
    }

    @Test
    @DisplayName("6. Login Fail - InValid Password")
    void loginFailInValidPassword6() {
        userService.register(gt(), "roi", "roilevi", 10);
        Response<String> result = userService.login(gt(), "roi", null);
        assertTrue(result.isError());
    }

    @Test
    @DisplayName("7. Login Fail - User Not Found")
    void loginFailUserNotFound7() {
        Response<String> result = userService.login(gt(), "roi", "roilevi");
        assertTrue(result.isError());
    }

    @Test
    @DisplayName("8. Logout Success")
    void logoutSuccess8() {
        userService.register(gt(), "roi", "roilevi", 10);
        String token = userService.login(gt(), "roi", "roilevi").getData();
        Response<String> result = userService.logout(token);
        assertTrue(result.isSuccess());
        assertEquals("success", result.getData());
    }

    @Test
    @DisplayName("9. Logout Failed")
    void logoutFailed9() {
        userService.register(gt(), "roi", "roilevi", 10);
        Response<String> result = userService.logout("token");
        assertTrue(result.isError());
    }

    @Test
    @DisplayName("10. Login After Logout Success")
    void loginAfterLogoutSuccess10() {
        userService.register(gt(), "roi", "roilevi", 10);
        String token = userService.login(gt(), "roi", "roilevi").getData();
        userService.logout(token);
        Response<String> result = userService.login(gt(), "roi", "roilevi");
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
    }

    @Test
    @DisplayName("11. Get User Profile Success")
    void getUserInfoSuccess11() {
        userService.register(gt(), "roi", "roilevi", 10);
        String token = userService.login(gt(), "roi", "roilevi").getData();
        Response<UserDTO> result = userService.getUserProfile(token);
        assertTrue(result.isSuccess());
        assertEquals("roi", result.getData().getName());
    }

    @Test
    @DisplayName("12. Get User Profile Invalid Token")
    void getUserInfoNotExist12() {
        Response<UserDTO> result = userService.getUserProfile("Non Exist User");
        assertTrue(result.isError());
        assertEquals("Invalid token", result.getMessage());
    }

    @Test
    @DisplayName("13. Update User Profile Success")
    void updateUserInfoSuccess13() {
        userService.register(gt(), "roi", "roilevi", 10);
        String token = userService.login(gt(), "roi", "roilevi").getData();
        Response<String> result = userService.updateUserPassword(token, "new");
        assertTrue(result.isSuccess());
        assertEquals("success", result.getData());
    }

    @Test
    @DisplayName("14. Update User Profile Not Exist")
    void updateUserInfoNotExist14() {
        Response<String> result = userService.updateUserPassword("token", "wrong");
        assertTrue(result.isError());
    }

    @Test
    @DisplayName("15. Update User Profile And Then Login Success")
    void updateUserInfoAndThenLoginSuccess15() {
        userService.register(gt(), "roi", "roilevi", 10);
        String token = userService.login(gt(), "roi", "roilevi").getData();
        userService.updateUserPassword(token, "new");
        userService.logout(token);
        Response<String> result = userService.login(gt(), "roi", "new");
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
    }

    @Test
    void testRegisterInvalidToken() {
        assertTrue(userService.register("", "eventId", "", 1).isError());
    }

    @Test
    void testLoginInvalidToken() {
        assertTrue(userService.login("", "eventId", "").isError());
    }
}
