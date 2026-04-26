package AcceptanceTest.users;


import AcceptanceTest.users.AdminTests.AdminTests;
import AcceptanceTest.users.CompanyManagementTest.CompanyManagementTest;
import AcceptanceTest.users.EventInfo.informationEventsTests;
import AcceptanceTest.users.EventManagementTest.EventManagementTest;
import AcceptanceTest.users.Order.ReseveTicketTests;
import AcceptanceTest.users.PurchasedOrder.PurchaseOrderTests;
import AcceptanceTest.users.WatingQueueTests.WaitingQueueTests;
import AcceptanceTest.users.visitorTests.UserActionInfo;
//<<<<<<< Updated upstream
import Appliction.*;
import Domain.AdminAggregate.iAdminRepository;
import Domain.Company.iCompanyRepository;
import Domain.Domains.*;
import Domain.Event.iEventRepository;
import Domain.Order.IActiveOrderRepository;
import Domain.OwnerManagerTree.iTreeOfRoleRepository;
import Domain.PurchasedOrderAggregate.iPurchasedOrderRepository;
import Domain.QueueAggregates.iQueueRepository;
import Domain.Ticket.iTicketRepository;
import Domain.User.IUserRepository;
import Infastructure.*;


public class AllTestRun {
    private UserActionInfo visitorActionTest;
    private CompanyManagementTest companyManagementTest;
    private EventManagementTest eventManagementTest;

    private WaitingQueueTests waitingQueueTests;
    private ReseveTicketTests reseveTicketTests;
    private informationEventsTests informationEventsTests;
    private PurchaseOrderTests purchaseOrderTests;
    private AdminTests adminTests;
//    private AdminTests adminTests;
    public AllTestRun() {
        ISupplyService supplyService=new SupplyServiceMock();
        IPaymentService paymentService=new PaymentServiceMock();
        IBarcodeGenerator barcodeGenerator=new BarcodeGeneratorMock();
        iTreeOfRoleRepository iTreeOfRoleRepository =new TreeOfRoleRepositoryImpl();
        iCompanyRepository iCompanyRepository =new CompanyRepositoryImpl();
        IUserRepository iUserRepository =new UserRepositoryImpl();
        IPasswordEncoder iPasswordEncoder =new PasswordEncoderImpl();
        TokenService tokenService = new TokenService();
        IActiveOrderRepository activeOrderRepository= new OrderRepositoryImpl();
        iTicketRepository iTicketRepository =new TicketRepositoryImpl();
        iEventRepository iEventRepository =new EventRepositoryImpl();
        iQueueRepository iQueueRepository =new QueueRepositoryImpl();
        iPurchasedOrderRepository iPurchasedOrderRepository =new PurchasedOrderRepositoryImpl();
        iAdminRepository iAdminRepository =new AdminRepositoryImpl();
        initTheSystem initTheSystem=new initTheSystem(iTreeOfRoleRepository,iCompanyRepository,iUserRepository,iPasswordEncoder,tokenService,iTicketRepository,iEventRepository,iQueueRepository,activeOrderRepository,iPurchasedOrderRepository);

        UserDomain userService=new UserDomain(iPasswordEncoder,iUserRepository,tokenService);
        CompanyDomain companyDomain =new CompanyDomain(iCompanyRepository,iUserRepository,iTreeOfRoleRepository,tokenService);
        EventDomain eventDomain = new EventDomain(iCompanyRepository, iEventRepository, tokenService, iTreeOfRoleRepository, iTicketRepository,iQueueRepository);
        OrderDomain orderDomain = new OrderDomain(activeOrderRepository,tokenService,iTicketRepository);
        QueueDomain queueDomain = new QueueDomain(iQueueRepository);
        PurchasedDomain purchasedDomain =new PurchasedDomain(activeOrderRepository,iTicketRepository,iPurchasedOrderRepository,supplyService,paymentService,barcodeGenerator,tokenService,iTreeOfRoleRepository);
        AdminDomain adminDomain =new AdminDomain(iTreeOfRoleRepository,iCompanyRepository,iAdminRepository,iUserRepository,iPurchasedOrderRepository,iTicketRepository,iEventRepository);

        UserService userService1=new UserService(userService);
        CompanyService companyService=new CompanyService(companyDomain);
        EventService eventService=new EventService(eventDomain);
        OrderService orderService=new OrderService(orderDomain);
        QueueService queueService=new QueueService(queueDomain);
        PurchasedSevice purchasedSevice=new PurchasedSevice(purchasedDomain);
        AdminService adminService=new AdminService(adminDomain);

        visitorActionTest = new UserActionInfo(userService1,initTheSystem);
        companyManagementTest=new CompanyManagementTest(companyService,userService1,initTheSystem);
        eventManagementTest = new EventManagementTest(userService1, eventService, initTheSystem, companyService);
        reseveTicketTests= new ReseveTicketTests(userService1, companyService, eventService, orderService, initTheSystem);
        waitingQueueTests=new WaitingQueueTests(userService1, companyService, eventService, queueService,initTheSystem);

        informationEventsTests=new informationEventsTests(userService1, companyService, eventService,initTheSystem);
        purchaseOrderTests=new PurchaseOrderTests(userService1, companyService, eventService, orderService, purchasedSevice,initTheSystem);
        adminTests=new AdminTests(userService1, companyService, eventService, orderService, purchasedSevice, adminService,initTheSystem);



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
        String EventActionTestResults=eventManagementTest.whichTestPass();
        String EventActionTestResultsFailed=eventManagementTest.SeeFailTest();
        String ReseveTicketTestResults=reseveTicketTests.whichTestPass();
        String ReseveTicketTestResultsFailed=reseveTicketTests.SeeFailTest();
        String InformationEventsTestResults=informationEventsTests.whichTestPass();
        String InformationEventsTestResultsFailed=informationEventsTests.SeeFailTest();
        String PurchaseOrderTestResults=purchaseOrderTests.whichTestPass();
        String PurchaseOrderTestResultsFailed=purchaseOrderTests.SeeFailTest();
        String AdminTestResults=adminTests.whichTestPass();
        String AdminTestResultsFailed=adminTests.SeeFailTest();

        System.out.println(visitorActionTestResults);
        System.out.println(visitorActionTestResultsFailed);
        System.out.println("-------------------------------------------------");
        System.out.println(CompanyActionTestResults);
        System.out.println(CompanyActionTestResultsFailed);
        System.out.println("-------------------------------------------------");
        System.out.println(WatingQueueTestResults);
        System.out.println(WatingQueueTestResultsFailed);
        System.out.println("-------------------------------------------------");
        System.out.println(EventActionTestResults);
        System.out.println(EventActionTestResultsFailed);
        System.out.println("-------------------------------------------------");
        System.out.println(ReseveTicketTestResults);
        System.out.println(ReseveTicketTestResultsFailed);

        System.out.println("-------------------------------------------------");
        System.out.println(InformationEventsTestResults);
        System.out.println(InformationEventsTestResultsFailed);
        System.out.println("-------------------------------------------------");
        System.out.println(PurchaseOrderTestResults);
        System.out.println(PurchaseOrderTestResultsFailed);
        System.out.println("-------------------------------------------------");

        System.out.println(AdminTestResults);
        System.out.println(AdminTestResultsFailed);

    }
}
