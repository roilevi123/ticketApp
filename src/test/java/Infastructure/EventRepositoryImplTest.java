package Infastructure;

import com.ticketing.ticketapp.Domain.Event.*;
import com.ticketing.ticketapp.Infastructure.EventRepositoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EventRepositoryImplTest {

    private EventRepositoryImpl repo;

    private static final String COMPANY = "CompanyA";
    private static final String EVENT_NAME = "Concert";
    private static final MapArea[][] MAP = {{MapArea.SEAT, MapArea.SEAT}};

    @BeforeEach
    void setUp() {
        repo = new EventRepositoryImpl();
    }

    private Event storeTestEvent(String name, String company) {
        return repo.store(name, "Artist", EventType.LIVE_PERFORMANCE, 50.0, new Date(), "NYC", company, MAP);
    }

    @Test
    void store_CreatesEvent() {
        Event e = storeTestEvent(EVENT_NAME, COMPANY);
        assertNotNull(e);
        assertEquals(EVENT_NAME, e.getName());
        assertEquals(COMPANY, e.getCompany());
    }

    @Test
    void store_DuplicateEventAndCompany_Throws() {
        storeTestEvent(EVENT_NAME, COMPANY);
        assertThrows(RuntimeException.class, () -> storeTestEvent(EVENT_NAME, COMPANY));
    }

    @Test
    void getEvent_Found_ReturnsEvent() {
        storeTestEvent(EVENT_NAME, COMPANY);
        Event e = repo.getEvent(EVENT_NAME, COMPANY);
        assertNotNull(e);
        assertEquals(EVENT_NAME, e.getName());
    }

    @Test
    void getEvent_NotFound_ReturnsNull() {
        assertNull(repo.getEvent("NonExistent", COMPANY));
    }

    @Test
    void getEventById_Found_ReturnsEvent() {
        Event stored = storeTestEvent(EVENT_NAME, COMPANY);
        Event found = repo.getEventById(stored.getId(), COMPANY);
        assertNotNull(found);
        assertEquals(stored.getId(), found.getId());
    }

    @Test
    void getEventById_NotFound_ReturnsNull() {
        assertNull(repo.getEventById("bad-id", COMPANY));
    }

    @Test
    void getEventsByCompany_ReturnsOnlyCompanyEvents() {
        storeTestEvent("Event1", COMPANY);
        storeTestEvent("Event2", COMPANY);
        storeTestEvent("Event3", "OtherCompany");
        List<Event> events = repo.getEventsByCompany(COMPANY);
        assertEquals(2, events.size());
    }

    @Test
    void save_UpdatesEvent() {
        Event stored = storeTestEvent(EVENT_NAME, COMPANY);
        stored.setPrice(99.0);
        repo.save(stored);
        Event updated = repo.getEvent(EVENT_NAME, COMPANY);
        assertEquals(99.0, updated.getPrice(), 0.001);
    }

    @Test
    void save_NotFound_Throws() {
        Event phantom = new Event("id", COMPANY, null, "Ghost", "NYC", "Artist",
                new Date(), 50.0, 100, EventType.LIVE_PERFORMANCE, MAP);
        assertThrows(RuntimeException.class, () -> repo.save(phantom));
    }

    @Test
    void getMapArea_Found_ReturnsMap() {
        storeTestEvent(EVENT_NAME, COMPANY);
        MapArea[][] map = repo.getMapArea(COMPANY, EVENT_NAME);
        assertNotNull(map);
    }

    @Test
    void getMapArea_NotFound_Throws() {
        assertThrows(RuntimeException.class, () -> repo.getMapArea(COMPANY, "NoEvent"));
    }

    @Test
    void deleteEvent_RemovesEvent() {
        Event stored = storeTestEvent(EVENT_NAME, COMPANY);
        repo.deleteEvent(stored.getId(), COMPANY);
        assertNull(repo.getEvent(EVENT_NAME, COMPANY));
    }

    @Test
    void deleteEvent_NotFound_Throws() {
        assertThrows(RuntimeException.class, () -> repo.deleteEvent("bad-id", COMPANY));
    }

    @Test
    void deleteAllEvents_ClearsAll() {
        storeTestEvent("E1", COMPANY);
        storeTestEvent("E2", COMPANY);
        repo.deleteAllEvents();
        assertTrue(repo.getEventsByCompany(COMPANY).isEmpty());
    }

    @Test
    void deleteCompanyEvent_RemovesCompanyEvents() {
        storeTestEvent("E1", COMPANY);
        storeTestEvent("E2", COMPANY);
        storeTestEvent("E3", "OtherCompany");
        repo.deleteCompanyEvent(COMPANY);
        assertTrue(repo.getEventsByCompany(COMPANY).isEmpty());
        assertFalse(repo.getEventsByCompany("OtherCompany").isEmpty());
    }

    @Test
    void searchEvents_ByQuery_ReturnsMatch() {
        storeTestEvent("Rock Concert", COMPANY);
        storeTestEvent("Jazz Night", COMPANY);
        List<Event> results = repo.searchEvents("Rock", null, null, null, null, null, null, null, null);
        assertEquals(1, results.size());
        assertEquals("Rock Concert", results.get(0).getName());
    }

    @Test
    void searchEvents_ByType_ReturnsMatch() {
        storeTestEvent("Concert", COMPANY);
        List<Event> results = repo.searchEvents(null, null, EventType.LIVE_PERFORMANCE, null, null, null, null, null, null);
        assertEquals(1, results.size());
    }

    @Test
    void searchEvents_ByPriceRange_ReturnsMatch() {
        storeTestEvent("Concert", COMPANY);
        List<Event> results = repo.searchEvents(null, null, null, 10.0, 100.0, null, null, null, null);
        assertEquals(1, results.size());
    }

    @Test
    void searchEvents_ByPriceRange_NoMatch() {
        storeTestEvent("Concert", COMPANY);
        List<Event> results = repo.searchEvents(null, null, null, 200.0, 300.0, null, null, null, null);
        assertTrue(results.isEmpty());
    }

    @Test
    void searchEvents_ByCompany_FiltersToCompany() {
        storeTestEvent("Event1", COMPANY);
        storeTestEvent("Event2", "OtherCompany");
        List<Event> results = repo.searchEvents(null, COMPANY, null, null, null, null, null, null, null);
        assertEquals(1, results.size());
    }
}
