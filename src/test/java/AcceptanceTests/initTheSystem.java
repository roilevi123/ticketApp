package AcceptanceTests;

import Appliction.*;
import Domain.AdminAggregate.iAdminRepository;
import Domain.Company.iCompanyRepository;
import Domain.Event.iEventRepository;
import Domain.Order.IActiveOrderRepository;
import Domain.OwnerManagerTree.iTreeOfRoleRepository;
import Domain.PurchasedOrderAggregate.iPurchasedOrderRepository;
import Domain.QueueAggregates.iQueueRepository;
import Domain.Ticket.iTicketRepository;
import Domain.User.IUserRepository;
import Infastructure.*;

public class initTheSystem {
    // Repositories
    public iTreeOfRoleRepository treeOfRoleRepository = new TreeOfRoleRepositoryImpl();
    public iCompanyRepository companyRepository = new CompanyRepositoryImpl();
    public IUserRepository userRepository = new UserRepositoryImpl();
    public IPasswordEncoder passwordEncoder = new PasswordEncoderImpl();
    public TokenService tokenService = new TokenService();
    public IActiveOrderRepository activeOrderRepository = new OrderRepositoryImpl();
    public iTicketRepository ticketRepository = new TicketRepositoryImpl();
    public iEventRepository eventRepository = new EventRepositoryImpl();
    public iQueueRepository queueRepository = new QueueRepositoryImpl();
    public iPurchasedOrderRepository purchasedOrderRepository = new PurchasedOrderRepositoryImpl();
    public iAdminRepository adminRepository = new AdminRepositoryImpl();

    // Services
    public UserService userService;
    public CompanyService companyService;
    public EventService eventService;
    public OrderService orderService;
    public QueueService queueService;
    public PurchasedService purchasedService;
    public AdminService adminService;

    public initTheSystem() {
        ISupplyService supplyService = new SupplyServiceMock();
        IPaymentService paymentService = new PaymentServiceMock();
        IBarcodeGenerator barcodeGenerator = new BarcodeGeneratorMock();

        userService = new UserService(passwordEncoder, userRepository, tokenService);
        companyService = new CompanyService(companyRepository, userRepository, treeOfRoleRepository, tokenService);
        eventService = new EventService(companyRepository, eventRepository, tokenService, treeOfRoleRepository, ticketRepository, queueRepository);
        orderService = new OrderService(activeOrderRepository, tokenService, ticketRepository);
        queueService = new QueueService(queueRepository);
        purchasedService = new PurchasedService(activeOrderRepository, ticketRepository, purchasedOrderRepository, supplyService, paymentService, barcodeGenerator, tokenService, treeOfRoleRepository);
        adminService = new AdminService(treeOfRoleRepository, companyRepository, adminRepository, userRepository, purchasedOrderRepository, ticketRepository, eventRepository);
    }

    public void init() {
        activeOrderRepository.deleteAllActiveOrders();
        eventRepository.deleteAllEvents();
        treeOfRoleRepository.deleteAllRoles();
        companyRepository.deleteAllCompany();
        purchasedOrderRepository.deleteAll();
        queueRepository.deleteAll();
        ticketRepository.deleteAllTickets();
        userRepository.deleteAll();
        tokenService.clearAllData();
    }
}