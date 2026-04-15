package AcceptanceTest.users;


import AcceptanceTest.users.CompanyManagementTest.CompanyManagementTest;
//import AcceptanceTest.users.visitorTests.UserActionInfo;
import Appliction.CompanyService;
import Appliction.IPasswordEncoder;
import Appliction.UserService;
import Domain.Company.iCompanyRepository;
import Domain.OwnerManagerTree.iTreeOfRoleRepository;
import Domain.User.IUserRepository;
import Infastructure.*;


public class AllTestRun {
//    private UserActionInfo visitorActionTest;
    private CompanyManagementTest companyManagementTest;


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


//        visitorActionTest = new UserActionInfo(userService,initTheSystem);
        companyManagementTest=new CompanyManagementTest(companyService,userService,initTheSystem);

    }
    public void runAllTests() {
        System.out.println("Visitor action test ");
//        String visitorActionTestResults=visitorActionTest.whichTestPass();
//        String visitorActionTestResultsFailed=visitorActionTest.SeeFailTest();
        String CompanyActionTestResults=companyManagementTest.whichTestPass();
        String CompanyActionTestResultsFailed=companyManagementTest.SeeFailTest();


//        System.out.println(visitorActionTestResults);
//        System.out.println(visitorActionTestResultsFailed);
        System.out.println(CompanyActionTestResults);
        System.out.println(CompanyActionTestResultsFailed);


    }
}
