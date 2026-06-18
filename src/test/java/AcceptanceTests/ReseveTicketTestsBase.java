package AcceptanceTests;

import com.ticketing.ticketapp.Appliction.*;
import com.ticketing.ticketapp.Domain.AdminAggregate.iAdminRepository;
import com.ticketing.ticketapp.Domain.Company.iCompanyRepository;
import com.ticketing.ticketapp.Domain.Discount.iDiscountPolicyRepository;
import com.ticketing.ticketapp.Domain.Event.EventType;
import com.ticketing.ticketapp.Domain.Event.MapArea;
import com.ticketing.ticketapp.Domain.Event.iEventRepository;
import com.ticketing.ticketapp.Domain.Order.IActiveOrderRepository;
import com.ticketing.ticketapp.Domain.OwnerManagerTree.iTreeOfRoleRepository;
import com.ticketing.ticketapp.Domain.PurchasePolicy.iPurchasePolicyRepository;
import com.ticketing.ticketapp.Domain.PurchasedOrderAggregate.iPurchasedOrderRepository;
import com.ticketing.ticketapp.Domain.QueueAggregates.iQueueRepository;
import com.ticketing.ticketapp.Domain.Ticket.TicketDTO;
import com.ticketing.ticketapp.Domain.Ticket.iTicketRepository;
import com.ticketing.ticketapp.Domain.User.IUserRepository;
import com.ticketing.ticketapp.Infastructure.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@SpringBootTest(classes = com.ticketing.ticketapp.TicketappApplication.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Transactional
@DisplayName("Complete Reserve Ticket Acceptance Tests")
public abstract class ReseveTicketTestsBase {

    @Autowired private IUserRepository userRepository;
    @Autowired private iCompanyRepository companyRepository;
    @Autowired private iEventRepository eventRepository;
    @Autowired private iQueueRepository queueRepository;
    @Autowired private iTreeOfRoleRepository treeOfRoleRepository;
    @Autowired private IActiveOrderRepository activeOrderRepository;
    @Autowired private iTicketRepository ticketRepository;
    @Autowired private iPurchasedOrderRepository purchasedOrderRepository;
    @Autowired private iAdminRepository adminRepository;
    @Autowired private TokenService tokenService;

    @Autowired protected iPurchasePolicyRepository purchasePolicyRepository;

    private UserService userService;
    private CompanyService companyService;
    private EventService eventService;
    private OrderService reserveTicketService;
    private AdminService adminService;

    @BeforeEach
    void setUp() {
        INotifier notifierMock = mock(INotifier.class);
        IPasswordEncoder passwordEncoder = new PasswordEncoderImpl();

        this.userService = new UserService(passwordEncoder, userRepository, tokenService, new NotificationRepositoryImpl(), notifierMock, treeOfRoleRepository);
        this.companyService = new CompanyService(companyRepository, userRepository, treeOfRoleRepository, tokenService, notifierMock);
        this.eventService = new EventService(companyRepository, eventRepository, tokenService, treeOfRoleRepository, ticketRepository, queueRepository, purchasedOrderRepository, userRepository, notifierMock, mock(iDiscountPolicyRepository.class));
        this.reserveTicketService = new OrderService(activeOrderRepository, tokenService, ticketRepository, userRepository, purchasePolicyRepository, notifierMock, eventRepository, mock(LotteryService.class));
        this.adminService = new AdminService(treeOfRoleRepository, companyRepository, adminRepository, userRepository, purchasedOrderRepository, ticketRepository, eventRepository, tokenService, new NotifierImpl(new Broadcaster(new NotificationRepositoryImpl())), activeOrderRepository);
    }

    private boolean isValidOrderId(String str) { return str != null && !str.isBlank(); }
    private String gt() { return tokenService.generateGuestToken(); }
    private void reg(String username, String password) { userService.register(gt(), username, password, 10, username + "@test.com"); }
    private String log(String username, String password) { return userService.login(gt(), username, password).getData(); }

    private MapArea[][] getMapArea() {
        MapArea[][] map = new MapArea[2][2];
        for (int i = 0; i < 2; i++) for (int j = 0; j < 2; j++) map[i][j] = MapArea.SEAT;
        return map;
    }

    private MapArea[][] getMapArea1() {
        MapArea[][] map = getMapArea();
        map[1][1] = MapArea.STAND;
        return map;
    }

    @Test @DisplayName("1. Reserve Ticket Success")
    void reserveTicketTestPass1() {
        reg("1", "1"); String token = log("1", "1");
        companyService.CreateCompany("1", token);
        eventService.createEvent(token, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea());
        reg("2", "2"); String token1 = log("2", "2");
        String orderId = reserveTicketService.reserveTickets(token1, "1", "1", List.of(new int[]{0, 0, 1}), null).getData();
        assertTrue(isValidOrderId(orderId));
    }

    @Test @DisplayName("2. Fail - Not Enough Tickets (Double Booking)")
    void reserveTicketNotEnoughTickets2() {
        reg("1", "1"); String token = log("1", "1");
        companyService.CreateCompany("1", token);
        eventService.createEvent(token, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea());
        reg("2", "2"); String token1 = log("2", "2");
        reserveTicketService.reserveTickets(token, "1", "1", List.of(new int[]{0, 0, 1}), null);
        String orderId1 = reserveTicketService.reserveTickets(token1, "1", "1", List.of(new int[]{0, 0, 1}), null).getData();
        assertFalse(isValidOrderId(orderId1));
    }

    @Test @DisplayName("3. Fail - Out of Bounds")
    void reserveTicketOutOfBounds3() {
        reg("1", "1"); String token = log("1", "1");
        companyService.CreateCompany("1", token);
        eventService.createEvent(token, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea());
        String result = reserveTicketService.reserveTickets(token, "1", "1", List.of(new int[]{5, 5, 1}), null).getData();
        assertFalse(isValidOrderId(result));
    }

    @Test @DisplayName("4. Reserve Multiple Spots Success")
    void reserveTicketMultipleSpots4() {
        reg("1", "1"); String token = log("1", "1");
        companyService.CreateCompany("1", token);
        eventService.createEvent(token, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea());
        String orderId = reserveTicketService.reserveTickets(token, "1", "1", Arrays.asList(new int[]{0, 0, 1}, new int[]{1, 1, 1}), null).getData();
        assertTrue(isValidOrderId(orderId));
    }

    @Test @DisplayName("5. Fail - Already Reserved Spot")
    void reserveTicketAlreadyReserved5() {
        reg("1", "1"); String t1 = log("1", "1");
        companyService.CreateCompany("1", t1);
        eventService.createEvent(t1, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea());
        reg("2", "2"); String t2 = log("2", "2");
        List<int[]> req = List.of(new int[]{0, 1, 1});
        assertTrue(isValidOrderId(reserveTicketService.reserveTickets(t2, "1", "1", req, null).getData()));
        assertFalse(isValidOrderId(reserveTicketService.reserveTickets(t2, "1", "1", req, null).getData()));
    }

    @Test @DisplayName("6. Concurrent Reservations - Conflict")
    void reserveTicketConcurrentTwoThreads6() throws InterruptedException {
        reg("1", "1"); String token = log("1", "1");
        companyService.CreateCompany("1", token);
        eventService.createEvent(token, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea());

        int threadCount = 2;
        ExecutorService service = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(1);
        List<String> results = Collections.synchronizedList(new ArrayList<>());
        List<String> errors = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            final int id = i + 2;
            service.submit(() -> {
                try {
                    userService.register(gt(), "u" + id, "p", 10, "u" + id + "@test.com");
                    String ut = userService.login(gt(), "u" + id, "p").getData();
                    latch.await();
                    var response = reserveTicketService.reserveTickets(ut, "1", "1", List.of(new int[]{0, 0, 1}), null);
                    if (response.isSuccess()) {
                        results.add(response.getData());
                    } else {
                        errors.add(response.getMessage());
                    }
                } catch (Exception e) { e.printStackTrace(); }
            });
        }
        latch.countDown();
        service.shutdown();
        service.awaitTermination(10, TimeUnit.SECONDS);

        assertTrue(results.size() <= 1, "Results size was: " + results.size());
    }

    @Test @DisplayName("7. Sequential Stand/Seat Success")
    void reserveTicketStandSequential7() {
        reg("1", "1"); String t = log("1", "1");
        companyService.CreateCompany("1", t);
        eventService.createEvent(t, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea1());

        reg("u1", "p"); String t1 = log("u1", "p");
        reg("u2", "p"); String t2 = log("u2", "p");

        assertTrue(isValidOrderId(reserveTicketService.reserveTickets(t1, "1", "1", List.of(new int[]{1, 1, 1}), null).getData()));
        assertTrue(isValidOrderId(reserveTicketService.reserveTickets(t2, "1", "1", List.of(new int[]{0, 0, 1}), null).getData()));
    }

    @Test @DisplayName("9. Expired Ticket Becomes Available Again")
    void reserveTicketExpiredTicketAvailableAgain9() throws InterruptedException {
        reg("1", "1"); String token = log("1", "1");
        companyService.CreateCompany("1", token);
        eventService.createEvent(token, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea());
        reg("u1", "p"); String t1 = log("u1", "p");
        reg("u2", "p"); String t2 = log("u2", "p");
        List<int[]> req = List.of(new int[]{0, 0, 1});
        assertTrue(isValidOrderId(reserveTicketService.reserveTickets(t1, "1", "1", req, null).getData()));
        assertFalse(isValidOrderId(reserveTicketService.reserveTickets(t2, "1", "1", req, null).getData()));
        Thread.sleep(10005);
        assertTrue(isValidOrderId(reserveTicketService.reserveTickets(t2, "1", "1", req, null).getData()));
    }

    @Test @DisplayName("10. Get Active Order Tickets - Single")
    void getActiveOrderTicketsSingle10() {
        reg("1", "1"); String token = log("1", "1");
        companyService.CreateCompany("1", token);
        eventService.createEvent(token, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea());
        reg("u1", "p"); String t1 = log("u1", "p");
        String oid = reserveTicketService.reserveTickets(t1, "1", "1", List.of(new int[]{0, 0, 1}), null).getData();
        assertTrue(isValidOrderId(oid));
        Response<List<TicketDTO>> ticketsResp = reserveTicketService.getActiveOrderTickets(t1, oid);
        assertTrue(ticketsResp.isSuccess());
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

        String oid2 = reserveTicketService.reserveTickets(t2, "1", "1", req, null).getData();
        assertTrue(isValidOrderId(oid2));

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
        String oid = reserveTicketService.reserveTickets(guestToken, "1", "1", List.of(new int[]{0, 0, 1}), null).getData();
        assertTrue(isValidOrderId(oid));

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
        String oid = reserveTicketService.reserveTickets(t1, "1", "1", List.of(new int[]{0, 0, 1}), null).getData();
        assertTrue(isValidOrderId(oid));

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
        String oid = reserveTicketService.reserveTickets(guestToken, "1", "1", List.of(new int[]{0, 0, 1}), null).getData();
        assertTrue(isValidOrderId(oid));

        assertTrue(reserveTicketService.getActiveOrderTickets(guestToken, "invalidOrderId").isError());
    }

    @Test
    void InValidToken() {
        assertTrue(reserveTicketService.getActiveOrderTickets("", null).isError());
    }

    @Test @DisplayName("17. Fail - Reserve Ticket When User Is Suspended")
    void reserveTicketFailedUserSuspended17() {
        reg("owner", "123"); String ownerToken = log("owner", "123");
        companyService.CreateCompany("comp1", ownerToken);
        eventService.createEvent(ownerToken, "event1", "art", EventType.PLAY, 100, new Date(), "loc", "comp1", getMapArea());
        reg("buyer", "456"); String buyerToken = log("buyer", "456");

        adminRepository.addAdmin("admin");
        adminService.suspendUser(tokenService.extractUserId(buyerToken), "admin", 7);

        Response<String> response = reserveTicketService.reserveTickets(buyerToken, "comp1", "event1", List.of(new int[]{0, 0, 1}), null);
        assertFalse(response.isSuccess());
        assertEquals("User is suspended", response.getMessage());
    }
}