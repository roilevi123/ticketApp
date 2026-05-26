package AcceptanceTests;

import com.ticketing.ticketapp.Appliction.*;
import com.ticketing.ticketapp.Domain.Company.iCompanyRepository;
import com.ticketing.ticketapp.Domain.Event.EventType;
import com.ticketing.ticketapp.Domain.Event.MapArea;
import com.ticketing.ticketapp.Domain.Event.iEventRepository;
import com.ticketing.ticketapp.Domain.Order.IActiveOrderRepository;
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

import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Waiting Queue Management Acceptance Tests")
public class WaitingQueueTests {

    private UserService userService;
    private CompanyService companyService;
    private EventService eventService;
    private QueueService queueService;

    private IUserRepository userRepository;
    private iCompanyRepository companyRepository;
    private iEventRepository eventRepository;
    private iQueueRepository queueRepository;
    private iTreeOfRoleRepository treeOfRoleRepository;
    private IActiveOrderRepository activeOrderRepository;
    private iTicketRepository ticketRepository;
    private iPurchasedOrderRepository purchasedOrderRepository;
    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        this.userRepository = new UserRepositoryImpl();
        this.companyRepository = new CompanyRepositoryImpl();
        this.eventRepository = new EventRepositoryImpl();
        this.queueRepository = new QueueRepositoryImpl();
        this.treeOfRoleRepository = new TreeOfRoleRepositoryImpl();
        this.activeOrderRepository = new OrderRepositoryImpl();
        this.ticketRepository = new TicketRepositoryImpl();
        this.purchasedOrderRepository = new PurchasedOrderRepositoryImpl();
        this.tokenService = new TokenService();

        IPasswordEncoder passwordEncoder = new PasswordEncoderImpl();
        IPendingNotificationRepository notificationRepository = new PendingNotificationRepositoryImpl();

        this.userService = new UserService(passwordEncoder, userRepository, tokenService, notificationRepository, treeOfRoleRepository);
        INotifier notifierMock = mock(INotifier.class);
        this.companyService = new CompanyService(companyRepository, userRepository, treeOfRoleRepository, tokenService, notifierMock, notificationRepository);
        this.eventService = new EventService(companyRepository, eventRepository, tokenService, treeOfRoleRepository, ticketRepository, queueRepository, purchasedOrderRepository, userRepository, notifierMock);
        this.queueService = new QueueService(queueRepository, tokenService, notifierMock);

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

    private MapArea[][] getMapArea() {
        MapArea[][] map = new MapArea[2][2];
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                map[i][j] = MapArea.SEAT;
            }
        }
        return map;
    }

    private void createEventHelper(String creator, String eventName) {
        userService.register(gt(), creator, creator, 10, creator + "@test.com");
        String token = userService.login(gt(), creator, creator).getData();
        companyService.CreateCompany(creator, token);
        eventService.createEvent(token, eventName, "Artist", EventType.CONFERENCE, 100.0, new Date(), "City", creator, getMapArea());
    }

    @Test
    @DisplayName("1. Sequential Queue Entry")
    void testSequentialQueueEntry1() {
        createEventHelper("creator", "Rock Festival");
        String eventId = "Rock Festivalcreator";

        String user1Token = gt();
        String user2Token = gt();
        String status1 = queueService.checkStatus(user1Token, eventId).getData();
        String status2 = queueService.checkStatus(user2Token, eventId).getData();

        assertEquals("AUTHORIZED", status1);
        assertEquals("AUTHORIZED", status2);
    }

    @Test
    @DisplayName("2. Queue Position Increments")
    void testQueuePositionIncrements2() {
        createEventHelper("creator", "Rock Festival");
        String eventId = "Rock Festivalcreator";

        for (int i = 1; i <= 100; i++) {
            queueService.checkStatus(gt(), eventId);
        }

        String status101 = queueService.checkStatus(gt(), eventId).getData();
        String status102 = queueService.checkStatus(gt(), eventId).getData();

        assertEquals("WAITING_POSITION_1", status101);
        assertEquals("WAITING_POSITION_2", status102);
    }

    @Test
    @DisplayName("3. Concurrent Queue Access")
    void testConcurrentQueueAccess3() throws InterruptedException {
        createEventHelper("creator3", "Concurrent Fest");
        String eventId = "Concurrent Festcreator3";
        int threadCount = 50;

        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            tokens.add(gt());
        }

        CountDownLatch latch = new CountDownLatch(1);
        ExecutorService service = Executors.newFixedThreadPool(threadCount);
        List<String> results = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            final String tok = tokens.get(i);
            service.submit(() -> {
                try {
                    latch.await();
                    results.add(queueService.checkStatus(tok, eventId).getData());
                } catch (Exception ignored) {}
            });
        }

        latch.countDown();
        service.shutdown();
        assertTrue(service.awaitTermination(10, TimeUnit.SECONDS));

        long authorizedCount = results.stream().filter(r -> "AUTHORIZED".equals(r)).count();
        assertEquals(threadCount, authorizedCount);
    }

    @Test
    @DisplayName("4. Concurrent Queue Overflow")
    void testConcurrentQueueOverflow4() throws InterruptedException {
        createEventHelper("creator4", "Overfill Fest");
        String eventId = "Overfill Festcreator4";
        int threadCount = 150;

        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            tokens.add(gt());
        }

        CountDownLatch latch = new CountDownLatch(1);
        ExecutorService service = Executors.newFixedThreadPool(threadCount);
        List<String> results = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            final String tok = tokens.get(i);
            service.submit(() -> {
                try {
                    latch.await();
                    results.add(queueService.checkStatus(tok, eventId).getData());
                } catch (Exception ignored) {}
            });
        }

        latch.countDown();
        service.shutdown();
        assertTrue(service.awaitTermination(15, TimeUnit.SECONDS));

        long authorizedCount = results.stream().filter(r -> "AUTHORIZED".equals(r)).count();
        long waitingCount = results.stream().filter(r -> r != null && r.startsWith("WAITING_POSITION_")).count();

        assertEquals(100, authorizedCount);
        assertEquals(50, waitingCount);
    }

    @Test
    @DisplayName("5. Concurrent Same User Refresh")
    void testConcurrentSameUserRefresh5() throws InterruptedException {
        createEventHelper("creator5", "Refresh Fest");
        String eventId = "Refresh Festcreator5";

        for (int i = 1; i <= 100; i++) {
            queueService.checkStatus(gt(), eventId);
        }

        List<String> waitingTokens = new ArrayList<>();
        for (int i = 1; i <= 50; i++) {
            String wt = gt();
            waitingTokens.add(wt);
            queueService.checkStatus(wt, eventId);
        }

        String waitingUser25Token = waitingTokens.get(24);

        int threadCount = 150;
        CountDownLatch latch = new CountDownLatch(1);
        ExecutorService service = Executors.newFixedThreadPool(threadCount);
        List<String> results = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            service.submit(() -> {
                try {
                    latch.await();
                    results.add(queueService.checkStatus(waitingUser25Token, eventId).getData());
                } catch (Exception ignored) {}
            });
        }

        latch.countDown();
        service.shutdown();
        assertTrue(service.awaitTermination(15, TimeUnit.SECONDS));

        long correctPositionCount = results.stream().filter(r -> "WAITING_POSITION_25".equals(r)).count();
        assertEquals(threadCount, correctPositionCount);
    }

    @Test
    public void testCheckStatusInvalidToken() {
        Response<String> result = queueService.checkStatus("", "eventId");
        assertTrue(result.isError());
    }
}
