package AcceptanceTest.users.EventManagementTest;

import AcceptanceTest.users.initTheSystem;
import Appliction.CompanyService;
import Appliction.UserService;
import Appliction.EventService;
import Appliction.Response;
import Domain.Event.EventType;

import java.util.*;
import java.util.function.Supplier;
import Domain.Event.MapArea;
import Domain.OwnerManagerTree.Permission;

public class EventManagementTest {

    private EventService eventService;
    private CompanyService companyService;
    private UserService userService;
    private final List<String> failTests = new ArrayList<>();
    private final List<String> passTests = new ArrayList<>();
    private final Map<String, Supplier<Boolean>> testMap = new LinkedHashMap<>();
    private initTheSystem initTheSystem;

    public EventManagementTest(UserService userService, EventService eventService, initTheSystem initTheSystem, CompanyService companyService) {
        this.userService = userService;
        this.eventService = eventService;
        this.initTheSystem = initTheSystem;
        this.companyService = companyService;
        initTestMap();
    }

    private void initTestMap() {
        testMap.put("1", this::CreateEventAsFounderSuccess1);
        testMap.put("2", this::CreateEventAsManagerSuccess2);
        testMap.put("3", this::CreateEventAsOwnerSuccess3);
        testMap.put("4", this::CreateEventFailedNotAuthorizedNotPartOfTheCompany4);
        testMap.put("5", this::CreateEventFailedNotAuthorizedNotAprroveTheOwnerShip5);
        testMap.put("6", this::CreateEventFailedNotAuthorizedNotHaveTheRightPermission6);
        testMap.put("7", this::DeleteEventAsFounderSuccess7);
        testMap.put("8", this::DeleteEventAsManagerSuccess8);
        testMap.put("9", this::DeleteEventAsOwnerSuccess9);
        testMap.put("10", this::DeleteEventFailedNoEventFound10);
        testMap.put("11", this::DeleteEventFailedNotAuthorizedNotPartOfTheCompany11);
        testMap.put("12", this::DeleteEventFailedNotAuthorizedNotAprroveTheOwnerShip12);
        testMap.put("13", this::DeleteEventFailedNotAuthorizedNotHaveTheRightPermission13);

        testMap.put("14", this::UpdateEventAsFounderSuccess14);
        testMap.put("15", this::UpdateEventAsManagerSuccess15);
        testMap.put("16", this::UpdateEventAsOwnerSuccess16);
        testMap.put("17", this::UpdateEventFailedNoEventFound17);
        testMap.put("18", this::UpdateEventFailedNotAuthorizedNotPartOfTheCompany18);
        testMap.put("19", this::UpdateEventFailedNotAuthorizedNotAprroveTheOwnerShip19);
        testMap.put("20", this::UpdateEventFailedNotAuthorizedNotHaveTheRightPermission20);
    }

    public String whichTestPass() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Event management test:\n");

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
        MapArea[][] MAP=new MapArea[10][10];
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                MAP[i][j] = MapArea.SEAT;
            }
        }
        return MAP;
    }

    public boolean CreateEventAsFounderSuccess1() {
        userService.register("1","1");
        String token=userService.login("1","1");
        companyService.CreateCompany("1",token);
        Response<String> result=eventService.createEvent(token,"1","1", EventType.PLAY,100,new Date(),"1","1",getMapArea());
        return result.isSuccess();
    }
    public boolean CreateEventAsManagerSuccess2() {
        userService.register("1","1");
        String token=userService.login("1","1");
        userService.register("2","2");
        String token2=userService.login("2","2");
        companyService.CreateCompany("1",token);
        Set<Permission> permissions = new HashSet<>();
        permissions.add(Permission.MANAGE_INVENTORY);
        companyService.AppointAManager("2","1",permissions,token);
        companyService.ApproveAppointmentForManager(token2,"1");
        Response<String> result=eventService.createEvent(token2,"1","1", EventType.PLAY,100,new Date(),"1","1",getMapArea());
        return result.isSuccess();
    }
    public boolean CreateEventAsOwnerSuccess3() {
        userService.register("1","1");
        String token=userService.login("1","1");
        userService.register("2","2");
        String token2=userService.login("2","2");
        companyService.CreateCompany("1",token);
        companyService.AppointOwner("2","1",token);
        companyService.ApproveAppointmentForOwner(token2,"1");
        Response<String> result=eventService.createEvent(token2,"1","1", EventType.PLAY,100,new Date(),"1","1",getMapArea());
        return result.isSuccess();
    }
    public boolean CreateEventFailedNotAuthorizedNotPartOfTheCompany4() {
        userService.register("1","1");
        String token=userService.login("1","1");
        userService.register("2","2");
        String token2=userService.login("2","2");
        companyService.CreateCompany("1",token);
//        companyService.AppointOwner("2","1",token);
//        companyService.ApproveAppointmentForOwner(token2,"1");
        Response<String> result=eventService.createEvent(token2,"1","1", EventType.PLAY,100,new Date(),"1","1",getMapArea());
        return !result.isSuccess();
    }
    public boolean CreateEventFailedNotAuthorizedNotAprroveTheOwnerShip5() {
        userService.register("1","1");
        String token=userService.login("1","1");
        userService.register("2","2");
        String token2=userService.login("2","2");
        companyService.CreateCompany("1",token);
        companyService.AppointOwner("2","1",token);
//        companyService.ApproveAppointmentForOwner(token2,"1");
        Response<String> result=eventService.createEvent(token2,"1","1", EventType.PLAY,100,new Date(),"1","1",getMapArea());
        return !result.isSuccess();
    }
    public boolean CreateEventFailedNotAuthorizedNotHaveTheRightPermission6() {
        userService.register("1","1");
        String token=userService.login("1","1");
        userService.register("2","2");
        String token2=userService.login("2","2");
        companyService.CreateCompany("1",token);
        Set<Permission> permissions = new HashSet<>();
        permissions.add(Permission.CHANGE_POLICIES);
        companyService.AppointAManager("2","1",permissions,token);
        companyService.ApproveAppointmentForManager(token2,"1");
        Response<String> result=eventService.createEvent(token2,"1","1", EventType.PLAY,100,new Date(),"1","1",getMapArea());
        return !result.isSuccess();
    }
    public boolean DeleteEventAsFounderSuccess7() {
        userService.register("1","1");
        String token=userService.login("1","1");
        companyService.CreateCompany("1",token);
        eventService.createEvent(token,"1","1", EventType.PLAY,100,new Date(),"1","1",getMapArea());
        Response<Void> result=eventService.deleteEvent("1","1",token);
        return result.isSuccess();
    }
    public boolean DeleteEventAsManagerSuccess8() {
        userService.register("1","1");
        String token=userService.login("1","1");
        userService.register("2","2");
        String token2=userService.login("2","2");
        companyService.CreateCompany("1",token);
        Set<Permission> permissions = new HashSet<>();
        permissions.add(Permission.MANAGE_INVENTORY);
        companyService.AppointAManager("2","1",permissions,token);
        companyService.ApproveAppointmentForManager(token2,"1");
        eventService.createEvent(token,"1","1", EventType.PLAY,100,new Date(),"1","1",getMapArea());
        Response<Void> result=eventService.deleteEvent("1","1",token2);

        return result.isSuccess();
    }
    public boolean DeleteEventAsOwnerSuccess9() {
        userService.register("1","1");
        String token=userService.login("1","1");
        userService.register("2","2");
        String token2=userService.login("2","2");
        companyService.CreateCompany("1",token);
        companyService.AppointOwner("2","1",token);
        companyService.ApproveAppointmentForOwner(token2,"1");
        eventService.createEvent(token,"1","1", EventType.PLAY,100,new Date(),"1","1",getMapArea());
        Response<Void> result=eventService.deleteEvent("1","1",token2);
        return result.isSuccess();

    }
    public boolean DeleteEventFailedNoEventFound10() {
        userService.register("1","1");
        String token=userService.login("1","1");
        userService.register("2","2");
        String token2=userService.login("2","2");
        companyService.CreateCompany("1",token);
        companyService.AppointOwner("2","1",token);
        companyService.ApproveAppointmentForOwner(token2,"1");
//        eventService.CreateEvent(token,"1","1", EventType.PLAY,100,new Date(),"1","1",getMapArea());
        Response<Void> result=eventService.deleteEvent("1","1",token2);
        return !result.isSuccess();
    }


    public boolean DeleteEventFailedNotAuthorizedNotPartOfTheCompany11() {
        userService.register("1","1");
        String token=userService.login("1","1");
        userService.register("2","2");
        String token2=userService.login("2","2");
        companyService.CreateCompany("1",token);
//        companyService.AppointOwner("2","1",token);
//        companyService.ApproveAppointmentForOwner(token2,"1");
        eventService.createEvent(token,"1","1", EventType.PLAY,100,new Date(),"1","1",getMapArea());
        Response<Void> result=eventService.deleteEvent("1","1",token2);

        return !result.isSuccess();
    }
    public boolean DeleteEventFailedNotAuthorizedNotAprroveTheOwnerShip12() {
        userService.register("1","1");
        String token=userService.login("1","1");
        userService.register("2","2");
        String token2=userService.login("2","2");
        companyService.CreateCompany("1",token);
        companyService.AppointOwner("2","1",token);
//        companyService.ApproveAppointmentForOwner(token2,"1");
        eventService.createEvent(token,"1","1", EventType.PLAY,100,new Date(),"1","1",getMapArea());
        Response<Void> result=eventService.deleteEvent("1","1",token2);

        return !result.isSuccess();
    }
    public boolean DeleteEventFailedNotAuthorizedNotHaveTheRightPermission13() {
        userService.register("1","1");
        String token=userService.login("1","1");
        userService.register("2","2");
        String token2=userService.login("2","2");
        companyService.CreateCompany("1",token);
        Set<Permission> permissions = new HashSet<>();
        permissions.add(Permission.CHANGE_POLICIES);
        companyService.AppointAManager("2","1",permissions,token);
        companyService.ApproveAppointmentForManager(token2,"1");
        eventService.createEvent(token,"1","1", EventType.PLAY,100,new Date(),"1","1",getMapArea());
        Response<Void> result=eventService.deleteEvent("1","1",token2);

        return !result.isSuccess();
    }
//
    public boolean UpdateEventAsFounderSuccess14() {
        userService.register("1","1");
        String token=userService.login("1","1");
        companyService.CreateCompany("1",token);
        eventService.createEvent(token,"1","1", EventType.PLAY,100,new Date(),"1","1",getMapArea());
        String result=eventService.UpdateEvent(token,"1","2", EventType.PLAY,100,new Date(),"1","1",getMapArea(),0);
        return result.equals("success");
    }
    public boolean UpdateEventAsManagerSuccess15() {
        userService.register("1","1");
        String token=userService.login("1","1");
        userService.register("2","2");
        String token2=userService.login("2","2");
        companyService.CreateCompany("1",token);
        Set<Permission> permissions = new HashSet<>();
        permissions.add(Permission.MANAGE_INVENTORY);
        companyService.AppointAManager("2","1",permissions,token);
        companyService.ApproveAppointmentForManager(token2,"1");
        eventService.createEvent(token,"1","1", EventType.PLAY,100,new Date(),"1","1",getMapArea());
        String result=eventService.UpdateEvent(token2,"1","2", EventType.PLAY,100,new Date(),"1","1",getMapArea(),0);

        return result.equals("success");
    }
    public boolean UpdateEventAsOwnerSuccess16() {
        userService.register("1","1");
        String token=userService.login("1","1");
        userService.register("2","2");
        String token2=userService.login("2","2");
        companyService.CreateCompany("1",token);
        companyService.AppointOwner("2","1",token);
        companyService.ApproveAppointmentForOwner(token2,"1");
        eventService.createEvent(token,"1","1", EventType.PLAY,100,new Date(),"1","1",getMapArea());
        String result=eventService.UpdateEvent(token2,"1","2", EventType.PLAY,100,new Date(),"1","1",getMapArea(),0);
        return result.equals("success");

    }
    public boolean UpdateEventFailedNoEventFound17() {
        userService.register("1","1");
        String token=userService.login("1","1");
        userService.register("2","2");
        String token2=userService.login("2","2");
        companyService.CreateCompany("1",token);
        companyService.AppointOwner("2","1",token);
        companyService.ApproveAppointmentForOwner(token2,"1");
//        eventService.CreateEvent(token,"1","1", EventType.PLAY,100,new Date(),"1","1",getMapArea());
        String result=eventService.UpdateEvent(token2,"1","2", EventType.PLAY,100,new Date(),"1","1",getMapArea(),0);
        return result.equals("failed");
    }


    public boolean UpdateEventFailedNotAuthorizedNotPartOfTheCompany18() {
        userService.register("1","1");
        String token=userService.login("1","1");
        userService.register("2","2");
        String token2=userService.login("2","2");
        companyService.CreateCompany("1",token);
//        companyService.AppointOwner("2","1",token);
//        companyService.ApproveAppointmentForOwner(token2,"1");
        eventService.createEvent(token,"1","1", EventType.PLAY,100,new Date(),"1","1",getMapArea());
        String result=eventService.UpdateEvent(token2,"1","2", EventType.PLAY,100,new Date(),"1","1",getMapArea(),0);

        return result.equals("failed");
    }
    public boolean UpdateEventFailedNotAuthorizedNotAprroveTheOwnerShip19() {
        userService.register("1","1");
        String token=userService.login("1","1");
        userService.register("2","2");
        String token2=userService.login("2","2");
        companyService.CreateCompany("1",token);
        companyService.AppointOwner("2","1",token);
//        companyService.ApproveAppointmentForOwner(token2,"1");
        eventService.createEvent(token,"1","1", EventType.PLAY,100,new Date(),"1","1",getMapArea());
        String result=eventService.UpdateEvent(token2,"1","2", EventType.PLAY,100,new Date(),"1","1",getMapArea(),0);

        return result.equals("failed");
    }
    public boolean UpdateEventFailedNotAuthorizedNotHaveTheRightPermission20() {
        userService.register("1","1");
        String token=userService.login("1","1");
        userService.register("2","2");
        String token2=userService.login("2","2");
        companyService.CreateCompany("1",token);
        Set<Permission> permissions = new HashSet<>();
        permissions.add(Permission.CHANGE_POLICIES);
        companyService.AppointAManager("2","1",permissions,token);
        companyService.ApproveAppointmentForManager(token2,"1");
        eventService.createEvent(token,"1","1", EventType.PLAY,100,new Date(),"1","1",getMapArea());
        String result=eventService.UpdateEvent(token2,"1","2", EventType.PLAY,100,new Date(),"1","1",getMapArea(),0);

        return result.equals("failed");
    }
}