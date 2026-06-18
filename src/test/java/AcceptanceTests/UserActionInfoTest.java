package AcceptanceTests;

import com.ticketing.ticketapp.Appliction.*;
import com.ticketing.ticketapp.Domain.AdminAggregate.iAdminRepository;
import com.ticketing.ticketapp.Domain.User.IUserRepository;
import com.ticketing.ticketapp.Domain.User.UserDTO;
import com.ticketing.ticketapp.Infastructure.*;
import com.ticketing.ticketapp.Domain.OwnerManagerTree.iTreeOfRoleRepository;
import com.ticketing.ticketapp.Domain.Company.iCompanyRepository;
import com.ticketing.ticketapp.Domain.PurchasedOrderAggregate.iPurchasedOrderRepository;
import com.ticketing.ticketapp.Domain.Ticket.iTicketRepository;
import com.ticketing.ticketapp.Domain.Event.iEventRepository;
import com.ticketing.ticketapp.Domain.Order.IActiveOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@SpringBootTest(classes = com.ticketing.ticketapp.TicketappApplication.class)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Transactional
@DisplayName("User Action Acceptance Tests")
public class UserActionInfoTest {

    @Autowired private IUserRepository userRepository;
    @Autowired private iTreeOfRoleRepository treeOfRoleRepository;
    @Autowired private iAdminRepository adminRepository;
    @Autowired private iCompanyRepository companyRepository;
    @Autowired private iPurchasedOrderRepository purchasedOrderRepository;
    @Autowired private iTicketRepository ticketRepository;
    @Autowired private iEventRepository eventRepository;
    @Autowired private IActiveOrderRepository activeOrderRepository;
    @Autowired private TokenService tokenService;

    private UserService userService;
    private AdminService adminService;

    @BeforeEach
    void setUp() {
        INotifier notifierMock = mock(INotifier.class);
        IPasswordEncoder passwordEncoder = new PasswordEncoderImpl();

        this.userService = new UserService(passwordEncoder, userRepository, tokenService, new NotificationRepositoryImpl(), notifierMock, treeOfRoleRepository);

        this.adminService = new AdminService(treeOfRoleRepository, companyRepository, adminRepository, userRepository,
                purchasedOrderRepository, ticketRepository, eventRepository, tokenService,
                new NotifierImpl(new Broadcaster(new NotificationRepositoryImpl())), activeOrderRepository);

        userRepository.deleteAll();
        adminRepository.deleteAll();
        tokenService.clearAllData();

        adminRepository.addAdmin("admin");
    }

    private String gt() { return tokenService.generateGuestToken(); }

    @Test @DisplayName("1. Register Success")
    void registerSuccess1() {
        Response<String> result = userService.register(gt(), "roi", "roilevi", 10, "roi@test.com");
        assertTrue(result.isSuccess());
    }

    @Test @DisplayName("2. Register Fail - Invalid Password")
    void registerFailInvalidPassword2() {
        assertTrue(userService.register(gt(), "roi", null, 10, "roi@test.com").isError());
    }

    @Test
    @DisplayName("3. Register Fail - Already User In This UserName")
    void registerFailAlreadyUserInThisUserName3() {
        userService.register(gt(), "roi", "roilevi", 10, "roi@test.com");
        Response<String> result = userService.register(gt(), "roi", "roilevi", 10, "roi@test.com");
        assertTrue(result.isError());
    }

    @Test
    @DisplayName("4. Login Success")
    void loginSuccess4() {
        userService.register(gt(), "roi", "roilevi", 10, "roi@test.com");
        Response<String> result = userService.login(gt(), "roi", "roilevi");
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
    }

    @Test
    @DisplayName("5. Login Fail - Wrong Password")
    void loginFailWrongPassword5() {
        userService.register(gt(), "roi", "roilevi", 10, "roi@test.com");
        Response<String> result = userService.login(gt(), "roi", "wrong_pass");
        assertTrue(result.isError());
    }

    @Test
    @DisplayName("6. Login Fail - InValid Password")
    void loginFailInValidPassword6() {
        userService.register(gt(), "roi", "roilevi", 10, "roi@test.com");
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
        userService.register(gt(), "roi", "roilevi", 10, "roi@test.com");
        String token = userService.login(gt(), "roi", "roilevi").getData();
        Response<String> result = userService.logout(token);
        assertTrue(result.isSuccess());
        assertEquals("success", result.getData());
    }

    @Test
    @DisplayName("9. Logout Failed")
    void logoutFailed9() {
        userService.register(gt(), "roi", "roilevi", 10, "roi@test.com");
        Response<String> result = userService.logout("token");
        assertTrue(result.isError());
    }

    @Test
    @DisplayName("10. Login After Logout Success")
    void loginAfterLogoutSuccess10() {
        userService.register(gt(), "roi", "roilevi", 10, "roi@test.com");
        String token = userService.login(gt(), "roi", "roilevi").getData();
        userService.logout(token);
        Response<String> result = userService.login(gt(), "roi", "roilevi");
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
    }

    @Test
    @DisplayName("11. Get User Profile Success")
    void getUserInfoSuccess11() {
        userService.register(gt(), "roi", "roilevi", 10, "roi@test.com");
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
        userService.register(gt(), "roi", "roilevi", 10, "roi@test.com");
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
        userService.register(gt(), "roi", "roilevi", 10, "roi@test.com");
        String token = userService.login(gt(), "roi", "roilevi").getData();
        userService.updateUserPassword(token, "new");
        userService.logout(token);
        Response<String> result = userService.login(gt(), "roi", "new");
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
    }

    @Test
    void testRegisterInvalidToken() {
        assertTrue(userService.register("", "eventId", "", 1, "eventId@test.com").isError());
    }

    @Test
    void testLoginInvalidToken() {
        assertTrue(userService.login("", "eventId", "").isError());
    }


    @Test @DisplayName("16. Update User Profile Fail - User Is Suspended")
    void updateUserProfileFailedUserSuspended() {
        userService.register(gt(), "suspended_user", "password123", 20, "suspended@test.com");
        String token = userService.login(gt(), "suspended_user", "password123").getData();
        String userId = userRepository.getUserByUsername("suspended_user").getID();

        adminService.suspendUser(userId, "admin", 7);

        UserDTO updateRequest = new UserDTO();
        updateRequest.setName("newName");
        updateRequest.setEmail("new@test.com");

        Response<String> result = userService.updateUserProfile(token, updateRequest);
        assertTrue(result.isError());
        assertEquals("User is suspended", result.getMessage());
    }

    @Test
    @DisplayName("17. Update User Password Fail - User Is Suspended")
    void updateUserPasswordFailedUserSuspended() {
        userService.register(gt(), "suspended_user", "password123", 20, "suspended@test.com");
        String token = userService.login(gt(), "suspended_user", "password123").getData();
        String userId = userRepository.getUserByUsername("suspended_user").getID();

        adminService.suspendUser(userId, "admin", 7);

        Response<String> result = userService.updateUserPassword(token, "newPassword123");
        assertTrue(result.isError());
        assertEquals("User is suspended", result.getMessage());
    }

    @Test
    @DisplayName("18. Submit Complaint Fail - User Is Suspended")
    void submitComplaintFailedUserSuspended() {
        userService.register(gt(), "suspended_user", "password123", 20, "suspended@test.com");
        String token = userService.login(gt(), "suspended_user", "password123").getData();
        String userId = userRepository.getUserByUsername("suspended_user").getID();

        adminService.suspendUser(userId, "admin", 7);

        Response<String> result = userService.submitUserComplaint(token, "Admin", "This is a complaint");
        assertTrue(result.isError());
        assertEquals("User is suspended", result.getMessage());
    }
}
