package AcceptanceTest.users.OrderManagementTest;
import AcceptanceTest.users.initTheSystem;
import Appliction.EventService;
import Appliction.OrderService;
import Appliction.UserService;
import Appliction.Response;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class ReserveOrderTest {

    private UserService userService;
    private EventService eventService;
    private OrderService orderService;
    private initTheSystem initTheSystem;

    private final List<String> failTests = new ArrayList<>();
    private final List<String> passTests = new ArrayList<>();
    private final Map<String, Supplier<Boolean>> testMap = new LinkedHashMap<>();

    public ReserveOrderTest(UserService userService, EventService eventService, OrderService orderService, initTheSystem initTheSystem) {
        this.userService = userService;
        this.eventService = eventService;
        this.orderService = orderService;
        this.initTheSystem = initTheSystem;
        initTestMap();
    }

    private void initTestMap() {
        testMap.put("1", this::reserveOrderSuccess);
        testMap.put("2", this::reserveOrderFailedEventNotFound);
        testMap.put("3", this::reserveOrderFailedNotEnoughTickets);
        testMap.put("4", this::reserveOrderFailedNotLoggedIn);
        testMap.put("5", this::reserveOrderFailedNegativeAmount);
        testMap.put("6", this::reserveOrderFailedZeroAmount);
        testMap.put("7", this::reserveOrderFailedExceedUserLimit);
        testMap.put("8", this::reserveOrderFailedSoldOut);
        testMap.put("9", this::reserveOrderFailedEventDeleted);
        testMap.put("10", this::reserveMultipleOrdersSuccess);
    }

    public String runAllTests() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Reserve Order Acceptance Tests:\n");

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

    public boolean reserveOrderSuccess() {
        userService.register("user1", "pass1");
        String token = userService.login("user1", "pass1");
        
        userService.register("admin", "adminPass");
        String adminToken = userService.login("admin", "adminPass");
        String eventId = eventService.createEvent("Rock Concert", "2026-10-10", "TLV", adminToken).getData();

        Response<String> result = orderService.reserveTickets(eventId, 2, token);
        return result.isSuccess();
    }

    public boolean reserveOrderFailedEventNotFound() {
        userService.register("user1", "pass1");
        String token = userService.login("user1", "pass1");
        Response<String> result = orderService.reserveTickets("fake-id-999", 2, token);
        return !result.isSuccess();
    }

    public boolean reserveOrderFailedNotEnoughTickets() {
        userService.register("user1", "pass1");
        String token = userService.login("user1", "pass1");
        
        userService.register("admin", "adminPass");
        String adminToken = userService.login("admin", "adminPass");
        String eventId = eventService.createEvent("Small Show", "2026-11-11", "Barby", adminToken).getData();

        Response<String> result = orderService.reserveTickets(eventId, 150, token);
        return !result.isSuccess();
    }

    public boolean reserveOrderFailedNotLoggedIn() {
        userService.register("admin", "adminPass");
        String adminToken = userService.login("admin", "adminPass");
        String eventId = eventService.createEvent("Open Air", "2026-12-12", "Park", adminToken).getData();

        Response<String> result = orderService.reserveTickets(eventId, 2, "invalid-token");
        return !result.isSuccess();
    }

    public boolean reserveOrderFailedNegativeAmount() {
        userService.register("user1", "pass1");
        String token = userService.login("user1", "pass1");
        
        userService.register("admin", "adminPass");
        String adminToken = userService.login("admin", "adminPass");
        String eventId = eventService.createEvent("Jazz Night", "2026-08-08", "Jerusalem", adminToken).getData();

        Response<String> result = orderService.reserveTickets(eventId, -5, token);
        return !result.isSuccess();
    }

    public boolean reserveOrderFailedZeroAmount() {
        userService.register("user1", "pass1");
        String token = userService.login("user1", "pass1");
        
        userService.register("admin", "adminPass");
        String adminToken = userService.login("admin", "adminPass");
        String eventId = eventService.createEvent("Pop Show", "2026-09-09", "Haifa", adminToken).getData();

        Response<String> result = orderService.reserveTickets(eventId, 0, token);
        return !result.isSuccess();
    }

    public boolean reserveOrderFailedExceedUserLimit() {
        userService.register("user1", "pass1");
        String token = userService.login("user1", "pass1");
        
        userService.register("admin", "adminPass");
        String adminToken = userService.login("admin", "adminPass");
        String eventId = eventService.createEvent("Big Festival", "2026-07-07", "Eilat", adminToken).getData();

        Response<String> result = orderService.reserveTickets(eventId, 15, token);
        return !result.isSuccess();
    }

    public boolean reserveOrderFailedSoldOut() {
        userService.register("user1", "pass1");
        String token = userService.login("user1", "pass1");
        Response<String> result = orderService.reserveTickets("sold-out-id", 2, token);
        return !result.isSuccess();
    }

    public boolean reserveOrderFailedEventDeleted() {
        userService.register("user1", "pass1");
        String token = userService.login("user1", "pass1");
        Response<String> result = orderService.reserveTickets("deleted-id-123", 1, token);
        return !result.isSuccess();
    }

    public boolean reserveMultipleOrdersSuccess() {
        userService.register("user1", "pass1");
        String token = userService.login("user1", "pass1");
        
        userService.register("admin", "adminPass");
        String adminToken = userService.login("admin", "adminPass");
        String eventId = eventService.createEvent("Classic Concert", "2026-05-05", "Tzfat", adminToken).getData();

        Response<String> result1 = orderService.reserveTickets(eventId, 2, token);
        Response<String> result2 = orderService.reserveTickets(eventId, 3, token);
        
        return result1.isSuccess() && result2.isSuccess();
    }
}