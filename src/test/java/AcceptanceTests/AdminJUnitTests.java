package AcceptanceTests;

import com.ticketing.ticketapp.Appliction.*;
import com.ticketing.ticketapp.Domain.AdminAggregate.iAdminRepository;
import com.ticketing.ticketapp.Domain.Company.iCompanyRepository;
import com.ticketing.ticketapp.Domain.Discount.JpaDiscountPolicyRepository;
import com.ticketing.ticketapp.Domain.Discount.iDiscountPolicyRepository;
import com.ticketing.ticketapp.Domain.Event.EventType;
import com.ticketing.ticketapp.Domain.Event.MapArea;
import com.ticketing.ticketapp.Domain.Event.iEventRepository;
import com.ticketing.ticketapp.Domain.Order.IActiveOrderRepository;
import com.ticketing.ticketapp.Domain.OwnerManagerTree.iTreeOfRoleRepository;
import com.ticketing.ticketapp.Domain.PurchasePolicy.iPurchasePolicyRepository;
import com.ticketing.ticketapp.Domain.PurchasedOrderAggregate.PurchaseOrderDTO;
import com.ticketing.ticketapp.Domain.PurchasedOrderAggregate.iPurchasedOrderRepository;
import com.ticketing.ticketapp.Domain.QueueAggregates.iQueueRepository;
import com.ticketing.ticketapp.Domain.Ticket.TicketDTO;
import com.ticketing.ticketapp.Domain.Ticket.iTicketRepository;
import com.ticketing.ticketapp.Domain.User.*;
import com.ticketing.ticketapp.Domain.payment.CreditCardDetails;
import com.ticketing.ticketapp.Infastructure.*;
import com.ticketing.ticketapp.Infastructure.DataBaseInterface.DiscountPolicyRepositoryAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.beans.factory.annotation.Autowired;

import static org.mockito.Mockito.mock;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
@DataJpaTest
@org.springframework.test.context.ContextConfiguration(classes = com.ticketing.ticketapp.TicketappApplication.class)
@org.springframework.boot.autoconfigure.domain.EntityScan(basePackages = "com.ticketing.ticketapp")
@org.springframework.data.jpa.repository.config.EnableJpaRepositories(basePackages = "com.ticketing.ticketapp")
@DisplayName("Admin Management Acceptance Tests")
public class AdminJUnitTests {

    private UserService userService;
    private CompanyService companyService;
    private EventService eventService;
    private OrderService reserveTicketService;
    private PurchasedService purchasedService;
    private AdminService adminService;
    private IUserRepository userRepository;
    private TokenService tokenService;
    @Autowired
    private JpaDiscountPolicyRepository jpaDiscountPolicyRepository;
    @Autowired
    private JpaPurchasePolicyRepository jpaPurchasePolicyRepository;
    @BeforeEach
    void setUp() {
        this.userRepository = new UserRepositoryImpl();
        iCompanyRepository companyRepository = new CompanyRepositoryImpl();
        iEventRepository eventRepository = new EventRepositoryImpl();
        iQueueRepository queueRepository = new QueueRepositoryImpl();
        iTreeOfRoleRepository treeOfRoleRepository = new TreeOfRoleRepositoryImpl();
        IActiveOrderRepository activeOrderRepository = new OrderRepositoryImpl();
        iTicketRepository ticketRepository = new TicketRepositoryImpl();
        iPurchasedOrderRepository purchasedOrderRepository = new PurchasedOrderRepositoryImpl();
        iDiscountPolicyRepository discountPolicyRepository = new DiscountPolicyRepositoryAdapter(jpaDiscountPolicyRepository);
        iPurchasePolicyRepository purchasePolicyRepository = new PurchasePolicyRepositoryAdapter(jpaPurchasePolicyRepository);
        iAdminRepository adminRepository = new AdminRepositoryImpl(){
            @Override
            public boolean isAdmin(String userID) {
                return userID.equals("admin");
            }
        };
        this.tokenService = new TokenService();
        IPasswordEncoder passwordEncoder = new PasswordEncoderImpl();
        ISupplyService supplyService = new SupplyServiceMock();
        IPaymentService paymentService = new PaymentServiceMock();
        IBarcodeGenerator barcodeGenerator = new BarcodeGeneratorMock();

        INotifier notifierMock = mock(INotifier.class);
        this.userService = new UserService(passwordEncoder, userRepository, tokenService, new NotificationRepositoryImpl(), notifierMock, treeOfRoleRepository);

        this.companyService = new CompanyService(companyRepository, userRepository, treeOfRoleRepository, tokenService, notifierMock);

        this.eventService = new EventService(companyRepository, eventRepository, tokenService,
                treeOfRoleRepository, ticketRepository, queueRepository, purchasedOrderRepository, userRepository, notifierMock, discountPolicyRepository);

        this.reserveTicketService = new OrderService(activeOrderRepository, tokenService, ticketRepository, userRepository, purchasePolicyRepository, notifierMock, eventRepository, mock(LotteryService.class));

        this.purchasedService = new PurchasedService(activeOrderRepository, ticketRepository,
                purchasedOrderRepository, supplyService,
                paymentService, barcodeGenerator,
                tokenService, treeOfRoleRepository, discountPolicyRepository, userRepository, notifierMock);

        this.adminService = new AdminService(
                treeOfRoleRepository,
                companyRepository,
                adminRepository,
                userRepository,
                purchasedOrderRepository,
                ticketRepository,
                eventRepository,
                tokenService,
                notifierMock,
                activeOrderRepository
        );

        userRepository.deleteAll();
        companyRepository.deleteAllCompany();
        eventRepository.deleteAllEvents();
        activeOrderRepository.deleteAllActiveOrders();
        purchasedOrderRepository.deleteAll();
        treeOfRoleRepository.deleteAllRoles();
        ticketRepository.deleteAllTickets();
        queueRepository.deleteAll();
        tokenService.clearAllData();
        adminRepository.deleteAll();
        purchasePolicyRepository.deleteAll();
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

    private MapArea[][] getMapArea() {
        MapArea[][] map = new MapArea[2][2];
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                map[i][j] = MapArea.SEAT;
            }
        }
        return map;
    }
    private CreditCardDetails createCreditCardDetails() {
        return      new CreditCardDetails(
                "0000000000000000", // card_number
                "12",               // month
                "2030",             // year
                "System Check",     // holder
                "000",              // cvv
                "00000000"          // id
        );
    }
    @Test
    @DisplayName("1. Close Company Success")
    void closeCompanySuccess1() {
        reg("owner1", "password");
        String token = log("owner1", "password");
        companyService.CreateCompany("C1", token);
        eventService.createEvent(token, "E1", "C1", EventType.PLAY, 100, new Date(), "Loc", "C1", getMapArea());

        var response = adminService.CloseCompany("C1", "admin");

        assertTrue(response.isSuccess());
        assertTrue(eventService.getCompanyInfo(token, "C1").isError());
        assertTrue(eventService.getCompanyEvents(token, "C1").isError());
        assertTrue(companyService.GetRoleTreeString(token, "C1").isError());
    }

    @Test
    @DisplayName("2. Close Company Failed - Not Admin")
    void closeCompanyFailedNotAdmin2() {
        reg("owner1", "password");
        String token = log("owner1", "password");
        companyService.CreateCompany("C1", token);

        var response = adminService.CloseCompany("C1", "not_admin");
        assertFalse(response.isSuccess());
    }

    @Test
    @DisplayName("3. Remove User Success")
    void removeUserSuccess3() {
        reg("userToRemove", "123");
        String userID = (userRepository.getUserByUsername("userToRemove")).getID();
        var response = adminService.removeUser(userID, "admin");
        assertTrue(response.isSuccess());

        assertTrue(userService.login(gt(), "userToRemove", "123").isError());
    }

    @Test
    @DisplayName("4. Remove User Failed - Not Admin")
    void removeUserFailedNotAdmin4() {
        reg("user1", "123");
        var response = adminService.removeUser("user1", "not_admin");
        assertFalse(response.isSuccess());
    }

    @Test
    @DisplayName("5. Get All Purchased Orders Success")
    void getAllPurchasedOrdersSuccess5() {
        reg("owner", "p");
        String tO = log("owner", "p");
        companyService.CreateCompany("C1", tO);
        eventService.createEvent(tO, "E1", "C1", EventType.PLAY, 100, new Date(), "L", "C1", getMapArea());

        reg("buyer", "p");
        String tB = log("buyer", "p");
        String orderId = reserveTicketService.reserveTickets(tB, "C1", "E1", List.of(new int[]{0, 0, 1}), null).getData();
        purchasedService.PurchaseTicket("b@gmail.com", orderId, "buyer", "none",createCreditCardDetails());

        var response = adminService.GetAllPurchasedOrders("admin");
        assertTrue(response.isSuccess());
        List<PurchaseOrderDTO> result = response.getData();
        boolean isCompanyExist = false;
        boolean isEventExist = false;
        boolean isPurchased = false;
        for (PurchaseOrderDTO po : result) {
            List<TicketDTO> ticketsList = po.tickets();
            for (TicketDTO ticket : ticketsList) {
                if(ticket.isPurchased()){ isPurchased = true; }
                if(ticket.company().equals("C1")){ isCompanyExist = true; }
                if(ticket.event().equals("E1")){ isEventExist = true; }
            }
        }
        assertNotNull(result);
        assertTrue(isCompanyExist);
        assertTrue(isEventExist);
        assertTrue(isPurchased);
    }

    @Test
    @DisplayName("6. Get All Purchased Orders Multiple Success")
    void getAllPurchasedOrdersMultipleSuccess6() {
        reg("o1", "p");
        String tO = log("o1", "p");
        companyService.CreateCompany("C1", tO);
        eventService.createEvent(tO, "E1", "C1", EventType.PLAY, 100, new Date(), "L", "C1", getMapArea());

        reg("b1", "p");
        String tB1 = log("b1", "p");
        String o1 = reserveTicketService.reserveTickets(tB1, "C1", "E1", List.of(new int[]{0, 0, 1}), null).getData();
        purchasedService.PurchaseTicket("b1@gmail.com", o1, "b1", "none",createCreditCardDetails());

        reg("b2", "p");
        String tB2 = log("b2", "p");
        String o2 = reserveTicketService.reserveTickets(tB2, "C1", "E1", List.of(new int[]{1, 1, 1}), null).getData();
        purchasedService.PurchaseTicket("b2@gmail.com", o2, "b2", "none",createCreditCardDetails());

        var response = adminService.GetAllPurchasedOrders("admin");
        assertTrue(response.isSuccess());
        List<PurchaseOrderDTO> result = response.getData();
        boolean isCompanyExist = false;
        boolean isEventExist = false;
        boolean isPurchased = false;

        for (PurchaseOrderDTO po : result) {
            List<TicketDTO> ticketsList = po.tickets();
            for (TicketDTO ticket : ticketsList) {
                if(ticket.isPurchased()){ isPurchased = true; }
                if(ticket.company().equals("C1")){ isCompanyExist = true; }
                if(ticket.event().equals("E1")){ isEventExist = true; }
            }
        }
        assertNotNull(result);
        assertTrue(isCompanyExist);
        assertTrue(isEventExist);
        assertTrue(isPurchased);
    }

    @Test
    @DisplayName("7. Get All Purchased Orders Failed - Not Admin")
    void getAllPurchasedOrdersFailedNotAdmin7() {
        var response = adminService.GetAllPurchasedOrders("not_an_admin");
        assertFalse(response.isSuccess());
        assertNull(response.getData());
    }

    @Test
    @DisplayName("8. Get All Purchased Orders Two Companies")
    void getAllPurchasedOrdersTwoCompanies8() {
        reg("ownerA", "p");
        String tOA = log("ownerA", "p");
        companyService.CreateCompany("CompA", tOA);
        eventService.createEvent(tOA, "EventA", "CompA", EventType.PLAY, 100, new Date(), "LocA", "CompA", getMapArea());

        reg("ownerB", "p");
        String tOB = log("ownerB", "p");
        companyService.CreateCompany("CompB", tOB);
        eventService.createEvent(tOB, "EventB", "CompB", EventType.PLAY, 100, new Date(), "LocB", "CompB", getMapArea());

        reg("buyer8", "p");
        String tB = log("buyer8", "p");

        String orderA = reserveTicketService.reserveTickets(tB, "CompA", "EventA", List.of(new int[]{0, 0, 1}), null).getData();
        purchasedService.PurchaseTicket("b@gmail.com", orderA, "buyer8", "none",createCreditCardDetails());

        String orderB = reserveTicketService.reserveTickets(tB, "CompB", "EventB", List.of(new int[]{0, 0, 1}), null).getData();
        purchasedService.PurchaseTicket("b@gmail.com", orderB, "buyer8", "none",createCreditCardDetails());

        var response = adminService.GetAllPurchasedOrders("admin");
        assertTrue(response.isSuccess());
        List<PurchaseOrderDTO> result = response.getData();
        assertNotNull(result);
        assertEquals(2, result.size());

        boolean sawCompA = false;
        boolean sawCompB = false;
        for (PurchaseOrderDTO po : result) {
            for (TicketDTO ticket : po.tickets()) {
                if (ticket.company().equals("CompA")) sawCompA = true;
                if (ticket.company().equals("CompB")) sawCompB = true;
            }
        }
        assertTrue(sawCompA);
        assertTrue(sawCompB);
    }

    @Test
    @DisplayName("9. Suspend User Temporarily Success")
    void suspendUserTemporarilySuccess9() {
        reg("userToSuspend", "password123");
        String targetUserId = userRepository.getUserByUsername("userToSuspend").getID();

        var response = adminService.suspendUser(targetUserId, "admin", 7);

        assertTrue(response.isSuccess());

        Suspension suspension = userRepository.getCurrentSuspensionByUserID(targetUserId);
        assertNotNull(suspension);
        assertTrue(suspension.getEndTime().isAfter(LocalDateTime.now()));
    }

    @Test
    @DisplayName("10. Suspend User Permanently Success")
    void suspendUserPermanentlySuccess10() {
        reg("userToSuspendForever", "password123");
        String targetUserId = userRepository.getUserByUsername("userToSuspendForever").getID();

        var response = adminService.suspendUser(targetUserId, "admin", 0);

        assertTrue(response.isSuccess());

        Suspension suspension =userRepository.getCurrentSuspensionByUserID(targetUserId);
        assertNotNull(suspension);
        assertNull(suspension.getEndTime());
        assertTrue(suspension.isPermanent());
    }

    @Test
    @DisplayName("11. Cancel Suspension Success")
    void cancelSuspensionSuccess11() {
        reg("suspendedUser", "password123");
        String targetUserId = userRepository.getUserByUsername("suspendedUser").getID();
        adminService.suspendUser(targetUserId, "admin", 5);

        assertNotNull(userRepository.getCurrentSuspensionByUserID(targetUserId));

        var response = adminService.cancelSuspension(targetUserId, "admin");

        assertTrue(response.isSuccess());

        assertNull(userRepository.getCurrentSuspensionByUserID(targetUserId));
        assertFalse(userRepository.isUserSuspendedNow(targetUserId));
    }

    @Test
    @DisplayName("12. Get All Suspensions Success")
    void getAllSuspensionsSuccess12() {
        reg("userA", "p");
        reg("userB", "p");
        String idA = userRepository.getUserByUsername("userA").getID();
        String idB = userRepository.getUserByUsername("userB").getID();

        adminService.suspendUser(idA, "admin", 3);
        adminService.suspendUser(idB, "admin", 5);

        var response = adminService.getAllSuspensions("admin");

        assertTrue(response.isSuccess());
        assertNotNull(response.getData());
        assertTrue(response.getData().size() == 2);
    }

    @Test
    @DisplayName("13. Suspend User Failed - Not Admin")
    void suspendUserFailedNotAdmin13() {
        reg("userToSuspend3", "password123");
        String targetUserId = userRepository.getUserByUsername("userToSuspend3").getID();

        var response = adminService.suspendUser(targetUserId, "not_admin", 5);

        assertFalse(response.isSuccess());

        assertNull(userRepository.getCurrentSuspensionByUserID(targetUserId));
    }

    @Test
    @DisplayName("14. Suspend User Failed - User Not Found")
    void suspendUserFailedUserNotFound14() {
        var response = adminService.suspendUser("fake-user-id-123", "admin", 5);

        assertFalse(response.isSuccess());
        assertNull(userRepository.getCurrentSuspensionByUserID("fake-user-id-123"));
    }

    @Test
    @DisplayName("15. Cancel Suspension Failed - Not Admin")
    void cancelSuspensionFailedNotAdmin15() {
        reg("suspendedUser3", "password123");
        String targetUserId = userRepository.getUserByUsername("suspendedUser3").getID();
        adminService.suspendUser(targetUserId, "admin", 5);

        var response = adminService.cancelSuspension(targetUserId, "not_admin");

        assertFalse(response.isSuccess());
        assertNotNull(userRepository.getCurrentSuspensionByUserID(targetUserId));
    }

    @Test
    @DisplayName("16. Get All Suspensions Failed - Not Admin")
    void getAllSuspensionsFailedNotAdmin16() {
        var response = adminService.getAllSuspensions("hacker_user");

        assertFalse(response.isSuccess());
        assertNull(response.getData());
    }

    @Test
    @DisplayName("17. Cancel Suspension Failed - User Not Suspended")
    void cancelSuspensionFailedNotSuspended17() {
        reg("activeUser", "password123");
        String targetUserId = userRepository.getUserByUsername("activeUser").getID();

        var response = adminService.cancelSuspension(targetUserId, "admin");

        assertFalse(response.isSuccess());
    }

    @Test
    @DisplayName("18. Cancel Suspension Failed - User Not Found")
    void cancelSuspensionFailedUserNotFound18() {
        var response = adminService.cancelSuspension("fake-user-id-999", "admin");

        assertFalse(response.isSuccess());
    }
}
