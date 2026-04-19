package AcceptanceTest.users;


import AcceptanceTest.users.CompanyManagementTest.CompanyManagementTest;
import AcceptanceTest.users.EventManagementTest.EventManagementTest;
import AcceptanceTest.users.EventManagementTest.ViewEventInfoTest;
import AcceptanceTest.users.OrderManagementTest.ReserveOrderTest;
import AcceptanceTest.users.visitorTests.UserActionInfo;
import Appliction.CompanyService;
import Appliction.EventService;
import Appliction.IPasswordEncoder;
import Appliction.OrderService;
import Appliction.UserService;
import Domain.Company.iCompanyRepository;
import Domain.OwnerManagerTree.iTreeOfRoleRepository;
import Domain.User.IUserRepository;
import Infastructure.*;


public class AllTestRun {
    private UserActionInfo visitorActionTest;
    private CompanyManagementTest companyManagementTest;
    private EventManagementTest eventManagementTest;
    private ViewEventInfoTest viewEventInfoTest;
    private ReserveOrderTest reserveOrderTest;

//    private AdminTests adminTests;
    public AllTestRun() {
        iTreeOfRoleRepository iTreeOfRoleRepository =new TreeOfRoleRepositoryImpl();
        iCompanyRepository iCompanyRepository =new CompanyRepositoryImpl();
        IUserRepository iUserRepository =new UserRepositoryImpl();
        IPasswordEncoder iPasswordEncoder =new PasswordEncoderImpl();
        TokenService tokenService = new TokenService();
        initTheSystem initTheSystem=new initTheSystem(iTreeOfRoleRepository,iCompanyRepository,iUserRepository,iPasswordEncoder,tokenService);


        UserService userService=new UserService(iPasswordEncoder,iUserRepository,tokenService);
        CompanyService companyService=new CompanyService(iCompanyRepository,iUserRepository,iTreeOfRoleRepository,tokenService);
        EventService eventService = new EventService();
        OrderService orderService = new OrderService();

        visitorActionTest = new UserActionInfo(userService,initTheSystem);
        companyManagementTest=new CompanyManagementTest(companyService,userService,initTheSystem);
        eventManagementTest = new EventManagementTest(userService, eventService, initTheSystem);
        viewEventInfoTest = new ViewEventInfoTest(userService, eventService, initTheSystem);
        reserveOrderTest = new ReserveOrderTest(userService, eventService, orderService, initTheSystem);
    }
    public void runAllTests() {
        System.out.println("Visitor action test ");
        String visitorActionTestResults=visitorActionTest.whichTestPass();
        String visitorActionTestResultsFailed=visitorActionTest.SeeFailTest();
        String CompanyActionTestResults=companyManagementTest.whichTestPass();
        String CompanyActionTestResultsFailed=companyManagementTest.SeeFailTest();
        String eventTestResults = eventManagementTest.runAllTests();
        String viewEventTestResults = viewEventInfoTest.runAllTests();
        String reserveOrderTestResults = reserveOrderTest.runAllTests();

        System.out.println(visitorActionTestResults);
        System.out.println(visitorActionTestResultsFailed);
        System.out.println("-------------------------------------------------");
        System.out.println(CompanyActionTestResults);
        System.out.println(CompanyActionTestResultsFailed);
        System.out.println("-------------------------------------------------");
        System.out.println(eventTestResults);
        System.out.println("-------------------------------------------------");
        System.out.println(viewEventTestResults);
        System.out.println("-------------------------------------------------");
        System.out.println(reserveOrderTestResults);
        

    }
}
