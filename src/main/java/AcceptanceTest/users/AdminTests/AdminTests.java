package AcceptanceTest.users.AdminTests;

import AcceptanceTest.users.initTheSystem;
import Appliction.*;
import Domain.Event.EventType;
import Domain.Event.MapArea;


import java.util.*;
import java.util.function.Supplier;

public class AdminTests {
    UserService userService;
    CompanyService companyService;
    EventService eventService;
    OrderService reserveTicketService;
    PurchasedService purchasedService;
    AdminService adminService;

    private final List<String> failTests = new ArrayList<>();
    private final List<String> passTests = new ArrayList<>();
    private final Map<String, Supplier<Boolean>> testMap = new LinkedHashMap<>();
    private initTheSystem initTheSystem;
    public AdminTests(UserService userService,
                      CompanyService companyService,
                      EventService eventService,
                      OrderService reserveTicketService,
                      PurchasedService purchasedService,
                      AdminService adminService,
                      initTheSystem initTheSystem
                     ) {
        this.userService = userService;
        this.companyService = companyService;
        this.eventService = eventService;
        this.reserveTicketService = reserveTicketService;
        this.purchasedService = purchasedService;
        this.adminService = adminService;
        this.initTheSystem = initTheSystem;
        initTestMap();
    }
    private void initTestMap() {
        testMap.put("1", this::CloseCompanySuccess1);
        testMap.put("2", this::CloseCompanyFailedNotAdmin2);
        testMap.put("3", this::RemoveUserSuccess3);
        testMap.put("4", this::RemoveUserFailedNotAdmin4);
        testMap.put("5", this::GetAllPurchasedOrdersSuccess5);
        testMap.put("6", this::GetAllPurchasedOrdersMultipleSuccess6);
        testMap.put("7", this::GetAllPurchasedOrdersFailedNotAdmin7);
        testMap.put("8", this::GetAllPurchasedOrdersTwoCompanies8);



    }
    public String whichTestPass() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Admin management test:\n");

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
    public boolean CloseCompanySuccess1(){
        userService.register("1","1");
        String token=userService.login("1","1");
        companyService.CreateCompany("1",token);
        eventService.createEvent(token,"1","1", EventType.PLAY,100,new Date(),"1","1",getMapArea());
        String result=adminService.CloseCompany("1","admin");
        String company=eventService.getCompanyInfo("1");
        List<String> events=eventService.getCompanyEvents("1");
        String treeOfRole=companyService.GetRoleTreeString(token,"1");
        System.out.println(result);
        System.out.println(company==null);
        System.out.println(treeOfRole==null);
        System.out.println(events==null);
        return result.equals("success") && company==(null) && events==null &&treeOfRole==null;
    }
    public boolean CloseCompanyFailedNotAdmin2(){
        userService.register("1","1");
        String token=userService.login("1","1");
        companyService.CreateCompany("1",token);
        eventService.createEvent(token,"1","1", EventType.PLAY,100,new Date(),"1","1",getMapArea());
        String result=adminService.CloseCompany("1","admin1");
        return result.equals("error");
    }
    public boolean RemoveUserSuccess3(){
        userService.register("1","1");
        String token=userService.login("1","1");
        String result=adminService.removeUser("1","admin");
        String isExist=userService.login("1","1");
        return result.equals("success")&& isExist==null;
    }
    public boolean RemoveUserFailedNotAdmin4(){
        userService.register("1","1");
        String token=userService.login("1","1");
        String result=adminService.removeUser("1","admin1");
        return result.equals("error");
    }
    public boolean GetAllPurchasedOrdersSuccess5() {
        userService.register("owner", "p");
        String tO = userService.login("owner", "p");
        companyService.CreateCompany("C1", tO);
        eventService.createEvent(tO, "E1", "C1", EventType.PLAY, 100, new Date(), "L", "C1", getMapArea());

        userService.register("buyer", "p");
        String tB = userService.login("buyer", "p");
        List<int[]> req = new ArrayList<>();
        req.add(new int[]{0, 0, 1});
        String orderId = reserveTicketService.reserveTickets(tB, "C1", "E1", req);
        purchasedService.PurchaseTicket("b@gmail.com", orderId);

        String result = adminService.GetAllPurchasedOrders("admin");

        return result != null &&
                result.contains("company='C1'") &&
                result.contains("buyer='buyer'");
    }

    public boolean GetAllPurchasedOrdersMultipleSuccess6() {
        userService.register("o1", "p");
        String tO = userService.login("o1", "p");
        companyService.CreateCompany("C1", tO);
        eventService.createEvent(tO, "E1", "C1", EventType.PLAY, 100, new Date(), "L", "C1", getMapArea());

        userService.register("b1", "p");
        String tB1 = userService.login("b1", "p");
        userService.register("b2", "p");
        String tB2 = userService.login("b2", "p");

        List<int[]> req = new ArrayList<>();
        req.add(new int[]{0, 0, 1});
        List<int[]> req1 = new ArrayList<>();
        req.add(new int[]{1, 1, 1});
        String o1 = reserveTicketService.reserveTickets(tB1, "C1", "E1", req);
        purchasedService.PurchaseTicket("b1@gmail.com", o1);


        String o2 = reserveTicketService.reserveTickets(tB2, "C1", "E1", req1);
        purchasedService.PurchaseTicket("b2@gmail.com", o2);

        String result = adminService.GetAllPurchasedOrders("admin");

        return result != null &&
                result.contains("buyer='b1'") &&
                result.contains("buyer='b2'");
    }

    public boolean GetAllPurchasedOrdersFailedNotAdmin7() {
        String result = adminService.GetAllPurchasedOrders("notAdminUser");

        return result == null;
    }
    public boolean GetAllPurchasedOrdersTwoCompanies8() {
        // הגדרת חברה א' ואירוע
        userService.register("ownerA", "p");
        String tOA = userService.login("ownerA", "p");
        companyService.CreateCompany("CompA", tOA);
        eventService.createEvent(tOA, "EventA", "CompA", EventType.PLAY, 100, new Date(), "LocA", "CompA", getMapArea());

        userService.register("ownerB", "p");
        String tOB = userService.login("ownerB", "p");
        companyService.CreateCompany("CompB", tOB);
        eventService.createEvent(tOB, "EventB", "CompB", EventType.PLAY, 100, new Date(), "LocB", "CompB", getMapArea());

        userService.register("buyer8", "p");
        String tB = userService.login("buyer8", "p");
        List<int[]> req = new ArrayList<>();
        req.add(new int[]{0, 0, 1});

        String orderA = reserveTicketService.reserveTickets(tB, "CompA", "EventA", req);
        purchasedService.PurchaseTicket("b@gmail.com", orderA);

        String orderB = reserveTicketService.reserveTickets(tB, "CompB", "EventB", req);
        purchasedService.PurchaseTicket("b@gmail.com", orderB);

        String result = adminService.GetAllPurchasedOrders("admin");

        return result != null &&
                result.contains("company='CompA'") &&
                result.contains("event='EventA'") &&
                result.contains("company='CompB'") &&
                result.contains("event='EventB'") &&
                result.contains("buyer='buyer8'");
    }


}
