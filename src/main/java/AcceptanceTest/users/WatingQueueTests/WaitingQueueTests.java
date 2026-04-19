package AcceptanceTest.users.WatingQueueTests;

import AcceptanceTest.users.initTheSystem;
import Appliction.CompanyService;
import Appliction.EventService;
import Appliction.QueueService;
import Appliction.UserService;
import Domain.Event.EventType;
import Domain.Event.MapArea;


import java.util.*;
import java.util.function.Supplier;

public class WaitingQueueTests {
    UserService userService;
    CompanyService companyService;
    EventService eventService;
    QueueService queueService;
    private final List<String> failTests = new ArrayList<>();
    private final List<String> passTests = new ArrayList<>();
    private final Map<String, Supplier<Boolean>> testMap = new LinkedHashMap<>();
    private initTheSystem initTheSystem;
    private List<String> tokens;
    public WaitingQueueTests(UserService userService, CompanyService companyService, EventService eventService, QueueService queueService, initTheSystem initTheSystem) {
        this.userService = userService;
        this.companyService = companyService;
        this.eventService = eventService;
        this.queueService = queueService;
        this.initTheSystem = initTheSystem;
        initTestMap();
    }
    private void initTestMap() {
        testMap.put("1", this::TestSequentialQueueEntry1);
        testMap.put("2", this::TestQueuePositionIncrements2);
        testMap.put("3", this::TestConcurrentQueueAccess3);
        testMap.put("4", this::TestConcurrentQueueOverflow4);
        testMap.put("5", this::TestConcurrentSameUserRefresh5);


    }

    public String whichTestPass() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Waiting Queue management test:\n");

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
        stringBuilder.append("event management test that fail:\n");
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
    public void setUp(){
        userService.register("creator","creator");
        String token1 = userService.login("creator","creator");
        companyService.CreateCompany("creator",token1);
        eventService.createEvent(token1, "Rock Festival", "Arctic Monkeys", EventType.CONFERENCE, 100.0, new Date(), "Tel Aviv", "creator", getMapArea());

    }
    public boolean TestSequentialQueueEntry1() {
        setUp();
        String eventId = "Rock Festivalcreator";

        String status1 = queueService.checkStatus(eventId, "user1");

        String status2 = queueService.checkStatus(eventId, "user2");

        return status1.equals("AUTHORIZED") && status2.equals("AUTHORIZED");
    }
    public boolean TestQueuePositionIncrements2() {
        setUp();
        String eventId = "Rock Festivalcreator";

        for (int i = 1; i <= 100; i++) {
            queueService.checkStatus(eventId, "dummyUser" + i);
        }

        String status101 = queueService.checkStatus(eventId, "waiter1");
        String status102 = queueService.checkStatus(eventId, "waiter2");

        return status101.equals("WAITING_POSITION_1") && status102.equals("WAITING_POSITION_2");
    }
    public boolean TestConcurrentQueueAccess3() {
        userService.register("creator3", "creator");
        String token = userService.login("creator3", "creator");
        companyService.CreateCompany("creator3", token);
        eventService.createEvent(token, "Concurrent Fest", "Artist", EventType.CONFERENCE, 100.0, new Date(), "Haifa", "creator3", getMapArea());

        String eventId = "Concurrent Festcreator3";
        int threadCount = 50;

        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.ExecutorService service = java.util.concurrent.Executors.newFixedThreadPool(threadCount);

        List<String> results = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            final String uname = "concurrentUser" + i;
            service.submit(() -> {
                try {
                    latch.await();
                    results.add(queueService.checkStatus(eventId, uname));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        latch.countDown();
        service.shutdown();
        try {
            if (!service.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS)) {
                return false;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }

        long authorizedCount = results.stream().filter(r -> r.equals("AUTHORIZED")).count();

        return authorizedCount == threadCount;
    }

    public boolean TestConcurrentQueueOverflow4() {
        userService.register("creator4", "creator");
        String token = userService.login("creator4", "creator");
        companyService.CreateCompany("creator4", token);
        eventService.createEvent(token, "Overfill Fest", "Artist", EventType.CONFERENCE, 100.0, new Date(), "Eilat", "creator4", getMapArea());

        String eventId = "Overfill Festcreator4";
        int threadCount = 150;

        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.ExecutorService service = java.util.concurrent.Executors.newFixedThreadPool(threadCount);

        List<String> results = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            final String uname = "overflowUser" + i;
            service.submit(() -> {
                try {
                    latch.await();
                    results.add(queueService.checkStatus(eventId, uname));
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

        long authorizedCount = results.stream().filter(r -> r.equals("AUTHORIZED")).count();
        long waitingCount = results.stream().filter(r -> r.startsWith("WAITING_POSITION_")).count();

        return authorizedCount == 100 && waitingCount == 50;
    }
    public boolean TestConcurrentSameUserRefresh5() {
        userService.register("creator5", "creator");
        String token = userService.login("creator5", "creator");
        companyService.CreateCompany("creator5", token);
        eventService.createEvent(token, "Refresh Fest", "Artist", EventType.CONFERENCE, 100.0, new Date(), "Jerusalem", "creator5", getMapArea());

        String eventId = "Refresh Festcreator5";

        for (int i = 1; i <= 100; i++) {
            queueService.checkStatus(eventId, "dummyUser" + i);
        }

        for (int i = 1; i <= 50; i++) {
            queueService.checkStatus(eventId, "waitingUser" + i);
        }

        int threadCount = 150;
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.ExecutorService service = java.util.concurrent.Executors.newFixedThreadPool(threadCount);

        List<String> results = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            service.submit(() -> {
                try {
                    latch.await();
                    results.add(queueService.checkStatus(eventId, "waitingUser25"));
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

        long correctPositionCount = results.stream().filter(r -> r.equals("WAITING_POSITION_25")).count();

        return correctPositionCount == threadCount;
    }
}
