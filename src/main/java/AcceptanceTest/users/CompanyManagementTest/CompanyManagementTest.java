package AcceptanceTest.users.CompanyManagementTest;

import AcceptanceTest.users.initTheSystem;
import Appliction.CompanyService;
import Appliction.UserService;
import Domain.OwnerManagerTree.Permission;


import java.util.*;
import java.util.function.Supplier;

public class CompanyManagementTest {
    private CompanyService companyService;
    private UserService userService;
    private final List<String> failTests = new ArrayList<>();
    private final List<String> passTests = new ArrayList<>();
    private final Map<String, Supplier<Boolean>> testMap = new LinkedHashMap<>();
    private initTheSystem initTheSystem;
    public CompanyManagementTest(CompanyService companyService, UserService userService,initTheSystem initTheSystem) {
        this.companyService = companyService;
        this.userService = userService;
        this.initTheSystem = initTheSystem;
        initTestMap();
    }
    public void setUserService(UserService userService) {
        this.userService = userService;
    }
    private void initTestMap() {
        testMap.put("1", this::CreateCompanySuccess1);
        testMap.put("2", this::CreateCompanyFailedUserNotFound2);
        testMap.put("3", this::AppointManagerSuccess3);
        testMap.put("4", this::AppointManagerFailedUserNotFound4);
        testMap.put("5", this::AppointManagerFailedNotOwner5);
        testMap.put("6", this::ApproveManagerRequestSuccess6);
        testMap.put("7", this::ApproveManagerRequestFaildCompanyNotExitst7);
        testMap.put("8", this::RejectManagerRequestSuccess8);
        testMap.put("9", this::RejectManagerRequestFailedNoOfferExist9);
        testMap.put("10", this::AppointOwnerSuccess10);
        testMap.put("11", this::AppointOwnerFailedUserNotFound11);
        testMap.put("12", this::AppointOwnerFailedNotOwner12);
        testMap.put("13", this::ApproveOwnerRequestSuccess13);
        testMap.put("14", this::ApproveOwnerRequestFaildCompanyNotExitst14);
        testMap.put("15", this::RejectOwnerRequestSuccess15);
        testMap.put("16", this::RejectOwnerRequestFailedNoOfferExist16);
        testMap.put("17", this::FireOwnerRequestSuccess17);
        testMap.put("18", this::FireOwnerFailedNotTheAppointer18);
        testMap.put("19", this::FireOwnerFailedCanNotFireTheFounder19);
        testMap.put("20", this::FireOwnerFailedCanNotOwner20);
        testMap.put("21", this::FireManagerRequestSuccess21);
        testMap.put("22", this::FireManagerFailedNotTheAppointer22);
        testMap.put("23", this::FireManagerFailedCanNotManager23);
        testMap.put("24", this::ChangeManagerPermissionsSuccess24);
        testMap.put("25", this::ChangeManagerPermissionsNotAppointer25);
        testMap.put("26", this::ChangeManagerPermissionsNotManager26);
        testMap.put("27", this::FreezeCompanySuccess27);
        testMap.put("28", this::FreezeCompanyNotFounder28);
        testMap.put("29", this::FreezeCompanyFailedAlreadyFreeze29);
        testMap.put("30", this::UnFreezeCompanySuccess30);
        testMap.put("31", this::UnFreezeCompanyNotFounder31);
        testMap.put("32", this::UnFreezeCompanyFailedAlreadyUnFreeze32);
        testMap.put("33", this::GetMangerPermissions33);
        testMap.put("34", this::GetMangerPermissionsNotOwner34);
        testMap.put("35", this::GetTreeOfRolesSuccess35);
        testMap.put("36", this::GetTreeOfRolesSuccess36);
        testMap.put("37", this::GetTreeOfRolesSuccess37);



    }
    public String whichTestPass() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Company action test:\n");

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
        stringBuilder.append("Company action test that fail:\n");
        if (failTests.isEmpty()) {
            stringBuilder.append("None!");
        } else {
            stringBuilder.append(String.join(" , ", failTests));
        }
        return stringBuilder.toString();
    }

    public boolean CreateCompanySuccess1() {
        userService.register("1","1");
        String token=userService.login("1","1");
        String result =companyService.CreateCompany("1",token);
        return result.equals("success");
    }
    public boolean CreateCompanyFailedUserNotFound2() {
//        userService.register("1","1");
//        String token=userService.login("1","1");
        String result =companyService.CreateCompany("1","token");
        return result.equals("failed");
    }
    public boolean AppointManagerSuccess3() {
        userService.register("1","1");
        String token=userService.login("1","1");
        userService.register("2","2");
        String token2=userService.login("2","2");
        companyService.CreateCompany("1",token);
        Set<Permission> permissions = new HashSet<>();
        permissions.add(Permission.MANAGE_INVENTORY);
        String result =companyService.AppointAManager("2","1",permissions,token);
        return result.equals("success");
    }
    public boolean AppointManagerFailedUserNotFound4() {
        userService.register("1","1");
        String token=userService.login("1","1");
        companyService.CreateCompany("1",token);
        Set<Permission> permissions = new HashSet<>();
        permissions.add(Permission.MANAGE_INVENTORY);
        String result =companyService.AppointAManager("2","1",permissions,token);
        return result.equals("failed");
    }
    public boolean AppointManagerFailedNotOwner5() {
        userService.register("1","1");
        String token1=userService.login("1","1");
        userService.register("2","2");
        String token2=userService.login("2","2");
        userService.register("3","3");
        String token3=userService.login("3","3");
        companyService.CreateCompany("1",token1);
        Set<Permission> permissions = new HashSet<>();
        permissions.add(Permission.MANAGE_INVENTORY);
        String result =companyService.AppointAManager("2","1",permissions,token3);
        return result.equals("failed");
    }
    public boolean ApproveManagerRequestSuccess6() {
        userService.register("1","1");
        String token=userService.login("1","1");
        userService.register("2","2");
        String token2=userService.login("2","2");
        companyService.CreateCompany("1",token);
        Set<Permission> permissions = new HashSet<>();
        permissions.add(Permission.MANAGE_INVENTORY);
        companyService.AppointAManager("2","1",permissions,token);
        String result=companyService.ApproveAppointmentForManager(token2,"1");
        return result.equals("success");
    }
    public boolean ApproveManagerRequestFaildCompanyNotExitst7() {
        userService.register("1","1");
        String token=userService.login("1","1");
        userService.register("2","2");
        String token2=userService.login("2","2");
        companyService.CreateCompany("1",token);
        Set<Permission> permissions = new HashSet<>();
        permissions.add(Permission.MANAGE_INVENTORY);
        companyService.AppointAManager("2","1",permissions,token);
        String result=companyService.ApproveAppointmentForManager(token2,"2");
        return result.equals("failed");
    }
    public boolean RejectManagerRequestSuccess8() {
        userService.register("1","1");
        String token=userService.login("1","1");
        userService.register("2","2");
        String token2=userService.login("2","2");
        companyService.CreateCompany("1",token);
        Set<Permission> permissions = new HashSet<>();
        permissions.add(Permission.MANAGE_INVENTORY);
        companyService.AppointAManager("2","1",permissions,token);
        String result=companyService.RejectAppointmentForManager(token2,"1");
        return result.equals("success");
    }
    public boolean RejectManagerRequestFailedNoOfferExist9() {
        userService.register("1","1");
        String token=userService.login("1","1");
        userService.register("2","2");
        String token2=userService.login("2","2");
        companyService.CreateCompany("1",token);
        Set<Permission> permissions = new HashSet<>();
        permissions.add(Permission.MANAGE_INVENTORY);
//        companyService.AppointAManager("2","1",permissions,token);
        String result=companyService.RejectAppointmentForManager(token2,"1");
        return result.equals("failed");
    }


    public boolean AppointOwnerSuccess10() {
        userService.register("1","1");
        String token=userService.login("1","1");
        userService.register("2","2");
        String token2=userService.login("2","2");
        companyService.CreateCompany("1",token);
        String result =companyService.AppointOwner("2","1",token);
        return result.equals("success");
    }

    public boolean AppointOwnerFailedUserNotFound11() {
        userService.register("1","1");
        String token=userService.login("1","1");
        companyService.CreateCompany("1",token);
        String result =companyService.AppointOwner("2","1",token);
        return result.equals("failed");
    }
    public boolean AppointOwnerFailedNotOwner12() {
        userService.register("1","1");
        String token=userService.login("1","1");
        userService.register("2","2");
        String token2=userService.login("2","2");
        userService.register("3","3");
        String token3=userService.login("3","3");
        companyService.CreateCompany("1",token);
        String result =companyService.AppointOwner("2","1",token3);
        return result.equals("failed");
    }
    public boolean ApproveOwnerRequestSuccess13() {
        userService.register("1","1");
        String token=userService.login("1","1");
        userService.register("2","2");
        String token2=userService.login("2","2");
        companyService.CreateCompany("1",token);
        companyService.AppointOwner("2","1",token);
        String result=companyService.ApproveAppointmentForOwner(token2,"1");
        return result.equals("success");
    }
    public boolean ApproveOwnerRequestFaildCompanyNotExitst14() {
        userService.register("1","1");
        String token=userService.login("1","1");
        userService.register("2","2");
        String token2=userService.login("2","2");
        companyService.CreateCompany("1",token);

        companyService.AppointOwner("2","1",token);
        String result=companyService.ApproveAppointmentForOwner(token2,"2");
        return result.equals("failed");
    }
    public boolean RejectOwnerRequestSuccess15() {
        userService.register("1","1");
        String token=userService.login("1","1");
        userService.register("2","2");
        String token2=userService.login("2","2");
        companyService.CreateCompany("1",token);

        companyService.AppointOwner("2","1",token);
        String result=companyService.RejectAppointmentForOwner(token2,"1");
        return result.equals("success");
    }
    public boolean RejectOwnerRequestFailedNoOfferExist16() {
        userService.register("1","1");
        String token=userService.login("1","1");
        userService.register("2","2");
        String token2=userService.login("2","2");
        companyService.CreateCompany("1",token);
        String result=companyService.RejectAppointmentForOwner(token2,"1");
        return result.equals("failed");
    }
    public boolean FireOwnerRequestSuccess17() {
        userService.register("1","1");
        String token=userService.login("1","1");
        userService.register("2","2");
        String token2=userService.login("2","2");
        companyService.CreateCompany("1",token);
        companyService.AppointOwner("2","1",token);
        companyService.ApproveAppointmentForOwner(token2,"1");
        String result=companyService.FireOwner(token,"1","2");
        return result.equals("success");
    }
    public boolean FireOwnerFailedNotTheAppointer18() {
        userService.register("1","1");
        String token1=userService.login("1","1");
        userService.register("2","2");
        String token2=userService.login("2","2");
        userService.register("3","3");
        String token3=userService.login("3","3");
        companyService.CreateCompany("1",token1);
        companyService.AppointOwner("2","1",token1);
        companyService.ApproveAppointmentForOwner(token2,"1");
        companyService.AppointOwner("3","1",token1);
        companyService.ApproveAppointmentForOwner(token3,"1");
        String result=companyService.FireOwner(token2,"1","3");
        return result.equals("failed");
    }
    public boolean FireOwnerFailedCanNotFireTheFounder19() {
        userService.register("1","1");
        String token1=userService.login("1","1");
        userService.register("2","2");
        String token2=userService.login("2","2");
        companyService.CreateCompany("1",token1);
        companyService.AppointOwner("2","1",token1);
        companyService.ApproveAppointmentForOwner(token2,"1");
        String result=companyService.FireOwner(token2,"1","1");
        return result.equals("failed");
    }
    public boolean FireOwnerFailedCanNotOwner20() {
        userService.register("1","1");
        String token=userService.login("1","1");
        userService.register("2","2");
        String token2=userService.login("2","2");
        companyService.CreateCompany("1",token);
        companyService.AppointOwner("2","1",token);
//        companyService.ApproveAppointmentForOwner(token2,"1");
        String result=companyService.FireOwner(token,"1","2");
        return result.equals("failed");
    }

    public boolean FireManagerRequestSuccess21() {
        userService.register("1","1");
        String token=userService.login("1","1");
        userService.register("2","2");
        String token2=userService.login("2","2");
        companyService.CreateCompany("1",token);
        Set<Permission> permissions=new HashSet<>();
        permissions.add(Permission.MANAGE_INVENTORY);
        companyService.AppointAManager("2","1",permissions,token);
        companyService.ApproveAppointmentForManager(token2,"1");
        String result=companyService.FireManager(token,"1","2");
        return result.equals("success");
    }
    public boolean FireManagerFailedNotTheAppointer22() {
        userService.register("1","1");
        String token1=userService.login("1","1");
        userService.register("2","2");
        String token2=userService.login("2","2");
        userService.register("3","3");
        String token3=userService.login("3","3");
        companyService.CreateCompany("1",token1);
        companyService.AppointOwner("2","1",token1);
        companyService.ApproveAppointmentForOwner(token2,"1");
        Set<Permission> permissions=new HashSet<>();
        permissions.add(Permission.MANAGE_INVENTORY);
        companyService.AppointAManager("3","1",permissions,token1);
        companyService.ApproveAppointmentForManager(token3,"1");

        String result=companyService.FireManager(token2,"1","3");
        return result.equals("failed");
    }
    public boolean FireManagerFailedCanNotManager23() {
        userService.register("1","1");
        String token=userService.login("1","1");
        userService.register("2","2");
        String token2=userService.login("2","2");
        companyService.CreateCompany("1",token);
        Set<Permission> permissions=new HashSet<>();
        permissions.add(Permission.MANAGE_INVENTORY);
        companyService.AppointAManager("2","1",permissions,token);
//        companyService.ApproveAppointmentForManager(token2,"1");
        String result=companyService.FireManager(token,"1","2");
        return result.equals("failed");
    }
    public boolean ChangeManagerPermissionsSuccess24() {
        userService.register("1","1");
        String token=userService.login("1","1");
        userService.register("2","2");
        String token2=userService.login("2","2");
        companyService.CreateCompany("1",token);
        Set<Permission> permissions=new HashSet<>();
        permissions.add(Permission.MANAGE_INVENTORY);
        companyService.AppointAManager("2","1",permissions,token);
        companyService.ApproveAppointmentForManager(token2,"1");
        Set<Permission> permissions1=new HashSet<>();
        permissions.add(Permission.CHANGE_POLICIES);
        String result=companyService.ChangeManagerPermissions(token,"1","2",permissions1);
        return result.equals("success");
    }
    public boolean ChangeManagerPermissionsNotAppointer25() {
        userService.register("1","1");
        String token1=userService.login("1","1");
        userService.register("2","2");
        String token2=userService.login("2","2");
        userService.register("3","3");
        String token3=userService.login("3","3");
        companyService.CreateCompany("1",token1);
        companyService.AppointOwner("2","1",token1);
        companyService.ApproveAppointmentForOwner(token2,"1");
        Set<Permission> permissions=new HashSet<>();
        permissions.add(Permission.MANAGE_INVENTORY);
        companyService.AppointAManager("3","1",permissions,token1);
        companyService.ApproveAppointmentForManager(token3,"1");
        Set<Permission> permissions1=new HashSet<>();
        permissions.add(Permission.CHANGE_POLICIES);
        String result=companyService.ChangeManagerPermissions(token2,"1","3",permissions1);
        return result.equals("failed");
    }
    public boolean ChangeManagerPermissionsNotManager26() {
        userService.register("1","1");
        String token=userService.login("1","1");
        userService.register("2","2");
        String token2=userService.login("2","2");
        companyService.CreateCompany("1",token);
        Set<Permission> permissions=new HashSet<>();
        permissions.add(Permission.MANAGE_INVENTORY);
        companyService.AppointAManager("2","1",permissions,token);
//        companyService.ApproveAppointmentForManager(token2,"1");
        Set<Permission> permissions1=new HashSet<>();
        permissions.add(Permission.CHANGE_POLICIES);
        String result=companyService.ChangeManagerPermissions(token,"1","2",permissions1);
        return result.equals("failed");
    }
    public boolean FreezeCompanySuccess27() {
        userService.register("1","1");
        String token=userService.login("1","1");
        companyService.CreateCompany("1",token);
        String res=companyService.freezeCompany("1",token);
        return res.equals("success");
    }
    public boolean FreezeCompanyNotFounder28() {
        userService.register("1","1");
        String token=userService.login("1","1");
        userService.register("2","2");
        String token2=userService.login("2","2");
        companyService.CreateCompany("1",token);
        companyService.AppointOwner("2","1",token);
        companyService.ApproveAppointmentForOwner(token2,"1");
        String res=companyService.freezeCompany("1",token2);
        return res.equals("failed");
    }
    public boolean FreezeCompanyFailedAlreadyFreeze29() {
        userService.register("1","1");
        String token=userService.login("1","1");
        companyService.CreateCompany("1",token);
        companyService.freezeCompany("1",token);
        String res=companyService.freezeCompany("1",token);
        return res.equals("failed");
    }
    public boolean UnFreezeCompanySuccess30() {
        userService.register("1","1");
        String token=userService.login("1","1");
        companyService.CreateCompany("1",token);
        companyService.freezeCompany("1",token);
        String res=companyService.unfreezeCompany("1",token);
        return res.equals("success");
    }
    public boolean UnFreezeCompanyNotFounder31() {
        userService.register("1","1");
        String token=userService.login("1","1");
        userService.register("2","2");
        String token2=userService.login("2","2");
        companyService.CreateCompany("1",token);
        companyService.AppointOwner("2","1",token);
        companyService.ApproveAppointmentForOwner(token2,"1");
        companyService.freezeCompany("1",token);
        String res=companyService.unfreezeCompany("1",token2);
        return res.equals("failed");
    }
    public boolean UnFreezeCompanyFailedAlreadyUnFreeze32() {
        userService.register("1","1");
        String token=userService.login("1","1");
        companyService.CreateCompany("1",token);
        String res=companyService.unfreezeCompany("1",token);
        return res.equals("failed");
    }
    public boolean GetMangerPermissions33() {
        userService.register("1","1");
        String token=userService.login("1","1");
        userService.register("2","2");
        String token2=userService.login("2","2");
        companyService.CreateCompany("1",token);
        Set<Permission> permissions = new HashSet<>();
        permissions.add(Permission.MANAGE_INVENTORY);
        companyService.AppointAManager("2","1",permissions,token);
        Set<Permission> list=companyService.GetManagerPermissions(token,"1","2");
        return list.contains(Permission.MANAGE_INVENTORY)&&list.size()==1;
    }
    public boolean GetMangerPermissionsNotOwner34() {
        userService.register("1","1");
        String token=userService.login("1","1");
        userService.register("2","2");
        String token2=userService.login("2","2");
        userService.register("3","3");
        String token3=userService.login("3","3");
        companyService.CreateCompany("1",token);
        Set<Permission> permissions = new HashSet<>();
        permissions.add(Permission.MANAGE_INVENTORY);
        companyService.AppointAManager("2","1",permissions,token);
        companyService.ApproveAppointmentForManager(token2,"1");
        Set<Permission> list=companyService.GetManagerPermissions(token3,"1","2");
        return list==null;
    }
    public boolean GetTreeOfRolesSuccess35() {
        userService.register("1","1");
        String token=userService.login("1","1");
        userService.register("2","2");
        String token2=userService.login("2","2");
        userService.register("3","3");
        String token3=userService.login("3","3");
        companyService.CreateCompany("1",token);
        Set<Permission> permissions = new HashSet<>();
        permissions.add(Permission.MANAGE_INVENTORY);
        companyService.AppointAManager("2","1",permissions,token);
        companyService.ApproveAppointmentForManager(token2,"1");
        companyService.AppointOwner("3","1",token);
        companyService.ApproveAppointmentForOwner(token3,"1");
        String treeOfRoles=companyService.GetRoleTreeString(token,"1");
        String expectedTree =
                "|-- 1 (Owner)\n" +
                        "  |-- 3 (Owner)\n" +
                        "  |-- 2 (Manager)\n";
        return treeOfRoles.equals(expectedTree);
    }
    public boolean GetTreeOfRolesSuccess36() {
        userService.register("1","1");
        String token=userService.login("1","1");
        userService.register("2","2");
        String token2=userService.login("2","2");
        userService.register("3","3");
        String token3=userService.login("3","3");
        userService.register("4","4");
        String token4=userService.login("4","4");
        userService.register("5","5");
        String token5=userService.login("5","5");
        companyService.CreateCompany("1",token);
        Set<Permission> permissions = new HashSet<>();
        permissions.add(Permission.MANAGE_INVENTORY);
        companyService.AppointAManager("2","1",permissions,token);
        companyService.ApproveAppointmentForManager(token2,"1");
        companyService.AppointOwner("3","1",token);
        companyService.ApproveAppointmentForOwner(token3,"1");

        companyService.AppointAManager("4","1",permissions,token3);
        companyService.ApproveAppointmentForManager(token4,"1");
        companyService.AppointOwner("5","1",token3);
        companyService.ApproveAppointmentForOwner(token5,"1");

        String treeOfRoles=companyService.GetRoleTreeString(token,"1");
        String expectedTree =
                "|-- 1 (Owner)\n" +
                        "  |-- 3 (Owner)\n" +
                        "    |-- 5 (Owner)\n" +
                        "    |-- 4 (Manager)\n" +
                        "  |-- 2 (Manager)\n";
        return treeOfRoles.equals(expectedTree);
    }
    public boolean GetTreeOfRolesSuccess37() {
        userService.register("1","1");
        String token=userService.login("1","1");
        userService.register("2","2");
        String token2=userService.login("2","2");
        userService.register("3","3");
        String token3=userService.login("3","3");
        userService.register("4","4");
        String token4=userService.login("4","4");
        userService.register("5","5");
        String token5=userService.login("5","5");
        companyService.CreateCompany("1",token);
        Set<Permission> permissions = new HashSet<>();
        permissions.add(Permission.MANAGE_INVENTORY);
        companyService.AppointAManager("2","1",permissions,token);
        companyService.ApproveAppointmentForManager(token2,"1");
        companyService.AppointOwner("3","1",token);
        companyService.ApproveAppointmentForOwner(token3,"1");

        companyService.AppointAManager("4","1",permissions,token3);
        companyService.ApproveAppointmentForManager(token4,"1");
        companyService.AppointOwner("5","1",token3);
        companyService.ApproveAppointmentForOwner(token5,"1");

        String res =companyService.FireManager(token3,"1","4");
        System.out.println(res);
        String treeOfRoles=companyService.GetRoleTreeString(token,"1");
        System.out.println(treeOfRoles);
        String expectedTree =
                "|-- 1 (Owner)\n" +
                        "  |-- 3 (Owner)\n" +
                        "    |-- 5 (Owner)\n" +
//                        "    |-- 4 (Manager)\n" +
                        "  |-- 2 (Manager)\n";
        return treeOfRoles.equals(expectedTree);
    }



}
