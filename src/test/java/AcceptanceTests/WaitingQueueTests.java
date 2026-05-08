package AcceptanceTest;

import Appliction.*;
import Domain.AdminAggregate.iAdminRepository;
import Domain.Company.iCompanyRepository;
import Domain.Event.EventType;
import Domain.Event.MapArea;
import Domain.Event.iEventRepository;
import Domain.Order.IActiveOrderRepository;
import Domain.OwnerManagerTree.iTreeOfRoleRepository;
import Domain.PurchasedOrderAggregate.iPurchasedOrderRepository;
import Domain.QueueAggregates.iQueueRepository;
import Domain.Ticket.iTicketRepository;
import Domain.User.IUserRepository;
import Infastructure.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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

        this.userService = new UserService(passwordEncoder, userRepository, tokenService);
        this.companyService = new CompanyService(companyRepository, userRepository, treeOfRoleRepository, tokenService);
        this.eventService = new EventService(companyRepository, eventRepository, tokenService, treeOfRoleRepository, ticketRepository, queueRepository);
        this.queueService = new QueueService(queueRepository);

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
        userService.register(creator, creator);
        String token = userService.login(creator, creator);
        companyService.CreateCompany(creator, token);
        eventService.createEvent(token, eventName, "Artist", EventType.CONFERENCE, 100.0, new Date(), "City", creator, getMapArea());
    }

    @Test
    @DisplayName("1. Sequential Queue Entry")
    void testSequentialQueueEntry1() {
        createEventHelper("creator", "Rock Festival");
        String eventId = "Rock Festivalcreator";

        String status1 = queueService.checkStatus(eventId, "user1");
        String status2 = queueService.checkStatus(eventId, "user2");

        assertEquals("AUTHORIZED", status1);
        assertEquals("AUTHORIZED", status2);
    }

    @Test
    @DisplayName("2. Queue Position Increments")
    void testQueuePositionIncrements2() {
        createEventHelper("creator", "Rock Festival");
        String eventId = "Rock Festivalcreator";

        for (int i = 1; i <= 100; i++) {
            queueService.checkStatus(eventId, "dummyUser" + i);
        }

        String status101 = queueService.checkStatus(eventId, "waiter1");
        String status102 = queueService.checkStatus(eventId, "waiter2");

        assertEquals("WAITING_POSITION_1", status101);
        assertEquals("WAITING_POSITION_2", status102);
    }

    @Test
    @DisplayName("3. Concurrent Queue Access")
    void testConcurrentQueueAccess3() throws InterruptedException {
        createEventHelper("creator3", "Concurrent Fest");
        String eventId = "Concurrent Festcreator3";
        int threadCount = 50;

        CountDownLatch latch = new CountDownLatch(1);
        ExecutorService service = Executors.newFixedThreadPool(threadCount);
        List<String> results = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            final String uname = "concurrentUser" + i;
            service.submit(() -> {
                try {
                    latch.await();
                    results.add(queueService.checkStatus(eventId, uname));
                } catch (Exception ignored) {}
            });
        }

        latch.countDown();
        service.shutdown();
        assertTrue(service.awaitTermination(10, TimeUnit.SECONDS));

        long authorizedCount = results.stream().filter(r -> r.equals("AUTHORIZED")).count();
        assertEquals(threadCount, authorizedCount);
    }

    @Test
    @DisplayName("4. Concurrent Queue Overflow")
    void testConcurrentQueueOverflow4() throws InterruptedException {
        createEventHelper("creator4", "Overfill Fest");
        String eventId = "Overfill Festcreator4";
        int threadCount = 150;

        CountDownLatch latch = new CountDownLatch(1);
        ExecutorService service = Executors.newFixedThreadPool(threadCount);
        List<String> results = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            final String uname = "overflowUser" + i;
            service.submit(() -> {
                try {
                    latch.await();
                    results.add(queueService.checkStatus(eventId, uname));
                } catch (Exception ignored) {}
            });
        }

        latch.countDown();
        service.shutdown();
        assertTrue(service.awaitTermination(15, TimeUnit.SECONDS));

        long authorizedCount = results.stream().filter(r -> r.equals("AUTHORIZED")).count();
        long waitingCount = results.stream().filter(r -> r.startsWith("WAITING_POSITION_")).count();

        assertEquals(100, authorizedCount);
        assertEquals(50, waitingCount);
    }

    @Test
    @DisplayName("5. Concurrent Same User Refresh")
    void testConcurrentSameUserRefresh5() throws InterruptedException {
        createEventHelper("creator5", "Refresh Fest");
        String eventId = "Refresh Festcreator5";

        for (int i = 1; i <= 100; i++) {
            queueService.checkStatus(eventId, "dummyUser" + i);
        }
        for (int i = 1; i <= 50; i++) {
            queueService.checkStatus(eventId, "waitingUser" + i);
        }

        int threadCount = 150;
        CountDownLatch latch = new CountDownLatch(1);
        ExecutorService service = Executors.newFixedThreadPool(threadCount);
        List<String> results = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            service.submit(() -> {
                try {
                    latch.await();
                    results.add(queueService.checkStatus(eventId, "waitingUser25"));
                } catch (Exception ignored) {}
            });
        }

        latch.countDown();
        service.shutdown();
        assertTrue(service.awaitTermination(15, TimeUnit.SECONDS));

        long correctPositionCount = results.stream().filter(r -> r.equals("WAITING_POSITION_25")).count();
        assertEquals(threadCount, correctPositionCount);
    }
}