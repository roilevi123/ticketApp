package AcceptanceTest.users.EventManagementTest;

import AcceptanceTest.users.initTheSystem;
import Appliction.UserService;
import Appliction.EventService;

import java.util.ArrayList;
import java.util.List;

public class EventManagementTest {
    
    private UserService userService;
    private EventService eventService; 
    private initTheSystem initTheSystem;
    
    private final List<String> failTests = new ArrayList<>();
    private final List<String> passTests = new ArrayList<>();

    public EventManagementTest(UserService userService, EventService eventService, initTheSystem initTheSystem) {
        this.userService = userService;
        this.eventService = eventService;
        this.initTheSystem = initTheSystem;
    }

    public String runAllTests() {
        StringBuilder results = new StringBuilder();
        results.append("Event Management Acceptance Tests:\n");

        runTest("1", "Full Cycle: Create, Update, and Delete Event", this::fullEventLifecycleTest, results);
        runTest("2", "Fail to Update Event That Does Not Exist", this::updateNonExistentEventTest, results);
        runTest("3", "Fail to Delete Event Without Permissions", this::deleteEventWithoutPermissionsTest, results);

        return results.toString();
    }

    private void runTest(String testId, String description, java.util.function.Supplier<Boolean> testLogic, StringBuilder results) {
        initTheSystem.init(); 
        boolean result;
        try {
            result = testLogic.get();
        } catch (Exception e) {
            result = false;
        }

        results.append("Test ").append(testId).append(" - ").append(description).append(": ").append(result ? "PASS" : "FAIL").append("\n");
        if (result) passTests.add(testId); else failTests.add(testId);
    }

    public boolean fullEventLifecycleTest() {
        userService.register("adminUser", "password123");
        String token = userService.login("adminUser", "password123");

        String eventId = eventService.createEvent("Shlomo Artzi Concert", "2026-08-15", "Caesarea", token);
        if (eventId == null || eventId.isEmpty() || eventId.equals("failed")) {
            return false;
        }

        String updateResult = eventService.updateEventDate(eventId, "2026-08-16", token);
        if (!updateResult.equals("success")) {
            return false;
        }

        String deleteResult = eventService.deleteEvent(eventId, token);
        if (!deleteResult.equals("success")) {
            return false;
        }

        String getEventResult = eventService.getEventInfo(eventId);
        return getEventResult.equals("failed_not_found");
    }

    public boolean updateNonExistentEventTest() {
        userService.register("user1", "pass");
        String token = userService.login("user1", "pass");
        
        String result = eventService.updateEventDate("fake-id-999", "2026-10-10", token);
        return result.equals("failed");
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
}