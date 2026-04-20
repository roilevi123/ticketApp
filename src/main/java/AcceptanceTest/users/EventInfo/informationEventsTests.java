package AcceptanceTest.users.EventInfo;

import AcceptanceTest.users.initTheSystem;
import Appliction.CompanyService;
import Appliction.EventService;
import Appliction.UserService;
import Domain.Event.EventType;
import Domain.Event.MapArea;


import java.util.*;
import java.util.function.Supplier;

public class informationEventsTests {
    UserService userService;
    CompanyService companyService;
    EventService eventService;
    private final List<String> failTests = new ArrayList<>();
    private final List<String> passTests = new ArrayList<>();
    private final Map<String, Supplier<Boolean>> testMap = new LinkedHashMap<>();
    private initTheSystem initTheSystem;
    public informationEventsTests(UserService userService,
                                  CompanyService companyService,
                                  EventService eventService,
                                  initTheSystem initTheSystem) {
        this.userService = userService;
        this.companyService = companyService;
        this.eventService = eventService;
        this.initTheSystem = initTheSystem;
        initTestMap();
    }
    private void initTestMap() {
        testMap.put("1", this::GetCompanyInfoSuccess1);
        testMap.put("2", this::GetCompanyInfoFailedCompanyNotExitst2);
        testMap.put("3", this::GetCompanyEventsSuccess3);
        testMap.put("4", this::GetCompanyEventsSuccessButNoEventsExist4);
        testMap.put("5", this::GetCompanyEventsFailedNoCompanyExist5);
        testMap.put("6", this::GetEventMapSuccess6);
        testMap.put("7", this::GetEventMapFailedNoEventsExist7);
        testMap.put("8", this::GetEventMapFailedNoCompanyExist8);
        testMap.put("9", this::SearchEventsByEventNameQuery9);
        testMap.put("10", this::SearchEventsByArtistNameQuery10);
        testMap.put("11", this::SearchEventsByPriceRangeMultipleCompanies11);
        testMap.put("12", this::SearchEventsByDateRange12);
        testMap.put("13", this::SearchEventsByLocation13);
        testMap.put("14", this::SearchEventsByMinRating14);



    }
    public String whichTestPass() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Event info test:\n");

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
        stringBuilder.append("Event info test that fail:\n");
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
    public boolean GetCompanyInfoSuccess1() {
        userService.register("1","1");
        String token=userService.login("1","1");
        companyService.CreateCompany("1",token);
        String companyInfo = eventService.getCompanyInfo("1");
        String expectedInfo = "Company Summary:" +
                "\nName: 1" +
                "\nFounder/Owner: 1" +
                "\nStatus: Active" +
                "\nRating: 0.0";
        return companyInfo.equals(expectedInfo);
    }
    public boolean GetCompanyInfoFailedCompanyNotExitst2() {
        userService.register("1","1");
        String token=userService.login("1","1");
//        companyService.CreateCompany("1",token);
        String companyInfo = eventService.getCompanyInfo("1");
        return companyInfo==null;
    }
    public boolean GetCompanyEventsSuccess3() {
        userService.register("1", "1");
        String token = userService.login("1", "1");
        companyService.CreateCompany("1", token);
        Date eventDate = new Date();
        MapArea[][] map = getMapArea();
        eventService.createEvent(token, "1", "1", EventType.PLAY, 100, eventDate, "1", "1", map);
        eventService.createEvent(token, "2", "2", EventType.CONFERENCE, 200, eventDate, "2", "1", map);
        List<String> list = eventService.getCompanyEvents("1");
        String expectedEvent1 = "Event{eventName='1', artistName='1', eventType=PLAY, price=100.0, date=" + eventDate + ", location='1', rating=0.0, company='1', version=0, map=[[SEAT, SEAT], [SEAT, SEAT]]}";
        String expectedEvent2 = "Event{eventName='2', artistName='2', eventType=CONFERENCE, price=200.0, date=" + eventDate + ", location='2', rating=0.0, company='1', version=0, map=[[SEAT, SEAT], [SEAT, SEAT]]}";
        List<String> expectedList = List.of(expectedEvent1, expectedEvent2);
        for (String event : list) {
            System.out.println(event);
        }
        return list.size() == expectedList.size() && list.containsAll(expectedList);
    }
    public boolean GetCompanyEventsSuccessButNoEventsExist4() {
        userService.register("1", "1");
        String token = userService.login("1", "1");
        companyService.CreateCompany("1", token);
        List<String> list = eventService.getCompanyEvents("1");

        return list.isEmpty();
    }
    public boolean GetCompanyEventsFailedNoCompanyExist5() {
        userService.register("1", "1");
        String token = userService.login("1", "1");
//        companyService.CreateCompany("1", token);
        List<String> list = eventService.getCompanyEvents("1");
        return list==null;
    }

    public boolean GetEventMapSuccess6() {
        userService.register("1", "1");
        String token = userService.login("1", "1");
        companyService.CreateCompany("1", token);
        Date eventDate = new Date();
        MapArea[][] map = getMapArea();
        eventService.createEvent(token, "1", "1", EventType.PLAY, 100, eventDate, "1", "1", map);
        MapArea[][] list = eventService.getMapArea("1","1");

        return Arrays.deepEquals(map, list);
    }
    public boolean GetEventMapFailedNoEventsExist7() {
        userService.register("1", "1");
        String token = userService.login("1", "1");
        companyService.CreateCompany("1", token);
        MapArea[][] list = eventService.getMapArea("1","1");

        return list==null;
    }
    public boolean GetEventMapFailedNoCompanyExist8() {
        userService.register("1", "1");
        String token = userService.login("1", "1");
//        companyService.CreateCompany("1", token);
        MapArea[][] list = eventService.getMapArea("1","1");
        return list==null;
    }
    private void setupEnvironment() {
        userService.register("user1", "pass1");
        String token1 = userService.login("user1", "pass1");
        companyService.CreateCompany("CompanyA", token1);

        userService.register("user2", "pass2");
        String token2 = userService.login("user2", "pass2");
        companyService.CreateCompany("CompanyB", token2);

        long day = 24 * 60 * 60 * 1000L;
        Date today = new Date();
        Date nextWeek = new Date(today.getTime() + 7 * day);

        eventService.createEvent(token1, "Rock Festival", "Arctic Monkeys", EventType.CONFERENCE, 100.0, today, "Tel Aviv", "CompanyA", getMapArea());
        eventService.createEvent(token1, "Jazz Night", "Norah Jones", EventType.CONFERENCE, 200.0, nextWeek, "Haifa", "CompanyA", getMapArea());

        eventService.createEvent(token2, "Tech Talk", "Elon Mask", EventType.CONFERENCE, 0.0, today, "Tel Aviv", "CompanyB", getMapArea());
        eventService.createEvent(token2, "Opera Show", "Pavaraotti", EventType.PLAY, 500.0, nextWeek, "Jerusalem", "CompanyB", getMapArea());

    }
    public boolean SearchEventsByEventNameQuery9() {
        setupEnvironment();
        List<String> results = eventService.searchEvents("Rock", null, null, null, null, null, null, null, null);
        return results != null && results.size() == 1 && results.get(0).contains("Rock Festival");
    }
    public boolean SearchEventsByArtistNameQuery10() {
        setupEnvironment();
        List<String> results = eventService.searchEvents("Monkeys", null, null, null, null, null, null, null, null);
        return results != null && results.size() == 1 && results.get(0).contains("Arctic Monkeys");
    }
    public boolean SearchEventsByPriceRangeMultipleCompanies11() {
        setupEnvironment();
        List<String> results = eventService.searchEvents(null, null, null, 0.0, 150.0, null, null, null, null);
        return results != null && results.size() == 2;
    }
    public boolean SearchEventsByDateRange12() {
        setupEnvironment();

        long fiveMinutesAgo = System.currentTimeMillis() - (5 * 60 * 1000L);
        Date startDate = new Date(fiveMinutesAgo);

        Date tomorrow = new Date(System.currentTimeMillis() + (24 * 60 * 60 * 1000L));

        List<String> results = eventService.searchEvents(null, null, null, null, null, startDate, tomorrow, null, null);

        if (results == null || results.size() != 2) {
            System.out.println("Test 12 failed. Found size: " + (results != null ? results.size() : "null"));
        }

        return results != null && results.size() == 2;
    }
    public boolean SearchEventsByLocation13() {
        setupEnvironment();
        List<String> results = eventService.searchEvents(null, null, null, null, null, null, null, "Tel Aviv", null);
        return results != null && results.size() == 2;
    }
    public boolean SearchEventsByMinRating14() {
        setupEnvironment();
        List<String> results = eventService.searchEvents(null, null, null, null, null, null, null, null, 1.0);
        return results != null && results.isEmpty();
    }


}
