package AcceptanceTest.users.PurchasedOrder;

import AcceptanceTest.users.initTheSystem;

import Appliction.*;
import Domain.Domains.*;
import Domain.Event.EventType;
import Domain.Event.MapArea;
import Domain.OwnerManagerTree.Permission;

import java.util.*;
import java.util.function.Supplier;

public class PurchaseOrderTests {
    UserService userService;
    CompanyService companyDomain;
    EventService eventDomain;
    OrderService reserveTicketService;
    PurchasedSevice purchasedDomain;
    private final List<String> failTests = new ArrayList<>();
    private final List<String> passTests = new ArrayList<>();
    private final Map<String, Supplier<Boolean>> testMap = new LinkedHashMap<>();
    private initTheSystem initTheSystem;
    public PurchaseOrderTests(UserService userService
            , CompanyService companyDomain
            , EventService eventDomain
            , OrderService reserveTicketService
            , PurchasedSevice purchasedDomain
            , initTheSystem initTheSystem){
        this.userService = userService;
        this.companyDomain = companyDomain;
        this.eventDomain = eventDomain;
        this.reserveTicketService = reserveTicketService;
        this.purchasedDomain = purchasedDomain;
        this.initTheSystem = initTheSystem;
        initTestMap();
    }
    private void initTestMap() {
        testMap.put("1", this::PurchasedTicketSuccess1);
        testMap.put("2", this::PurchasedTicketFailedOrderExpired2);
        testMap.put("3", this::PurchasedTicketSuccess3);
        testMap.put("4", this::GetCompanyTransactionSuccess4);
        testMap.put("5", this::GetCompanyTransactionSuccess5);
        testMap.put("6", this::GetCompanyTransactionSuccess6);
        testMap.put("7", this::GetCompanyTransactionSuccess7);
        testMap.put("8", this::GetCompanyTransactionSecurity8);
        testMap.put("9", this::GetCompanyTransactionMultipleEvents9);
        testMap.put("10", this::GetUserTransactionSuccess10);
        testMap.put("11", this::GetUserTransactionMultipleCompanies11);
        testMap.put("12", this::GetUserTransactionSecurity12);
        testMap.put("13", this::PurchaseOrderAsGuest13);
        testMap.put("14", this::PurchasedTicketSuccessAndGETOutTheApp14);
        testMap.put("15", this::PurchaseOrderAsGuestFaildBecauseHegetOut15);
        testMap.put("16", this::PurchasedTicketSuccessAndGETOutTheAppAndTheOrderExpired16);


    }
    public String whichTestPass() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Purchased order  test:\n");

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
        stringBuilder.append("Purchased order test that fail:\n");
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
    public boolean PurchasedTicketSuccess1() {
        userService.register("1","1");
        String token=userService.login("1","1");
        companyDomain.CreateCompany("1",token);
        eventDomain.createEvent(token,"1","1", EventType.PLAY,100,new Date(),"1","1",getMapArea());
        userService.register("2","2");
        String token1=userService.login("2","2");
        List<int[]> requests = new ArrayList<>();
        requests.add(new int[]{0, 0, 1});
        String orderId = reserveTicketService.reserveTickets(token1, "1", "1", requests);
        String result= purchasedDomain.PurchaseTicket("ro@gmail.com",orderId,"2");
        return result.equals("success");
    }
    public boolean PurchasedTicketFailedOrderExpired2() {
        userService.register("1","1");
        String token=userService.login("1","1");
        companyDomain.CreateCompany("1",token);
        eventDomain.createEvent(token,"1","1", EventType.PLAY,100,new Date(),"1","1",getMapArea());
        userService.register("2","2");
        String token1=userService.login("2","2");
        List<int[]> requests = new ArrayList<>();
        requests.add(new int[]{0, 0, 1});
        String orderId = reserveTicketService.reserveTickets(token1, "1", "1", requests);
        try {
            Thread.sleep(11000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        String result= purchasedDomain.PurchaseTicket("ro@gmail.com",orderId,"2");
        return result.equals("error");

    }
    public boolean PurchasedTicketSuccess3() {
        userService.register("1","1");
        String token=userService.login("1","1");
        companyDomain.CreateCompany("1",token);
        eventDomain.createEvent(token,"1","1", EventType.PLAY,100,new Date(),"1","1",getMapArea1());
        userService.register("2","2");
        String token1=userService.login("2","2");
        List<int[]> requests = new ArrayList<>();
        requests.add(new int[]{0, 0, 1});
        requests.add(new int[]{1,1, 1});
        String orderId = reserveTicketService.reserveTickets(token1, "1", "1", requests);
        String result= purchasedDomain.PurchaseTicket("ro@gmail.com",orderId,"2");
        return result.equals("success");
    }
    public boolean GetCompanyTransactionSuccess4() {
        userService.register("1","1");
        String token=userService.login("1","1");
        companyDomain.CreateCompany("1",token);
        eventDomain.createEvent(token,"1","1", EventType.PLAY,100,new Date(),"1","1",getMapArea());
        userService.register("2","2");
        String token1=userService.login("2","2");
        List<int[]> requests = new ArrayList<>();
        requests.add(new int[]{0, 0, 1});
        String orderId = reserveTicketService.reserveTickets(token1, "1", "1", requests);
        purchasedDomain.PurchaseTicket("ro@gmail.com",orderId,"2");
        String result= purchasedDomain.getCompanyTransaction("1",token);
        String expected="PurchaseOrder{company='1', event='1', buyer='2'}\n" +
                "Ticket{company='1', event='1', date=Tue Apr 14 19:11:43 IDT 2026, verticalSpote=0, horizontalSpote=0}";
        return result != null &&
                result.contains("company='1'") &&
                result.contains("event='1'") &&
                result.contains("buyer='2'") &&
                result.contains("verticalSpote=0") &&
                result.contains("horizontalSpote=0");
    }
    public boolean GetCompanyTransactionSuccess5() {
        userService.register("1", "1");
        String token = userService.login("1", "1");
        companyDomain.CreateCompany("1", token);
        eventDomain.createEvent(token, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea1());
        userService.register("2", "2");
        String token1 = userService.login("2", "2");
        List<int[]> requests = new ArrayList<>();
        requests.add(new int[]{0, 0, 1});
        requests.add(new int[]{1, 1, 1});
        String orderId = reserveTicketService.reserveTickets(token1, "1", "1", requests);
        purchasedDomain.PurchaseTicket("ro@gmail.com", orderId,"2");
        String result = purchasedDomain.getCompanyTransaction("1", token);

        boolean basicInfo = result != null &&
                result.contains("company='1'") &&
                result.contains("event='1'") &&
                result.contains("buyer='2'");

        boolean seatExists = result.contains("verticalSpote=0") && result.contains("horizontalSpote=0");

        int countStand = 0;
        String standPattern = "verticalSpote=1, horizontalSpote=1";
//        int lastIndex = 0;
//        while ((lastIndex = result.indexOf(standPattern, lastIndex)) != -1) {
//            countStand++;
//            lastIndex += standPattern.length();
//        }

        return basicInfo && seatExists ;
    }
    public boolean GetCompanyTransactionSuccess6() {
        userService.register("1", "1");
        String token = userService.login("1", "1");
        companyDomain.CreateCompany("1", token);
        eventDomain.createEvent(token, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea1());
        userService.register("2", "2");
        String token1 = userService.login("2", "2");
        userService.register("3", "3");
        String token2 = userService.login("3", "3");
        companyDomain.AppointAManager("3","1",Set.of(Permission.GENERATE_SALES_REPORTS),token);
        companyDomain.ApproveAppointmentForManager(token2,"1");
        List<int[]> requests = new ArrayList<>();
        requests.add(new int[]{0, 0, 1});
        requests.add(new int[]{1, 1, 1});
        String orderId = reserveTicketService.reserveTickets(token1, "1", "1", requests);
        purchasedDomain.PurchaseTicket("ro@gmail.com", orderId,"2");
        String result = purchasedDomain.getCompanyTransaction("1", token2);

        boolean basicInfo = result != null &&
                result.contains("company='1'") &&
                result.contains("event='1'") &&
                result.contains("buyer='2'");

        boolean seatExists = result.contains("verticalSpote=0") && result.contains("horizontalSpote=0");

        int countStand = 0;
        String standPattern = "verticalSpote=1, horizontalSpote=1";
        int lastIndex = 0;


        return basicInfo && seatExists;
    }
    public boolean GetCompanyTransactionSuccess7() {
        userService.register("1", "1");
        String token = userService.login("1", "1");
        companyDomain.CreateCompany("1", token);

        eventDomain.createEvent(token, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea());
        eventDomain.createEvent(token, "2", "2", EventType.PLAY, 100, new Date(), "2", "1", getMapArea());

        userService.register("2", "2");
        String token1 = userService.login("2", "2");
        userService.register("3", "3");
        String token2 = userService.login("3", "3");

        List<int[]> requests = new ArrayList<>();
        requests.add(new int[]{0, 0, 1});

        String orderId = reserveTicketService.reserveTickets(token1, "1", "1", requests);
        String orderId1 = reserveTicketService.reserveTickets(token2, "1", "2", requests);

        purchasedDomain.PurchaseTicket("ro@gmail.com", orderId,"2");
        purchasedDomain.PurchaseTicket("ro@gmail.com", orderId1,"3");

        String result = purchasedDomain.getCompanyTransaction("1", token);
        System.out.println(result);

        boolean event1Valid = result.contains("event='1'") && result.contains("buyer='2'");
        boolean event2Valid = result.contains("event='2'") && result.contains("buyer='3'");

        boolean companyValid = result.contains("company='1'");

        return result != null && companyValid && event1Valid && event2Valid;
    }
    public boolean GetCompanyTransactionSecurity8() {
        userService.register("owner1", "pass");
        String tokenOwner1 = userService.login("owner1", "pass");
        companyDomain.CreateCompany("Company1", tokenOwner1);
        eventDomain.createEvent(tokenOwner1, "Event1", "Company1", EventType.PLAY, 100, new Date(), "Loc1", "1", getMapArea());

        userService.register("owner2", "pass");
        String tokenOwner2 = userService.login("owner2", "pass");
        companyDomain.CreateCompany("Company2", tokenOwner2);
        eventDomain.createEvent(tokenOwner2, "Event2", "Company2", EventType.PLAY, 100, new Date(), "Loc2", "1", getMapArea());

        userService.register("buyer", "pass");
        String tokenBuyer = userService.login("buyer", "pass");
        List<int[]> requests = new ArrayList<>();
        requests.add(new int[]{0, 0, 1});
        String orderId = reserveTicketService.reserveTickets(tokenBuyer, "Company2", "Event2", requests);
        purchasedDomain.PurchaseTicket("buyer@gmail.com", orderId,"buyer");

        String result = purchasedDomain.getCompanyTransaction("Company2", tokenOwner1);

        return result != null && result.contains("User not authorized");
    }

    public boolean GetCompanyTransactionMultipleEvents9() {
        userService.register("owner", "pass");
        String token = userService.login("owner", "pass");
        companyDomain.CreateCompany("1", token);

        eventDomain.createEvent(token, "EventA", "1", EventType.PLAY, 100, new Date(), "LocA", "1", getMapArea());
        eventDomain.createEvent(token, "EventB", "1", EventType.PLAY, 100, new Date(), "LocB", "1", getMapArea());

        userService.register("buyer", "pass");
        String tokenBuyer = userService.login("buyer", "pass");

        List<int[]> reqA = new ArrayList<>();
        reqA.add(new int[]{0, 0, 1});
        String orderA = reserveTicketService.reserveTickets(tokenBuyer, "1", "EventA", reqA);
        purchasedDomain.PurchaseTicket("b@gmail.com", orderA,"buyer");

        List<int[]> reqB = new ArrayList<>();
        reqB.add(new int[]{0, 0, 1});
        String orderB = reserveTicketService.reserveTickets(tokenBuyer, "1", "EventB", reqB);
        purchasedDomain.PurchaseTicket("b@gmail.com", orderB,"buyer");

        String result = purchasedDomain.getCompanyTransaction("1", token);

        return result != null &&
                result.contains("event='EventA'") &&
                result.contains("event='EventB'") &&
                result.contains("buyer='buyer'");
    }
    public boolean GetUserTransactionSuccess10() {
        userService.register("10", "10");
        String tO = userService.login("10", "10");
        companyDomain.CreateCompany("C10", tO);
        eventDomain.createEvent(tO, "E10", "C10", EventType.PLAY, 100, new Date(), "L", "C10", getMapArea());

        userService.register("20", "20");
        String tB = userService.login("20", "20");
        List<int[]> req = new ArrayList<>();
        req.add(new int[]{0, 0, 1});

        String orderId = reserveTicketService.reserveTickets(tB, "C10", "E10", req);
        purchasedDomain.PurchaseTicket("b@gmail.com", orderId,"20");

        String result = purchasedDomain.getUserTransaction(tB);

        return result != null && result.contains("company='C10'");
    }

    public boolean GetUserTransactionMultipleCompanies11() {
        userService.register("100", "100");
        String tO1 = userService.login("100", "100");
        companyDomain.CreateCompany("CA", tO1);
        eventDomain.createEvent(tO1, "EA", "CA", EventType.PLAY, 100, new Date(), "LA", "CA", getMapArea());

        userService.register("200", "200");
        String tO2 = userService.login("200", "200");
        companyDomain.CreateCompany("CB", tO2);
        eventDomain.createEvent(tO2, "EB", "CB", EventType.PLAY, 100, new Date(), "LB", "CB", getMapArea());

        userService.register("300", "300");
        String tB = userService.login("300", "300");
        List<int[]> req = new ArrayList<>();
        req.add(new int[]{0, 0, 1});

        String orderA = reserveTicketService.reserveTickets(tB, "CA", "EA", req);
        purchasedDomain.PurchaseTicket("b@gmail.com", orderA,"300");

        String orderB = reserveTicketService.reserveTickets(tB, "CB", "EB", req);
        purchasedDomain.PurchaseTicket("b@gmail.com", orderB,"300");

        String result = purchasedDomain.getUserTransaction(tB);

        return result != null && result.contains("company='CA'") && result.contains("company='CB'");
    }

    public boolean GetUserTransactionSecurity12() {
        userService.register("own", "p");
        String tO = userService.login("own", "p");
        companyDomain.CreateCompany("SecC", tO);
        eventDomain.createEvent(tO, "SecE", "SecC", EventType.PLAY, 100, new Date(), "L", "SecC", getMapArea());

        userService.register("user1", "p");
        String t1 = userService.login("user1", "p");
        List<int[]> req = new ArrayList<>();
        req.add(new int[]{0, 0, 1});
        String order1 = reserveTicketService.reserveTickets(t1, "SecC", "SecE", req);
        purchasedDomain.PurchaseTicket("u1@gmail.com", order1,"user1");

        userService.register("user2", "p");
        String t2 = userService.login("user2", "p");
        String result = purchasedDomain.getUserTransaction(t2);

        return result != null && !result.contains("buyer='user1'");
    }
    public boolean PurchaseOrderAsGuest13(){
        userService.register("own", "p");
        String tO = userService.login("own", "p");
        companyDomain.CreateCompany("SecC", tO);
        eventDomain.createEvent(tO, "SecE", "SecC", EventType.PLAY, 100, new Date(), "L", "SecC", getMapArea());
        List<int[]> req = new ArrayList<>();
        req.add(new int[]{0, 0, 1});
        String order1 = reserveTicketService.reserveTickets("nonExist", "SecC", "SecE", req);
        String result= purchasedDomain.PurchaseTicket("u1@gmail.com", order1,"user1");
        return result.equals("success");
    }

    public boolean PurchasedTicketSuccessAndGETOutTheApp14() {
        userService.register("1","1");
        String token=userService.login("1","1");
        companyDomain.CreateCompany("1",token);
        eventDomain.createEvent(token,"1","1", EventType.PLAY,100,new Date(),"1","1",getMapArea());
        userService.register("2","2");
        String token1=userService.login("2","2");
        List<int[]> requests = new ArrayList<>();
        requests.add(new int[]{0, 0, 1});

        String orderId = reserveTicketService.reserveTickets(token1, "1", "1", requests);
        userService.logout(token1);
        String t1 = userService.login("2","2");
        String result= purchasedDomain.PurchaseTicket("ro@gmail.com","orderId",t1);
        return result.equals("success");
    }
    public boolean PurchaseOrderAsGuestFaildBecauseHegetOut15(){
        userService.register("own", "p");
        String tO = userService.login("own", "p");
        companyDomain.CreateCompany("SecC", tO);
        eventDomain.createEvent(tO, "SecE", "SecC", EventType.PLAY, 100, new Date(), "L", "SecC", getMapArea());
        List<int[]> req = new ArrayList<>();
        req.add(new int[]{0, 0, 1});
        String order1 = reserveTicketService.reserveTickets("nonExist", "SecC", "SecE", req);
        String result= purchasedDomain.PurchaseTicket("u1@gmail.com", "order1","user1");
        return result.equals("error");
    }
    public boolean PurchasedTicketSuccessAndGETOutTheAppAndTheOrderExpired16() {
        userService.register("1","1");
        String token=userService.login("1","1");
        companyDomain.CreateCompany("1",token);
        eventDomain.createEvent(token,"1","1", EventType.PLAY,100,new Date(),"1","1",getMapArea());
        userService.register("2","2");
        String token1=userService.login("2","2");
        List<int[]> requests = new ArrayList<>();
        requests.add(new int[]{0, 0, 1});

        String orderId = reserveTicketService.reserveTickets(token1, "1", "1", requests);
        userService.logout(token1);
        String t1 = userService.login("2","2");
        try {
            Thread.sleep(11000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        String result= purchasedDomain.PurchaseTicket("ro@gmail.com","orderId",t1);
        return result.equals("error");
    }


}
