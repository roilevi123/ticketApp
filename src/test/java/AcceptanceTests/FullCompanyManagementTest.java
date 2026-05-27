package AcceptanceTests;

import com.ticketing.ticketapp.Appliction.*;
import com.ticketing.ticketapp.Domain.AdminAggregate.iAdminRepository;
import com.ticketing.ticketapp.Domain.Company.iCompanyRepository;
import com.ticketing.ticketapp.Domain.Event.iEventRepository;
import com.ticketing.ticketapp.Domain.Order.IActiveOrderRepository;
import com.ticketing.ticketapp.Domain.OwnerManagerTree.Permission;
import com.ticketing.ticketapp.Domain.OwnerManagerTree.iTreeOfRoleRepository;
import com.ticketing.ticketapp.Domain.PurchasedOrderAggregate.iPurchasedOrderRepository;
import com.ticketing.ticketapp.Domain.QueueAggregates.iQueueRepository;
import com.ticketing.ticketapp.Domain.Ticket.iTicketRepository;
import com.ticketing.ticketapp.Domain.User.IUserRepository;
import com.ticketing.ticketapp.Infastructure.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Company Management Acceptance Tests")
public class FullCompanyManagementTest {

    private CompanyService companyService;
    private UserService userService;
    private TokenService tokenService;
    private IUserRepository userRepository;
    private AdminService adminService;

    @BeforeEach
    void setUp() {
        IUserRepository userRepository = new UserRepositoryImpl();
        iCompanyRepository companyRepository = new CompanyRepositoryImpl();
        iEventRepository eventRepository = new EventRepositoryImpl();
        iQueueRepository queueRepository = new QueueRepositoryImpl();
        iTreeOfRoleRepository treeOfRoleRepository = new TreeOfRoleRepositoryImpl();
        IActiveOrderRepository activeOrderRepository = new OrderRepositoryImpl();
        iTicketRepository ticketRepository = new TicketRepositoryImpl();
        iPurchasedOrderRepository purchasedOrderRepository = new PurchasedOrderRepositoryImpl();
        this.tokenService = new TokenService();
        IPasswordEncoder passwordEncoder = new PasswordEncoderImpl();
        IPendingNotificationRepository notificationRepository = new PendingNotificationRepositoryImpl();
        this.userService = new UserService(passwordEncoder, userRepository, tokenService, notificationRepository);
        this.companyService = new CompanyService(companyRepository, userRepository, treeOfRoleRepository, tokenService, mock(INotifier.class), notificationRepository);
        this.userRepository=userRepository;
        iAdminRepository adminRepository = new AdminRepositoryImpl(){
            @Override
            public boolean isAdmin(String userID) {
                return userID.equals("admin");
            }
        };
        this.adminService= new AdminService(treeOfRoleRepository,companyRepository,adminRepository,userRepository, purchasedOrderRepository, ticketRepository, eventRepository, tokenService, new NotifierImpl(new Broadcaster(new PendingNotificationRepositoryImpl())));

        activeOrderRepository.deleteAllActiveOrders();
        eventRepository.deleteAllEvents();
        treeOfRoleRepository.deleteAllRoles();
        companyRepository.deleteAllCompany();
        purchasedOrderRepository.deleteAll();
        queueRepository.deleteAll();
        ticketRepository.deleteAllTickets();
        userRepository.deleteAll();
        tokenService.clearAllData();
    }

    private String gt() {
        return tokenService.generateGuestToken();
    }

    private void reg(String username, String password) {
        userService.register(gt(), username, password, 10, username + "@test.com");
    }

    private String log(String username, String password) {
        return userService.login(gt(), username, password).getData();
    }

    // --- Company Creation ---

    @Test @DisplayName("1. Create Company - Success")
    void createCompanySuccess1() {
        reg("1", "1");
        String token = log("1", "1");
        assertTrue(companyService.CreateCompany("1", token).isSuccess());
    }

    @Test @DisplayName("2. Create Company - Fail (User Not Found)")
    void createCompanyFailedUserNotFound2() {
        assertTrue(companyService.CreateCompany("1", "invalid_token").isError());
    }

    // --- Manager Appointment ---

    @Test @DisplayName("3. Appoint Manager - Success")
    void appointManagerSuccess3() {
        reg("1", "1");
        String token = log("1", "1");
        reg("2", "2");
        companyService.CreateCompany("1", token);
        Set<Permission> permissions = new HashSet<>();
        permissions.add(Permission.MANAGE_INVENTORY);
        assertTrue(companyService.AppointAManager("2", "1", permissions, token).isSuccess());
    }

    @Test @DisplayName("4. Appoint Manager - Fail (Target User Not Found)")
    void appointManagerFailedUserNotFound4() {
        reg("1", "1");
        String token = log("1", "1");
        companyService.CreateCompany("1", token);
        assertTrue(companyService.AppointAManager("non_existent", "1", new HashSet<>(), token).isError());
    }

    @Test @DisplayName("5. Appoint Manager - Fail (Not Owner)")
    void appointManagerFailedNotOwner5() {
        reg("1", "1");
        String token1 = log("1", "1");
        reg("2", "2");
        reg("3", "3");
        String token3 = log("3", "3");
        companyService.CreateCompany("1", token1);
        assertTrue(companyService.AppointAManager("2", "1", new HashSet<>(), token3).isError());
    }

    @Test @DisplayName("6. Approve Manager Request - Success")
    void approveManagerRequestSuccess6() {
        reg("1", "1");
        String token = log("1", "1");
        reg("2", "2");
        String token2 = log("2", "2");
        companyService.CreateCompany("1", token);
        companyService.AppointAManager("2", "1", new HashSet<>(), token);
        assertTrue(companyService.ApproveAppointmentForManager(token2, "1").isSuccess());
    }

    @Test @DisplayName("7. Approve Manager Request - Fail (Company Not Exists)")
    void approveManagerRequestFaildCompanyNotExitst7() {
        reg("1", "1");
        String token = log("1", "1");
        reg("2", "2");
        String token2 = log("2", "2");
        companyService.CreateCompany("1", token);
        companyService.AppointAManager("2", "1", new HashSet<>(), token);
        assertTrue(companyService.ApproveAppointmentForManager(token2, "non_existent").isError());
    }

    @Test @DisplayName("8. Reject Manager Request - Success")
    void rejectManagerRequestSuccess8() {
        reg("1", "1");
        String token = log("1", "1");
        reg("2", "2");
        String token2 = log("2", "2");
        companyService.CreateCompany("1", token);
        companyService.AppointAManager("2", "1", new HashSet<>(), token);
        assertTrue(companyService.RejectAppointmentForManager(token2, "1").isSuccess());
    }

    @Test @DisplayName("9. Reject Manager Request - Fail (No Offer)")
    void rejectManagerRequestFailedNoOfferExist9() {
        reg("1", "1");
        String token = log("1", "1");
        reg("2", "2");
        String token2 = log("2", "2");
        companyService.CreateCompany("1", token);
        assertTrue(companyService.RejectAppointmentForManager(token2, "1").isError());
    }

    // --- Owner Appointment ---

    @Test @DisplayName("10. Appoint Owner - Success")
    void appointOwnerSuccess10() {
        reg("1", "1");
        String token = log("1", "1");
        reg("2", "2");
        companyService.CreateCompany("1", token);
        assertTrue(companyService.AppointOwner("2", "1", token).isSuccess());
    }

    @Test @DisplayName("11. Appoint Owner - Fail (Target User Not Found)")
    void appointOwnerFailedUserNotFound11() {
        reg("1", "1");
        String token = log("1", "1");
        companyService.CreateCompany("1", token);
        assertTrue(companyService.AppointOwner("non_existent", "1", token).isError());
    }

    @Test @DisplayName("12. Appoint Owner - Fail (Not Owner)")
    void appointOwnerFailedNotOwner12() {
        reg("1", "1");
        String token = log("1", "1");
        reg("2", "2");
        reg("3", "3");
        String token3 = log("3", "3");
        companyService.CreateCompany("1", token);
        assertTrue(companyService.AppointOwner("2", "1", token3).isError());
    }

    @Test @DisplayName("13. Approve Owner Request - Success")
    void approveOwnerRequestSuccess13() {
        reg("1", "1");
        String token = log("1", "1");
        reg("2", "2");
        String token2 = log("2", "2");
        companyService.CreateCompany("1", token);
        companyService.AppointOwner("2", "1", token);
        assertTrue(companyService.ApproveAppointmentForOwner(token2, "1").isSuccess());
    }

    @Test @DisplayName("14. Approve Owner Request - Fail (Company Not Exists)")
    void approveOwnerRequestFaildCompanyNotExitst14() {
        reg("1", "1");
        String token = log("1", "1");
        reg("2", "2");
        String token2 = log("2", "2");
        companyService.CreateCompany("1", token);
        companyService.AppointOwner("2", "1", token);
        assertTrue(companyService.ApproveAppointmentForOwner(token2, "non_existent").isError());
    }

    @Test @DisplayName("15. Reject Owner Request - Success")
    void rejectOwnerRequestSuccess15() {
        reg("1", "1");
        String token = log("1", "1");
        reg("2", "2");
        String token2 = log("2", "2");
        companyService.CreateCompany("1", token);
        companyService.AppointOwner("2", "1", token);
        assertTrue(companyService.RejectAppointmentForOwner(token2, "1").isSuccess());
    }

    @Test @DisplayName("16. Reject Owner Request - Fail (No Offer)")
    void rejectOwnerRequestFailedNoOfferExist16() {
        reg("1", "1");
        String token = log("1", "1");
        reg("2", "2");
        String token2 = log("2", "2");
        companyService.CreateCompany("1", token);
        assertTrue(companyService.RejectAppointmentForOwner(token2, "1").isError());
    }

    // --- Firing ---

    @Test @DisplayName("17. Fire Owner - Success")
    void fireOwnerRequestSuccess17() {
        reg("1", "1");
        String token = log("1", "1");
        reg("2", "2");
        String token2 = log("2", "2");
        companyService.CreateCompany("1", token);
        companyService.AppointOwner("2", "1", token);
        companyService.ApproveAppointmentForOwner(token2, "1");
        assertTrue(companyService.FireOwner(token, "1", "2").isSuccess());
    }

    @Test @DisplayName("18. Fire Owner - Fail (Not Appointer)")
    void fireOwnerFailedNotTheAppointer18() {
        reg("1", "1");
        String token1 = log("1", "1");
        reg("2", "2");
        String token2 = log("2", "2");
        reg("3", "3");
        String token3 = log("3", "3");
        companyService.CreateCompany("1", token1);
        companyService.AppointOwner("2", "1", token1);
        companyService.ApproveAppointmentForOwner(token2, "1");
        companyService.AppointOwner("3", "1", token1);
        companyService.ApproveAppointmentForOwner(token3, "1");
        assertTrue(companyService.FireOwner(token2, "1", "3").isError());
    }

    @Test @DisplayName("19. Fire Owner - Fail (Fire Founder)")
    void fireOwnerFailedCanNotFireTheFounder19() {
        reg("1", "1");
        String token1 = log("1", "1");
        reg("2", "2");
        String token2 = log("2", "2");
        companyService.CreateCompany("1", token1);
        companyService.AppointOwner("2", "1", token1);
        companyService.ApproveAppointmentForOwner(token2, "1");
        assertTrue(companyService.FireOwner(token2, "1", "1").isError());
    }

    @Test @DisplayName("20. Fire Owner - Fail (Owner Approval Pending)")
    void fireOwnerFailedCanNotOwner20() {
        reg("1", "1");
        String token = log("1", "1");
        reg("2", "2");
        companyService.CreateCompany("1", token);
        companyService.AppointOwner("2", "1", token);
        assertTrue(companyService.FireOwner(token, "1", "2").isError());
    }

    @Test @DisplayName("21. Fire Manager - Success")
    void fireManagerRequestSuccess21() {
        reg("1", "1");
        String token = log("1", "1");
        reg("2", "2");
        String token2 = log("2", "2");
        companyService.CreateCompany("1", token);
        companyService.AppointAManager("2", "1", new HashSet<>(), token);
        companyService.ApproveAppointmentForManager(token2, "1");
        assertTrue(companyService.FireManager(token, "1", "2").isSuccess());
    }

    @Test @DisplayName("22. Fire Manager - Fail (Not Appointer)")
    void fireManagerFailedNotTheAppointer22() {
        reg("1", "1");
        String token1 = log("1", "1");
        reg("2", "2");
        String token2 = log("2", "2");
        reg("3", "3");
        String token3 = log("3", "3");
        companyService.CreateCompany("1", token1);
        companyService.AppointOwner("2", "1", token1);
        companyService.ApproveAppointmentForOwner(token2, "1");
        companyService.AppointAManager("3", "1", new HashSet<>(), token1);
        companyService.ApproveAppointmentForManager(token3, "1");
        assertTrue(companyService.FireManager(token2, "1", "3").isError());
    }

    @Test @DisplayName("23. Fire Manager - Fail (Not Approved Yet)")
    void fireManagerFailedCanNotManager23() {
        reg("1", "1");
        String token = log("1", "1");
        reg("2", "2");
        companyService.CreateCompany("1", token);
        companyService.AppointAManager("2", "1", new HashSet<>(), token);
        assertTrue(companyService.FireManager(token, "1", "2").isError());
    }

    // --- Permissions ---

    @Test @DisplayName("24. Change Permissions - Success")
    void changeManagerPermissionsSuccess24() {
        reg("1", "1");
        String token = log("1", "1");
        reg("2", "2");
        String token2 = log("2", "2");
        companyService.CreateCompany("1", token);
        companyService.AppointAManager("2", "1", new HashSet<>(), token);
        companyService.ApproveAppointmentForManager(token2, "1");
        Set<Permission> newPerms = new HashSet<>();
        newPerms.add(Permission.CHANGE_POLICIES);
        assertTrue(companyService.ChangeManagerPermissions(token, "1", "2", newPerms).isSuccess());
    }

    @Test @DisplayName("25. Change Permissions - Fail (Not Appointer)")
    void changeManagerPermissionsNotAppointer25() {
        reg("1", "1");
        String token1 = log("1", "1");
        reg("2", "2");
        String token2 = log("2", "2");
        reg("3", "3");
        String token3 = log("3", "3");
        companyService.CreateCompany("1", token1);
        companyService.AppointOwner("2", "1", token1);
        companyService.ApproveAppointmentForOwner(token2, "1");
        companyService.AppointAManager("3", "1", new HashSet<>(), token1);
        companyService.ApproveAppointmentForManager(token3, "1");
        assertTrue(companyService.ChangeManagerPermissions(token2, "1", "3", new HashSet<>()).isError());
    }

    @Test @DisplayName("26. Change Permissions - Fail (Not Manager)")
    void changeManagerPermissionsNotManager26() {
        reg("1", "1");
        String token = log("1", "1");
        reg("2", "2");
        companyService.CreateCompany("1", token);
        companyService.AppointAManager("2", "1", new HashSet<>(), token);
        assertTrue(companyService.ChangeManagerPermissions(token, "1", "2", new HashSet<>()).isError());
    }

    // --- Freeze / Unfreeze ---

    @Test @DisplayName("27. Freeze Company - Success")
    void freezeCompanySuccess27() {
        reg("1", "1");
        String token = log("1", "1");
        companyService.CreateCompany("1", token);
        assertTrue(companyService.freezeCompany("1", token).isSuccess());
    }

    @Test @DisplayName("28. Freeze Company - Fail (Not Founder)")
    void freezeCompanyNotFounder28() {
        reg("1", "1");
        String token = log("1", "1");
        reg("2", "2");
        String token2 = log("2", "2");
        companyService.CreateCompany("1", token);
        companyService.AppointOwner("2", "1", token);
        companyService.ApproveAppointmentForOwner(token2, "1");
        assertTrue(companyService.freezeCompany("1", token2).isError());
    }

    @Test @DisplayName("29. Freeze Company - Fail (Already Frozen)")
    void freezeCompanyFailedAlreadyFreeze29() {
        reg("1", "1");
        String token = log("1", "1");
        companyService.CreateCompany("1", token);
        companyService.freezeCompany("1", token);
        assertTrue(companyService.freezeCompany("1", token).isError());
    }

    @Test @DisplayName("30. Unfreeze Company - Success")
    void unfreezeCompanySuccess30() {
        reg("1", "1");
        String token = log("1", "1");
        companyService.CreateCompany("1", token);
        companyService.freezeCompany("1", token);
        assertTrue(companyService.unfreezeCompany("1", token).isSuccess());
    }

    @Test @DisplayName("31. Unfreeze Company - Fail (Not Founder)")
    void unfreezeCompanyNotFounder31() {
        reg("1", "1");
        String token = log("1", "1");
        reg("2", "2");
        String token2 = log("2", "2");
        companyService.CreateCompany("1", token);
        companyService.AppointOwner("2", "1", token);
        companyService.ApproveAppointmentForOwner(token2, "1");
        companyService.freezeCompany("1", token);
        assertTrue(companyService.unfreezeCompany("1", token2).isError());
    }

    @Test @DisplayName("32. Unfreeze Company - Fail (Already Active)")
    void unfreezeCompanyFailedAlreadyUnFreeze32() {
        reg("1", "1");
        String token = log("1", "1");
        companyService.CreateCompany("1", token);
        assertTrue(companyService.unfreezeCompany("1", token).isError());
    }

    // --- Permissions Query ---

    @Test @DisplayName("33. Get Manager Permissions - Success")
    void getMangerPermissions33() {
        reg("1", "1");
        String token = log("1", "1");
        reg("2", "2");
        companyService.CreateCompany("1", token);
        Set<Permission> perms = new HashSet<>();
        perms.add(Permission.MANAGE_INVENTORY);
        companyService.AppointAManager("2", "1", perms, token);
        Response<Set<Permission>> fetched = companyService.GetManagerPermissions(token, "1", "2");
        assertTrue(fetched.isSuccess());
        assertNotNull(fetched.getData());
        assertTrue(fetched.getData().contains(Permission.MANAGE_INVENTORY));
    }

    @Test @DisplayName("34. Get Manager Permissions - Fail (Not Owner)")
    void getMangerPermissionsNotOwner34() {
        reg("1", "1");
        String token = log("1", "1");
        reg("2", "2");
        reg("3", "3");
        String token3 = log("3", "3");
        companyService.CreateCompany("1", token);
        companyService.AppointAManager("2", "1", new HashSet<>(), token);
        assertTrue(companyService.GetManagerPermissions(token3, "1", "2").isError());
    }

    // --- Role Tree ---

    @Test @DisplayName("35. Get Role Tree - Simple")
    void getTreeOfRolesSuccess35() {
        reg("1", "1");
        String token = log("1", "1");
        reg("2", "2");
        String token2 = log("2", "2");
        reg("3", "3");
        String token3 = log("3", "3");
        companyService.CreateCompany("1", token);
        companyService.AppointAManager("2", "1", new HashSet<>(), token);
        companyService.ApproveAppointmentForManager(token2, "1");
        companyService.AppointOwner("3", "1", token);
        companyService.ApproveAppointmentForOwner(token3, "1");

        String expectedTree = "|-- 1 (Owner)\n  |-- 3 (Owner)\n  |-- 2 (Manager)\n";
        assertEquals(expectedTree, companyService.GetRoleTreeString(token, "1").getData());
    }

    @Test @DisplayName("36. Get Role Tree - Nested")
    void getTreeOfRolesSuccess36() {
        reg("1", "1");
        String token = log("1", "1");
        reg("2", "2");
        String token2 = log("2", "2");
        reg("3", "3");
        String token3 = log("3", "3");
        reg("4", "4");
        String token4 = log("4", "4");
        reg("5", "5");
        String token5 = log("5", "5");

        companyService.CreateCompany("1", token);
        companyService.AppointAManager("2", "1", new HashSet<>(), token);
        companyService.ApproveAppointmentForManager(token2, "1");
        companyService.AppointOwner("3", "1", token);
        companyService.ApproveAppointmentForOwner(token3, "1");
        companyService.AppointAManager("4", "1", new HashSet<>(), token3);
        companyService.ApproveAppointmentForManager(token4, "1");
        companyService.AppointOwner("5", "1", token3);
        companyService.ApproveAppointmentForOwner(token5, "1");

        String expectedTree = "|-- 1 (Owner)\n" +
                "  |-- 3 (Owner)\n" +
                "    |-- 5 (Owner)\n" +
                "    |-- 4 (Manager)\n" +
                "  |-- 2 (Manager)\n";
        assertEquals(expectedTree, companyService.GetRoleTreeString(token, "1").getData());
    }

    @Test @DisplayName("37. Get Role Tree - After Firing")
    void getTreeOfRolesSuccess37() {
        reg("1", "1");
        String token = log("1", "1");
        reg("2", "2");
        String token2 = log("2", "2");
        reg("3", "3");
        String token3 = log("3", "3");
        reg("4", "4");
        String token4 = log("4", "4");
        reg("5", "5");
        String token5 = log("5", "5");

        companyService.CreateCompany("1", token);
        companyService.AppointAManager("2", "1", new HashSet<>(), token);
        companyService.ApproveAppointmentForManager(token2, "1");
        companyService.AppointOwner("3", "1", token);
        companyService.ApproveAppointmentForOwner(token3, "1");
        companyService.AppointAManager("4", "1", new HashSet<>(), token3);
        companyService.ApproveAppointmentForManager(token4, "1");
        companyService.AppointOwner("5", "1", token3);
        companyService.ApproveAppointmentForOwner(token5, "1");

        companyService.FireManager(token3, "1", "4");

        String expectedTree = "|-- 1 (Owner)\n" +
                "  |-- 3 (Owner)\n" +
                "    |-- 5 (Owner)\n" +
                "  |-- 2 (Manager)\n";
        assertEquals(expectedTree, companyService.GetRoleTreeString(token, "1").getData());
    }

    @Test
    void AppointAManagerInvalidToken() {
        assertTrue(companyService.AppointAManager("2", "1", new HashSet<>(), null).isError());
    }

    @Test
    void ApproveAppointmentForManagerInvalidToken() {
        assertTrue(companyService.ApproveAppointmentForManager("null", null).isError());
    }

    @Test
    void RejectAppointmentForManagerInvalidToken() {
        assertTrue(companyService.RejectAppointmentForManager("null", null).isError());
    }

    @Test
    void AppointOwnerInvalidToken() {
        assertTrue(companyService.AppointOwner("null", null, null).isError());
    }

    @Test
    void ApproveAppointmentForOwnerInvalidToken() {
        assertTrue(companyService.ApproveAppointmentForOwner("null", null).isError());
    }

    @Test
    void FireOwnerInvalidToken() {
        assertTrue(companyService.FireOwner("null", null, null).isError());
    }

    @Test
    void FireManagerInvalidToken() {
        assertTrue(companyService.FireManager("null", null, null).isError());
    }

    @Test
    void ChangeManagerPermissionsInvalidToken() {
        assertTrue(companyService.ChangeManagerPermissions("null", null, null, null).isError());
    }

    @Test
    void ChangeManagerPermissionNotManager() {
        reg("1", "1");
        String token = log("1", "1");
        assertTrue(companyService.ChangeManagerPermissions(token, null, null, null).isError());
    }

    @Test
    void freezeCompanyInvalidToken() {
        assertTrue(companyService.freezeCompany("null", null).isError());
    }

    @Test
    void GetManagerPermissionsInvalidToken() {
        assertTrue(companyService.GetManagerPermissions("null", null, null).isError());
    }

    @Test @DisplayName("Create Company - Fail (User is Suspended)")
    void createCompanyFailedUserSuspended() {
        reg("admin", "adminPassword");
        reg("suspended_user", "password123");
        String userToken = log("suspended_user", "password123");

        String idA = userRepository.getUserByUsername("suspended_user").getID();

        adminService.suspendUser(idA, "admin", 7);

        Response<String> response = companyService.CreateCompany("MyCompany", userToken);

        assertFalse(response.isSuccess());
        assertTrue(response.isError());
        assertEquals("User is suspended", response.getMessage());
    }

}
