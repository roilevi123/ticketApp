package AcceptanceTest.users.Order;

import AcceptanceTest.users.initTheSystem;
import Appliction.CompanyService;
import Appliction.EventService;
import Appliction.OrderService;
import Appliction.UserService;
import Domain.Domains.CompanyDomain;
import Domain.Domains.EventDomain;
import Domain.Domains.OrderDomain;
import Domain.Domains.UserDomain;
import Domain.Event.EventType;
import Domain.Event.MapArea;


import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.function.Supplier;

public class ReseveTicketTests {
    UserService userService;
    CompanyService companyDomain;
    EventService eventDomain;
    OrderService reserveTicketService;
    private final List<String> failTests = new ArrayList<>();
    private final List<String> passTests = new ArrayList<>();
    private final Map<String, Supplier<Boolean>> testMap = new LinkedHashMap<>();
    private initTheSystem initTheSystem;
    public ReseveTicketTests(UserService userService, CompanyService companyDomain, EventService eventDomain, OrderService reserveTicketService, initTheSystem initTheSystem) {
        this.userService = userService;
        this.companyDomain = companyDomain;
        this.eventDomain = eventDomain;
        this.reserveTicketService = reserveTicketService;
        this.initTheSystem = initTheSystem;
        initTestMap();
    }
    private void initTestMap() {
        testMap.put("1", this::ReseveTicketTestPass1);
        testMap.put("2", this::ReseveTicketNotEnoughTickets2);
        testMap.put("3", this::ReseveTicketOutOfBounds3);
        testMap.put("4", this::ReseveTicketMultipleSpots4);
        testMap.put("5", this::ReseveTicketAlreadyReserved5);
        testMap.put("6", this::ReseveTicketConcurrentTwoThreads6);
        testMap.put("7", this::ReseveTicketStandSequential7);
//        testMap.put("8", this::ReseveTicketConcurrentStand8);
        testMap.put("9", this::ReseveTicketExpiredTicketAvailableAgain9);
//
        testMap.put("10", this::GetActiveOrderTicketsSingle10);
        testMap.put("11", this::GetActiveOrderTicketsSequential11);
        testMap.put("12", this::GetActiveOrderTicketsConcurrent12);
        testMap.put("13", this::GetActiveOrderTicketsExpired13);
        testMap.put("14", this::GetActiveOrderTicketsAsGuest14);
        testMap.put("15", this::GetActiveOrderTicketsAAfterLogOut15);
        testMap.put("16", this::GetActiveOrderTicketsAsGuestThatGetOUt16);




    }
    public String whichTestPass() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Reserve Ticket test:\n");

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
    public String SeeFailTest() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Reserve Ticket test that fail:\n");
        if (failTests.isEmpty()) {
            stringBuilder.append("None!");
        } else {
            stringBuilder.append(String.join(" , ", failTests));
        }
        return stringBuilder.toString();
    }
    public MapArea[][] getMapArea() {
        MapArea[][] MAP=new MapArea[2][2];
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                MAP[i][j] = MapArea.SEAT;
            }
        }
        return MAP;
    }
    public MapArea[][] getMapArea1() {
        MapArea[][] MAP=new MapArea[2][2];
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                MAP[i][j] = MapArea.SEAT;
            }
        }
        MAP[1][1] = MapArea.STAND;
        return MAP;
    }
    public boolean ReseveTicketTestPass1() {
        userService.register("1","1");
        String token=userService.login("1","1");
        companyDomain.CreateCompany("1",token);
        eventDomain.createEvent(token,"1","1", EventType.PLAY,100,new Date(),"1","1",getMapArea());
        userService.register("2","2");
        String token1=userService.login("2","2");
        List<int[]> requests = new ArrayList<>();
        requests.add(new int[]{0, 0});
        String orderId = reserveTicketService.reserveTickets(token1, "1", "1", requests);
        return orderId != null;
    }
    public boolean ReseveTicketNotEnoughTickets2() {
        userService.register("1", "1");
        String token = userService.login("1", "1");
        userService.register("2", "2");
        String token1 = userService.login("2", "2");
        companyDomain.CreateCompany("1", token);
        eventDomain.createEvent(token, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea());

        List<int[]> requests = new ArrayList<>();
        requests.add(new int[]{0, 0});

        String orderId = reserveTicketService.reserveTickets(token, "1", "1", requests);
        String orderId1 = reserveTicketService.reserveTickets(token1, "1", "1", requests);

        return orderId1 == null;
    }

    public boolean ReseveTicketOutOfBounds3() {
        userService.register("1", "1");
        String token = userService.login("1", "1");
        companyDomain.CreateCompany("1", token);
        eventDomain.createEvent(token, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea());

        List<int[]> requests = new ArrayList<>();
        requests.add(new int[]{5, 5});

        String orderId = reserveTicketService.reserveTickets(token, "1", "1", requests);

        return orderId == null;
    }

    public boolean ReseveTicketMultipleSpots4() {
        userService.register("1", "1");
        String token = userService.login("1", "1");
        companyDomain.CreateCompany("1", token);
        eventDomain.createEvent(token, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea());

        List<int[]> requests = new ArrayList<>();
        requests.add(new int[]{0, 0});
        requests.add(new int[]{1, 1});

        String orderId = reserveTicketService.reserveTickets(token, "1", "1", requests);

        return orderId != null;
    }

    public boolean ReseveTicketAlreadyReserved5() {
        userService.register("1", "1");
        String token1 = userService.login("1", "1");
        companyDomain.CreateCompany("1", token1);
        eventDomain.createEvent(token1, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea());

        userService.register("2", "2");
        String token2 = userService.login("2", "2");

        List<int[]> requests = new ArrayList<>();
        requests.add(new int[]{0, 1, 1});

        String orderId1 = reserveTicketService.reserveTickets(token1, "1", "1", requests);

        String orderId2 = reserveTicketService.reserveTickets(token2, "1", "1", requests);

        return orderId1 != null && orderId2 == null;
    }
    public boolean ReseveTicketConcurrentTwoThreads6() {
        userService.register("1", "1");
        String token = userService.login("1", "1");
        companyDomain.CreateCompany("1", token);
        eventDomain.createEvent(token, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea());

        int threadCount = 2;
        CountDownLatch latch = new CountDownLatch(1);
        java.util.concurrent.ExecutorService service = java.util.concurrent.Executors.newFixedThreadPool(threadCount);
        List<String> results = Collections.synchronizedList(new ArrayList<>());

        List<int[]> requests = new ArrayList<>();
        requests.add(new int[]{0, 0, 1});

        for (int i = 0; i < threadCount; i++) {
            final int userNum = i + 2;
            service.submit(() -> {
                try {
                    userService.register("user" + userNum, "pass");
                    String userToken = userService.login("user" + userNum, "pass");

                    latch.await();

                    String orderId = reserveTicketService.reserveTickets(userToken, "1", "1", requests);
                    if (orderId != null) {
                        results.add(orderId);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        latch.countDown();
        service.shutdown();
        try {
            if (!service.awaitTermination(15, java.util.concurrent.TimeUnit.SECONDS)) {
                return false;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }

        return results.size() == 1;
    }
    public boolean ReseveTicketStandSequential7() {
        userService.register("1", "1");
        String token = userService.login("1", "1");
        companyDomain.CreateCompany("1", token);
        eventDomain.createEvent(token, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea1());

        userService.register("user1", "pass1");
        String token1 = userService.login("user1", "pass1");
        List<int[]> requests1 = new ArrayList<>();
        requests1.add(new int[]{1, 1, 1});
        String orderId1 = reserveTicketService.reserveTickets(token1, "1", "1", requests1);

        userService.register("user2", "pass2");
        String token2 = userService.login("user2", "pass2");
        List<int[]> requests2 = new ArrayList<>();
        requests2.add(new int[]{0, 0, 1});
        String orderId2 = reserveTicketService.reserveTickets(token2, "1", "1", requests2);

        return orderId1 != null && orderId2 != null;
    }
    public boolean ReseveTicketConcurrentStand8() {
        userService.register("1", "1");
        String token = userService.login("1", "1");
        companyDomain.CreateCompany("1", token);
        eventDomain.createEvent(token, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea1());

        int threadCount = 2;
        CountDownLatch latch = new CountDownLatch(1);
        java.util.concurrent.ExecutorService service = java.util.concurrent.Executors.newFixedThreadPool(threadCount);
        List<String> results = Collections.synchronizedList(new ArrayList<>());

        List<int[]> requests = new ArrayList<>();
        requests.add(new int[]{1, 1, 1});

        for (int i = 0; i < threadCount; i++) {
            final int userNum = i + 10;
            service.submit(() -> {
                try {
                    userService.register("user" + userNum, "pass");
                    String userToken = userService.login("user" + userNum, "pass");

                    latch.await();

                    String orderId = reserveTicketService.reserveTickets(userToken, "1", "1", requests);
                    if (orderId != null) {
                        results.add(orderId);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        latch.countDown();
        service.shutdown();
        try {
            if (!service.awaitTermination(15, java.util.concurrent.TimeUnit.SECONDS)) {
                return false;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }

        return results.size() == 2;
    }
    public boolean ReseveTicketExpiredTicketAvailableAgain9() {
        userService.register("1", "1");
        String token = userService.login("1", "1");
        companyDomain.CreateCompany("1", token);
        eventDomain.createEvent(token, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea());

        userService.register("user1", "pass1");
        String token1 = userService.login("user1", "pass1");
        List<int[]> requests = new ArrayList<>();
        requests.add(new int[]{0, 0, 1});

        String orderId1 = reserveTicketService.reserveTickets(token1, "1", "1", requests);

        userService.register("user2", "pass2");
        String token2 = userService.login("user2", "pass2");
        String orderId2_FirstAttempt = reserveTicketService.reserveTickets(token2, "1", "1", requests);

        try {
            Thread.sleep(10005);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }

        String orderId2_SecondAttempt = reserveTicketService.reserveTickets(token2, "1", "1", requests);

        return orderId1 != null && orderId2_FirstAttempt == null && orderId2_SecondAttempt != null;
    }
    public boolean GetActiveOrderTicketsSingle10() {
        userService.register("1", "1");
        String token = userService.login("1", "1");
        companyDomain.CreateCompany("1", token);
        eventDomain.createEvent(token, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea());

        userService.register("user1", "pass1");
        String token1 = userService.login("user1", "pass1");
        List<int[]> requests = new ArrayList<>();
        requests.add(new int[]{0, 0, 1});

        String orderId = reserveTicketService.reserveTickets(token1, "1", "1", requests);
        String tickets = reserveTicketService.getActiveOrderTickets(token1,orderId);

        return orderId != null && tickets != null &&
                tickets.contains("company='1'") &&
                tickets.contains("event='1'") &&
                tickets.contains("verticalSpote=0") &&
                tickets.contains("horizontalSpote=0");
    }
    public boolean GetActiveOrderTicketsSequential11() {
        userService.register("1", "1");
        String token = userService.login("1", "1");
        companyDomain.CreateCompany("1", token);
        eventDomain.createEvent(token, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea1());

        userService.register("user1", "pass1");
        String token1 = userService.login("user1", "pass1");
        List<int[]> requests1 = new ArrayList<>();
        requests1.add(new int[]{1, 1, 1});
        String orderId1 = reserveTicketService.reserveTickets(token1, "1", "1", requests1);
        String tickets1 = reserveTicketService.getActiveOrderTickets(token1,orderId1);

        userService.register("user2", "pass2");
        String token2 = userService.login("user2", "pass2");
        List<int[]> requests2 = new ArrayList<>();
        requests2.add(new int[]{0, 0, 1});
        String orderId2 = reserveTicketService.reserveTickets(token2, "1", "1", requests2);
        String tickets2 = reserveTicketService.getActiveOrderTickets(token2,orderId2);

        return orderId1 != null && orderId2 != null &&
                tickets1 != null &&
                tickets1.contains("company='1'") &&
                tickets1.contains("event='1'") &&
                tickets1.contains("verticalSpote=1") &&
                tickets1.contains("horizontalSpote=1") &&
                tickets2 != null &&
                tickets2.contains("company='1'") &&
                tickets2.contains("event='1'") &&
                tickets2.contains("verticalSpote=0") &&
                tickets2.contains("horizontalSpote=0");
    }
    public boolean GetActiveOrderTicketsConcurrent12() {
        userService.register("1", "1");
        String token = userService.login("1", "1");
        companyDomain.CreateCompany("1", token);
        eventDomain.createEvent(token, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea());

        int threadCount = 2;
        CountDownLatch latch = new CountDownLatch(1);
        java.util.concurrent.ExecutorService service = java.util.concurrent.Executors.newFixedThreadPool(threadCount);
        List<String> results = Collections.synchronizedList(new ArrayList<>());
        List<String> tokens = Collections.synchronizedList(new ArrayList<>());

        List<int[]> requests = new ArrayList<>();
        requests.add(new int[]{0, 0, 1});

        for (int i = 0; i < threadCount; i++) {
            final int userNum = i + 2;
            service.submit(() -> {
                try {
                    userService.register("user" + userNum, "pass");
                    String userToken = userService.login("user" + userNum, "pass");
                    tokens.add(userToken);

                    latch.await();

                    String orderId = reserveTicketService.reserveTickets(userToken, "1", "1", requests);
                    if (orderId != null) {
                        results.add(orderId);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        latch.countDown();
        service.shutdown();
        try {
            if (!service.awaitTermination(15, java.util.concurrent.TimeUnit.SECONDS)) {
                return false;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }

        int activeTicketsCount = 0;
        boolean contentValid = false;
        for (String t : tokens) {
            String tickets = reserveTicketService.getActiveOrderTickets(t,"orderId");
            if (tickets != null && !tickets.trim().isEmpty()) {
                activeTicketsCount++;
                if (tickets.contains("company='1'") &&
                        tickets.contains("event='1'") &&
                        tickets.contains("verticalSpote=0") &&
                        tickets.contains("horizontalSpote=0")) {
                    contentValid = true;
                }
            }
        }

        return results.size() == 1 && activeTicketsCount == 1 && contentValid;
    }

    public boolean GetActiveOrderTicketsExpired13() {
        userService.register("1", "1");
        String token = userService.login("1", "1");
        companyDomain.CreateCompany("1", token);
        eventDomain.createEvent(token, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea());

        userService.register("user1", "pass1");
        String token1 = userService.login("user1", "pass1");
        List<int[]> requests = new ArrayList<>();
        requests.add(new int[]{0, 0, 1});

        String orderId1 = reserveTicketService.reserveTickets(token1, "1", "1", requests);

        try {
            Thread.sleep(10005);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }

        userService.register("user2", "pass2");
        String token2 = userService.login("user2", "pass2");
        String orderId2 = reserveTicketService.reserveTickets(token2, "1", "1", requests);

        String tickets2 = reserveTicketService.getActiveOrderTickets(token2,orderId2);

        return orderId1 != null && orderId2 != null &&
                tickets2 != null &&
                tickets2.contains("company='1'") &&
                tickets2.contains("event='1'") &&
                tickets2.contains("verticalSpote=0") &&
                tickets2.contains("horizontalSpote=0");
    }
    public boolean GetActiveOrderTicketsAsGuest14() {
        userService.register("1", "1");
        String token = userService.login("1", "1");
        companyDomain.CreateCompany("1", token);
        eventDomain.createEvent(token, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea());


        List<int[]> requests = new ArrayList<>();
        requests.add(new int[]{0, 0, 1});

        String orderId = reserveTicketService.reserveTickets("token1", "1", "1", requests);
        String tickets = reserveTicketService.getActiveOrderTickets("token1",orderId);

        return orderId != null && tickets != null &&
                tickets.contains("company='1'") &&
                tickets.contains("event='1'") &&
                tickets.contains("verticalSpote=0") &&
                tickets.contains("horizontalSpote=0");
    }

    public boolean GetActiveOrderTicketsAAfterLogOut15() {
        userService.register("1", "1");
        String token = userService.login("1", "1");
        companyDomain.CreateCompany("1", token);
        eventDomain.createEvent(token, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea());

        userService.register("user1", "pass1");
        String token1 = userService.login("user1", "pass1");
        List<int[]> requests = new ArrayList<>();
        requests.add(new int[]{0, 0, 1});

        String orderId = reserveTicketService.reserveTickets(token1, "1", "1", requests);
        userService.logout(token1);
        String token2 =userService.login("user1", "pass1");
        String tickets = reserveTicketService.getActiveOrderTickets(token2,null);

        return orderId != null && tickets != null &&
                tickets.contains("company='1'") &&
                tickets.contains("event='1'") &&
                tickets.contains("verticalSpote=0") &&
                tickets.contains("horizontalSpote=0");
    }
    public boolean GetActiveOrderTicketsAsGuestThatGetOUt16() {
        userService.register("1", "1");
        String token = userService.login("1", "1");
        companyDomain.CreateCompany("1", token);
        eventDomain.createEvent(token, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea());


        List<int[]> requests = new ArrayList<>();
        requests.add(new int[]{0, 0, 1});

        String orderId = reserveTicketService.reserveTickets("token1", "1", "1", requests);
        String tickets = reserveTicketService.getActiveOrderTickets("token1","orderId");

        return tickets==null;
    }
}
