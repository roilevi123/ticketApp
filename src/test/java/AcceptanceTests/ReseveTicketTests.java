package AcceptanceTests;

import com.ticketing.ticketapp.Appliction.*;
import com.ticketing.ticketapp.Domain.AdminAggregate.iAdminRepository;
import com.ticketing.ticketapp.Domain.Company.iCompanyRepository;
import com.ticketing.ticketapp.Domain.Event.EventType;
import com.ticketing.ticketapp.Domain.Event.MapArea;
import com.ticketing.ticketapp.Domain.Event.iEventRepository;
import com.ticketing.ticketapp.Domain.Order.IActiveOrderRepository;
import com.ticketing.ticketapp.Domain.OwnerManagerTree.iTreeOfRoleRepository;
import com.ticketing.ticketapp.Domain.PurchasePolicy.iPurchasePolicyRepository;
import com.ticketing.ticketapp.Domain.PurchasedOrderAggregate.iPurchasedOrderRepository;
import com.ticketing.ticketapp.Domain.QueueAggregates.iQueueRepository;
import com.ticketing.ticketapp.Domain.Ticket.TicketDTO;
import com.ticketing.ticketapp.Domain.Discount.iDiscountPolicyRepository;
import com.ticketing.ticketapp.Domain.Ticket.iTicketRepository;
import com.ticketing.ticketapp.Domain.User.IUserRepository;
import com.ticketing.ticketapp.Infastructure.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@SpringBootTest(classes = com.ticketing.ticketapp.TicketappApplication.class)
@ActiveProfiles("test")
@DisplayName("Complete Reserve Ticket Acceptance Tests")
public class ReseveTicketTests {

    @Autowired private IUserRepository userRepository;
    @Autowired private iCompanyRepository companyRepository;
    @Autowired private iEventRepository eventRepository;
    @Autowired private iQueueRepository queueRepository;
    @Autowired private iTreeOfRoleRepository treeOfRoleRepository;
    @Autowired private IActiveOrderRepository activeOrderRepository;
    @Autowired private iTicketRepository ticketRepository;
    @Autowired private iPurchasedOrderRepository purchasedOrderRepository;
    @Autowired private JpaPurchasePolicyRepository jpaPurchasePolicyRepository;

    private UserService userService;
    private CompanyService companyService;
    private EventService eventService;
    private OrderService reserveTicketService;
    private TokenService tokenService;
    private AdminService adminService;

    @BeforeEach
    void setUp() {
        this.tokenService = new TokenService();
        IPasswordEncoder passwordEncoder = new PasswordEncoderImpl();
        iPurchasePolicyRepository purchasePolicyRepository = new PurchasePolicyRepositoryAdapter(jpaPurchasePolicyRepository);
        INotifier notifierMock = mock(INotifier.class);
        LotteryService lotteryServiceMock = mock(LotteryService.class);

        this.userService = new UserService(passwordEncoder, userRepository, tokenService, new NotificationRepositoryImpl(), notifierMock, treeOfRoleRepository);
        this.companyService = new CompanyService(companyRepository, userRepository, treeOfRoleRepository, tokenService, notifierMock);
        this.eventService = new EventService(companyRepository, eventRepository, tokenService, treeOfRoleRepository, ticketRepository, queueRepository, purchasedOrderRepository, userRepository, notifierMock, mock(iDiscountPolicyRepository.class));
        this.reserveTicketService = new OrderService(activeOrderRepository, tokenService, ticketRepository, userRepository, purchasePolicyRepository, notifierMock, eventRepository, lotteryServiceMock);

        iAdminRepository adminRepository = new AdminRepositoryImpl(){
            @Override
            public boolean isAdmin(String userID) {
                return userID.equals("admin");
            }
        };
        this.adminService = new AdminService(treeOfRoleRepository, companyRepository, adminRepository, userRepository, purchasedOrderRepository, ticketRepository, eventRepository, tokenService, new NotifierImpl(new Broadcaster(new NotificationRepositoryImpl())), activeOrderRepository);

        activeOrderRepository.deleteAllActiveOrders();
        eventRepository.deleteAllEvents();
        treeOfRoleRepository.deleteAllRoles();
        companyRepository.deleteAllCompany();
        purchasedOrderRepository.deleteAll();
        queueRepository.deleteAll();
        ticketRepository.deleteAllTickets();
        userRepository.deleteAll();
        purchasePolicyRepository.deleteAll();
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

    @Test @DisplayName("1. Reserve Ticket Success")
    void reserveTicketTestPass1() {
        reg("1", "1");
        String token = log("1", "1");
        companyService.CreateCompany("1", token);
        eventService.createEvent(token, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea());

        reg("2", "2");
        String token1 = log("2", "2");
        List<int[]> requests = List.of(new int[]{0, 0, 1});

        Response<String> result = reserveTicketService.reserveTickets(token1, "1", "1", requests, null);
        assertTrue(result.isSuccess(), "Reservation should succeed");
        assertNotNull(result.getData(), "Expected a valid order ID on success");
    }

    @Test @DisplayName("2. Fail - Not Enough Tickets (Double Booking)")
    void reserveTicketNotEnoughTickets2() {
        reg("1", "1");
        String token = log("1", "1");
        companyService.CreateCompany("1", token);
        eventService.createEvent(token, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea());

        reg("2", "2");
        String token1 = log("2", "2");
        List<int[]> requests = List.of(new int[]{0, 0, 1});

        reserveTicketService.reserveTickets(token, "1", "1", requests, null);

        Response<String> result = reserveTicketService.reserveTickets(token1, "1", "1", requests, null);
        assertTrue(result.isError(), "Expected error for double booking");
    }

    @Test @DisplayName("3. Fail - Out of Bounds")
    void reserveTicketOutOfBounds3() {
        reg("1", "1");
        String token = log("1", "1");
        companyService.CreateCompany("1", token);
        eventService.createEvent(token, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea());

        List<int[]> requests = List.of(new int[]{5, 5, 1});
        Response<String> result = reserveTicketService.reserveTickets(token, "1", "1", requests, null);
        assertTrue(result.isError(), "Expected error for out of bounds coordinates");
    }

    @Test @DisplayName("4. Reserve Multiple Spots Success")
    void reserveTicketMultipleSpots4() {
        reg("1", "1");
        String token = log("1", "1");
        companyService.CreateCompany("1", token);
        eventService.createEvent(token, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea());

        List<int[]> requests = Arrays.asList(new int[]{0, 0, 1}, new int[]{1, 1, 1});
        Response<String> result = reserveTicketService.reserveTickets(token, "1", "1", requests, null);
        assertTrue(result.isSuccess(), "Reservation should succeed for multiple valid spots");
        assertNotNull(result.getData());
    }

    @Test @DisplayName("5. Fail - Already Reserved Spot")
    void reserveTicketAlreadyReserved5() {
        reg("1", "1");
        String t1 = log("1", "1");
        companyService.CreateCompany("1", t1);
        eventService.createEvent(t1, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea());

        reg("2", "2");
        String t2 = log("2", "2");
        List<int[]> req = List.of(new int[]{0, 1, 1});

        Response<String> res1 = reserveTicketService.reserveTickets(t2, "1", "1", req, null);
        assertTrue(res1.isSuccess(), "First reservation should succeed");

        Response<String> res2 = reserveTicketService.reserveTickets(t2, "1", "1", req, null);
        assertTrue(res2.isError(), "Second reservation of same spot should fail");
    }

    @Test @DisplayName("6. Concurrent Reservations - Conflict")
    void reserveTicketConcurrentTwoThreads6() throws InterruptedException {
        reg("1", "1");
        String token = log("1", "1");
        companyService.CreateCompany("1", token);
        eventService.createEvent(token, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea());

        int threadCount = 2;
        ExecutorService service = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(1);
        List<String> results = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            final int id = i + 2;
            service.submit(() -> {
                try {
                    userService.register(gt(), "u" + id, "p", 10, "u" + id + "@test.com");
                    String ut = userService.login(gt(), "u" + id, "p").getData();
                    latch.await();
                    Response<String> resp = reserveTicketService.reserveTickets(ut, "1", "1", List.of(new int[]{0, 0, 1}), null);
                    if (resp.isSuccess() && resp.getData() != null) {
                        results.add(resp.getData());
                    }
                } catch (Exception ignored) {}
            });
        }
        latch.countDown();
        service.shutdown();
        service.awaitTermination(10, TimeUnit.SECONDS);
        assertEquals(1, results.size(), "Only one thread should successfully reserve the spot");
    }

    @Test @DisplayName("7. Sequential Stand/Seat Success")
    void reserveTicketStandSequential7() {
        reg("1", "1");
        String t = log("1", "1");
        companyService.CreateCompany("1", t);
        eventService.createEvent(t, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea1());

        reg("u1", "p"); String t1 = log("u1", "p");
        reg("u2", "p"); String t2 = log("u2", "p");

        Response<String> res1 = reserveTicketService.reserveTickets(t1, "1", "1", List.of(new int[]{1, 1, 1}), null);
        assertTrue(res1.isSuccess(), "Stand reservation should succeed");

        Response<String> res2 = reserveTicketService.reserveTickets(t2, "1", "1", List.of(new int[]{0, 0, 1}), null);
        assertTrue(res2.isSuccess(), "Seat reservation should succeed");
    }

    @Test @DisplayName("9. Expired Ticket Becomes Available Again")
    void reserveTicketExpiredTicketAvailableAgain9() throws InterruptedException {
        reg("1", "1");
        String token = log("1", "1");
        companyService.CreateCompany("1", token);
        eventService.createEvent(token, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea());

        reg("u1", "p"); String t1 = log("u1", "p");
        reg("u2", "p"); String t2 = log("u2", "p");
        List<int[]> req = List.of(new int[]{0, 0, 1});

        assertTrue(reserveTicketService.reserveTickets(t1, "1", "1", req, null).isSuccess(), "T1 should succeed initially");
        assertTrue(reserveTicketService.reserveTickets(t2, "1", "1", req, null).isError(), "T2 should fail as it's already reserved");

        Thread.sleep(10005);

        Response<String> res3 = reserveTicketService.reserveTickets(t2, "1", "1", req, null);
        assertTrue(res3.isSuccess(), "T2 should succeed after the first reservation expires");
        assertNotNull(res3.getData());
    }

    @Test @DisplayName("10. Get Active Order Tickets - Single")
    void getActiveOrderTicketsSingle10() {
        reg("1", "1");
        String token = log("1", "1");
        companyService.CreateCompany("1", token);
        eventService.createEvent(token, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea());

        reg("u1", "p"); String t1 = log("u1", "p");
        Response<String> res = reserveTicketService.reserveTickets(t1, "1", "1", List.of(new int[]{0, 0, 1}), null);
        assertTrue(res.isSuccess());
        String oid = res.getData();

        Response<List<TicketDTO>> ticketsResp = reserveTicketService.getActiveOrderTickets(t1, oid);
        assertTrue(ticketsResp.isSuccess());
        assertFalse(ticketsResp.getData().isEmpty());
        assertEquals("1", ticketsResp.getData().get(0).company());
    }

    @Test @DisplayName("13. Get Active Order - Success After Expiration Replacement")
    void getActiveOrderTicketsExpired13() throws InterruptedException {
        reg("1", "1");
        String t = log("1", "1");
        companyService.CreateCompany("1", t);
        eventService.createEvent(t, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea());

        reg("u1", "p"); String t1 = log("u1", "p");
        reg("u2", "p"); String t2 = log("u2", "p");
        List<int[]> req = List.of(new int[]{0, 0, 1});

        reserveTicketService.reserveTickets(t1, "1", "1", req, null);
        Thread.sleep(10005);

        Response<String> res = reserveTicketService.reserveTickets(t2, "1", "1", req, null);
        assertTrue(res.isSuccess());
        String oid2 = res.getData();

        Response<List<TicketDTO>> ticketsResp = reserveTicketService.getActiveOrderTickets(t2, oid2);
        assertTrue(ticketsResp.isSuccess());
        assertFalse(ticketsResp.getData().isEmpty());
    }

    @Test @DisplayName("14. Get Active Order Tickets - Guest Success")
    void getActiveOrderTicketsAsGuest14() {
        reg("1", "1");
        String t = log("1", "1");
        companyService.CreateCompany("1", t);
        eventService.createEvent(t, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea());

        String guestToken = tokenService.generateGuestToken();
        Response<String> res = reserveTicketService.reserveTickets(guestToken, "1", "1", List.of(new int[]{0, 0, 1}), null);
        assertTrue(res.isSuccess());
        String oid = res.getData();

        Response<List<TicketDTO>> ticketsResp = reserveTicketService.getActiveOrderTickets(guestToken, oid);
        assertTrue(ticketsResp.isSuccess());
        assertEquals("1", ticketsResp.getData().get(0).company());
    }

    @Test @DisplayName("15. Get Active Order Success After Re-Login")
    void getActiveOrderTicketsAAfterLogOut15() {
        reg("1", "1");
        String t = log("1", "1");
        companyService.CreateCompany("1", t);
        eventService.createEvent(t, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea());

        reg("u1", "p"); String t1 = log("u1", "p");
        Response<String> res = reserveTicketService.reserveTickets(t1, "1", "1", List.of(new int[]{0, 0, 1}), null);
        assertTrue(res.isSuccess());

        userService.logout(t1);
        String t1New = log("u1", "p");
        Response<List<TicketDTO>> ticketsResp = reserveTicketService.getActiveOrderTickets(t1New, null);
        assertTrue(ticketsResp.isSuccess());
        assertFalse(ticketsResp.getData().isEmpty());
    }

    @Test @DisplayName("16. Get Active Order Guest Fail After Data Loss")
    void getActiveOrderTicketsAsGuestThatGetOUt16() {
        reg("1", "1");
        String t = log("1", "1");
        companyService.CreateCompany("1", t);
        eventService.createEvent(t, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea());

        String guestToken = tokenService.generateGuestToken();
        Response<String> res = reserveTicketService.reserveTickets(guestToken, "1", "1", List.of(new int[]{0, 0, 1}), null);
        assertTrue(res.isSuccess());

        assertTrue(reserveTicketService.getActiveOrderTickets(guestToken, "invalidOrderId").isError());
    }

    @Test
    void InValidToken() {
        assertTrue(reserveTicketService.getActiveOrderTickets("", null).isError());
    }

    @Test
    @DisplayName("17. Fail - Reserve Ticket When User Is Suspended")
    void reserveTicketFailedUserSuspended17() {
        reg("owner_user", "password123");
        String ownerToken = log("owner_user", "password123");
        companyService.CreateCompany("company1", ownerToken);
        eventService.createEvent(ownerToken, "event1", "artist1", EventType.PLAY, 100, new Date(), "location1", "company1", getMapArea());

        reg("buyer_user", "password456");
        String buyerToken = log("buyer_user", "password456");

        reg("admin", "admin");
        log("admin", "admin");

        String buyerId = tokenService.extractUserId(buyerToken);
        adminService.suspendUser(buyerId, "admin", 7);

        List<int[]> requests = List.of(new int[]{0, 0, 1});
        Response<String> response = reserveTicketService.reserveTickets(buyerToken, "company1", "event1", requests, null);

        assertFalse(response.isSuccess(), "Reservation should not be successful for suspended user");
        assertTrue(response.isError());
        assertEquals("User is suspended", response.getMessage());
    }
}
