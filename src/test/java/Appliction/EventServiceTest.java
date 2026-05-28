package Appliction;

import com.ticketing.ticketapp.Appliction.*;
import com.ticketing.ticketapp.Domain.Event.*;
import com.ticketing.ticketapp.Domain.OwnerManagerTree.Manager;
import com.ticketing.ticketapp.Domain.OwnerManagerTree.Permission;
import com.ticketing.ticketapp.Domain.OwnerManagerTree.iTreeOfRoleRepository;
import com.ticketing.ticketapp.Domain.Company.Company;
import com.ticketing.ticketapp.Domain.Company.iCompanyRepository;
import com.ticketing.ticketapp.Domain.PurchasedOrderAggregate.iPurchasedOrderRepository;
import com.ticketing.ticketapp.Domain.QueueAggregates.iQueueRepository;
import com.ticketing.ticketapp.Domain.User.IUserRepository;
import com.ticketing.ticketapp.Domain.Ticket.iTicketRepository;
import com.ticketing.ticketapp.Domain.Discount.iDiscountPolicyRepository;
import com.ticketing.ticketapp.Domain.Order.IActiveOrderRepository;
import com.ticketing.ticketapp.Domain.Ticket.Ticket;
import com.ticketing.ticketapp.Infastructure.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;


import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class EventServiceTest {
    private EventService eventService;

    @Mock private iCompanyRepository companyRepository;
    @Mock private IUserRepository userRepository;
    @Mock private iTreeOfRoleRepository treeOfRoleRepository;
    @Mock private iEventRepository eventRepository;
    @Mock private TokenService tokenService;
    @Mock private iTicketRepository ticketRepository;
    @Mock private iQueueRepository queueRepository;
    @Mock private iPurchasedOrderRepository purchasedOrderRepository;
    @Mock private INotifier notifier;
    @Mock private IActiveOrderRepository activeOrderRepository;

    private final String TOKEN = "valid_token";
    private final String USERNAME = "test_user";
    private final String COMPANY = "test_company";
    private final String EVENT_NAME = "Live Concert";
    private final MapArea[][] MAP = new MapArea[10][10];

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                MAP[i][j] = MapArea.SEAT;
            }
        }
        eventRepository = new EventRepositoryImpl();
        ticketRepository = spy(new TicketRepositoryImpl());
        eventService = new EventService(companyRepository, eventRepository, tokenService, treeOfRoleRepository, ticketRepository, queueRepository, purchasedOrderRepository, userRepository, notifier, mock(iDiscountPolicyRepository.class), activeOrderRepository);
    }

    @Test
    void createEvent_Success_AsOwner() {
        String artist = "Artist";
        double price = 100.0;
        Date date = new Date();
        String location = "Tel Aviv";
        MAP[0][0] = MapArea.STAGE;
        MAP[0][1] = MapArea.SEAT;
        MAP[0][2] = MapArea.STAND;

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);
        when(treeOfRoleRepository.exitsOwner(USERNAME, COMPANY)).thenReturn(true);

        Response<String> res = eventService.createEvent(TOKEN, EVENT_NAME, artist, EventType.LIVE_PERFORMANCE, price, date, location, COMPANY, MAP);
        assertTrue(res.isSuccess());
        assertEquals("success", res.getData());
        Event savedEvent = eventRepository.getEvent(EVENT_NAME, COMPANY);

        assertNotNull(savedEvent);
        assertEquals(EVENT_NAME, savedEvent.getName());
        assertEquals(artist, savedEvent.getArtistName());
        assertEquals(EventType.LIVE_PERFORMANCE, savedEvent.getType());
        assertEquals(price, savedEvent.getPrice());
        assertEquals(date, savedEvent.getDate());
        assertEquals(location, savedEvent.getLocation());
        assertEquals(COMPANY, savedEvent.getCompany());

        List<Ticket> createdTickets = ((TicketRepositoryImpl) ticketRepository).getAllTicketsByEventAndCompany(EVENT_NAME, COMPANY);
        long seatCount = createdTickets.stream().filter(t -> t.getCol() == 0 && t.getRow() == 1).count();
        long standCount = createdTickets.stream().filter(t -> t.getCol() == 0 && t.getRow() == 2).count();

        assertEquals(1, seatCount);
        assertEquals(1, standCount);
        assertTrue(createdTickets.stream().allMatch(t -> t.getCompany().equals(COMPANY) && t.getEvent().equals(EVENT_NAME)));
    }

    @Test
    void createEvent_Success_AsManagerWithPermissions() {
        String artist = "Manager's Artist";
        double price = 150.0;
        Date date = new Date();
        String location = "Haifa";
        MAP[0][0] = MapArea.SEAT;

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);

        Manager mockManager = mock(Manager.class);
        when(treeOfRoleRepository.exitsOwner(USERNAME, COMPANY)).thenReturn(false);
        when(treeOfRoleRepository.ManagerPermitedToCreateUpdateDelete(USERNAME, COMPANY)).thenReturn(true);
        when(mockManager.getPermissions()).thenReturn(Set.of(Permission.MANAGE_INVENTORY));

        Response<String> res = eventService.createEvent(TOKEN, EVENT_NAME, artist, EventType.LIVE_PERFORMANCE, price, date, location, COMPANY, MAP);
        assertTrue(res.isSuccess());
        assertEquals("success", res.getData());
        Event savedEvent = eventRepository.getEvent(EVENT_NAME, COMPANY);

        assertNotNull(savedEvent);
        assertEquals(EVENT_NAME, savedEvent.getName());
        assertEquals(artist, savedEvent.getArtistName());
        assertEquals(EventType.LIVE_PERFORMANCE, savedEvent.getType());
        assertEquals(price, savedEvent.getPrice());
        assertEquals(date, savedEvent.getDate());
        assertEquals(location, savedEvent.getLocation());
        assertEquals(COMPANY, savedEvent.getCompany());

        List<Ticket> createdTickets = ((TicketRepositoryImpl) ticketRepository).getAllTicketsByEventAndCompany(EVENT_NAME, COMPANY);
        boolean hasSeatTicket = createdTickets.stream().anyMatch(t -> t.getCol() == 0 && t.getRow() == 0);

        assertFalse(createdTickets.isEmpty());
        assertTrue(hasSeatTicket);
        assertEquals(COMPANY, createdTickets.get(0).getCompany());
    }

    @Test
    void createEvent_Failure_InvalidToken() {
        when(tokenService.validateToken(TOKEN)).thenReturn(false);

        Response<String> res = eventService.createEvent(TOKEN, EVENT_NAME, "artist", EventType.LIVE_PERFORMANCE, 100, new Date(), "location", COMPANY, MAP);
        assertFalse(res.isSuccess());
        Event savedEvent = eventRepository.getEvent(EVENT_NAME, COMPANY);
        assertNull(savedEvent, "Event should not be created with an invalid token");
    }

    @Test
    void createEvent_Failure_NoPermissions() {
        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);

        when(treeOfRoleRepository.exitsOwner(USERNAME, COMPANY)).thenReturn(false);
        when(treeOfRoleRepository.ManagerPermitedToCreateUpdateDelete(USERNAME, COMPANY)).thenReturn(false);

        Response<String> res = eventService.createEvent(TOKEN, EVENT_NAME, "artist", EventType.LIVE_PERFORMANCE, 100, new Date(), "location", COMPANY, MAP);
        assertFalse(res.isSuccess());
        Event savedEvent = eventRepository.getEvent(EVENT_NAME, COMPANY);
        assertNull(savedEvent, "Event should not be created if the user has no permissions");
    }

    @Test
    void createEvent_Failure_ManagerWithoutRequiredPermission() {
        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);

        Manager mockManager = mock(Manager.class);
        when(treeOfRoleRepository.exitsOwner(USERNAME, COMPANY)).thenReturn(false);
        when(treeOfRoleRepository.ManagerPermitedToCreateUpdateDelete(USERNAME, COMPANY)).thenReturn(false);
        when(mockManager.getPermissions()).thenReturn(Set.of(Permission.RESPOND_TO_INQUIRIES));

        Response<String> res = eventService.createEvent(TOKEN, EVENT_NAME, "artist", EventType.LIVE_PERFORMANCE, 100, new Date(), "location", COMPANY, MAP);
        assertFalse(res.isSuccess());
        Event savedEvent = eventRepository.getEvent(EVENT_NAME, COMPANY);
        assertNull(savedEvent, "Event should not be created if the manager lacks MANAGE_INVENTORY permission");
    }

    //----------------------------------DELETE EVENT TESTS----------------------------------

    @Test
    void deleteEvent_Success_AsOwner() {
        Event event = eventRepository.store(EVENT_NAME, "Artist", EventType.LIVE_PERFORMANCE, 100.0, new Date(), "Tel Aviv", COMPANY, MAP);
        assertNotNull(eventRepository.getEvent(EVENT_NAME, COMPANY));

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);
        when(treeOfRoleRepository.exitsOwner(USERNAME, COMPANY)).thenReturn(true);

        Response<String> res = eventService.deleteEvent(event.getId(), COMPANY, TOKEN);
        assertTrue(res.isSuccess());
        assertNull(eventRepository.getEvent(EVENT_NAME, COMPANY));
    }

    @Test
    void deleteEvent_Success_AsManagerWithPermissions() {
        Event event = eventRepository.store(EVENT_NAME, "Artist", EventType.LIVE_PERFORMANCE, 100.0, new Date(), "Tel Aviv", COMPANY, MAP);

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);

        Manager mockManager = mock(Manager.class);
        when(treeOfRoleRepository.exitsOwner(USERNAME, COMPANY)).thenReturn(false);
        when(treeOfRoleRepository.ManagerPermitedToCreateUpdateDelete(USERNAME, COMPANY)).thenReturn(true);
        when(mockManager.getPermissions()).thenReturn(Set.of(Permission.MANAGE_INVENTORY));

        Response<String> res = eventService.deleteEvent(event.getId(), COMPANY, TOKEN);
        assertTrue(res.isSuccess());
        assertNull(eventRepository.getEvent(EVENT_NAME, COMPANY));
        List<Ticket> remainingTickets = ((TicketRepositoryImpl) ticketRepository).getTicketsForEvent(COMPANY, EVENT_NAME);
        assertTrue(remainingTickets.isEmpty());
    }

    @Test
    void deleteEvent_Failure_Unauthorized() {
        Event event = eventRepository.store(EVENT_NAME, "Artist", EventType.LIVE_PERFORMANCE, 100.0, new Date(), "Tel Aviv", COMPANY, MAP);

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);

        when(treeOfRoleRepository.exitsOwner(USERNAME, COMPANY)).thenReturn(false);
        when(treeOfRoleRepository.ManagerPermitedToCreateUpdateDelete(USERNAME, COMPANY)).thenReturn(false);

        Response<String> res = eventService.deleteEvent(event.getId(), COMPANY, TOKEN);
        assertFalse(res.isSuccess());
        assertNotNull(eventRepository.getEvent(EVENT_NAME, COMPANY));
    }

    //----------------------------------UPDATE EVENT TESTS----------------------------------

    @Test
    void updateEvent_Success_AsOwner() {
        eventRepository.store(EVENT_NAME, "Old Artist", EventType.LIVE_PERFORMANCE, 100.0, new Date(), "Tel Aviv", COMPANY, MAP);

        String newArtist = "New Artist";
        double newPrice = 200.0;
        double newRating = 4.5;

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);
        when(treeOfRoleRepository.exitsOwner(USERNAME, COMPANY)).thenReturn(true);

        Response<String> res = eventService.UpdateEvent(TOKEN, EVENT_NAME, newArtist, EventType.PLAY, newPrice, new Date(), "New Loc", COMPANY, MAP, newRating);
        assertTrue(res.isSuccess());
        assertEquals("success", res.getData());
        Event updated = eventRepository.getEvent(EVENT_NAME, COMPANY);
        assertNotNull(updated);
        assertEquals(newArtist, updated.getArtistName());
        assertEquals(newPrice, updated.getPrice());
        assertEquals(newRating, updated.getRating());
        assertEquals(EventType.PLAY, updated.getType());
    }

    @Test
    void updateEvent_Success_AsManagerWithPermissions() {
        eventRepository.store(EVENT_NAME, "Old Artist", EventType.LIVE_PERFORMANCE, 100.0, new Date(), "Tel Aviv", COMPANY, MAP);

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);

        Manager mockManager = mock(Manager.class);
        when(treeOfRoleRepository.exitsOwner(USERNAME, COMPANY)).thenReturn(false);
        when(treeOfRoleRepository.ManagerPermitedToCreateUpdateDelete(USERNAME, COMPANY)).thenReturn(true);
        when(mockManager.getPermissions()).thenReturn(Set.of(Permission.MANAGE_INVENTORY));

        Response<String> res = eventService.UpdateEvent(TOKEN, EVENT_NAME, "Manager Artist", EventType.FESTIVAL, 300.0, new Date(), "Loc", COMPANY, MAP, 5.0);

        assertTrue(res.isSuccess());
        assertEquals("success", res.getData());
        Event updated = eventRepository.getEvent(EVENT_NAME, COMPANY);
        assertEquals("Manager Artist", updated.getArtistName());
        assertEquals(5.0, updated.getRating());
    }

    @Test
    void updateEvent_Failure_Unauthorized() {
        String oldArtist = "Old Artist";
        eventRepository.store(EVENT_NAME, oldArtist, EventType.LIVE_PERFORMANCE, 100.0, new Date(), "Tel Aviv", COMPANY, MAP);

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);

        when(treeOfRoleRepository.exitsOwner(USERNAME, COMPANY)).thenReturn(false);
        when(treeOfRoleRepository.ManagerPermitedToCreateUpdateDelete(USERNAME, COMPANY)).thenReturn(false);

        Response<String> res = eventService.UpdateEvent(TOKEN, EVENT_NAME, "New Artist", EventType.PLAY, 500.0, new Date(), "New Loc", COMPANY, MAP, 1.0);
        assertFalse(res.isSuccess());
        Event notUpdated = eventRepository.getEvent(EVENT_NAME, COMPANY);
        assertEquals(oldArtist, notUpdated.getArtistName());
        assertNotEquals(500.0, notUpdated.getPrice());
    }

    @Test
    void getCompanyInfo_Success() {
        String companyName = "LiveNation";
        Company realCompany = new Company(companyName, "admin");
        realCompany.setActive(true);
        String expectedInfo = realCompany.toString();

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(companyRepository.isCompanyActive(companyName)).thenReturn(true);
        when(companyRepository.getCompanyDescription(companyName)).thenReturn(expectedInfo);

        Response<String> result = eventService.getCompanyInfo(TOKEN, companyName);

        assertTrue(result.isSuccess());
        assertEquals(expectedInfo, result.getData());
    }

    @Test
    void getCompanyInfo_Failure_NotActive() {
        String companyName = "InactiveCorp";
        Company realCompany = new Company(companyName, "admin");
        realCompany.setActive(false);

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(companyRepository.isCompanyActive(companyName)).thenReturn(false);

        Response<String> result = eventService.getCompanyInfo(TOKEN, companyName);
        assertTrue(result.isError());
    }

    @Test
    void getMapArea_Success() {
        MapArea[][] customMap = new MapArea[2][2];
        customMap[0][0] = MapArea.STAGE;
        customMap[0][1] = MapArea.ENTRANCE;
        customMap[1][0] = MapArea.SEAT;
        customMap[1][1] = MapArea.STAND;

        eventRepository.store(EVENT_NAME, "Artist", EventType.LIVE_PERFORMANCE, 100.0, new Date(), "Tel Aviv", COMPANY, customMap);

        when(tokenService.validateToken(TOKEN)).thenReturn(true);

        doReturn(customMap).when(ticketRepository).getMapAreas(eq(COMPANY), eq(EVENT_NAME), any(MapArea[][].class));

        Response<MapArea[][]> result = eventService.getMapArea(TOKEN, COMPANY, EVENT_NAME);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertArrayEquals(customMap, result.getData());
        assertEquals(MapArea.STAGE, result.getData()[0][0]);
        assertEquals(MapArea.ENTRANCE, result.getData()[0][1]);
    }

    @Test
    void getMapArea_Failure_EventNotFound() {
        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        Response<MapArea[][]> result = eventService.getMapArea(TOKEN, COMPANY, "NonExistentEvent");

        assertTrue(result.isError());
    }

    @Test
    void searchEvents_FilterByCompany() {
        eventRepository.store("Rock", "Artist A", EventType.LIVE_PERFORMANCE, 100.0, new Date(), "Tel Aviv", "CompanyA", MAP);
        eventRepository.store("Jazz", "Artist B", EventType.LIVE_PERFORMANCE, 150.0, new Date(), "Haifa", "CompanyB", MAP);

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        Response<List<EventDTO>> result = eventService.searchEvents(TOKEN, null, "CompanyA", null, null, null, null, null, null, null);

        assertTrue(result.isSuccess());
        assertEquals(1, result.getData().size());
        assertTrue(result.getData().get(0).name().contains("Rock"));
    }

    @Test
    void searchEvents_FilterByQuery() {
        eventRepository.store("Special Show", "Artist A", EventType.LIVE_PERFORMANCE, 100.0, new Date(), "Tel Aviv", COMPANY, MAP);
        eventRepository.store("Regular Event", "Artist B", EventType.LIVE_PERFORMANCE, 150.0, new Date(), "Haifa", COMPANY, MAP);

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        Response<List<EventDTO>> result = eventService.searchEvents(TOKEN, "Special", null, null, null, null, null, null, null, null);

        assertTrue(result.isSuccess());
        assertEquals(1, result.getData().size());
        assertTrue(result.getData().get(0).name().contains("Special Show"));
    }

    @Test
    void searchEvents_FilterByPriceRange() {
        eventRepository.store("Cheap", "A", EventType.LIVE_PERFORMANCE, 50.0, new Date(), "L", COMPANY, MAP);
        eventRepository.store("Mid", "B", EventType.LIVE_PERFORMANCE, 150.0, new Date(), "L", COMPANY, MAP);
        eventRepository.store("Expensive", "C", EventType.LIVE_PERFORMANCE, 300.0, new Date(), "L", COMPANY, MAP);

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        Response<List<EventDTO>> result = eventService.searchEvents(TOKEN, null, null, null, 100.0, 200.0, null, null, null, null);

        assertTrue(result.isSuccess());
        assertEquals(1, result.getData().size());
        assertTrue(result.getData().get(0).name().contains("Mid"));
    }

    @Test
    void searchEvents_FilterByType() {
        eventRepository.store("Concert", "A", EventType.LIVE_PERFORMANCE, 100.0, new Date(), "L", COMPANY, MAP);
        eventRepository.store("Theater", "B", EventType.PLAY, 100.0, new Date(), "L", COMPANY, MAP);

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        Response<List<EventDTO>> result = eventService.searchEvents(TOKEN, null, null, EventType.PLAY, null, null, null, null, null, null);

        assertTrue(result.isSuccess());
        assertEquals(1, result.getData().size());
        assertTrue(result.getData().get(0).type().equals(EventType.PLAY));
    }

    @Test
    void searchEvents_CombinedFilters() {
        eventRepository.store("Target-Perfect", "Artist", EventType.LIVE_PERFORMANCE, 150.0, new Date(), "Tel Aviv", COMPANY, MAP);
        eventRepository.store("Wrong-Name", "Artist", EventType.LIVE_PERFORMANCE, 150.0, new Date(), "Tel Aviv", COMPANY, MAP);
        eventRepository.store("Target-Wrong-Type", "Artist", EventType.PLAY, 150.0, new Date(), "Tel Aviv", COMPANY, MAP);

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        Response<List<EventDTO>> result = eventService.searchEvents(
                TOKEN,
                "Target",
                COMPANY,
                EventType.LIVE_PERFORMANCE,
                100.0,
                200.0,
                null,
                null,
                "Tel Aviv",
                null
        );

        assertTrue(result.isSuccess());
        assertEquals(1, result.getData().size());
        assertTrue(result.getData().get(0).name().contains("Target-Perfect"));
        assertTrue(result.getData().get(0).type().equals(EventType.LIVE_PERFORMANCE));
    }

    @Test
    void searchEvents_NoResults() {
        eventRepository.store("Event", "Artist", EventType.LIVE_PERFORMANCE, 100.0, new Date(), "Tel Aviv", COMPANY, MAP);

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        Response<List<EventDTO>> result = eventService.searchEvents(TOKEN, "NonExistent", null, null, null, null, null, null, null, null);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertTrue(result.getData().isEmpty());
    }

    @Test
    void searchEvents_CaseInsensitive_LowercaseQuery_MatchesUppercaseEventName() {
        eventRepository.store("Rock Concert", "Artist A", EventType.LIVE_PERFORMANCE, 100.0, new Date(), "Tel Aviv", COMPANY, MAP);
        eventRepository.store("Jazz Night", "Artist B", EventType.LIVE_PERFORMANCE, 150.0, new Date(), "Haifa", COMPANY, MAP);

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        // Search with all lowercase — event name is mixed-case "Rock Concert"
        Response<List<EventDTO>> result = eventService.searchEvents(TOKEN, "rock concert", null, null, null, null, null, null, null, null);

        assertTrue(result.isSuccess());
        assertEquals(1, result.getData().size());
        assertEquals("Rock Concert", result.getData().get(0).name());
    }

    @Test
    void searchEvents_CaseInsensitive_UppercaseQuery_MatchesLowercaseArtistName() {
        eventRepository.store("Summer Fest", "john doe", EventType.FESTIVAL, 200.0, new Date(), "Tel Aviv", COMPANY, MAP);
        eventRepository.store("Winter Gala", "jane smith", EventType.FESTIVAL, 250.0, new Date(), "Haifa", COMPANY, MAP);

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        // Search with all uppercase — artist name is lowercase "john doe"
        Response<List<EventDTO>> result = eventService.searchEvents(TOKEN, "JOHN", null, null, null, null, null, null, null, null);

        assertTrue(result.isSuccess());
        assertEquals(1, result.getData().size());
        assertEquals("Summer Fest", result.getData().get(0).name());
    }
    void getCompanyEvents_Success() {
        eventRepository.store(EVENT_NAME, "Artist", EventType.LIVE_PERFORMANCE, 100.0, new Date(), "TLV", COMPANY, MAP);
        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(companyRepository.isCompanyActive(COMPANY)).thenReturn(true);

        Response<List<EventDTO>> result = eventService.getCompanyEvents(TOKEN, COMPANY);

        assertTrue(result.isSuccess());
        assertFalse(result.getData().isEmpty());
        assertEquals(EVENT_NAME, result.getData().get(0).name());
    }

    @Test
    void getCompanyEvents_Failure_CompanyNotActive() {
        when(companyRepository.isCompanyActive(COMPANY)).thenReturn(false);

        Response<List<EventDTO>> result = eventService.getCompanyEvents(TOKEN, COMPANY);

        assertTrue(result.isError());
    }

    @Test
    void getEvent_Success() {
        eventRepository.store(EVENT_NAME, "Artist", EventType.LIVE_PERFORMANCE, 100.0, new Date(), "TLV", COMPANY, MAP);
        when(tokenService.validateToken(TOKEN)).thenReturn(true);

        Response<EventDTO> result = eventService.getEvent(TOKEN, COMPANY, EVENT_NAME);

        assertTrue(result.isSuccess());
        assertEquals(EVENT_NAME, result.getData().name());
    }

    @Test
    void getEvent_NotFound_ReturnsError() {
        when(tokenService.validateToken(TOKEN)).thenReturn(true);

        Response<EventDTO> result = eventService.getEvent(TOKEN, COMPANY, "NonExistentEvent");

        assertTrue(result.isError());
        assertEquals("Event not found", result.getMessage());
    }

    @Test
    void createEvent_Failure_UserSuspended() {
        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);
        when(treeOfRoleRepository.exitsOwner(USERNAME, COMPANY)).thenReturn(true);

        when(userRepository.isUserSuspendedNow(USERNAME)).thenReturn(true);

        Response<String> res = eventService.createEvent(TOKEN, EVENT_NAME, "Artist", EventType.LIVE_PERFORMANCE, 100.0, new Date(), "Tel Aviv", COMPANY, MAP);

        assertFalse(res.isSuccess());
        assertTrue(res.isError());
        assertTrue(res.getMessage().contains("User is suspended"));
        assertNull(eventRepository.getEvent(EVENT_NAME, COMPANY));
    }

    @Test
    void deleteEvent_Failure_UserSuspended() {
        Event event = eventRepository.store(EVENT_NAME, "Artist", EventType.LIVE_PERFORMANCE, 100.0, new Date(), "Tel Aviv", COMPANY, MAP);
        assertNotNull(eventRepository.getEvent(EVENT_NAME, COMPANY));

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);
        when(treeOfRoleRepository.exitsOwner(USERNAME, COMPANY)).thenReturn(true);

        when(userRepository.isUserSuspendedNow(USERNAME)).thenReturn(true);

        Response<String> res = eventService.deleteEvent(event.getId(), COMPANY, TOKEN);

        assertFalse(res.isSuccess());
        assertTrue(res.isError());
        assertTrue(res.getMessage().contains("User is suspended"));
        assertNotNull(eventRepository.getEvent(EVENT_NAME, COMPANY));
    }

    @Test
    void updateEvent_Failure_UserSuspended() {
        eventRepository.store(EVENT_NAME, "Old Artist", EventType.LIVE_PERFORMANCE, 100.0, new Date(), "Tel Aviv", COMPANY, MAP);

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);
        when(treeOfRoleRepository.exitsOwner(USERNAME, COMPANY)).thenReturn(true);

        when(userRepository.isUserSuspendedNow(USERNAME)).thenReturn(true);

        Response<String> res = eventService.UpdateEvent(TOKEN, EVENT_NAME, "New Artist", EventType.PLAY, 200.0, new Date(), "New Loc", COMPANY, MAP, 5.0);

        assertFalse(res.isSuccess());
        assertTrue(res.isError());
        assertEquals("User is suspended", res.getMessage());
        Event notUpdated = eventRepository.getEvent(EVENT_NAME, COMPANY);
        assertEquals("Old Artist", notUpdated.getArtistName());
        assertEquals(100.0, notUpdated.getPrice());
    }
}
