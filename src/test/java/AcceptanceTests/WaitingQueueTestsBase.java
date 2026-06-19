package AcceptanceTests;

import com.ticketing.ticketapp.Appliction.*;
import com.ticketing.ticketapp.Domain.Company.iCompanyRepository;
import com.ticketing.ticketapp.Domain.Discount.iDiscountPolicyRepository;
import com.ticketing.ticketapp.Domain.Event.EventType;
import com.ticketing.ticketapp.Domain.Event.MapArea;
import com.ticketing.ticketapp.Domain.Event.iEventRepository;
import com.ticketing.ticketapp.Domain.Order.IActiveOrderRepository;
import com.ticketing.ticketapp.Domain.OwnerManagerTree.iTreeOfRoleRepository;
import com.ticketing.ticketapp.Domain.PurchasedOrderAggregate.iPurchasedOrderRepository;
import com.ticketing.ticketapp.Domain.QueueAggregates.iQueueRepository;
import com.ticketing.ticketapp.Domain.Ticket.iTicketRepository;
import com.ticketing.ticketapp.Domain.User.IUserRepository;
import com.ticketing.ticketapp.Domain.Notification.INotificationRepository;
import com.ticketing.ticketapp.Infastructure.PasswordEncoderImpl;
import com.ticketing.ticketapp.Infastructure.TokenService;
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
public abstract class WaitingQueueTestsBase {

    @Autowired protected IUserRepository userRepository;
    @Autowired protected iCompanyRepository companyRepository;
    @Autowired protected iEventRepository eventRepository;
    @Autowired protected iQueueRepository queueRepository;
    @Autowired protected iTreeOfRoleRepository treeOfRoleRepository;
    @Autowired protected IActiveOrderRepository activeOrderRepository;
    @Autowired protected iTicketRepository ticketRepository;
    @Autowired protected iPurchasedOrderRepository purchasedOrderRepository;
    @Autowired protected TokenService tokenService;
    @Autowired protected INotificationRepository notificationRepository;
    protected UserService userService;
    protected CompanyService companyService;
    protected EventService eventService;
    protected QueueService queueService;

    @BeforeEach
    void setUp() {
        INotifier notifierMock = mock(INotifier.class);
        IPasswordEncoder passwordEncoder = new PasswordEncoderImpl();

        this.userService = new UserService(
                passwordEncoder,
                userRepository,
                tokenService,
                notificationRepository,
                notifierMock,
                treeOfRoleRepository
        );

        this.companyService = new CompanyService(
                companyRepository,
                userRepository,
                treeOfRoleRepository,
                tokenService,
                notifierMock
        );

        this.eventService = new EventService(
                companyRepository,
                eventRepository,
                tokenService,
                treeOfRoleRepository,
                ticketRepository,
                queueRepository,
                purchasedOrderRepository,
                userRepository,
                notifierMock,
                mock(iDiscountPolicyRepository.class)
        );

        var adminRepositoryMock =
                mock(com.ticketing.ticketapp.Domain.AdminAggregate.iAdminRepository.class);

        this.queueService = new QueueService(
                queueRepository,
                tokenService,
                notifierMock,
                adminRepositoryMock
        );
    }

    protected String gt() {
        return tokenService.generateGuestToken();
    }

    protected MapArea[][] getMapArea() {
        MapArea[][] map = new MapArea[2][2];

        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                map[i][j] = MapArea.SEAT;
            }
        }

        return map;
    }

    protected void createEventHelper(String creator, String eventName) {
        userService.register(
                gt(),
                creator,
                creator,
                10,
                creator + "@test.com"
        );

        String token =
                userService.login(gt(), creator, creator).getData();

        companyService.CreateCompany(
                creator,
                token
        );

        eventService.createEvent(
                token,
                eventName,
                "Artist",
                EventType.CONFERENCE,
                100.0,
                new Date(),
                "City",
                creator,
                getMapArea()
        );
    }

    @Test
    @DisplayName("1. Sequential Queue Entry")
    void testSequentialQueueEntry1() {
        createEventHelper("creator", "Rock Festival");

        String eventId = "Rock Festivalcreator";

        assertEquals(
                "AUTHORIZED",
                queueService.checkStatus(gt(), eventId).getData()
        );
    }

    @Test
    @DisplayName("2. Queue Position Increments")
    void testQueuePositionIncrements2() {
        createEventHelper("creator", "Rock Festival");

        String eventId = "Rock Festivalcreator";

        for (int i = 1; i <= 100; i++) {
            queueService.checkStatus(gt(), eventId);
        }

        assertEquals(
                "WAITING_POSITION_1",
                queueService.checkStatus(gt(), eventId).getData()
        );

        assertEquals(
                "WAITING_POSITION_2",
                queueService.checkStatus(gt(), eventId).getData()
        );
    }

    @Test
    @DisplayName("3. Concurrent Queue Access")
    void testConcurrentQueueAccess3() throws InterruptedException {
        createEventHelper("creator3", "Concurrent Fest");

        String eventId = "Concurrent Festcreator3";

        int threadCount = 50;

        CountDownLatch latch = new CountDownLatch(1);

        ExecutorService service =
                Executors.newFixedThreadPool(threadCount);

        List<String> results =
                Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            service.submit(() -> {
                try {
                    latch.await();

                    results.add(
                            queueService.checkStatus(
                                    gt(),
                                    eventId
                            ).getData()
                    );
                } catch (Exception ignored) {
                }
            });
        }

        latch.countDown();

        service.shutdown();

        service.awaitTermination(10, TimeUnit.SECONDS);

        assertEquals(
                threadCount,
                results.stream()
                        .filter(r -> "AUTHORIZED".equals(r))
                        .count()
        );
    }

    @Test
    @DisplayName("4. Concurrent Queue Overflow")
    void testConcurrentQueueOverflow4() throws InterruptedException {
        createEventHelper("creator4", "Overfill Fest");

        String eventId = "Overfill Festcreator4";

        int threadCount = 150;

        CountDownLatch latch = new CountDownLatch(1);

        ExecutorService service =
                Executors.newFixedThreadPool(threadCount);

        List<String> results =
                Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            service.submit(() -> {
                try {
                    latch.await();

                    results.add(
                            queueService.checkStatus(
                                    gt(),
                                    eventId
                            ).getData()
                    );
                } catch (Exception ignored) {
                }
            });
        }

        latch.countDown();

        service.shutdown();

        service.awaitTermination(15, TimeUnit.SECONDS);

        assertEquals(
                100,
                results.stream()
                        .filter(r -> "AUTHORIZED".equals(r))
                        .count()
        );

        assertEquals(
                50,
                results.stream()
                        .filter(r -> r != null &&
                                r.startsWith("WAITING_POSITION_"))
                        .count()
        );
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

        String waitingUser25Token =
                waitingTokens.get(24);

        int threadCount = 150;

        CountDownLatch latch = new CountDownLatch(1);

        ExecutorService service =
                Executors.newFixedThreadPool(threadCount);

        List<String> results =
                Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            service.submit(() -> {
                try {
                    latch.await();

                    results.add(
                            queueService.checkStatus(
                                    waitingUser25Token,
                                    eventId
                            ).getData()
                    );
                } catch (Exception ignored) {
                }
            });
        }

        latch.countDown();

        service.shutdown();

        assertTrue(
                service.awaitTermination(
                        15,
                        TimeUnit.SECONDS
                )
        );

        long correctPositionCount =
                results.stream()
                        .filter(r ->
                                "WAITING_POSITION_25".equals(r))
                        .count();

        assertEquals(
                threadCount,
                correctPositionCount
        );
    }

    @Test
    void testCheckStatusInvalidToken() {
        assertTrue(
                queueService.checkStatus("", "eventId")
                        .isError()
        );
    }
}