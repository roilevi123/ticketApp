package AcceptanceTest.users.EventManagementTest;

import AcceptanceTest.users.initTheSystem;
import Appliction.UserService;
import Appliction.EventService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class EventManagementTest {
    
    private UserService userService;
    private EventService eventService; 
    private initTheSystem initTheSystem;
    
    private final List<String> failTests = new ArrayList<>();
    private final List<String> passTests = new ArrayList<>();
    private final Map<String, Supplier<Boolean>> testMap = new LinkedHashMap<>();

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
            try {
                result = testLogic.get();
            } catch (Exception e) {
                result = false;
            }

            stringBuilder.append("the result of test ").append(id).append(": ").append(result).append("\n");

            if (result) {
                passTests.add(id);
            } else {
                failTests.add(id);
            }
        });

        return stringBuilder.toString();
    }

    public boolean fullEventLifecycleTest() {
        userService.register("adminUser", "password123");
        String token = userService.login("adminUser", "password123");

        String eventId = eventService.createEvent("Shlomo Artzi Concert", "2026-08-15", "Caesarea", token);
        if (eventId == null || eventId.isEmpty() || eventId.equals("failed")) return false;

        String updateResult = eventService.updateEventDate(eventId, "2026-08-16", token);
        if (!updateResult.equals("success")) return false;

        String deleteResult = eventService.deleteEvent(eventId, token);
        if (!deleteResult.equals("success")) return false;

        String getEventResult = eventService.getEventInfo(eventId);
        return getEventResult.equals("failed_not_found");
    }

    public boolean createEventFailedNoToken() {
        String result = eventService.createEvent("Coldplay Live", "2026-09-01", "Park Hayarkon", "");
        return result.equals("failed");
    }

    public boolean createEventFailedMissingFields() {
        userService.register("adminUser", "password123");
        String token = userService.login("adminUser", "password123");
        String result = eventService.createEvent("", "", "Tel Aviv", token);
        return result.equals("failed");
    }

    public boolean updateEventSuccess() {
        userService.register("adminUser", "password123");
        String token = userService.login("adminUser", "password123");
        String eventId = eventService.createEvent("Jazz Festival", "2026-05-05", "Jerusalem", token);
        
        String result = eventService.updateEventDate(eventId, "2026-05-06", token);
        return result.equals("success");
    }

    public boolean updateNonExistentEventTest() {
        userService.register("user1", "pass");
        String token = userService.login("user1", "pass");
        
        String result = eventService.updateEventDate("fake-id-999", "2026-10-10", token);
        return result.equals("failed");
    }

    public boolean updateEventFailedNoPermission() {
        userService.register("admin", "adminPass");
        String adminToken = userService.login("admin", "adminPass");
        String eventId = eventService.createEvent("Private Party", "2026-01-01", "Tel Aviv", adminToken);

        userService.register("hacker", "hackerPass");
        String hackerToken = userService.login("hacker", "hackerPass");
        
        String updateResult = eventService.updateEventDate(eventId, "2026-01-02", hackerToken);
        return updateResult.equals("failed_no_permission");
    }

    public boolean deleteEventSuccess() {
        userService.register("adminUser", "password123");
        String token = userService.login("adminUser", "password123");
        String eventId = eventService.createEvent("Tech Conference", "2026-11-11", "Expo TLV", token);
        
        String result = eventService.deleteEvent(eventId, token);
        return result.equals("success");
    }

    public boolean deleteEventWithoutPermissionsTest() {
        userService.register("admin", "adminPass");
        String adminToken = userService.login("admin", "adminPass");
        String eventId = eventService.createEvent("Private Party", "2026-01-01", "Tel Aviv", adminToken);

        userService.register("hacker", "hackerPass");
        String hackerToken = userService.login("hacker", "hackerPass");
        
        String deleteResult = eventService.deleteEvent(eventId, hackerToken);
        return deleteResult.equals("failed_no_permission");
    }

    public boolean deleteNonExistentEventTest() {
        userService.register("user1", "pass");
        String token = userService.login("user1", "pass");
        
        String result = eventService.deleteEvent("fake-id-999", token);
        return result.equals("failed");
    }

    public boolean createEventFailedPastDate() {
        userService.register("adminUser", "password123");
        String token = userService.login("adminUser", "password123");
        String result = eventService.createEvent("Old Event", "2020-01-01", "Haifa", token);
        return result.equals("failed");
    }

    public boolean createEventFailedInvalidDateFormat() {
        userService.register("adminUser", "password123");
        String token = userService.login("adminUser", "password123");
        String result = eventService.createEvent("Bad Date Event", "invalid-date", "Eilat", token);
        return result.equals("failed");
    }

    public boolean updateEventNameSuccess() {
        userService.register("adminUser", "password123");
        String token = userService.login("adminUser", "password123");
        String eventId = eventService.createEvent("Initial Name", "2026-08-15", "Caesarea", token);
        String result = eventService.updateEventName(eventId, "Updated Name", token);
        return result.equals("success");
    }

    public boolean updateEventNameFailedNoPermission() {
        userService.register("admin", "adminPass");
        String adminToken = userService.login("admin", "adminPass");
        String eventId = eventService.createEvent("My Event", "2026-01-01", "Tel Aviv", adminToken);

        userService.register("user2", "pass2");
        String userToken = userService.login("user2", "pass2");
        String result = eventService.updateEventName(eventId, "Hacked Name", userToken);
        return result.equals("failed_no_permission");
    }

    public boolean updateEventLocationSuccess() {
        userService.register("adminUser", "password123");
        String token = userService.login("adminUser", "password123");
        String eventId = eventService.createEvent("Moving Event", "2026-08-15", "Haifa", token);
        String result = eventService.updateEventLocation(eventId, "Tel Aviv", token);
        return result.equals("success");
    }

    public boolean deleteEventTwiceFailed() {
        userService.register("adminUser", "password123");
        String token = userService.login("adminUser", "password123");
        String eventId = eventService.createEvent("Double Delete", "2026-11-11", "Expo TLV", token);
        eventService.deleteEvent(eventId, token);
        String result = eventService.deleteEvent(eventId, token);
        return result.equals("failed_already_deleted");
    }

    public boolean getEventInfoSuccess() {
        userService.register("adminUser", "password123");
        String token = userService.login("adminUser", "password123");
        String eventId = eventService.createEvent("Info Event", "2026-12-12", "Eilat", token);
        String result = eventService.getEventInfo(eventId);
        return !result.equals("failed_not_found") && !result.isEmpty();
    }

    public boolean getEventInfoFailedNotExist() {
        String result = eventService.getEventInfo("fake-id-999");
        return result.equals("failed_not_found");
    }
}