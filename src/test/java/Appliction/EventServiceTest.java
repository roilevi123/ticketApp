package Appliction;
import Domain.Event.*;
import Domain.OwnerManagerTree.Manager;
import Domain.OwnerManagerTree.Owner;
import Domain.OwnerManagerTree.Permission;
import Domain.OwnerManagerTree.iTreeOfRoleRepository;
import Domain.Company.Company;
import Domain.Company.iCompanyRepository;
import Domain.QueueAggregates.iQueueRepository;
import Domain.User.IUserRepository;
import Domain.Ticket.iTicketRepository;
import Domain.Ticket.Ticket;
import Infastructure.*;
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
        eventService = new EventService(companyRepository, eventRepository, tokenService, treeOfRoleRepository, ticketRepository,queueRepository);
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
        when(tokenService.extractUsername(TOKEN)).thenReturn(USERNAME);
        when(treeOfRoleRepository.exitsOwner(USERNAME, COMPANY)).thenReturn(true);

        Response<String> res =eventService.createEvent(TOKEN, COMPANY, EVENT_NAME, EventType.LIVE_PERFORMANCE, location, artist, date, price, 1000, MAP);
        assertEquals(res.isSuccess(), true);
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
        when(tokenService.extractUsername(TOKEN)).thenReturn(USERNAME);

        Manager mockManager = mock(Manager.class);
        when(treeOfRoleRepository.exitsOwner(USERNAME, COMPANY)).thenReturn(false);
        when(treeOfRoleRepository.ManagerPermitedToCreateUpdateDelete(USERNAME, COMPANY)).thenReturn(true);
        when(mockManager.getPermissions()).thenReturn(Set.of(Permission.MANAGE_INVENTORY));

        Response<String> res=eventService.createEvent(TOKEN, COMPANY, EVENT_NAME, EventType.LIVE_PERFORMANCE, location, artist, date, price, 1000, MAP);
        assertEquals(res.getMessage(), "Event created successfully");
        assertEquals(res.isSuccess(), true);
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

        Response<String> res=eventService.createEvent(TOKEN, COMPANY, EVENT_NAME, EventType.LIVE_PERFORMANCE, "Tel Aviv", "Artist", new Date(), 100.0, 1000, MAP);
        assertEquals(res.isSuccess(), false);
        Event savedEvent = eventRepository.getEvent(EVENT_NAME, COMPANY);
        assertNull(savedEvent, "Event should not be created with an invalid token");
    }

    @Test
    void createEvent_Failure_NoPermissions() {
        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUsername(TOKEN)).thenReturn(USERNAME);

        when(treeOfRoleRepository.exitsOwner(USERNAME, COMPANY)).thenReturn(false);
        when(treeOfRoleRepository.ManagerPermitedToCreateUpdateDelete(USERNAME, COMPANY)).thenReturn(false);

        Response<String> res=eventService.createEvent(TOKEN, COMPANY, EVENT_NAME, EventType.LIVE_PERFORMANCE, "Tel Aviv", "Artist", new Date(), 100.0, 1000, MAP);
        assertEquals(res.isSuccess(), false);
        Event savedEvent = eventRepository.getEvent(EVENT_NAME, COMPANY);
        assertNull(savedEvent, "Event should not be created if the user has no permissions");
    }

    @Test
    void createEvent_Failure_ManagerWithoutRequiredPermission() {
        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUsername(TOKEN)).thenReturn(USERNAME);

        Manager mockManager = mock(Manager.class);
        when(treeOfRoleRepository.exitsOwner(USERNAME, COMPANY)).thenReturn(false);
        when(treeOfRoleRepository.ManagerPermitedToCreateUpdateDelete(USERNAME, COMPANY)).thenReturn(false);
        when(mockManager.getPermissions()).thenReturn(Set.of(Permission.RESPOND_TO_INQUIRIES));

        Response<String> res=eventService.createEvent(TOKEN, COMPANY, EVENT_NAME, EventType.LIVE_PERFORMANCE, "Tel Aviv", "Artist", new Date(), 100.0, 1000, MAP);
        assertEquals(res.isSuccess(), false);
        Event savedEvent = eventRepository.getEvent(EVENT_NAME, COMPANY);
        assertNull(savedEvent, "Event should not be created if the manager lacks MANAGE_INVENTORY permission");
    }

//----------------------------------DELETE EVENT TESTS----------------------------------

    @Test
    void deleteEvent_Success_AsOwner() {
        Event event = eventRepository.store(EVENT_NAME, "Artist", EventType.LIVE_PERFORMANCE, 100.0, new Date(), "Tel Aviv", COMPANY, 200, MAP);
        assertNotNull(eventRepository.getEvent(EVENT_NAME, COMPANY));

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUsername(TOKEN)).thenReturn(USERNAME);
        when(treeOfRoleRepository.exitsOwner(USERNAME, COMPANY)).thenReturn(true);

        Response<Void> res = eventService.deleteEvent(event.getId(), COMPANY, TOKEN);
        assertEquals(res.isSuccess(), true);
        assertNull(eventRepository.getEvent(EVENT_NAME, COMPANY));
    }
    @Test
    void deleteEvent_Success_AsManagerWithPermissions() {
        Event event = eventRepository.store(EVENT_NAME, "Artist", EventType.LIVE_PERFORMANCE, 100.0, new Date(), "Tel Aviv", COMPANY, 1500, MAP);

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUsername(TOKEN)).thenReturn(USERNAME);

        Manager mockManager = mock(Manager.class);
        when(treeOfRoleRepository.exitsOwner(USERNAME, COMPANY)).thenReturn(false);
        when(treeOfRoleRepository.ManagerPermitedToCreateUpdateDelete(USERNAME, COMPANY)).thenReturn(true);
        when(mockManager.getPermissions()).thenReturn(Set.of(Permission.MANAGE_INVENTORY));

        Response<Void> res = eventService.deleteEvent(event.getId(), COMPANY, TOKEN);
        assertEquals(res.isSuccess(), true);
        assertNull(eventRepository.getEvent(EVENT_NAME, COMPANY));
        List<Ticket> remainingTickets = ((TicketRepositoryImpl) ticketRepository).getTicketsForEvent(COMPANY, EVENT_NAME);
        assertTrue(remainingTickets.isEmpty());
    }
    @Test
    void deleteEvent_Failure_Unauthorized() {
        Event event = eventRepository.store(EVENT_NAME, "Artist", EventType.LIVE_PERFORMANCE, 100.0, new Date(), "Tel Aviv", COMPANY, 1000, MAP);

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUsername(TOKEN)).thenReturn(USERNAME);

        when(treeOfRoleRepository.exitsOwner(USERNAME, COMPANY)).thenReturn(false);
        when(treeOfRoleRepository.ManagerPermitedToCreateUpdateDelete(USERNAME, COMPANY)).thenReturn(false);

        Response<Void> res = eventService.deleteEvent(event.getId(), COMPANY, TOKEN);
        assertEquals(res.isSuccess(), false);
        assertNotNull(eventRepository.getEvent(EVENT_NAME, COMPANY));
        List<Ticket> remainingTickets = ((TicketRepositoryImpl) ticketRepository).getTicketsForEvent(COMPANY, EVENT_NAME);
        assertTrue(remainingTickets.isEmpty());
    }
    

//----------------------------------UPDATE EVENT TESTS----------------------------------

    @Test
    void updateEvent_Success_AsOwner() {
        Event event = eventRepository.store(EVENT_NAME, "Old Artist", EventType.LIVE_PERFORMANCE, 100.0, new Date(), "Old Loc", COMPANY, 1000, MAP);

        String newArtist = "New Artist";
        double newPrice = 200.0;
        double newRating = 4.5;

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUsername(TOKEN)).thenReturn(USERNAME);
        when(treeOfRoleRepository.exitsOwner(USERNAME, COMPANY)).thenReturn(true);

        String res=eventService.UpdateEvent(TOKEN, event.getId(), EVENT_NAME, newArtist, EventType.PLAY, newPrice, new Date(), "New Loc", COMPANY, MAP, newRating);
        assertEquals(res, "success");
        Event updated = eventRepository.getEvent(EVENT_NAME, COMPANY);
        assertNotNull(updated);
        assertEquals(newArtist, updated.getArtistName());
        assertEquals(newPrice, updated.getPrice());
        assertEquals(newRating, updated.getRating());
        assertEquals(EventType.PLAY, updated.getType());
    }

    @Test
    void updateEvent_Success_AsManagerWithPermissions() {
        Event event = eventRepository.store(EVENT_NAME, "Artist", EventType.LIVE_PERFORMANCE, 100.0, new Date(), "Loc", COMPANY, 1000, MAP);

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUsername(TOKEN)).thenReturn(USERNAME);

        Manager mockManager = mock(Manager.class);
        when(treeOfRoleRepository.exitsOwner(USERNAME, COMPANY)).thenReturn(false);
        when(treeOfRoleRepository.ManagerPermitedToCreateUpdateDelete(USERNAME, COMPANY)).thenReturn(true);
        when(mockManager.getPermissions()).thenReturn(Set.of(Permission.MANAGE_INVENTORY));

        String res=eventService.UpdateEvent(TOKEN, event.getId(), EVENT_NAME, "Manager Artist", EventType.FESTIVAL, 300.0, new Date(), "Loc", COMPANY, MAP, 5.0);
        assertEquals(res, "success");
        Event updated = eventRepository.getEvent(EVENT_NAME, COMPANY);
        assertEquals("Manager Artist", updated.getArtistName());
        assertEquals(5.0, updated.getRating());
    }

    @Test
    void updateEvent_Failure_Unauthorized() {
        String oldArtist = "Old Artist";
        Event event = eventRepository.store(EVENT_NAME, oldArtist, EventType.LIVE_PERFORMANCE, 100.0, new Date(), "Old Loc", COMPANY, 2000, MAP);
        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUsername(TOKEN)).thenReturn(USERNAME);

        when(treeOfRoleRepository.exitsOwner(USERNAME, COMPANY)).thenReturn(false);
        when(treeOfRoleRepository.ManagerPermitedToCreateUpdateDelete(USERNAME, COMPANY)).thenReturn(false);

        String res=eventService.UpdateEvent(TOKEN, event.getId(), EVENT_NAME, "New Artist", EventType.PLAY, 500.0, new Date(), "New Loc", COMPANY, MAP, 1.0);
        assertEquals(res, "failed");
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

        when(companyRepository.isCompanyActive(companyName)).thenReturn(true);
        when(companyRepository.getCompanyDescription(companyName)).thenReturn(expectedInfo);


        String result = eventService.getCompanyInfo(companyName);

        assertEquals(expectedInfo, result);
    }

    @Test
    void getCompanyInfo_Failure_NotActive() {
        String companyName = "InactiveCorp";
        Company realCompany = new Company(companyName, "admin");

        realCompany.setActive(false);

        when(companyRepository.isCompanyActive(companyName)).thenReturn(false);

        String result = eventService.getCompanyInfo(companyName);
//        System.out.println(result);
        assertEquals(null, result);
    }
    @Test
    void getMapArea_Success() {
        MapArea[][] customMap = new MapArea[2][2];
        customMap[0][0] = MapArea.STAGE;
        customMap[0][1] = MapArea.ENTRANCE;
        customMap[1][0] = MapArea.SEAT;
        customMap[1][1] = MapArea.STAND;

        eventRepository.store(EVENT_NAME, "Artist", EventType.LIVE_PERFORMANCE, 100.0, new Date(), "Tel Aviv", COMPANY, 100,customMap);

        MapArea[][] result = eventService.getMapArea(COMPANY, EVENT_NAME);

        assertNotNull(result);
        assertArrayEquals(customMap, result);
        assertEquals(MapArea.STAGE, result[0][0]);
        assertEquals(MapArea.ENTRANCE, result[0][1]);
    }

    @Test
    void getMapArea_Failure_EventNotFound() {
        MapArea[][] result = eventService.getMapArea(COMPANY, "NonExistentEvent");

        assertNull(result);
    }
    @Test
    void searchEvents_FilterByCompany() {
        eventRepository.store("Rock", "Artist A", EventType.LIVE_PERFORMANCE, 100.0, new Date(), "Tel Aviv", "CompanyA",100, MAP);
        eventRepository.store("Jazz", "Artist B", EventType.LIVE_PERFORMANCE, 150.0, new Date(), "Haifa", "CompanyB", 100,MAP);

        List<String> result = eventService.searchEvents(null, "CompanyA", null, null, null, null, null, null, null);

        assertEquals(1, result.size());
        assertTrue(result.get(0).contains("Rock"));
    }

    @Test
    void searchEvents_FilterByQuery() {
        eventRepository.store("Special Show", "Artist A", EventType.LIVE_PERFORMANCE, 100.0, new Date(), "Tel Aviv", COMPANY, 100,MAP);
        eventRepository.store("Regular Event", "Artist B", EventType.LIVE_PERFORMANCE, 150.0, new Date(), "Haifa", COMPANY,100, MAP);

        List<String> result = eventService.searchEvents("Special", null, null, null, null, null, null, null, null);

        assertEquals(1, result.size());
        assertTrue(result.get(0).contains("Special Show"));
    }

    @Test
    void searchEvents_FilterByPriceRange() {
        eventRepository.store("Cheap", "A", EventType.LIVE_PERFORMANCE, 50.0, new Date(), "L", COMPANY, 100,MAP);
        eventRepository.store("Mid", "B", EventType.LIVE_PERFORMANCE, 150.0, new Date(), "L", COMPANY,100, MAP);
        eventRepository.store("Expensive", "C", EventType.LIVE_PERFORMANCE, 300.0, new Date(), "L", COMPANY, 100,MAP);

        List<String> result = eventService.searchEvents(null, null, null, 100.0, 200.0, null, null, null, null);

        assertEquals(1, result.size());
        assertTrue(result.get(0).contains("Mid"));
    }

    @Test
    void searchEvents_FilterByType() {
        eventRepository.store("Concert", "A", EventType.LIVE_PERFORMANCE, 100.0, new Date(), "L", COMPANY, 100,MAP);
        eventRepository.store("Theater", "B", EventType.PLAY, 100.0, new Date(), "L", COMPANY,100, MAP);

        List<String> result = eventService.searchEvents(null, null, EventType.PLAY, null, null, null, null, null, null);

        assertEquals(1, result.size());
        assertTrue(result.get(0).contains("PLAY"));
    }

    @Test
    void searchEvents_CombinedFilters() {
        eventRepository.store("Target-Perfect", "Artist", EventType.LIVE_PERFORMANCE, 150.0, new Date(), "Tel Aviv", COMPANY,100, MAP);
        eventRepository.store("Wrong-Name", "Artist", EventType.LIVE_PERFORMANCE, 150.0, new Date(), "Tel Aviv", COMPANY, 100,MAP);
        eventRepository.store("Target-Wrong-Type", "Artist", EventType.PLAY, 150.0, new Date(), "Tel Aviv", COMPANY, 100,MAP);

        List<String> result = eventService.searchEvents(
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

        // רק Target-Perfect אמור לעבור את כל הפילטרים
        assertEquals(1, result.size());
        assertTrue(result.get(0).contains("Target-Perfect"));
        assertTrue(result.get(0).contains("LIVE_PERFORMANCE"));
    }

    @Test
    void searchEvents_NoResults() {
        eventRepository.store("Event", "Artist", EventType.LIVE_PERFORMANCE, 100.0, new Date(), "Tel Aviv", COMPANY, 100,MAP);

        List<String> result = eventService.searchEvents("NonExistent", null, null, null, null, null, null, null, null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }


}
