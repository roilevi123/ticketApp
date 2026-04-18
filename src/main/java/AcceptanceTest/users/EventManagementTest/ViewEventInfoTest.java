package AcceptanceTest.users.EventManagementTest;
import AcceptanceTest.users.initTheSystem;
import Appliction.UserService;
import Appliction.EventService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class ViewEventInfoTest {

    private UserService userService;
    private EventService eventService;
    private initTheSystem initTheSystem;

    private final List<String> failTests = new ArrayList<>();
    private final List<String> passTests = new ArrayList<>();
    private final Map<String, Supplier<Boolean>> testMap = new LinkedHashMap<>();

    public ViewEventInfoTest(UserService userService, EventService eventService, initTheSystem initTheSystem) {
        this.userService = userService;
        this.eventService = eventService;
        this.initTheSystem = initTheSystem;
        initTestMap();
    }

    private void initTestMap() {
        testMap.put("1", this::viewAllEventsSuccess);
        testMap.put("2", this::viewAllEventsEmptySystem);
        testMap.put("3", this::searchEventByDateSuccess);
        testMap.put("4", this::searchEventByDateNotFound);
        testMap.put("5", this::searchEventByCategorySuccess);
        testMap.put("6", this::searchEventByCategoryNotFound);
        testMap.put("7", this::checkAvailableTicketsSuccess);
        testMap.put("8", this::checkAvailableTicketsFailedEventNotFound);
        testMap.put("9", this::searchEventByNameSuccess);
        testMap.put("10", this::searchEventByNameNotFound);
        testMap.put("11", this::searchEventByLocationSuccess);
        testMap.put("12", this::searchEventByLocationNotFound);
        testMap.put("13", this::checkAvailableTicketsSoldOut);
        testMap.put("14", this::viewAllEventsAsGuestSuccess);
    }

    public String runAllTests() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("View Event Info Acceptance Tests:\n");

        testMap.forEach((id, testLogic) -> {
            initTheSystem.init();
            boolean result;
            try {
                result = testLogic.get();
            } catch (Exception e) {
                result = false;
            }

            stringBuilder.append("the result of test ").append(id).append(": ").append(result).append("\n");

            if (result) passTests.add(id);
            else failTests.add(id);
        });

        return stringBuilder.toString();
    }

    public boolean viewAllEventsSuccess() {
        userService.register("adminUser", "password");
        String token = userService.login("adminUser", "password");
        eventService.createEvent("Concert", "2026-10-10", "TLV", token);
        
        String result = eventService.getAllEvents();
        return !result.equals("failed_empty") && result.contains("Concert");
    }

    public boolean viewAllEventsEmptySystem() {
        String result = eventService.getAllEvents();
        return result.equals("failed_empty");
    }

    public boolean searchEventByDateSuccess() {
        userService.register("adminUser", "password");
        String token = userService.login("adminUser", "password");
        eventService.createEvent("Party", "2026-12-31", "Haifa", token);
        
        String result = eventService.searchEventsByDate("2026-12-31");
        return result.contains("Party");
    }

    public boolean searchEventByDateNotFound() {
        String result = eventService.searchEventsByDate("2030-01-01");
        return result.equals("failed_not_found");
    }

    public boolean searchEventByCategorySuccess() {
        userService.register("adminUser", "password");
        String token = userService.login("adminUser", "password");
        eventService.createEvent("Standup", "2026-08-08", "Eilat", token);
        
        String result = eventService.searchEventsByCategory("Comedy");
        return result.contains("Standup");
    }

    public boolean searchEventByCategoryNotFound() {
        String result = eventService.searchEventsByCategory("Sports");
        return result.equals("failed_not_found");
    }

    public boolean checkAvailableTicketsSuccess() {
        userService.register("adminUser", "password");
        String token = userService.login("adminUser", "password");
        String eventId = eventService.createEvent("Movie", "2026-07-07", "Cinema", token);
        
        int tickets = eventService.getAvailableTickets(eventId);
        return tickets > 0;
    }

    public boolean checkAvailableTicketsFailedEventNotFound() {
        int tickets = eventService.getAvailableTickets("fake-id-999");
        return tickets == -1;
    }

    public boolean searchEventByNameSuccess() {
        userService.register("adminUser", "password");
        String token = userService.login("adminUser", "password");
        eventService.createEvent("Coldplay", "2026-09-09", "Park Hayarkon", token);
        
        String result = eventService.searchEventsByName("Coldplay");
        return result.contains("Coldplay");
    }

    public boolean searchEventByNameNotFound() {
        String result = eventService.searchEventsByName("UnknownBand");
        return result.equals("failed_not_found");
    }

    public boolean searchEventByLocationSuccess() {
        userService.register("adminUser", "password");
        String token = userService.login("adminUser", "password");
        eventService.createEvent("Tech Meetup", "2026-05-05", "Tel Aviv", token);
        
        String result = eventService.searchEventsByLocation("Tel Aviv");
        return result.contains("Tech Meetup");
    }

    public boolean searchEventByLocationNotFound() {
        String result = eventService.searchEventsByLocation("NowhereCity");
        return result.equals("failed_not_found");
    }

    public boolean checkAvailableTicketsSoldOut() {
        userService.register("adminUser", "password");
        String token = userService.login("adminUser", "password");
        String eventId = eventService.createEvent("Sold Out Show", "2026-01-01", "Barby", token);
        
        int tickets = eventService.getAvailableTickets("sold-out-id");
        return tickets == 0;
    }

    public boolean viewAllEventsAsGuestSuccess() {
        userService.register("adminUser", "password");
        String adminToken = userService.login("adminUser", "password");
        eventService.createEvent("Public Event", "2026-11-11", "Square", adminToken);
        
        String result = eventService.getAllEvents();
        return result.contains("Public Event");
    }
}