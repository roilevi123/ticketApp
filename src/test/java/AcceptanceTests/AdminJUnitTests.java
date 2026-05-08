package AcceptanceTests;

import Appliction.*;
import Domain.AdminAggregate.iAdminRepository;
import Domain.Company.iCompanyRepository;
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
import Domain.User.IUserRepository;
import Infastructure.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
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

    @BeforeEach
    void setUp() {
        // --- Infrastructure & Repositories Setup ---
        IUserRepository userRepository = new UserRepositoryImpl();
        iCompanyRepository companyRepository = new CompanyRepositoryImpl();
        iEventRepository eventRepository = new EventRepositoryImpl();
        iQueueRepository queueRepository = new QueueRepositoryImpl();
        iTreeOfRoleRepository treeOfRoleRepository = new TreeOfRoleRepositoryImpl();
        IActiveOrderRepository activeOrderRepository = new OrderRepositoryImpl();
        iTicketRepository ticketRepository = new TicketRepositoryImpl();
        iPurchasedOrderRepository purchasedOrderRepository = new PurchasedOrderRepositoryImpl();

        // הוספת ה-AdminRepository הנדרש על פי ה-Service החדש
        iAdminRepository adminRepository = new AdminRepositoryImpl();

        TokenService tokenService = new TokenService();
        IPasswordEncoder passwordEncoder = new PasswordEncoderImpl();

        // --- Mock/External Services (Required for PurchasedService) ---
        ISupplyService supplyService = new SupplyServiceMock();
        IPaymentService paymentService = new PaymentServiceMock();
        IBarcodeGenerator barcodeGenerator = new BarcodeGeneratorMock();

        // --- Application Services Initialization ---
        this.userService = new UserService(passwordEncoder, userRepository, tokenService);
        this.companyService = new CompanyService(companyRepository, userRepository, treeOfRoleRepository, tokenService);

        this.eventService = new EventService(companyRepository, eventRepository, tokenService,
                treeOfRoleRepository, ticketRepository, queueRepository);

        this.reserveTicketService = new OrderService(activeOrderRepository, tokenService, ticketRepository);

        this.purchasedService = new PurchasedService(activeOrderRepository, ticketRepository,
                purchasedOrderRepository, supplyService,
                paymentService, barcodeGenerator,
                tokenService, treeOfRoleRepository);

        // עדכון אתחול AdminService לפי הקונסטרקטור שסיפקת (7 פרמטרים)
        this.adminService = new AdminService(
                treeOfRoleRepository,   // 1
                companyRepository,      // 2
                adminRepository,        // 3
                userRepository,         // 4
                purchasedOrderRepository, // 5
                ticketRepository,       // 6
                eventRepository         // 7
        );

        // --- Data Cleanup ---
        userRepository.deleteAll();
        companyRepository.deleteAllCompany();
        eventRepository.deleteAllEvents();
        activeOrderRepository.deleteAllActiveOrders();
        purchasedOrderRepository.deleteAll();
        treeOfRoleRepository.deleteAllRoles();
        ticketRepository.deleteAllTickets();
        queueRepository.deleteAll();
        tokenService.clearAllData();
        // הערה: יש לוודא שיש מתודת cleanup גם ל-AdminRepository אם הוא שומר נתונים
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
        userService.register("owner1", "password");
        String token = userService.login("owner1", "password");
        companyService.CreateCompany("C1", token);
        eventService.createEvent(token, "E1", "C1", EventType.PLAY, 100, new Date(), "Loc", "C1", getMapArea());

        // הנחה: בקונסטרוקטור של AdminRepositoryImpl, המילה "admin" מוגדרת כ-Admin מראש
        String result = adminService.CloseCompany("C1", "admin");

        assertEquals("success", result);
        assertNull(eventService.getCompanyInfo("C1"));
        assertNull(eventService.getCompanyEvents("C1"));
        assertNull(companyService.GetRoleTreeString(token, "C1"));
    }

    @Test
    @DisplayName("2. Close Company Failed - Not Admin")
    void closeCompanyFailedNotAdmin2() {
        userService.register("owner1", "password");
        String token = userService.login("owner1", "password");
        companyService.CreateCompany("C1", token);

        String result = adminService.CloseCompany("C1", "wrong_admin_token");
        assertNotEquals("success", result);
    }

    @Test
    @DisplayName("3. Remove User Success")
    void removeUserSuccess3() {
        userService.register("userToRemove", "123");

        String result = adminService.removeUser("userToRemove", "admin");
        assertEquals("success", result);

        // וודוא שהמשתמש נמחק ולא יכול להתחבר
        assertNull(userService.login("userToRemove", "123"));
    }

    @Test
    @DisplayName("4. Remove User Failed - Not Admin")
    void removeUserFailedNotAdmin4() {
        userService.register("user1", "123");
        String result = adminService.removeUser("user1", "not_admin");
        assertNotEquals("success", result);
    }

    @Test
    @DisplayName("5. Get All Purchased Orders Success")
    void getAllPurchasedOrdersSuccess5() {
        userService.register("owner", "p");
        String tO = userService.login("owner", "p");
        companyService.CreateCompany("C1", tO);
        eventService.createEvent(tO, "E1", "C1", EventType.PLAY, 100, new Date(), "L", "C1", getMapArea());

        userService.register("buyer", "p");
        String tB = userService.login("buyer", "p");
        String orderId = reserveTicketService.reserveTickets(tB, "C1", "E1", List.of(new int[]{0, 0, 1}));
        purchasedService.PurchaseTicket("b@gmail.com", orderId, "buyer");

        List<PurchaseOrderDTO> result = adminService.GetAllPurchasedOrders("admin");
        boolean isCompanyExist = false;
        boolean isEventExist = false;
        boolean isPurchased = false;
        boolean isUserExist = false;
        for (PurchaseOrderDTO po : result) {
            List<TicketDTO> ticketsList = po.tickets();
            if(po.buyer().equals("buyer")){
                isUserExist = true;
            }
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
        assertTrue(isUserExist);
    }

    @Test
    @DisplayName("6. Get All Purchased Orders Multiple Success")
    void getAllPurchasedOrdersMultipleSuccess6() {
        userService.register("o1", "p");
        String tO = userService.login("o1", "p");
        companyService.CreateCompany("C1", tO);
        eventService.createEvent(tO, "E1", "C1", EventType.PLAY, 100, new Date(), "L", "C1", getMapArea());

        userService.register("b1", "p");
        String tB1 = userService.login("b1", "p");
        String o1 = reserveTicketService.reserveTickets(tB1, "C1", "E1", List.of(new int[]{0, 0, 1}));
        purchasedService.PurchaseTicket("b1@gmail.com", o1, "b1");

        userService.register("b2", "p");
        String tB2 = userService.login("b2", "p");
        String o2 = reserveTicketService.reserveTickets(tB2, "C1", "E1", List.of(new int[]{1, 1, 1}));
        purchasedService.PurchaseTicket("b2@gmail.com", o2, "b2");

        List<PurchaseOrderDTO> result = adminService.GetAllPurchasedOrders("admin");
        boolean isCompanyExist = false;
        boolean isEventExist = false;
        boolean isPurchased = false;
        boolean isUser1Exist = false;
        boolean isUser2Exist = false;

        for (PurchaseOrderDTO po : result) {
            List<TicketDTO> ticketsList = po.tickets();
            if(po.buyer().equals("b1")){
                isUser1Exist = true;
            }
            if(po.buyer().equals("b1")){
                isUser2Exist = true;
            }

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
        assertTrue(isUser1Exist);
        assertTrue(isUser2Exist);
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
        userService.register("ownerA", "p");
        String tOA = userService.login("ownerA", "p");
        companyService.CreateCompany("CompA", tOA);
        eventService.createEvent(tOA, "EventA", "CompA", EventType.PLAY, 100, new Date(), "LocA", "CompA", getMapArea());

        userService.register("ownerB", "p");
        String tOB = userService.login("ownerB", "p");
        companyService.CreateCompany("CompB", tOB);
        eventService.createEvent(tOB, "EventB", "CompB", EventType.PLAY, 100, new Date(), "LocB", "CompB", getMapArea());

        userService.register("buyer8", "p");
        String tB = userService.login("buyer8", "p");

        String orderA = reserveTicketService.reserveTickets(tB, "CompA", "EventA", List.of(new int[]{0, 0, 1}));
        purchasedService.PurchaseTicket("b@gmail.com", orderA, "buyer8");

        String orderB = reserveTicketService.reserveTickets(tB, "CompB", "EventB", List.of(new int[]{0, 0, 1}));
        purchasedService.PurchaseTicket("b@gmail.com", orderB, "buyer8");

        List<PurchaseOrderDTO> result = adminService.GetAllPurchasedOrders("not_an_admin");
        assertEquals(result,null);
    }
}