package AcceptanceTest.users.visitorTests;

import AcceptanceTest.users.initTheSystem;
import Appliction.UserService;

import java.util.*;
import java.util.function.Supplier;

public class UserActionInfo {
    private final UserService userService;
    private final List<String> failTests = new ArrayList<>();
    private final List<String> passTests = new ArrayList<>();
    private initTheSystem initTheSystem;
    private final Map<String, Supplier<Boolean>> testMap = new LinkedHashMap<>();

    public UserActionInfo(UserService userService,initTheSystem initTheSystem) {
        this.userService = userService;
        this.initTheSystem = initTheSystem;
        initTestMap();
    }

    public UserService getUserService() {
        return userService;
    }

    private void initTestMap() {
        testMap.put("1", this::RegisterSuccess1);
        testMap.put("2", this::RegisterFailInvalidPassword2);
        testMap.put("3", this::RegisterFailAlreadyUserInThisUserName3);
        testMap.put("4", this::LoginSuccess4);
        testMap.put("5", this::LoginFailWrongPassword5);
        testMap.put("6", this::LoginFailInValidPassword6);
        testMap.put("7", this::LoginFailUserNotFound7);
        testMap.put("8", this::LogoutSuccess8);
        testMap.put("9", this::LogoutFailed9);
        testMap.put("10", this::LoginAfterLogoutSuccess10);
        testMap.put("11", this::getUserInfoSuccess11);
        testMap.put("12", this::getUserInfoNotExist12);

    }

    public String whichTestPass() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Visitor action test:\n");

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
        stringBuilder.append("Visitor action test that fail:\n");
        if (failTests.isEmpty()) {
            stringBuilder.append("None!");
        } else {
            stringBuilder.append(String.join(" , ", failTests));
        }
        return stringBuilder.toString();
    }


    public boolean RegisterSuccess1() {
        String result = userService.register("roi", "roilevi");
        return result.equals("success");
    }

    public boolean RegisterFailInvalidPassword2() {
        String result = userService.register("roi", null);
        return result.equals("failed");
    }

    public boolean RegisterFailAlreadyUserInThisUserName3() {
        userService.register("roi", "roilevi");
        String result = userService.register("roi", "roilevi");
        return result.equals("failed");
    }

    public boolean LoginSuccess4() {
        userService.register("roi", "roilevi");
        String result = userService.login("roi", "roilevi");
        return result != null;
    }

    public boolean LoginFailWrongPassword5() {
        userService.register("roi", "roilevi");
        String result = userService.login("roi", "wrong_pass");
        return result == null;
    }

    public boolean LoginFailInValidPassword6() {
        userService.register("roi", "roilevi");
        String result = userService.login("roi", null);
        return result == null;
    }

    public boolean LoginFailUserNotFound7() {
        String result = userService.login("roi", "roilevi");
        return result == null;
    }
    public boolean LogoutSuccess8() {
        userService.register("roi", "roilevi");
         String token=userService.login("roi", "roilevi");
        String result = userService.logout(token);
        return result.equals("success");
    }
    public boolean LogoutFailed9() {
        userService.register("roi", "roilevi");
        String result=userService.logout("token");
        return result.equals("failed");
    }
    public boolean LoginAfterLogoutSuccess10() {
        userService.register("roi", "roilevi");
        String token=userService.login("roi", "roilevi");
        userService.logout(token);
        String result=userService.login("roi", "roilevi");
        return result!=null;
    }
    public boolean getUserInfoSuccess11() {
        userService.register("roi", "roilevi");
        String token=userService.login("roi", "roilevi");
        String result=userService.getUserInfo(token);
        return result.equals("name=roi");
    }
    public boolean getUserInfoNotExist12() {
        String result=userService.getUserInfo("Non Exist User");
        return result==null;
    }




}