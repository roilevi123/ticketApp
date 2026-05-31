package AcceptanceTests;

import com.ticketing.ticketapp.Appliction.*;
import com.ticketing.ticketapp.Domain.AdminAggregate.iAdminRepository;
import com.ticketing.ticketapp.Domain.Company.iCompanyRepository;
import com.ticketing.ticketapp.Domain.Discount.iDiscountPolicyRepository;
import com.ticketing.ticketapp.Domain.Event.EventType;
import com.ticketing.ticketapp.Domain.Event.MapArea;
import com.ticketing.ticketapp.Domain.Event.iEventRepository;
import com.ticketing.ticketapp.Domain.Order.IActiveOrderRepository;
import com.ticketing.ticketapp.Domain.OwnerManagerTree.Permission;
import com.ticketing.ticketapp.Domain.OwnerManagerTree.iTreeOfRoleRepository;
import com.ticketing.ticketapp.Domain.PurchasePolicy.iPurchasePolicyRepository;
import com.ticketing.ticketapp.Domain.PurchasedOrderAggregate.PurchaseOrderDTO;
import com.ticketing.ticketapp.Domain.PurchasedOrderAggregate.iPurchasedOrderRepository;
import com.ticketing.ticketapp.Domain.QueueAggregates.iQueueRepository;
import com.ticketing.ticketapp.Domain.Ticket.TicketDTO;
import com.ticketing.ticketapp.Domain.Ticket.iTicketRepository;
import com.ticketing.ticketapp.Domain.User.IUserRepository;
import com.ticketing.ticketapp.Infastructure.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import com.ticketing.ticketapp.Appliction.INotifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
@org.springframework.boot.test.context.SpringBootTest
@org.springframework.test.context.ContextConfiguration(classes = com.ticketing.ticketapp.TicketappApplication.class)
@org.springframework.boot.autoconfigure.domain.EntityScan(basePackages = "com.ticketing.ticketapp")
@org.springframework.data.jpa.repository.config.EnableJpaRepositories(basePackages = "com.ticketing.ticketapp")
@org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase(replace = org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
@DisplayName("Complete Purchase Order Acceptance Tests")
public class PurchaseOrderTests {

    private UserService userService;
    private CompanyService companyService;
    private EventService eventService;
    private OrderService reserveTicketService;
    private PurchasedService purchasedService;
    private TokenService tokenService;
    private IUserRepository userRepository;
    private AdminService adminService;
    @org.springframework.beans.factory.annotation.Autowired
    private com.ticketing.ticketapp.Domain.Discount.JpaDiscountPolicyRepository jpaDiscountPolicyRepository;
    @Autowired
    private JpaPurchasePolicyRepository jpaPurchasePolicyRepository;
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
        iDiscountPolicyRepository discountPolicyRepository = new com.ticketing.ticketapp.Infastructure.DataBaseInterface.DiscountPolicyRepositoryAdapter(jpaDiscountPolicyRepository);
        iPurchasePolicyRepository purchasePolicyRepository = new com.ticketing.ticketapp.Infastructure.PurchasePolicyRepositoryAdapter(jpaPurchasePolicyRepository);
        INotifier notifierMock = mock(INotifier.class);

        this.tokenService = new TokenService();
        IPasswordEncoder passwordEncoder = new PasswordEncoderImpl();
        ISupplyService supplyService = new SupplyServiceMock();
        IPaymentService paymentService = new PaymentServiceMock();
        IBarcodeGenerator barcodeGenerator = new BarcodeGeneratorMock();

        this.userService = new UserService(passwordEncoder, userRepository, tokenService, new NotificationRepositoryImpl(), notifierMock, treeOfRoleRepository);
        this.companyService = new CompanyService(companyRepository, userRepository, treeOfRoleRepository, tokenService, notifierMock);
        this.eventService = new EventService(companyRepository, eventRepository, tokenService, treeOfRoleRepository, ticketRepository, queueRepository, purchasedOrderRepository, userRepository, notifierMock, discountPolicyRepository    );
        this.reserveTicketService = new OrderService(activeOrderRepository, tokenService, ticketRepository, userRepository, purchasePolicyRepository, notifierMock, eventRepository, mock(LotteryService.class));
        this.purchasedService = new PurchasedService(activeOrderRepository, ticketRepository, purchasedOrderRepository, supplyService, paymentService, barcodeGenerator, tokenService, treeOfRoleRepository, discountPolicyRepository, userRepository, notifierMock);

        this.userRepository = userRepository;
        iAdminRepository adminRepository = new AdminRepositoryImpl(){
            @Override
            public boolean isAdmin(String userID) {
                return userID.equals("admin");
            }
        };
        this.adminService = new AdminService(treeOfRoleRepository, companyRepository, adminRepository, userRepository, purchasedOrderRepository, ticketRepository, eventRepository, tokenService, new NotifierImpl(new Broadcaster(new NotificationRepositoryImpl())), new OrderRepositoryImpl());

        activeOrderRepository.deleteAllActiveOrders();
        eventRepository.deleteAllEvents();
        treeOfRoleRepository.deleteAllRoles();
        companyRepository.deleteAllCompany();
        purchasedOrderRepository.deleteAll();
        queueRepository.deleteAll();
        ticketRepository.deleteAllTickets();
        userRepository.deleteAll();
        tokenService.clearAllData();
//        jpaDiscountPolicyRepository.deleteAll();
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

    private MapArea[][] getMapArea1() {
        MapArea[][] map = getMapArea();
        map[1][1] = MapArea.STAND;
        return map;
    }

    @Test @DisplayName("1. Purchased Ticket Success")
    void purchasedTicketSuccess1() {
        reg("1", "1");
        String token = log("1", "1");
        companyService.CreateCompany("1", token);
        eventService.createEvent(token, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea());
        reg("2", "2");
        String token1 = log("2", "2");
        List<int[]> requests = List.of(new int[]{0, 0, 1});
        String orderId = reserveTicketService.reserveTickets(token1, "1", "1", requests, null).getData();
        assertEquals("success", purchasedService.PurchaseTicket("ro@gmail.com", orderId, "2", "none").getData());
    }

    @Test @DisplayName("2. Purchased Ticket Failed - Order Expired")
    void purchasedTicketFailedOrderExpired2() throws InterruptedException {
        reg("1", "1");
        String token = log("1", "1");
        companyService.CreateCompany("1", token);
        eventService.createEvent(token, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea());
        reg("2", "2");
        String token1 = log("2", "2");
        List<int[]> requests = List.of(new int[]{0, 0, 1});
        String orderId = reserveTicketService.reserveTickets(token1, "1", "1", requests, null).getData();
        Thread.sleep(11000);
        assertTrue(purchasedService.PurchaseTicket("ro@gmail.com", orderId, "2", "none").isError());
    }

    @Test @DisplayName("3. Purchased Ticket Success - Multiple Spots")
    void purchasedTicketSuccess3() {
        reg("1", "1");
        String token = log("1", "1");
        companyService.CreateCompany("1", token);
        eventService.createEvent(token, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea1());
        reg("2", "2");
        String token1 = log("2", "2");
        List<int[]> requests = Arrays.asList(new int[]{0, 0, 1}, new int[]{1, 1, 1});
        String orderId = reserveTicketService.reserveTickets(token1, "1", "1", requests, null).getData();
        assertEquals("success", purchasedService.PurchaseTicket("ro@gmail.com", orderId, "2", "none").getData());
    }

    @Test @DisplayName("4. Get Company Transaction Success")
    void getCompanyTransactionSuccess4() {
        reg("1", "1");
        String token = log("1", "1");
        companyService.CreateCompany("1", token);
        eventService.createEvent(token, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea());
        reg("2", "2");
        String token1 = log("2", "2");
        List<int[]> requests = List.of(new int[]{0, 0, 1});
        String orderId = reserveTicketService.reserveTickets(token1, "1", "1", requests, null).getData();
        purchasedService.PurchaseTicket("ro@gmail.com", orderId, "2", "none");
        List<PurchaseOrderDTO> result = purchasedService.getCompanyTransaction("1", token).getData();

        boolean isCompanyExist = false;
        boolean isEventExist = false;
        boolean isPurchased = false;

        for (PurchaseOrderDTO po : result) {
            List<TicketDTO> ticketsList = po.tickets();
            for (TicketDTO ticket : ticketsList) {
                if (ticket.isPurchased()) { isPurchased = true; }
                if (ticket.company().equals("1")) { isCompanyExist = true; }
                if (ticket.event().equals("1")) { isEventExist = true; }
            }
        }
        assertNotNull(result);
        assertTrue(isCompanyExist);
        assertTrue(isEventExist);
        assertTrue(isPurchased);
    }

    @Test @DisplayName("5. Get Company Transaction Success - Stand & Seat")
    void getCompanyTransactionSuccess5() {
        reg("1", "1");
        String token = log("1", "1");
        companyService.CreateCompany("1", token);
        eventService.createEvent(token, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea1());
        reg("2", "2");
        String token1 = log("2", "2");
        List<int[]> requests = Arrays.asList(new int[]{0, 0, 1}, new int[]{1, 1, 1});
        String orderId = reserveTicketService.reserveTickets(token1, "1", "1", requests, null).getData();
        purchasedService.PurchaseTicket("ro@gmail.com", orderId, "2", "none");
        List<PurchaseOrderDTO> result = purchasedService.getCompanyTransaction("1", token).getData();

        boolean isCompanyExist = false;
        boolean isEventExist = false;
        boolean isPurchased = false;
        boolean isTheStand = false;
        boolean isTheSeat = false;

        for (PurchaseOrderDTO po : result) {
            List<TicketDTO> ticketsList = po.tickets();
            for (TicketDTO ticket : ticketsList) {
                if (ticket.isPurchased()) { isPurchased = true; }
                if (ticket.company().equals("1")) { isCompanyExist = true; }
                if (ticket.event().equals("1")) { isEventExist = true; }
                if (ticket.col() == 0 && ticket.row() == 0) { isTheStand = true; }
                if (ticket.col() == 1 && ticket.row() == 1) { isTheSeat = true; }
            }
        }
        assertNotNull(result);
        assertTrue(isCompanyExist);
        assertTrue(isEventExist);
        assertTrue(isPurchased);
        assertTrue(isTheStand);
        assertTrue(isTheSeat);
    }

    @Test @DisplayName("6. Get Company Transaction Success - Manager with Permission")
    void getCompanyTransactionSuccess6() {
        reg("1", "1");
        String token = log("1", "1");
        companyService.CreateCompany("1", token);
        reg("3", "3");
        String token3 = log("3", "3");
        companyService.AppointAManager("3", "1", Set.of(Permission.GENERATE_SALES_REPORTS), token);
        companyService.ApproveAppointmentForManager(token3, "1");

        eventService.createEvent(token, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea());
        reg("2", "2");
        String token2 = log("2", "2");
        String orderId = reserveTicketService.reserveTickets(token2, "1", "1", List.of(new int[]{0, 0, 1}), null).getData();
        purchasedService.PurchaseTicket("ro@gmail.com", orderId, "2", "none");

        List<PurchaseOrderDTO> result = purchasedService.getCompanyTransaction("1", token3).getData();

        boolean isCompanyExist = false;
        boolean isEventExist = false;
        boolean isPurchased = false;

        for (PurchaseOrderDTO po : result) {
            List<TicketDTO> ticketsList = po.tickets();
            for (TicketDTO ticket : ticketsList) {
                if (ticket.isPurchased()) { isPurchased = true; }
                if (ticket.company().equals("1")) { isCompanyExist = true; }
                if (ticket.event().equals("1")) { isEventExist = true; }
            }
        }
        assertNotNull(result);
        assertTrue(isCompanyExist);
        assertTrue(isEventExist);
        assertTrue(isPurchased);
    }

    @Test
    @DisplayName("7. Get Company Transaction Success - Multiple Events")
    void getCompanyTransactionSuccess7() {
        reg("owner1", "p");
        String token = log("owner1", "p");
        companyService.CreateCompany("1", token);

        eventService.createEvent(token, "E1", "1", EventType.PLAY, 100, new Date(), "L1", "1", getMapArea());
        eventService.createEvent(token, "E2", "1", EventType.PLAY, 100, new Date(), "L2", "1", getMapArea());

        reg("buyer2", "p");
        String tB2 = log("buyer2", "p");
        reg("buyer3", "p");
        String tB3 = log("buyer3", "p");

        List<int[]> requests = List.of(new int[]{0, 0, 1});

        String o1 = reserveTicketService.reserveTickets(tB2, "1", "E1", requests, null).getData();
        String o2 = reserveTicketService.reserveTickets(tB3, "1", "E2", requests, null).getData();

        assertEquals("success", purchasedService.PurchaseTicket("r2@g.com", o1, "buyer2", "none").getData());
        assertEquals("success", purchasedService.PurchaseTicket("r3@g.com", o2, "buyer3", "none").getData());

        List<PurchaseOrderDTO> result = purchasedService.getCompanyTransaction("1", token).getData();

        boolean isCompanyExist = false;
        boolean isEvent1Exist = false;
        boolean isEvent2Exist = false;
        boolean isPurchased = false;

        for (PurchaseOrderDTO po : result) {
            List<TicketDTO> ticketsList = po.tickets();
            for (TicketDTO ticket : ticketsList) {
                if (ticket.isPurchased()) { isPurchased = true; }
                if (ticket.company().equals("1")) { isCompanyExist = true; }
                if (ticket.event().equals("E1")) { isEvent1Exist = true; }
                if (ticket.event().equals("E2")) { isEvent2Exist = true; }
            }
        }
        assertNotNull(result);
        assertTrue(isCompanyExist);
        assertTrue(isEvent1Exist);
        assertTrue(isEvent2Exist);
        assertTrue(isPurchased);
    }

    @Test @DisplayName("8. Get Company Transaction Security - Unauthorized")
    void getCompanyTransactionSecurity8() {
        reg("owner1", "pass");
        String token1 = log("owner1", "pass");
        companyService.CreateCompany("C1", token1);

        reg("owner2", "pass");
        String token2 = log("owner2", "pass");
        companyService.CreateCompany("C2", token2);

        Response<List<PurchaseOrderDTO>> result = purchasedService.getCompanyTransaction("C2", token1);
        assertTrue(result.isError());
    }

    @Test @DisplayName("9. Get Company Transaction - Multiple Events Detailed")
    void getCompanyTransactionMultipleEvents9() {
        reg("owner9", "p");
        String token = log("owner9", "p");
        companyService.CreateCompany("C9", token);

        eventService.createEvent(token, "E1", "C9", EventType.PLAY, 100, new Date(), "L1", "C9", getMapArea());
        eventService.createEvent(token, "E2", "C9", EventType.PLAY, 100, new Date(), "L2", "C9", getMapArea());

        reg("buyer9a", "p");
        String tA = log("buyer9a", "p");
        reg("buyer9b", "p");
        String tB = log("buyer9b", "p");

        String o1 = reserveTicketService.reserveTickets(tA, "C9", "E1", List.of(new int[]{0, 0, 1}), null).getData();
        String o2 = reserveTicketService.reserveTickets(tB, "C9", "E2", List.of(new int[]{0, 0, 1}), null).getData();

        assertEquals("success", purchasedService.PurchaseTicket("a@g.com", o1, "buyer9a", "none").getData());
        assertEquals("success", purchasedService.PurchaseTicket("b@g.com", o2, "buyer9b", "none").getData());

        List<PurchaseOrderDTO> result = purchasedService.getCompanyTransaction("C9", token).getData();
        assertNotNull(result);
        assertEquals(2, result.size());

        boolean sawE1 = false;
        boolean sawE2 = false;
        for (PurchaseOrderDTO po : result) {
            for (TicketDTO ticket : po.tickets()) {
                if (ticket.event().equals("E1")) sawE1 = true;
                if (ticket.event().equals("E2")) sawE2 = true;
            }
        }
        assertTrue(sawE1);
        assertTrue(sawE2);
    }

    @Test @DisplayName("10. Get User Transaction Success")
    void getUserTransactionSuccess10() {
        reg("10", "10");
        String tO = log("10", "10");
        companyService.CreateCompany("C10", tO);
        eventService.createEvent(tO, "E10", "C10", EventType.PLAY, 100, new Date(), "L", "C10", getMapArea());

        reg("20", "20");
        String tB = log("20", "20");
        String orderId = reserveTicketService.reserveTickets(tB, "C10", "E10", List.of(new int[]{0, 0, 1}), null).getData();
        purchasedService.PurchaseTicket("b@gmail.com", orderId, "20", "none");

        List<PurchaseOrderDTO> result = purchasedService.getUserTransaction(tB).getData();
        boolean isCompanyExist = false;
        boolean isEventExist = false;
        boolean isPurchased = false;
        for (PurchaseOrderDTO po : result) {
            List<TicketDTO> ticketsList = po.tickets();
            for (TicketDTO ticket : ticketsList) {
                if (ticket.isPurchased()) { isPurchased = true; }
                if (ticket.company().equals("C10")) { isCompanyExist = true; }
                if (ticket.event().equals("E10")) { isEventExist = true; }
            }
        }
        assertNotNull(result);
        assertTrue(isCompanyExist);
        assertTrue(isEventExist);
        assertTrue(isPurchased);
    }

    @Test
    @DisplayName("11. Get User Transaction - Multiple Companies")
    void getUserTransactionMultipleCompanies11() {
        reg("u1", "p");
        String tO1 = log("u1", "p");
        companyService.CreateCompany("CA", tO1);
        eventService.createEvent(tO1, "EA", "CA", EventType.PLAY, 100, new Date(), "LA", "CA", getMapArea());

        reg("u2", "p");
        String tO2 = log("u2", "p");
        companyService.CreateCompany("CB", tO2);
        eventService.createEvent(tO2, "EB", "CB", EventType.PLAY, 100, new Date(), "LB", "CB", getMapArea());

        reg("buyer", "p");
        String tB = log("buyer", "p");
        List<int[]> req = List.of(new int[]{0, 0, 1});

        String oA = reserveTicketService.reserveTickets(tB, "CA", "EA", req, null).getData();
        assertEquals("success", purchasedService.PurchaseTicket("b@g.com", oA, "buyer", "none").getData(), "Purchase for CA failed");

        String oB = reserveTicketService.reserveTickets(tB, "CB", "EB", req, null).getData();
        assertEquals("success", purchasedService.PurchaseTicket("b@g.com", oB, "buyer", "none").getData(), "Purchase for CB failed");

        List<PurchaseOrderDTO> result = purchasedService.getUserTransaction(tB).getData();
        boolean isCompany1Exist = false;
        boolean isCompany2Exist = false;
        boolean isEvent1Exist = false;
        boolean isEvent2Exist = false;
        boolean isPurchased = false;
        for (PurchaseOrderDTO po : result) {
            List<TicketDTO> ticketsList = po.tickets();
            for (TicketDTO ticket : ticketsList) {
                if (ticket.isPurchased()) { isPurchased = true; }
                if (ticket.company().equals("CA")) { isCompany1Exist = true; }
                if (ticket.company().equals("CB")) { isCompany2Exist = true; }
                if (ticket.event().equals("EA")) { isEvent1Exist = true; }
                if (ticket.event().equals("EB")) { isEvent2Exist = true; }
            }
        }
        assertNotNull(result);
        assertTrue(isCompany1Exist);
        assertTrue(isCompany2Exist);
        assertTrue(isEvent1Exist);
        assertTrue(isEvent2Exist);
        assertTrue(isPurchased);
    }

    @Test @DisplayName("12. Get User Transaction Security - Unauthorized View")
    void getUserTransactionSecurity12() {
        reg("u2", "p"); String t2 = log("u2", "p");
        Response<List<PurchaseOrderDTO>> result = purchasedService.getUserTransaction(t2);
        assertTrue(result.isSuccess());
        assertEquals(new ArrayList<>(), result.getData());
    }

    @Test @DisplayName("13. Purchase Order As Guest Success")
    void purchaseOrderAsGuest13() {
        reg("own", "p");
        String tO = log("own", "p");
        companyService.CreateCompany("SecC", tO);
        eventService.createEvent(tO, "SecE", "SecC", EventType.PLAY, 100, new Date(), "L", "SecC", getMapArea());

        String guestToken = tokenService.generateGuestToken();
        String order1 = reserveTicketService.reserveTickets(guestToken, "SecC", "SecE", List.of(new int[]{0, 0, 1}), null).getData();
        assertEquals("success", purchasedService.PurchaseTicket("u1@gmail.com", order1, "guestUser", "none").getData());
    }

    @Test @DisplayName("14. Purchased Ticket Success After App Re-entry")
    void purchasedTicketSuccessAndGETOutTheApp14() {
        reg("1", "1");
        String tO = log("1", "1");
        companyService.CreateCompany("C1", tO);
        eventService.createEvent(tO, "E1", "C1", EventType.PLAY, 100, new Date(), "L", "C1", getMapArea());

        reg("2", "2");
        String tB = log("2", "2");
        String orderId = reserveTicketService.reserveTickets(tB, "C1", "E1", List.of(new int[]{0, 0, 1}), null).getData();

        userService.logout(tB);
        log("2", "2");
        assertEquals("success", purchasedService.PurchaseTicket("ro@gmail.com", orderId, "2", "none").getData());
    }

    @Test @DisplayName("15. Purchase Order Guest Fail - App Exit")
    void purchaseOrderAsGuestFaildBecauseHegetOut15() {
        reg("own", "p");
        String tO = log("own", "p");
        companyService.CreateCompany("SecC", tO);
        eventService.createEvent(tO, "SecE", "SecC", EventType.PLAY, 100, new Date(), "L", "SecC", getMapArea());

        assertTrue(purchasedService.PurchaseTicket("u1@gmail.com", "invalid_id", "user1", "none").isError());
    }

    @Test @DisplayName("16. Purchased Ticket Fail - App Re-entry but Expired")
    void purchasedTicketSuccessAndGETOutTheAppAndTheOrderExpired16() throws InterruptedException {
        reg("1", "1");
        String tO = log("1", "1");
        companyService.CreateCompany("C1", tO);
        eventService.createEvent(tO, "E1", "C1", EventType.PLAY, 100, new Date(), "L", "C1", getMapArea());

        reg("2", "2");
        String tB = log("2", "2");
        String orderId = reserveTicketService.reserveTickets(tB, "C1", "E1", List.of(new int[]{0, 0, 1}), null).getData();

        userService.logout(tB);
        Thread.sleep(11000);
        log("2", "2");
        assertTrue(purchasedService.PurchaseTicket("ro@gmail.com", orderId, "2", "none").isError());
    }

    @Test
    void INValidTokenForSeeCompanyTransNotAuthorizred() {
        reg("1", "1");
        String tO = log("1", "1");
        Response<List<PurchaseOrderDTO>> t = purchasedService.getCompanyTransaction("q", tO);
        assertTrue(t.isError());
    }

    @Test
    void INValidTokenForSeeCompanyTrans() {
        Response<List<PurchaseOrderDTO>> t = purchasedService.getCompanyTransaction("q", "tO");
        assertTrue(t.isError());
    }

    @Test @DisplayName("17. Fail - Purchase Ticket When User Is Suspended")
    void purchaseTicketFailedUserSuspended17() {
        reg("owner_user", "password123");
        String ownerToken = log("owner_user", "password123");
        companyService.CreateCompany("company1", ownerToken);
        eventService.createEvent(ownerToken, "event1", "artist1", EventType.PLAY, 100, new Date(), "location1", "company1", getMapArea());

        reg("suspended_buyer", "password456");
        String buyerToken = log("suspended_buyer", "password456");

        List<int[]> requests = List.of(new int[]{0, 0, 1});
        String orderId = reserveTicketService.reserveTickets(buyerToken, "company1", "event1", requests, null).getData();
        assertTrue(isNumeric(orderId), "Reservation should succeed initially");

        reg("admin", "admin");
        log("admin", "admin");

        String realBuyerId = tokenService.extractUserId(buyerToken);

        adminService.suspendUser(realBuyerId, "admin", 7);

        Response<String> purchaseResponse = purchasedService.PurchaseTicket("buyer@gmail.com", orderId, buyerToken, "none");

        assertTrue(purchaseResponse.isError(), "Purchase should fail for suspended user");
        assertEquals("User is suspended", purchaseResponse.getMessage());
    }

    private boolean isNumeric(String str) {
        if (str == null) return false;
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}

