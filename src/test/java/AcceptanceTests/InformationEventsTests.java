package AcceptanceTests;

import com.ticketing.ticketapp.Appliction.*;
import com.ticketing.ticketapp.Domain.Company.iCompanyRepository;
import com.ticketing.ticketapp.Domain.Event.EventDTO;
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

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Complete Event Information Acceptance Tests")
public class InformationEventsTests {

    private UserService userService;
    private CompanyService companyService;
    private EventService eventService;
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

        INotifier notifierMock = mock(INotifier.class);
        this.userService = new UserService(passwordEncoder, userRepository, tokenService, new NotificationRepositoryImpl(), notifierMock, treeOfRoleRepository);
        this.companyService = new CompanyService(companyRepository, userRepository, treeOfRoleRepository, tokenService, notifierMock);
        this.eventService = new EventService(companyRepository, eventRepository, tokenService, treeOfRoleRepository, ticketRepository, queueRepository, purchasedOrderRepository, userRepository, notifierMock);

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

    private MapArea[][] getMapArea() {
        MapArea[][] map = new MapArea[2][2];
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                map[i][j] = MapArea.SEAT;
            }
        }
        return map;
    }

    private void setupSearchEnvironment() {
        reg("user1", "pass1");
        String token1 = log("user1", "pass1");
        companyService.CreateCompany("CompanyA", token1);

        reg("user2", "pass2");
        String token2 = log("user2", "pass2");
        companyService.CreateCompany("CompanyB", token2);

        long day = 24 * 60 * 60 * 1000L;
        Date today = new Date();
        Date nextWeek = new Date(today.getTime() + 7 * day);

        eventService.createEvent(token1, "Rock Festival", "Arctic Monkeys", EventType.CONFERENCE, 100.0, today, "Tel Aviv", "CompanyA", getMapArea());
        eventService.createEvent(token1, "Jazz Night", "Norah Jones", EventType.CONFERENCE, 200.0, nextWeek, "Haifa", "CompanyA", getMapArea());

        eventService.createEvent(token2, "Tech Talk", "Elon Mask", EventType.CONFERENCE, 0.0, today, "Tel Aviv", "CompanyB", getMapArea());
        eventService.createEvent(token2, "Opera Show", "Pavaraotti", EventType.PLAY, 500.0, nextWeek, "Jerusalem", "CompanyB", getMapArea());
    }

    @Test @DisplayName("1. Get Company Info - Success")
    void getCompanyInfoSuccess1() {
        reg("1", "1");
        String token = log("1", "1");
        companyService.CreateCompany("1", token);
        Response<String> companyInfo = eventService.getCompanyInfo(token, "1");
        assertTrue(companyInfo.isSuccess());
        String founderID = tokenService.extractUserId(token);
        String expectedInfo = "Company Summary:" +
                "\nName: 1" +
                "\nFounder/Owner ID: " + founderID +
                "\nStatus: Active" +
                "\nRating: 0.0";
        assertEquals(expectedInfo, companyInfo.getData());
    }

    @Test @DisplayName("2. Get Company Info - Fail (Company Not Found)")
    void getCompanyInfoFailedCompanyNotExitst2() {
        assertTrue(eventService.getCompanyInfo(gt(), "1").isError());
    }

    @Test @DisplayName("3. Get Company Events - Success")
    void getCompanyEventsSuccess3() {
        reg("1", "1");
        String token = log("1", "1");
        companyService.CreateCompany("1", token);
        Date eventDate = new Date();
        eventService.createEvent(token, "E1", "A1", EventType.PLAY, 100, eventDate, "L1", "1", getMapArea());
        eventService.createEvent(token, "E2", "A2", EventType.CONFERENCE, 200, eventDate, "L2", "1", getMapArea());

        List<EventDTO> list = eventService.getCompanyEvents(token, "1").getData();
        assertNotNull(list);
        assertEquals(2, list.size());
        assertEquals(list.get(0).name().equals("E1"), true);
        assertEquals(list.get(1).name().equals("E2"), true);
        assertEquals(list.get(0).companyName().equals("1"), true);
        assertEquals(list.get(1).companyName().equals("1"), true);
    }

    @Test @DisplayName("4. Get Company Events - Success (No Events)")
    void getCompanyEventsSuccessButNoEventsExist4() {
        reg("1", "1");
        String token = log("1", "1");
        companyService.CreateCompany("1", token);
        List<EventDTO> list = eventService.getCompanyEvents(token, "1").getData();
        assertTrue(list.isEmpty());
    }

    @Test @DisplayName("5. Get Company Events - Fail (Company Not Found)")
    void getCompanyEventsFailedNoCompanyExist5() {
        assertTrue(eventService.getCompanyEvents(gt(), "NonExistent").isError());
    }

    @Test @DisplayName("6. Get Event Map - Success")
    void getEventMapSuccess6() {
        reg("1", "1");
        String token = log("1", "1");
        companyService.CreateCompany("1", token);
        MapArea[][] map = getMapArea();
        eventService.createEvent(token, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", map);

        Response<MapArea[][]> fetchedMapResp = eventService.getMapArea(token, "1", "1");
        assertTrue(fetchedMapResp.isSuccess());
        assertArrayEquals(map, fetchedMapResp.getData());
    }

    @Test @DisplayName("7. Get Event Map - Fail (No Event)")
    void getEventMapFailedNoEventsExist7() {
        reg("1", "1");
        String token = log("1", "1");
        companyService.CreateCompany("1", token);
        assertTrue(eventService.getMapArea(token, "1", "NonExistent").isError());
    }

    @Test @DisplayName("8. Get Event Map - Fail (No Company)")
    void getEventMapFailedNoCompanyExist8() {
        assertTrue(eventService.getMapArea(gt(), "NonExistent", "EventName").isError());
    }

    @Test @DisplayName("9. Search Events By Event Name")
    void searchEventsByEventNameQuery9() {
        setupSearchEnvironment();
        List<EventDTO> results = eventService.searchEvents(gt(), "Rock", null, null, null, null, null, null, null, null).getData();
        boolean isRockFound = false;
        for (EventDTO event : results) {
            if (event.name().contains("Rock")) {
                isRockFound = true;
            }
        }
        assertTrue(isRockFound);
    }

    @Test @DisplayName("10. Search Events By Artist Name")
    void searchEventsByArtistNameQuery10() {
        setupSearchEnvironment();
        List<EventDTO> results = eventService.searchEvents(gt(), "Monkeys", null, null, null, null, null, null, null, null).getData();
        boolean isRockFound = false;
        for (EventDTO event : results) {
            if (event.name().contains("Rock")) {
                isRockFound = true;
            }
        }
        assertTrue(isRockFound);
    }

    @Test @DisplayName("11. Search Events By Price Range")
    void searchEventsByPriceRangeMultipleCompanies11() {
        setupSearchEnvironment();
        List<EventDTO> results = eventService.searchEvents(gt(), null, null, null, 0.0, 150.0, null, null, null, null).getData();
        assertEquals(2, results.size());
    }

    @Test @DisplayName("12. Search Events By Date Range")
    void searchEventsByDateRange12() {
        setupSearchEnvironment();
        long fiveMinutesAgo = System.currentTimeMillis() - (5 * 60 * 1000L);
        Date startDate = new Date(fiveMinutesAgo);
        Date tomorrow = new Date(System.currentTimeMillis() + (24 * 60 * 60 * 1000L));

        List<EventDTO> results = eventService.searchEvents(gt(), null, null, null, null, null, startDate, tomorrow, null, null).getData();
        assertEquals(2, results.size());
    }

    @Test @DisplayName("13. Search Events By Location")
    void searchEventsByLocation13() {
        setupSearchEnvironment();
        List<EventDTO> results = eventService.searchEvents(gt(), null, null, null, null, null, null, null, "Tel Aviv", null).getData();
        assertEquals(2, results.size());
    }

    @Test @DisplayName("14. Search Events By Min Rating - Empty")
    void searchEventsByMinRating14() {
        setupSearchEnvironment();
        List<EventDTO> results = eventService.searchEvents(gt(), null, null, null, null, null, null, null, null, 1.0).getData();
        assertTrue(results.isEmpty());
    }

    @Test
    void getCompanyInfoInvalidToken() {
        assertTrue(eventService.getCompanyInfo("a", "C1").isError());
    }

    @Test
    void getCompanyEventsInvalidToken() {
        assertTrue(eventService.getCompanyEvents("a", "C1").isError());
    }

    @Test
    void getCompanyEventsNotFound() {
        reg("1", "1");
        String a = log("1", "1");
        assertTrue(eventService.getCompanyEvents(a, "C1").isError());
    }

    @Test
    void getMapAreaInvalidToken() {
        assertTrue(eventService.getMapArea("a", "C1", "").isError());
    }

    @Test
    void searchEventsInvalidToken() {
        assertTrue(eventService.searchEvents("gt", null, null, null, null, null, null, null, null, 1.0).isError());
    }
}
