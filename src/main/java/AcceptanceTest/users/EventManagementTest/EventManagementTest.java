package AcceptanceTest.users.EventManagementTest;

import AcceptanceTest.users.initTheSystem;
import Appliction.UserService;
import Appliction.EventService;
import Appliction.Response;
import Domain.Event.EventType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import Domain.Event.MapArea;

public class EventManagementTest {
    
    private UserService userService;
    private EventService eventService; 
    private initTheSystem initTheSystem;
    
    private final List<String> failTests = new ArrayList<>();
    private final List<String> passTests = new ArrayList<>();
    private final Map<String, Supplier<Boolean>> testMap = new LinkedHashMap<>();
    private MapArea[][] map = new MapArea[10][10];



    public EventManagementTest(UserService userService, EventService eventService, initTheSystem initTheSystem) {
        this.userService = userService;
        this.eventService = eventService;
        this.initTheSystem = initTheSystem;
        initTestMap();
    }

    private void initTestMap() {
        testMap.put("1", this::fullEventLifecycleTest);
        testMap.put("2", this::createEventFailedNoToken);
        testMap.put("3", this::createEventFailedMissingFields);
        testMap.put("4", this::updateEventSuccess);
        testMap.put("5", this::updateNonExistentEventTest);
        testMap.put("6", this::updateEventFailedNoPermission);
        testMap.put("7", this::deleteEventSuccess);
        testMap.put("8", this::deleteEventWithoutPermissionsTest);
        testMap.put("9", this::deleteNonExistentEventTest);
        testMap.put("10", this::createEventFailedPastDate);
        testMap.put("11", this::createEventFailedInvalidDateFormat);
        testMap.put("12", this::updateEventNameSuccess);
        testMap.put("13", this::updateEventNameFailedNoPermission);
        testMap.put("14", this::updateEventLocationSuccess);
        testMap.put("15", this::deleteEventTwiceFailed);
        testMap.put("16", this::getEventInfoSuccess);
        testMap.put("17", this::getEventInfoFailedNotExist);
    }

    public String runAllTests() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Event Management Acceptance Tests:\n");

        testMap.forEach((id, testLogic) -> {
            initTheSystem.init(); 
            boolean result;
            try { result = testLogic.get(); } 
            catch (Exception e) { result = false; }
            stringBuilder.append("the result of test ").append(id).append(": ").append(result).append("\n");
            if (result) passTests.add(id); else failTests.add(id);
        });
        return stringBuilder.toString();
    }

    public boolean fullEventLifecycleTest() {
        userService.register("adminUser", "password123");
        String token = userService.login("adminUser", "password123");

        Response<String> createRes = eventService.createEvent(token, "CompanyA", "Shlomo Artzi Concert", EventType.LIVE_PERFORMANCE, "Caesarea", "Famous Band", new java.util.Date(), 200.0, 1500, createSampleMap());
        if (!createRes.isSuccess() || createRes.getData() == null) return false;

        String eventId = createRes.getData();
        Response<Void> updateRes = eventService.updateEventDate(eventId, "2026-08-16", token);
        if (!updateRes.isSuccess()) return false;

        Response<Void> deleteRes = eventService.deleteEvent(eventId, token);
        if (!deleteRes.isSuccess()) return false;

        Response<String> getRes = eventService.getEventInfo(eventId);
        return !getRes.isSuccess();
    }

    public boolean createEventFailedNoToken() {
        Response<String> result = eventService.createEvent("", "CompanyA", "Event Without Token", EventType.LIVE_PERFORMANCE, "Location", "Artist", new java.util.Date(), 100.0, 100, createSampleMap());
        return !result.isSuccess();
    }

    public boolean createEventFailedMissingFields() {
        userService.register("adminUser", "password123");
        String token = userService.login("adminUser", "password123");
        Response<String> result = eventService.createEvent(token, "CompanyA", "", EventType.LIVE_PERFORMANCE, "Tel Aviv", "Famous Band", new java.util.Date(), 0.0, 0, createSampleMap());
        return !result.isSuccess();
    }

    public boolean updateEventSuccess() {
        userService.register("adminUser", "password123");
        String token = userService.login("adminUser", "password123");
        String eventId = eventService.createEvent(token, "CompanyA", "Jazz Festival", EventType.LIVE_PERFORMANCE, "Jerusalem", "Famous Band", new java.util.Date(), 150.0, 1000, createSampleMap()).getData();
        
        Response<Void> result = eventService.updateEventDate(eventId, "2026-05-06", token);
        return result.isSuccess();
    }

    public boolean updateNonExistentEventTest() {
        userService.register("user1", "pass");
        String token = userService.login("user1", "pass");
        Response<Void> result = eventService.updateEventDate("fake-id-999", "2026-10-10", token);
        return !result.isSuccess();
    }

    public boolean updateEventFailedNoPermission() {
        userService.register("admin", "adminPass");
        String adminToken = userService.login("admin", "adminPass");
        String eventId = eventService.createEvent(adminToken, "CompanyA", "Private Party", EventType.LIVE_PERFORMANCE, "Tel Aviv", "Famous Band", new java.util.Date(), 100.0, 1000, createSampleMap()).getData();

        userService.register("hacker", "hackerPass");
        String hackerToken = userService.login("hacker", "hackerPass");
        
        Response<Void> result = eventService.updateEventDate(eventId, "2026-01-02", hackerToken);
        return !result.isSuccess();
    }

    public boolean deleteEventSuccess() {
        userService.register("adminUser", "password123");
        String token = userService.login("adminUser", "password123");
        String eventId = eventService.createEvent(token, "CompanyA", "Tech Conference", EventType.CONFERENCE, "Expo TLV", "Famous Speaker", new java.util.Date(), 250.0, 2000, createSampleMap()).getData();
        
        Response<Void> result = eventService.deleteEvent(eventId, token);
        return result.isSuccess();
    }

    public boolean deleteEventWithoutPermissionsTest() {
        userService.register("admin", "adminPass");
        String adminToken = userService.login("admin", "adminPass");
        String eventId = eventService.createEvent(adminToken, "CompanyA", "Private Party", EventType.LIVE_PERFORMANCE, "Tel Aviv", "Famous Band", new java.util.Date(), 100.0, 1000, createSampleMap()).getData();

        userService.register("hacker", "hackerPass");
        String hackerToken = userService.login("hacker", "hackerPass");
        
        Response<Void> result = eventService.deleteEvent(eventId, hackerToken);
        return !result.isSuccess();
    }

    public boolean deleteNonExistentEventTest() {
        userService.register("user1", "pass");
        String token = userService.login("user1", "pass");
        Response<Void> result = eventService.deleteEvent("fake-id-999", token);
        return !result.isSuccess();
    }

    public boolean createEventFailedPastDate() {
        userService.register("adminUser", "password123");
        String token = userService.login("adminUser", "password123");
        Response<String> result = eventService.createEvent(token, "CompanyA", "Old Event", EventType.LIVE_PERFORMANCE, "Haifa", "Famous Band", new java.util.Date(), 100.0, 1000, createSampleMap());
        return !result.isSuccess();
    }

    public boolean createEventFailedInvalidDateFormat() {
        userService.register("adminUser", "password123");
        String token = userService.login("adminUser", "password123");
        Response<String> result = eventService.createEvent(token, "CompanyA", "Bad Date Event", EventType.LIVE_PERFORMANCE, "Eilat", "Famous Band", new java.util.Date(), 100.0, 1000, createSampleMap());
        return !result.isSuccess();
    }

    public boolean updateEventNameSuccess() {
        userService.register("adminUser", "password123");
        String token = userService.login("adminUser", "password123");
        String eventId = eventService.createEvent(token, "CompanyA", "Initial Name", EventType.LIVE_PERFORMANCE, "Caesarea", "Famous Band", new java.util.Date(), 100.0, 1000, createSampleMap()).getData();
        Response<Void> result = eventService.updateEventName(eventId, "Updated Name", token);
        return result.isSuccess();
    }

    public boolean updateEventNameFailedNoPermission() {
        userService.register("admin", "adminPass");
        String adminToken = userService.login("admin", "adminPass");
        String eventId = eventService.createEvent(adminToken, "CompanyA", "My Event", EventType.LIVE_PERFORMANCE, "Tel Aviv", "Famous Band", new java.util.Date(), 100.0, 1000, createSampleMap()).getData();

        userService.register("user2", "pass2");
        String userToken = userService.login("user2", "pass2");
        Response<Void> result = eventService.updateEventName(eventId, "Hacked Name", userToken);
        return !result.isSuccess();
    }

    public boolean updateEventLocationSuccess() {
        userService.register("adminUser", "password123");
        String token = userService.login("adminUser", "password123");
        String eventId = eventService.createEvent(token, "CompanyA", "Moving Event", EventType.LIVE_PERFORMANCE, "Haifa", "Famous Band", new java.util.Date(), 100.0, 1000, createSampleMap()).getData();
        Response<Void> result = eventService.updateEventLocation(eventId, "Tel Aviv", token);
        return result.isSuccess();
    }

    public boolean deleteEventTwiceFailed() {
        userService.register("adminUser", "password123");
        String token = userService.login("adminUser", "password123");
        String eventId = eventService.createEvent(token, "CompanyA", "Double Delete", EventType.LIVE_PERFORMANCE, "Expo TLV", "Famous Band", new java.util.Date(), 100.0, 1000, createSampleMap()).getData();
        eventService.deleteEvent(eventId, token);
        Response<Void> result = eventService.deleteEvent(eventId, token);
        return !result.isSuccess();
    }

    public boolean getEventInfoSuccess() {
        userService.register("adminUser", "password123");
        String token = userService.login("adminUser", "password123");
        String eventId = eventService.createEvent(token, "CompanyA", "Info Event", EventType.LIVE_PERFORMANCE, "Eilat", "Famous Band", new java.util.Date(), 100.0, 1000, createSampleMap()).getData();
        Response<String> result = eventService.getEventInfo(eventId);
        return result.isSuccess() && result.getData() != null;
    }

    public boolean getEventInfoFailedNotExist() {
        Response<String> result = eventService.getEventInfo("fake-id-999");
        return !result.isSuccess();
    }

    public MapArea[][] createSampleMap() {
        MapArea[][] sampleMap = new MapArea[10][10];
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                sampleMap[i][j] = MapArea.SEAT;
            }
        }
        return sampleMap;
    }
}