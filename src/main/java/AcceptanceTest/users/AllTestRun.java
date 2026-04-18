package AcceptanceTest.users;


import AcceptanceTest.users.CompanyManagementTest.CompanyManagementTest;
import AcceptanceTest.users.EventManagementTest.EventManagementTest;
import AcceptanceTest.users.visitorTests.UserActionInfo;
import Appliction.CompanyService;
import Appliction.EventService;
import Appliction.IPasswordEncoder;
import Appliction.UserService;
import Domain.Company.iCompanyRepository;
import Domain.OwnerManagerTree.iTreeOfRoleRepository;
import Domain.User.IUserRepository;
import Infastructure.*;


public class AllTestRun {
    private UserActionInfo visitorActionTest;
    private CompanyManagementTest companyManagementTest;
    private EventManagementTest eventManagementTest;

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

        visitorActionTest = new UserActionInfo(userService,initTheSystem);
        companyManagementTest=new CompanyManagementTest(companyService,userService,initTheSystem);
        eventManagementTest = new EventManagementTest(userService, eventService, initTheSystem);
        
    }
    public void runAllTests() {
        System.out.println("Visitor action test ");
        String visitorActionTestResults=visitorActionTest.whichTestPass();
        String visitorActionTestResultsFailed=visitorActionTest.SeeFailTest();
        String CompanyActionTestResults=companyManagementTest.whichTestPass();
        String CompanyActionTestResultsFailed=companyManagementTest.SeeFailTest();
        String eventTestResults = eventManagementTest.runAllTests();

        System.out.println(visitorActionTestResults);
        System.out.println(visitorActionTestResultsFailed);
        System.out.println("-------------------------------------------------");
        System.out.println(CompanyActionTestResults);
        System.out.println(CompanyActionTestResultsFailed);
        System.out.println("-------------------------------------------------");
        System.out.println(eventTestResults);
        

    }
}
