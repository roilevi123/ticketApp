package AcceptanceTests;

import Appliction.*;
import Domain.Company.iCompanyRepository;
import Domain.Event.EventType;
import Domain.Event.MapArea;
import Domain.Event.iEventRepository;
import Domain.Order.IActiveOrderRepository;
import Domain.OwnerManagerTree.iTreeOfRoleRepository;
import Domain.PurchasePolicy.iPurchasePolicyRepository;
import Domain.PurchasedOrderAggregate.iPurchasedOrderRepository;
import Domain.QueueAggregates.iQueueRepository;
import Domain.Ticket.TicketDTO;
import Domain.Ticket.iTicketRepository;
import Domain.User.IUserRepository;
import Infastructure.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Complete Reserve Ticket Acceptance Tests")
public class ReseveTicketTests {

    private UserService userService;
    private CompanyService companyService;
    private EventService eventService;
    private OrderService reserveTicketService;
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
        iPurchasePolicyRepository purchasePolicyRepository=new InMemoryPurchasePolicyRepository();
        INotifer notifer= Mockito.mock(INotifer.class);

        this.userService = new UserService(passwordEncoder, userRepository, tokenService,notifer);
        this.companyService = new CompanyService(companyRepository, userRepository, treeOfRoleRepository, tokenService,notifer);
        this.eventService = new EventService(companyRepository, eventRepository,
                tokenService, treeOfRoleRepository, ticketRepository, queueRepository,purchasedOrderRepository,notifer);
        this.reserveTicketService = new OrderService(activeOrderRepository, tokenService, ticketRepository,userRepository,purchasePolicyRepository,notifer);

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

    public boolean isNumeric(String str) {
        if (str == null) return false;
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
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

        String orderId = reserveTicketService.reserveTickets(token1, "1", "1", requests);
        assertTrue(isNumeric(orderId), "Expected a numeric order ID on success");
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

        reserveTicketService.reserveTickets(token, "1", "1", requests);

        String orderId1 = reserveTicketService.reserveTickets(token1, "1", "1", requests);
        assertFalse(isNumeric(orderId1), "Expected error message (non-numeric) for double booking");
    }

    @Test @DisplayName("3. Fail - Out of Bounds")
    void reserveTicketOutOfBounds3() {
        reg("1", "1");
        String token = log("1", "1");
        companyService.CreateCompany("1", token);
        eventService.createEvent(token, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea());

        List<int[]> requests = List.of(new int[]{5, 5, 1});
        String result = reserveTicketService.reserveTickets(token, "1", "1", requests);
        assertFalse(isNumeric(result), "Expected error message (non-numeric) for out of bounds coordinates");
    }

    @Test @DisplayName("4. Reserve Multiple Spots Success")
    void reserveTicketMultipleSpots4() {
        reg("1", "1");
        String token = log("1", "1");
        companyService.CreateCompany("1", token);
        eventService.createEvent(token, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea());

        List<int[]> requests = Arrays.asList(new int[]{0, 0, 1}, new int[]{1, 1, 1});
        String orderId = reserveTicketService.reserveTickets(token, "1", "1", requests);
        assertTrue(isNumeric(orderId), "Expected numeric order ID for multiple valid spots");
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

        assertTrue(isNumeric(reserveTicketService.reserveTickets(t2, "1", "1", req)), "First reservation should succeed");
        assertFalse(isNumeric(reserveTicketService.reserveTickets(t2, "1", "1", req)), "Second reservation of same spot should fail");
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
                    userService.register(gt(), "u" + id, "p",10);
                    String ut = userService.login(gt(), "u" + id, "p");
                    latch.await();
                    String oid = reserveTicketService.reserveTickets(ut, "1", "1", List.of(new int[]{0, 0, 1}));
                    if (isNumeric(oid)) results.add(oid);
                } catch (Exception ignored) {}
            });
        }
        latch.countDown();
        service.shutdown();
        service.awaitTermination(10, TimeUnit.SECONDS);
        assertEquals(1, results.size(), "Only one thread should successfully receive a numeric order ID");
    }

    @Test @DisplayName("7. Sequential Stand/Seat Success")
    void reserveTicketStandSequential7() {
        reg("1", "1");
        String t = log("1", "1");
        companyService.CreateCompany("1", t);
        eventService.createEvent(t, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea1());

        reg("u1", "p"); String t1 = log("u1", "p");
        reg("u2", "p"); String t2 = log("u2", "p");

        assertTrue(isNumeric(reserveTicketService.reserveTickets(t1, "1", "1", List.of(new int[]{1, 1, 1}))), "Stand reservation should be numeric");
        assertTrue(isNumeric(reserveTicketService.reserveTickets(t2, "1", "1", List.of(new int[]{0, 0, 1}))), "Seat reservation should be numeric");
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

        assertTrue(isNumeric(reserveTicketService.reserveTickets(t1, "1", "1", req)), "T1 should get numeric ID");
        assertFalse(isNumeric(reserveTicketService.reserveTickets(t2, "1", "1", req)), "T2 should get error message");

        Thread.sleep(10005);

        assertTrue(isNumeric(reserveTicketService.reserveTickets(t2, "1", "1", req)), "T2 should get numeric ID after expiration");
    }

    @Test @DisplayName("10. Get Active Order Tickets - Single")
    void getActiveOrderTicketsSingle10() {
        reg("1", "1");
        String token = log("1", "1");
        companyService.CreateCompany("1", token);
        eventService.createEvent(token, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea());

        reg("u1", "p"); String t1 = log("u1", "p");
        String oid = reserveTicketService.reserveTickets(t1, "1", "1", List.of(new int[]{0, 0, 1}));
        assertTrue(isNumeric(oid));

        List<TicketDTO> tickets = reserveTicketService.getActiveOrderTickets(t1, oid);
        assertNotNull(tickets);
        assertFalse(tickets.isEmpty());
        assertEquals("1", tickets.get(0).company());
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

        reserveTicketService.reserveTickets(t1, "1", "1", req);
        Thread.sleep(10005);

        String oid2 = reserveTicketService.reserveTickets(t2, "1", "1", req);
        assertTrue(isNumeric(oid2));

        List<TicketDTO> tickets = reserveTicketService.getActiveOrderTickets(t2, oid2);
        assertNotNull(tickets);
        assertFalse(tickets.isEmpty());
    }

    @Test @DisplayName("14. Get Active Order Tickets - Guest Success")
    void getActiveOrderTicketsAsGuest14() {
        reg("1", "1");
        String t = log("1", "1");
        companyService.CreateCompany("1", t);
        eventService.createEvent(t, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea());

        String guestToken = tokenService.generateGuestToken();
        String oid = reserveTicketService.reserveTickets(guestToken, "1", "1", List.of(new int[]{0, 0, 1}));
        assertTrue(isNumeric(oid));

        List<TicketDTO> tickets = reserveTicketService.getActiveOrderTickets(guestToken, oid);
        assertNotNull(tickets);
        assertEquals("1", tickets.get(0).company());
    }

    @Test @DisplayName("15. Get Active Order Success After Re-Login")
    void getActiveOrderTicketsAAfterLogOut15() {
        reg("1", "1");
        String t = log("1", "1");
        companyService.CreateCompany("1", t);
        eventService.createEvent(t, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea());

        reg("u1", "p"); String t1 = log("u1", "p");
        String oid = reserveTicketService.reserveTickets(t1, "1", "1", List.of(new int[]{0, 0, 1}));
        assertTrue(isNumeric(oid));

        userService.logout(t1);
        String t1New = log("u1", "p");
        List<TicketDTO> tickets = reserveTicketService.getActiveOrderTickets(t1New, null);
        assertNotNull(tickets);
        assertFalse(tickets.isEmpty());
    }

    @Test @DisplayName("16. Get Active Order Guest Fail After Data Loss")
    void getActiveOrderTicketsAsGuestThatGetOUt16() {
        reg("1", "1");
        String t = log("1", "1");
        companyService.CreateCompany("1", t);
        eventService.createEvent(t, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea());

        String guestToken = tokenService.generateGuestToken();
        String oid = reserveTicketService.reserveTickets(guestToken, "1", "1", List.of(new int[]{0, 0, 1}));
        assertTrue(isNumeric(oid));

        assertNull(reserveTicketService.getActiveOrderTickets(guestToken, "invalidOrderId"));
    }
}
