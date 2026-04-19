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
            try { result = testLogic.get(); } 
            catch (Exception e) { result = false; }
            stringBuilder.append("the result of test ").append(id).append(": ").append(result).append("\n");
            if (result) passTests.add(id); else failTests.add(id);
        });
        return stringBuilder.toString();
    }

    public boolean viewAllEventsSuccess() {
        userService.register("adminUser", "password");
        String token = userService.login("adminUser", "password");
        eventService.createEvent(token, "CompanyA", "Concert", EventType.LIVE_PERFORMANCE, "TLV", "Famous Band", new java.util.Date(), 100.0, 1000);
        
        Response<String> result = eventService.getAllEvents();
        return result.isSuccess() && result.getData().contains("Concert");
    }

    public boolean viewAllEventsEmptySystem() {
        Response<String> result = eventService.getAllEvents();
        return !result.isSuccess();
    }

    public boolean searchEventByDateSuccess() {
        userService.register("adminUser", "password");
        String token = userService.login("adminUser", "password");
        eventService.createEvent(token, "CompanyA", "Party", EventType.LIVE_PERFORMANCE, "Haifa", "Famous Band", new java.util.Date(), 200.0, 1500);
        
        Response<String> result = eventService.searchEventsByDate("2026-12-31");
        return result.isSuccess() && result.getData().contains("Party");
    }

    public boolean searchEventByDateNotFound() {
        Response<String> result = eventService.searchEventsByDate("2030-01-01");
        return !result.isSuccess();
    }

    public boolean searchEventByCategorySuccess() {
        userService.register("adminUser", "password");
        String token = userService.login("adminUser", "password");
        eventService.createEvent(token, "CompanyA", "Standup", EventType.FESTIVAL, "Eilat", "Famous Comedian", new java.util.Date(), 150.0, 1000);
        
        Response<String> result = eventService.searchEventsByCategory("Comedy");
        return result.isSuccess() && result.getData().contains("Standup");
    }

    public boolean searchEventByCategoryNotFound() {
        Response<String> result = eventService.searchEventsByCategory("Sports");
        return !result.isSuccess();
    }

    public boolean checkAvailableTicketsSuccess() {
        userService.register("adminUser", "password");
        String token = userService.login("adminUser", "password");
        String eventId = eventService.createEvent(token, "CompanyA", "Movie", EventType.PLAY, "CinemaCity", "Famous Actor", new java.util.Date(), 120.0, 800).getData();
        
        Response<Integer> result = eventService.getAvailableTickets(eventId);
        return result.isSuccess() && result.getData() > 0;
    }

    public boolean checkAvailableTicketsFailedEventNotFound() {
        Response<Integer> result = eventService.getAvailableTickets("fake-id-999");
        return !result.isSuccess();
    }

    public boolean searchEventByNameSuccess() {
        userService.register("adminUser", "password");
        String token = userService.login("adminUser", "password");
        eventService.createEvent(token, "CompanyA", "Coldplay", EventType.LIVE_PERFORMANCE, "Park Hayarkon", "Famous Band", new java.util.Date(), 180.0, 1200);
        
        Response<String> result = eventService.searchEventsByName("Coldplay");
        return result.isSuccess() && result.getData().contains("Coldplay");
    }

    public boolean searchEventByNameNotFound() {
        Response<String> result = eventService.searchEventsByName("UnknownBand");
        return !result.isSuccess();
    }

    public boolean searchEventByLocationSuccess() {
        userService.register("adminUser", "password");
        String token = userService.login("adminUser", "password");
        eventService.createEvent(token, "CompanyA", "Tech Meetup", EventType.CONFERENCE, "Tel Aviv", "Famous Speaker", new java.util.Date(), 250.0, 2000);
        
        Response<String> result = eventService.searchEventsByLocation("Tel Aviv");
        return result.isSuccess() && result.getData().contains("Tech Meetup");
    }

    public boolean searchEventByLocationNotFound() {
        Response<String> result = eventService.searchEventsByLocation("NowhereCity");
        return !result.isSuccess();
    }

    public boolean checkAvailableTicketsSoldOut() {
        Response<Integer> result = eventService.getAvailableTickets("sold-out-id");
        return result.isSuccess() && result.getData() == 0;
    }

    public boolean viewAllEventsAsGuestSuccess() {
        userService.register("adminUser", "password");
        String adminToken = userService.login("adminUser", "password");
        eventService.createEvent(adminToken, "CompanyA", "Public Event", EventType.LIVE_PERFORMANCE, "Square", "Famous Band", new java.util.Date(), 100.0, 1000);
        
        Response<String> result = eventService.getAllEvents();
        return result.isSuccess() && result.getData().contains("Public Event");
    }
}