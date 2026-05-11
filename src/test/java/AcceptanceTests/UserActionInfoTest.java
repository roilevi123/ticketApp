package AcceptanceTests;

import com.ticketing.ticketapp.Appliction.UserService;
import com.ticketing.ticketapp.Domain.User.IUserRepository;
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
        String result = userService.register(gt(), "roi", "roilevi",10);
        assertEquals("success", result);
    }

    @Test
    @DisplayName("2. Register Fail - Invalid Password")
    void registerFailInvalidPassword2() {
        String result = userService.register(gt(), "roi", null,10);
        assertNotEquals("success", result);
    }

    @Test
    @DisplayName("3. Register Fail - Already User In This UserName")
    void registerFailAlreadyUserInThisUserName3() {
        userService.register(gt(), "roi", "roilevi",10);
        String result = userService.register(gt(), "roi", "roilevi",10);
        assertNotEquals("success", result);
    }

    @Test
    @DisplayName("4. Login Success")
    void loginSuccess4() {
        userService.register(gt(), "roi", "roilevi",10);
        String result = userService.login(gt(), "roi", "roilevi");
        assertNotNull(result);
    }

    @Test
    @DisplayName("5. Login Fail - Wrong Password")
    void loginFailWrongPassword5() {
        userService.register(gt(), "roi", "roilevi",10);
        String result = userService.login(gt(), "roi", "wrong_pass");
        assertNull(result);
    }

    @Test
    @DisplayName("6. Login Fail - InValid Password")
    void loginFailInValidPassword6() {
        userService.register(gt(), "roi", "roilevi",10);
        String result = userService.login(gt(), "roi", null);
        assertNull(result);
    }

    @Test
    @DisplayName("7. Login Fail - User Not Found")
    void loginFailUserNotFound7() {
        String result = userService.login(gt(), "roi", "roilevi");
        assertNull(result);
    }

    @Test
    @DisplayName("8. Logout Success")
    void logoutSuccess8() {
        userService.register(gt(), "roi", "roilevi",10);
        String token = userService.login(gt(), "roi", "roilevi");
        String result = userService.logout(token);
        assertEquals("success", result);
    }

    @Test
    @DisplayName("9. Logout Failed")
    void logoutFailed9() {
        userService.register(gt(), "roi", "roilevi",10);
        String result = userService.logout("token");
        assertNotEquals("success", result);
    }

    @Test
    @DisplayName("10. Login After Logout Success")
    void loginAfterLogoutSuccess10() {
        userService.register(gt(), "roi", "roilevi",10);
        String token = userService.login(gt(), "roi", "roilevi");
        userService.logout(token);
        String result = userService.login(gt(), "roi", "roilevi");
        assertNotNull(result);
    }

    @Test
    @DisplayName("11. Get User Info Success")
    void getUserInfoSuccess11() {
        userService.register(gt(), "roi", "roilevi",10);
        String token = userService.login(gt(), "roi", "roilevi");
        String result = userService.getUserInfo(token);
        assertEquals("name=roi", result);
    }

    @Test
    @DisplayName("12. Get User Info Not Exist")
    void getUserInfoNotExist12() {
        String result = userService.getUserInfo("Non Exist User");
        assertEquals("Invalid token", result);
    }

    @Test
    @DisplayName("13. Update User Info Success")
    void updateUserInfoSuccess13() {
        userService.register(gt(), "roi", "roilevi",10);
        String token = userService.login(gt(), "roi", "roilevi");
        String result = userService.updateUserPassword(token, "new");
        assertEquals("success", result);
    }

    @Test
    @DisplayName("14. Update User Info Not Exist")
    void updateUserInfoNotExist14() {
        String result = userService.updateUserPassword("token", "wrong");
        assertNotEquals("success", result);
    }

    @Test
    @DisplayName("15. Update User Info And Then Login Success")
    void updateUserInfoAndThenLoginSuccess15() {
        userService.register(gt(), "roi", "roilevi",10);
        String token = userService.login(gt(), "roi", "roilevi");
        userService.updateUserPassword(token, "new");
        userService.logout(token);
        String result = userService.login(gt(), "roi", "new");
        assertNotNull(result);
    }
}
