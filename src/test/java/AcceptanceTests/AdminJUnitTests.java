package AcceptanceTests;

import Appliction.*;
import Domain.AdminAggregate.iAdminRepository;
import Domain.Company.iCompanyRepository;
import Domain.Discount.iDiscountPolicyRepository;
import Domain.Event.EventType;
import Domain.Event.MapArea;
import Domain.Event.iEventRepository;
import Domain.Order.IActiveOrderRepository;
import Domain.OwnerManagerTree.iTreeOfRoleRepository;
import Domain.PurchasedOrderAggregate.PurchaseOrderDTO;
import Domain.PurchasedOrderAggregate.iPurchasedOrderRepository;
import Domain.QueueAggregates.iQueueRepository;
import Domain.Ticket.TicketDTO;
import Domain.Ticket.iTicketRepository;
import Domain.User.*;
import Infastructure.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Admin Management Acceptance Tests")
public class AdminJUnitTests {

    private UserService userService;
    private CompanyService companyService;
    private EventService eventService;
    private OrderService reserveTicketService;
    private PurchasedService purchasedService;
    private AdminService adminService;
    private IUserRepository userRepository;
    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        this.userRepository = new UserRepositoryImpl();
        iCompanyRepository companyRepository = new CompanyRepositoryImpl();
        iEventRepository eventRepository = new EventRepositoryImpl();
        iQueueRepository queueRepository = new QueueRepositoryImpl();
        iTreeOfRoleRepository treeOfRoleRepository = new TreeOfRoleRepositoryImpl();
        IActiveOrderRepository activeOrderRepository = new OrderRepositoryImpl();
        iTicketRepository ticketRepository = new TicketRepositoryImpl();
        iPurchasedOrderRepository purchasedOrderRepository = new PurchasedOrderRepositoryImpl();
        iDiscountPolicyRepository discountPolicyRepository=new InMemoryDiscountPolicyRepository();
        iAdminRepository adminRepository = new AdminRepositoryImpl(){
            @Override
            public boolean isAdmin(String userID) {
                return userID.equals("admin");
            }
        };
        this.tokenService = new TokenService();
        IPasswordEncoder passwordEncoder = new PasswordEncoderImpl();

        ISupplyService supplyService = new SupplyServiceMock();
        IPaymentService paymentService = new PaymentServiceMock();
        IBarcodeGenerator barcodeGenerator = new BarcodeGeneratorMock();

        this.userService = new UserService(passwordEncoder, userRepository, tokenService);
        this.companyService = new CompanyService(companyRepository, userRepository, treeOfRoleRepository, tokenService);

        this.eventService = new EventService(companyRepository, eventRepository, tokenService,
                treeOfRoleRepository, ticketRepository, queueRepository);

        this.reserveTicketService = new OrderService(activeOrderRepository, tokenService, ticketRepository);


        this.purchasedService = new PurchasedService(activeOrderRepository, ticketRepository,
                purchasedOrderRepository, supplyService,
                paymentService, barcodeGenerator,
                tokenService, treeOfRoleRepository, discountPolicyRepository);

        this.adminService = new AdminService(
                treeOfRoleRepository,
                companyRepository,
                adminRepository,
                userRepository,
                purchasedOrderRepository,
                ticketRepository,
                eventRepository,
                tokenService
        );

        userRepository.deleteAll();
        companyRepository.deleteAllCompany();
        eventRepository.deleteAllEvents();
        activeOrderRepository.deleteAllActiveOrders();
        purchasedOrderRepository.deleteAll();
        treeOfRoleRepository.deleteAllRoles();
        ticketRepository.deleteAllTickets();
        queueRepository.deleteAll();
        tokenService.clearAllData();
        adminRepository.deleteAll();
    }

    private String gt() {
        return tokenService.generateGuestToken();
    }

    private void reg(String username, String password) {
        userService.register(gt(), username, password);
    }

    private String log(String username, String password) {
        return userService.login(gt(), username, password);
    }

    private MapArea[][] getMapArea() {
        MapArea[][] map = new MapArea[2][2];
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                map[i][j] = MapArea.SEAT;
            }
        }
        return map;
    }

    @Test
    @DisplayName("1. Close Company Success")
    void closeCompanySuccess1() {
        reg("owner1", "password");
        String token = log("owner1", "password");
        companyService.CreateCompany("C1", token);
        eventService.createEvent(token, "E1", "C1", EventType.PLAY, 100, new Date(), "Loc", "C1", getMapArea());

        String result = adminService.CloseCompany("C1", "admin");

        assertEquals("success", result);
        assertNull(eventService.getCompanyInfo(token, "C1"));
        assertNull(eventService.getCompanyEvents(token, "C1"));
        assertNull(companyService.GetRoleTreeString(token, "C1"));
    }

    @Test
    @DisplayName("2. Close Company Failed - Not Admin")
    void closeCompanyFailedNotAdmin2() {
        reg("owner1", "password");
        String token = log("owner1", "password");
        companyService.CreateCompany("C1", token);

        String result = adminService.CloseCompany("C1", "not_admin");
        assertNotEquals("success", result);
    }

    @Test
    @DisplayName("3. Remove User Success")
    void removeUserSuccess3() {
        reg("userToRemove", "123");
        String userID = (userRepository.getUserByUsername("userToRemove")).getID();
        String result = adminService.removeUser(userID, "admin");
        assertEquals("success", result);

        assertNull(userService.login(gt(), "userToRemove", "123"));
    }

    @Test
    @DisplayName("4. Remove User Failed - Not Admin")
    void removeUserFailedNotAdmin4() {
        reg("user1", "123");
        String result = adminService.removeUser("user1", "not_admin");
        assertNotEquals("success", result);
    }

    @Test
    @DisplayName("5. Get All Purchased Orders Success")
    void getAllPurchasedOrdersSuccess5() {
        reg("owner", "p");
        String tO = log("owner", "p");
        companyService.CreateCompany("C1", tO);
        eventService.createEvent(tO, "E1", "C1", EventType.PLAY, 100, new Date(), "L", "C1", getMapArea());

        reg("buyer", "p");
        String tB = log("buyer", "p");
        String orderId = reserveTicketService.reserveTickets(tB, "C1", "E1", List.of(new int[]{0, 0, 1}));
        purchasedService.PurchaseTicket("b@gmail.com", orderId, "buyer","none");

        List<PurchaseOrderDTO> result = adminService.GetAllPurchasedOrders("admin");
        boolean isCompanyExist = false;
        boolean isEventExist = false;
        boolean isPurchased = false;
        for (PurchaseOrderDTO po : result) {
            List<TicketDTO> ticketsList = po.tickets();
            for (TicketDTO ticket : ticketsList) {
                if(ticket.isPurchased()){
                    isPurchased = true;
                }
                if(ticket.company().equals("C1")){
                    isCompanyExist = true;
                }
                if(ticket.event().equals("E1")){
                    isEventExist = true;
                }
            }
        }
        assertNotNull(result);
        assertTrue(isCompanyExist);
        assertTrue(isEventExist);
        assertTrue(isPurchased);
    }

    @Test
    @DisplayName("6. Get All Purchased Orders Multiple Success")
    void getAllPurchasedOrdersMultipleSuccess6() {
        reg("o1", "p");
        String tO = log("o1", "p");
        companyService.CreateCompany("C1", tO);
        eventService.createEvent(tO, "E1", "C1", EventType.PLAY, 100, new Date(), "L", "C1", getMapArea());

        reg("b1", "p");
        String tB1 = log("b1", "p");
        String o1 = reserveTicketService.reserveTickets(tB1, "C1", "E1", List.of(new int[]{0, 0, 1}));
        purchasedService.PurchaseTicket("b1@gmail.com", o1, "b1","none");

        reg("b2", "p");
        String tB2 = log("b2", "p");
        String o2 = reserveTicketService.reserveTickets(tB2, "C1", "E1", List.of(new int[]{1, 1, 1}));
        purchasedService.PurchaseTicket("b2@gmail.com", o2, "b2","none");

        List<PurchaseOrderDTO> result = adminService.GetAllPurchasedOrders("admin");
        boolean isCompanyExist = false;
        boolean isEventExist = false;
        boolean isPurchased = false;

        for (PurchaseOrderDTO po : result) {
            List<TicketDTO> ticketsList = po.tickets();
            for (TicketDTO ticket : ticketsList) {
                if(ticket.isPurchased()){
                    isPurchased = true;
                }
                if(ticket.company().equals("C1")){
                    isCompanyExist = true;
                }
                if(ticket.event().equals("E1")){
                    isEventExist = true;
                }
            }
        }
        assertNotNull(result);
        assertTrue(isCompanyExist);
        assertTrue(isEventExist);
        assertTrue(isPurchased);
    }

    @Test
    @DisplayName("7. Get All Purchased Orders Failed - Not Admin")
    void getAllPurchasedOrdersFailedNotAdmin7() {
        List<PurchaseOrderDTO> result = adminService.GetAllPurchasedOrders("not_an_admin");
        assertNull(result);
    }

    @Test
    @DisplayName("8. Get All Purchased Orders Two Companies")
    void getAllPurchasedOrdersTwoCompanies8() {
        reg("ownerA", "p");
        String tOA = log("ownerA", "p");
        companyService.CreateCompany("CompA", tOA);
        eventService.createEvent(tOA, "EventA", "CompA", EventType.PLAY, 100, new Date(), "LocA", "CompA", getMapArea());

        reg("ownerB", "p");
        String tOB = log("ownerB", "p");
        companyService.CreateCompany("CompB", tOB);
        eventService.createEvent(tOB, "EventB", "CompB", EventType.PLAY, 100, new Date(), "LocB", "CompB", getMapArea());

        reg("buyer8", "p");
        String tB = log("buyer8", "p");

        String orderA = reserveTicketService.reserveTickets(tB, "CompA", "EventA", List.of(new int[]{0, 0, 1}));
        purchasedService.PurchaseTicket("b@gmail.com", orderA, "buyer8","none");

        String orderB = reserveTicketService.reserveTickets(tB, "CompB", "EventB", List.of(new int[]{0, 0, 1}));
        purchasedService.PurchaseTicket("b@gmail.com", orderB, "buyer8","none");

        List<PurchaseOrderDTO> result = adminService.GetAllPurchasedOrders("admin");
        assertNotNull(result);
        assertEquals(2, result.size());

        boolean sawCompA = false;
        boolean sawCompB = false;
        for (PurchaseOrderDTO po : result) {
            for (TicketDTO ticket : po.tickets()) {
                if (ticket.company().equals("CompA")) sawCompA = true;
                if (ticket.company().equals("CompB")) sawCompB = true;
            }
        }
        assertTrue(sawCompA);
        assertTrue(sawCompB);
    }
}
