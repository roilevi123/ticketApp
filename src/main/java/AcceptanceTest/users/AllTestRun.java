package AcceptanceTest.users;


import AcceptanceTest.users.CompanyManagementTest.CompanyManagementTest;
import AcceptanceTest.users.EventManagementTest.EventManagementTest;
import AcceptanceTest.users.EventManagementTest.ViewEventInfoTest;
import AcceptanceTest.users.OrderManagementTest.ReserveOrderTest;
import AcceptanceTest.users.WatingQueueTests.WaitingQueueTests;
import AcceptanceTest.users.visitorTests.UserActionInfo;
//<<<<<<< Updated upstream
import Appliction.*;
import Domain.Company.iCompanyRepository;
import Domain.Event.iEventRepository;
import Domain.Order.IActiveOrderRepository;
import Domain.OwnerManagerTree.iTreeOfRoleRepository;
import Domain.QueueAggregates.iQueueRepository;
import Domain.Ticket.iTicketRepository;
import Domain.User.IUserRepository;
import Infastructure.*;


public class AllTestRun {
    private UserActionInfo visitorActionTest;
    private CompanyManagementTest companyManagementTest;
    private EventManagementTest eventManagementTest;
    private ViewEventInfoTest viewEventInfoTest;
    private ReserveOrderTest reserveOrderTest;
    private WaitingQueueTests waitingQueueTests;

//    private AdminTests adminTests;
    public AllTestRun() {
        iTreeOfRoleRepository iTreeOfRoleRepository =new TreeOfRoleRepositoryImpl();
        iCompanyRepository iCompanyRepository =new CompanyRepositoryImpl();
        IUserRepository iUserRepository =new UserRepositoryImpl();
        IPasswordEncoder iPasswordEncoder =new PasswordEncoderImpl();
        TokenService tokenService = new TokenService();
        IActiveOrderRepository activeOrderRepository= new OrderRepositoryImpl();
        iTicketRepository iTicketRepository =new TicketRepositoryImpl();
        iEventRepository iEventRepository =new EventRepositoryImpl();
        iQueueRepository iQueueRepository =new QueueRepositoryImpl();
        initTheSystem initTheSystem=new initTheSystem(iTreeOfRoleRepository,iCompanyRepository,iUserRepository,iPasswordEncoder,tokenService,iTicketRepository,iEventRepository,iQueueRepository);

        UserService userService=new UserService(iPasswordEncoder,iUserRepository,tokenService);
        CompanyService companyService=new CompanyService(iCompanyRepository,iUserRepository,iTreeOfRoleRepository,tokenService);
        EventService eventService = new EventService(iCompanyRepository, iEventRepository, tokenService, iTreeOfRoleRepository, iTicketRepository,iQueueRepository);
        OrderService orderService = new OrderService(activeOrderRepository,tokenService,iTicketRepository);
        QueueService queueService = new QueueService(iQueueRepository);

        visitorActionTest = new UserActionInfo(userService,initTheSystem);
        companyManagementTest=new CompanyManagementTest(companyService,userService,initTheSystem);
        eventManagementTest = new EventManagementTest(userService, eventService, initTheSystem);
        viewEventInfoTest = new ViewEventInfoTest(userService, eventService, initTheSystem);
        reserveOrderTest = new ReserveOrderTest(userService, eventService, orderService, initTheSystem);
        waitingQueueTests=new WaitingQueueTests(userService,companyService,eventService,queueService,initTheSystem);

    }
    public void runAllTests() {
        System.out.println("Visitor action test ");
        String visitorActionTestResults=visitorActionTest.whichTestPass();
        String visitorActionTestResultsFailed=visitorActionTest.SeeFailTest();
        String CompanyActionTestResults=companyManagementTest.whichTestPass();
        String CompanyActionTestResultsFailed=companyManagementTest.SeeFailTest();
        String WatingQueueTestResults=waitingQueueTests.whichTestPass();
        String WatingQueueTestResultsFailed=waitingQueueTests.SeeFailTest();
//
//        String eventTestResults = eventManagementTest.runAllTests();
//        String viewEventTestResults = viewEventInfoTest.runAllTests();
//        String reserveOrderTestResults = reserveOrderTest.runAllTests();

        System.out.println(visitorActionTestResults);
        System.out.println(visitorActionTestResultsFailed);
        System.out.println("-------------------------------------------------");
        System.out.println(CompanyActionTestResults);
        System.out.println(CompanyActionTestResultsFailed);
        System.out.println("-------------------------------------------------");
        System.out.println(WatingQueueTestResults);
        System.out.println(WatingQueueTestResultsFailed);
//        System.out.println("-------------------------------------------------");
//        System.out.println(eventTestResults);
//        System.out.println("-------------------------------------------------");
//        System.out.println(viewEventTestResults);
//        System.out.println("-------------------------------------------------");
//        System.out.println(reserveOrderTestResults);
        

    }
}
