package com.ticketing.ticketapp.Domain;

import com.ticketing.ticketapp.Domain.Event.Event;
import com.ticketing.ticketapp.Domain.Event.EventDTO;
import com.ticketing.ticketapp.Domain.Event.EventType;
import com.ticketing.ticketapp.Domain.Event.MapArea;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class EventDomainTest {

    @Test
    void event_Constructor_SetsAllFields() {
        MapArea[][] map = {{MapArea.SEAT}};
        Date date = new Date();
        Event event = new Event("e1", "Corp", "q1", "Concert", "Venue", "Artist",
                date, 50.0, 100, EventType.LIVE_PERFORMANCE, map);

        assertEquals("e1", event.getId());
        assertEquals("Corp", event.getCompany());
        assertEquals("q1", event.getQueueId());
        assertEquals("Concert", event.getName());
        assertEquals("Venue", event.getLocation());
        assertEquals("Artist", event.getArtistName());
        assertEquals(date, event.getDate());
        assertEquals(50.0, event.getPrice());
        assertEquals(100, event.getTotalTickets());
        assertEquals(100, event.getAvailableTickets());
        assertEquals(0, event.getVersion());
        assertEquals(0.0, event.getRating());
        assertEquals(EventType.LIVE_PERFORMANCE, event.getType());
        assertFalse(event.isHighDemand());
        assertNull(event.getLotteryEndDate());
        assertEquals(0, event.getLotteryMaxWinners());
    }

    @Test
    void event_CopyConstructor_CopiesAllFields() {
        MapArea[][] map = {{MapArea.SEAT}};
        Date date = new Date();
        Event original = new Event("e1", "Corp", "q1", "Concert", "Venue", "Artist",
                date, 50.0, 100, EventType.LIVE_PERFORMANCE, map);
        original.setHighDemand(true);
        original.setLotteryEndDate(date);
        original.setLotteryMaxWinners(5);
        original.setRating(4.5);
        original.setVersion(3);

        Event copy = new Event(original);

        assertEquals(original.getId(), copy.getId());
        assertEquals(original.getCompany(), copy.getCompany());
        assertEquals(original.getQueueId(), copy.getQueueId());
        assertEquals(original.getName(), copy.getName());
        assertEquals(original.getLocation(), copy.getLocation());
        assertEquals(original.getArtistName(), copy.getArtistName());
        assertEquals(original.getDate(), copy.getDate());
        assertEquals(original.getPrice(), copy.getPrice());
        assertEquals(original.getTotalTickets(), copy.getTotalTickets());
        assertEquals(original.getAvailableTickets(), copy.getAvailableTickets());
        assertEquals(original.getVersion(), copy.getVersion());
        assertEquals(original.getRating(), copy.getRating());
        assertEquals(original.getType(), copy.getType());
        assertTrue(copy.isHighDemand());
        assertEquals(original.getLotteryEndDate(), copy.getLotteryEndDate());
        assertEquals(original.getLotteryMaxWinners(), copy.getLotteryMaxWinners());
    }

    @Test
    void event_Setters_UpdateAllMutableFields() {
        Event event = makeEvent();
        Date newDate = new Date();

        event.setName("NewName");
        event.setType(EventType.FESTIVAL);
        event.setLocation("NewVenue");
        event.setArtistName("NewArtist");
        event.setPrice(99.9);
        event.setDate(newDate);
        event.setVersion(2);
        event.setRating(3.5);
        event.setCompany("NewCorp");
        event.setHighDemand(true);
        event.setLotteryEndDate(newDate);
        event.setLotteryMaxWinners(20);

        assertEquals("NewName", event.getName());
        assertEquals(EventType.FESTIVAL, event.getType());
        assertEquals("NewVenue", event.getLocation());
        assertEquals("NewArtist", event.getArtistName());
        assertEquals(99.9, event.getPrice());
        assertEquals(newDate, event.getDate());
        assertEquals(2, event.getVersion());
        assertEquals(3.5, event.getRating());
        assertEquals("NewCorp", event.getCompany());
        assertTrue(event.isHighDemand());
        assertEquals(newDate, event.getLotteryEndDate());
        assertEquals(20, event.getLotteryMaxWinners());
    }

    @Test
    void event_SetMap_UpdatesMap() {
        Event event = makeEvent();
        MapArea[][] newMap = {{MapArea.STAND, MapArea.SEAT}, {MapArea.STAGE, MapArea.ENTRANCE}};
        event.setMap(newMap);
        assertArrayEquals(newMap, event.getMap());
    }

    @Test
    void event_ToString_ContainsKeyFields() {
        Event event = makeEvent();
        String str = event.toString();
        assertTrue(str.contains("Concert"));
        assertTrue(str.contains("Artist"));
        assertTrue(str.contains("50.0"));
    }

    @Test
    void event_AllEventTypes_AreAccepted() {
        for (EventType type : EventType.values()) {
            Event event = new Event("e1", "Corp", "q1", "Concert", "Venue", "Artist",
                    new Date(), 10.0, 50, type, null);
            assertEquals(type, event.getType());
        }
    }

    @Test
    void event_AllMapAreaValues_AreValid() {
        MapArea[] allAreas = MapArea.values();
        assertTrue(allAreas.length > 0);
    }

    // ── EventDTO ─────────────────────────────────────────────────────────────

    @Test
    void eventDTO_FromEntity_NullEvent_ReturnsNull() {
        assertNull(EventDTO.fromEntity(null));
    }

    @Test
    void eventDTO_FromEntity_MapsAllFields() {
        MapArea[][] map = {{MapArea.SEAT, MapArea.STAND}};
        Date date = new Date();
        Event event = new Event("e1", "Corp", "q1", "Concert", "Venue", "Artist",
                date, 50.0, 100, EventType.LIVE_PERFORMANCE, map);
        event.setHighDemand(true);
        event.setLotteryMaxWinners(5);
        event.setLotteryEndDate(date);

        EventDTO dto = EventDTO.fromEntity(event);

        assertNotNull(dto);
        assertEquals("e1", dto.eventId());
        assertEquals("Corp", dto.companyName());
        assertEquals("q1", dto.queueId());
        assertEquals("Concert", dto.name());
        assertEquals("Venue", dto.location());
        assertEquals("Artist", dto.artistName());
        assertEquals(date, dto.date());
        assertEquals(50.0, dto.price());
        assertEquals(100, dto.totalTickets());
        assertEquals(EventType.LIVE_PERFORMANCE, dto.type());
        assertTrue(dto.highDemand());
        assertEquals(5, dto.lotteryMaxWinners());
        assertEquals(date, dto.lotteryEndDate());
        assertNotNull(dto.map());
    }

    @Test
    void eventDTO_FromEntity_NullMap_ReturnsNullMap() {
        Event event = new Event("e1", "Corp", "q1", "Concert", "Venue", "Artist",
                new Date(), 50.0, 100, EventType.LIVE_PERFORMANCE, null);
        EventDTO dto = EventDTO.fromEntity(event);
        assertNull(dto.map());
    }

    @Test
    void eventDTO_FromEntity_MapWithNullRow_HandledGracefully() {
        MapArea[][] map = new MapArea[2][];
        map[0] = new MapArea[]{MapArea.SEAT};
        map[1] = null;
        Event event = new Event("e1", "Corp", "q1", "Concert", "Venue", "Artist",
                new Date(), 50.0, 100, EventType.LIVE_PERFORMANCE, map);

        EventDTO dto = EventDTO.fromEntity(event);

        assertNotNull(dto.map());
        assertNotNull(dto.map()[0]);
        assertNull(dto.map()[1]);
    }

    @Test
    void eventDTO_FromEntity_MapIsCopiedNotSameReference() {
        MapArea[][] map = {{MapArea.SEAT}};
        Event event = new Event("e1", "Corp", "q1", "Concert", "Venue", "Artist",
                new Date(), 50.0, 100, EventType.LIVE_PERFORMANCE, map);

        EventDTO dto = EventDTO.fromEntity(event);

        assertNotSame(map, dto.map());
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Event makeEvent() {
        return new Event("e1", "Corp", "q1", "Concert", "Venue", "Artist",
                new Date(), 50.0, 100, EventType.LIVE_PERFORMANCE,
                new MapArea[][]{{MapArea.SEAT}});
    }
}
