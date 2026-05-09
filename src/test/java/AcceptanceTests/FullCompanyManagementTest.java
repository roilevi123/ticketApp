package AcceptanceTests;

import Appliction.CompanyService;
import Appliction.INotifer;
import Appliction.IPasswordEncoder;
import Appliction.UserService;
import Domain.Company.iCompanyRepository;
import Domain.Event.iEventRepository;
import Domain.Order.IActiveOrderRepository;
import Domain.OwnerManagerTree.Permission;
import Domain.OwnerManagerTree.iTreeOfRoleRepository;
import Domain.PurchasedOrderAggregate.iPurchasedOrderRepository;
import Domain.QueueAggregates.iQueueRepository;
import Domain.Ticket.iTicketRepository;
import Domain.User.IUserRepository;
import Infastructure.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Company Management Acceptance Tests")
public class FullCompanyManagementTest {

    private CompanyService companyService;
    private UserService userService;
    private TokenService tokenService;

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
        INotifer notifer= Mockito.mock(INotifer.class);

        this.userService = new UserService(passwordEncoder, userRepository, tokenService);
        this.companyService = new CompanyService(companyRepository, userRepository, treeOfRoleRepository, tokenService,notifer);

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
        userService.register(gt(), username, password,10);
    }

    private String log(String username, String password) {
        return userService.login(gt(), username, password);
    }

    // --- Company Creation ---

    @Test @DisplayName("1. Create Company - Success")
    void createCompanySuccess1() {
        reg("1", "1");
        String token = log("1", "1");
        assertEquals("success", companyService.CreateCompany("1", token));
    }

    @Test @DisplayName("2. Create Company - Fail (User Not Found)")
    void createCompanyFailedUserNotFound2() {
        assertNotEquals("success", companyService.CreateCompany("1", "invalid_token"));
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
        assertEquals("success", companyService.AppointAManager("2", "1", permissions, token));
    }

    @Test @DisplayName("4. Appoint Manager - Fail (Target User Not Found)")
    void appointManagerFailedUserNotFound4() {
        reg("1", "1");
        String token = log("1", "1");
        companyService.CreateCompany("1", token);
        assertNotEquals("success", companyService.AppointAManager("non_existent", "1", new HashSet<>(), token));
    }

    @Test @DisplayName("5. Appoint Manager - Fail (Not Owner)")
    void appointManagerFailedNotOwner5() {
        reg("1", "1");
        String token1 = log("1", "1");
        reg("2", "2");
        reg("3", "3");
        String token3 = log("3", "3");
        companyService.CreateCompany("1", token1);
        assertNotEquals("success", companyService.AppointAManager("2", "1", new HashSet<>(), token3));
    }

    @Test @DisplayName("6. Approve Manager Request - Success")
    void approveManagerRequestSuccess6() {
        reg("1", "1");
        String token = log("1", "1");
        reg("2", "2");
        String token2 = log("2", "2");
        companyService.CreateCompany("1", token);
        companyService.AppointAManager("2", "1", new HashSet<>(), token);
        assertEquals("success", companyService.ApproveAppointmentForManager(token2, "1"));
    }

    @Test @DisplayName("7. Approve Manager Request - Fail (Company Not Exists)")
    void approveManagerRequestFaildCompanyNotExitst7() {
        reg("1", "1");
        String token = log("1", "1");
        reg("2", "2");
        String token2 = log("2", "2");
        companyService.CreateCompany("1", token);
        companyService.AppointAManager("2", "1", new HashSet<>(), token);
        assertNotEquals("success", companyService.ApproveAppointmentForManager(token2, "non_existent"));
    }

    @Test @DisplayName("8. Reject Manager Request - Success")
    void rejectManagerRequestSuccess8() {
        reg("1", "1");
        String token = log("1", "1");
        reg("2", "2");
        String token2 = log("2", "2");
        companyService.CreateCompany("1", token);
        companyService.AppointAManager("2", "1", new HashSet<>(), token);
        assertEquals("success", companyService.RejectAppointmentForManager(token2, "1"));
    }

    @Test @DisplayName("9. Reject Manager Request - Fail (No Offer)")
    void rejectManagerRequestFailedNoOfferExist9() {
        reg("1", "1");
        String token = log("1", "1");
        reg("2", "2");
        String token2 = log("2", "2");
        companyService.CreateCompany("1", token);
        assertNotEquals("success", companyService.RejectAppointmentForManager(token2, "1"));
    }

    // --- Owner Appointment ---

    @Test @DisplayName("10. Appoint Owner - Success")
    void appointOwnerSuccess10() {
        reg("1", "1");
        String token = log("1", "1");
        reg("2", "2");
        companyService.CreateCompany("1", token);
        assertEquals("success", companyService.AppointOwner("2", "1", token));
    }

    @Test @DisplayName("11. Appoint Owner - Fail (Target User Not Found)")
    void appointOwnerFailedUserNotFound11() {
        reg("1", "1");
        String token = log("1", "1");
        companyService.CreateCompany("1", token);
        assertNotEquals("success", companyService.AppointOwner("non_existent", "1", token));
    }

    @Test @DisplayName("12. Appoint Owner - Fail (Not Owner)")
    void appointOwnerFailedNotOwner12() {
        reg("1", "1");
        String token = log("1", "1");
        reg("2", "2");
        reg("3", "3");
        String token3 = log("3", "3");
        companyService.CreateCompany("1", token);
        assertNotEquals("success", companyService.AppointOwner("2", "1", token3));
    }

    @Test @DisplayName("13. Approve Owner Request - Success")
    void approveOwnerRequestSuccess13() {
        reg("1", "1");
        String token = log("1", "1");
        reg("2", "2");
        String token2 = log("2", "2");
        companyService.CreateCompany("1", token);
        companyService.AppointOwner("2", "1", token);
        assertEquals("success", companyService.ApproveAppointmentForOwner(token2, "1"));
    }

    @Test @DisplayName("14. Approve Owner Request - Fail (Company Not Exists)")
    void approveOwnerRequestFaildCompanyNotExitst14() {
        reg("1", "1");
        String token = log("1", "1");
        reg("2", "2");
        String token2 = log("2", "2");
        companyService.CreateCompany("1", token);
        companyService.AppointOwner("2", "1", token);
        assertNotEquals("success", companyService.ApproveAppointmentForOwner(token2, "non_existent"));
    }

    @Test @DisplayName("15. Reject Owner Request - Success")
    void rejectOwnerRequestSuccess15() {
        reg("1", "1");
        String token = log("1", "1");
        reg("2", "2");
        String token2 = log("2", "2");
        companyService.CreateCompany("1", token);
        companyService.AppointOwner("2", "1", token);
        assertEquals("success", companyService.RejectAppointmentForOwner(token2, "1"));
    }

    @Test @DisplayName("16. Reject Owner Request - Fail (No Offer)")
    void rejectOwnerRequestFailedNoOfferExist16() {
        reg("1", "1");
        String token = log("1", "1");
        reg("2", "2");
        String token2 = log("2", "2");
        companyService.CreateCompany("1", token);
        assertNotEquals("success", companyService.RejectAppointmentForOwner(token2, "1"));
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
        assertEquals("success", companyService.FireOwner(token, "1", "2"));
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
        assertNotEquals("success", companyService.FireOwner(token2, "1", "3"));
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
        assertNotEquals("success", companyService.FireOwner(token2, "1", "1"));
    }

    @Test @DisplayName("20. Fire Owner - Fail (Owner Approval Pending)")
    void fireOwnerFailedCanNotOwner20() {
        reg("1", "1");
        String token = log("1", "1");
        reg("2", "2");
        companyService.CreateCompany("1", token);
        companyService.AppointOwner("2", "1", token);
        assertNotEquals("success", companyService.FireOwner(token, "1", "2"));
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
        assertEquals("success", companyService.FireManager(token, "1", "2"));
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
        assertNotEquals("success", companyService.FireManager(token2, "1", "3"));
    }

    @Test @DisplayName("23. Fire Manager - Fail (Not Approved Yet)")
    void fireManagerFailedCanNotManager23() {
        reg("1", "1");
        String token = log("1", "1");
        reg("2", "2");
        companyService.CreateCompany("1", token);
        companyService.AppointAManager("2", "1", new HashSet<>(), token);
        assertNotEquals("success", companyService.FireManager(token, "1", "2"));
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
        assertEquals("success", companyService.ChangeManagerPermissions(token, "1", "2", newPerms));
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
        assertNotEquals("success", companyService.ChangeManagerPermissions(token2, "1", "3", new HashSet<>()));
    }

    @Test @DisplayName("26. Change Permissions - Fail (Not Manager)")
    void changeManagerPermissionsNotManager26() {
        reg("1", "1");
        String token = log("1", "1");
        reg("2", "2");
        companyService.CreateCompany("1", token);
        companyService.AppointAManager("2", "1", new HashSet<>(), token);
        assertNotEquals("success", companyService.ChangeManagerPermissions(token, "1", "2", new HashSet<>()));
    }

    // --- Freeze / Unfreeze ---

    @Test @DisplayName("27. Freeze Company - Success")
    void freezeCompanySuccess27() {
        reg("1", "1");
        String token = log("1", "1");
        companyService.CreateCompany("1", token);
        assertEquals("success", companyService.freezeCompany("1", token));
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
        assertNotEquals("success", companyService.freezeCompany("1", token2));
    }

    @Test @DisplayName("29. Freeze Company - Fail (Already Frozen)")
    void freezeCompanyFailedAlreadyFreeze29() {
        reg("1", "1");
        String token = log("1", "1");
        companyService.CreateCompany("1", token);
        companyService.freezeCompany("1", token);
        assertNotEquals("success", companyService.freezeCompany("1", token));
    }

    @Test @DisplayName("30. Unfreeze Company - Success")
    void unfreezeCompanySuccess30() {
        reg("1", "1");
        String token = log("1", "1");
        companyService.CreateCompany("1", token);
        companyService.freezeCompany("1", token);
        assertEquals("success", companyService.unfreezeCompany("1", token));
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
        assertNotEquals("success", companyService.unfreezeCompany("1", token2));
    }

    @Test @DisplayName("32. Unfreeze Company - Fail (Already Active)")
    void unfreezeCompanyFailedAlreadyUnFreeze32() {
        reg("1", "1");
        String token = log("1", "1");
        companyService.CreateCompany("1", token);
        assertNotEquals("success", companyService.unfreezeCompany("1", token));
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
        Set<Permission> fetched = companyService.GetManagerPermissions(token, "1", "2");
        assertNotNull(fetched);
        assertTrue(fetched.contains(Permission.MANAGE_INVENTORY));
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
        assertNull(companyService.GetManagerPermissions(token3, "1", "2"));
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
        assertEquals(expectedTree, companyService.GetRoleTreeString(token, "1"));
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
        assertEquals(expectedTree, companyService.GetRoleTreeString(token, "1"));
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
        assertEquals(expectedTree, companyService.GetRoleTreeString(token, "1"));
    }
}
