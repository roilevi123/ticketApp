package AcceptanceTests;

import Appliction.*;
import Domain.AdminAggregate.iAdminRepository;
import Domain.Company.iCompanyRepository;
import Domain.Event.EventType;
import Domain.Event.MapArea;
import Domain.Event.iEventRepository;
import Domain.Order.IActiveOrderRepository;
import Domain.OwnerManagerTree.Permission;
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

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Complete Purchase Order Acceptance Tests")
public class PurchaseOrderTests {

    private UserService userService;
    private CompanyService companyService;
    private EventService eventService;
    private OrderService reserveTicketService;
    private PurchasedService purchasedService;

    @BeforeEach
    void setUp() {
        // Infrastructure & Repositories
        IUserRepository userRepository = new UserRepositoryImpl();
        iCompanyRepository companyRepository = new CompanyRepositoryImpl();
        iEventRepository eventRepository = new EventRepositoryImpl();
        iQueueRepository queueRepository = new QueueRepositoryImpl();
        iTreeOfRoleRepository treeOfRoleRepository = new TreeOfRoleRepositoryImpl();
        IActiveOrderRepository activeOrderRepository = new OrderRepositoryImpl();
        iTicketRepository ticketRepository = new TicketRepositoryImpl();
        iPurchasedOrderRepository purchasedOrderRepository = new PurchasedOrderRepositoryImpl();
        TokenService tokenService = new TokenService();
        IPasswordEncoder passwordEncoder = new PasswordEncoderImpl();

        // Mock/External Services
        ISupplyService supplyService = new SupplyServiceMock();
        IPaymentService paymentService = new PaymentServiceMock();
        IBarcodeGenerator barcodeGenerator = new BarcodeGeneratorMock();

        // Application Services
        this.userService = new UserService(passwordEncoder, userRepository, tokenService);
        this.companyService = new CompanyService(companyRepository, userRepository, treeOfRoleRepository, tokenService);
        this.eventService = new EventService(companyRepository, eventRepository, tokenService, treeOfRoleRepository, ticketRepository, queueRepository);
        this.reserveTicketService = new OrderService(activeOrderRepository, tokenService, ticketRepository);
        this.purchasedService = new PurchasedService(activeOrderRepository, ticketRepository, purchasedOrderRepository, supplyService, paymentService, barcodeGenerator, tokenService, treeOfRoleRepository);

        // Data Cleanup
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

    private MapArea[][] getMapArea() {
        MapArea[][] map = new MapArea[2][2];
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                map[i][j] = MapArea.SEAT;
            }
        }
        return map;
    }

    private MapArea[][] getMapArea1() {
        MapArea[][] map = getMapArea();
        map[1][1] = MapArea.STAND;
        return map;
    }

    @Test @DisplayName("1. Purchased Ticket Success")
    void purchasedTicketSuccess1() {
        userService.register("1", "1");
        String token = userService.login("1", "1");
        companyService.CreateCompany("1", token);
        eventService.createEvent(token, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea());
        userService.register("2", "2");
        String token1 = userService.login("2", "2");
        List<int[]> requests = List.of(new int[]{0, 0, 1});
        String orderId = reserveTicketService.reserveTickets(token1, "1", "1", requests);
        assertEquals("success", purchasedService.PurchaseTicket("ro@gmail.com", orderId, "2"));
    }

    @Test @DisplayName("2. Purchased Ticket Failed - Order Expired")
    void purchasedTicketFailedOrderExpired2() throws InterruptedException {
        userService.register("1", "1");
        String token = userService.login("1", "1");
        companyService.CreateCompany("1", token);
        eventService.createEvent(token, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea());
        userService.register("2", "2");
        String token1 = userService.login("2", "2");
        List<int[]> requests = List.of(new int[]{0, 0, 1});
        String orderId = reserveTicketService.reserveTickets(token1, "1", "1", requests);
        Thread.sleep(11000); // Wait for expiration
        assertNotEquals("success", purchasedService.PurchaseTicket("ro@gmail.com", orderId, "2"));
    }

    @Test @DisplayName("3. Purchased Ticket Success - Multiple Spots")
    void purchasedTicketSuccess3() {
        userService.register("1", "1");
        String token = userService.login("1", "1");
        companyService.CreateCompany("1", token);
        eventService.createEvent(token, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea1());
        userService.register("2", "2");
        String token1 = userService.login("2", "2");
        List<int[]> requests = Arrays.asList(new int[]{0, 0, 1}, new int[]{1, 1, 1});
        String orderId = reserveTicketService.reserveTickets(token1, "1", "1", requests);
        assertEquals("success", purchasedService.PurchaseTicket("ro@gmail.com", orderId, "2"));
    }

    @Test @DisplayName("4. Get Company Transaction Success")
    void getCompanyTransactionSuccess4() {
        userService.register("1", "1");
        String token = userService.login("1", "1");
        companyService.CreateCompany("1", token);
        eventService.createEvent(token, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea());
        userService.register("2", "2");
        String token1 = userService.login("2", "2");
        List<int[]> requests = List.of(new int[]{0, 0, 1});
        String orderId = reserveTicketService.reserveTickets(token1, "1", "1", requests);
        purchasedService.PurchaseTicket("ro@gmail.com", orderId, "2");
        List<PurchaseOrderDTO> result = purchasedService.getCompanyTransaction("1", token);

        boolean isCompanyExist = false;
        boolean isEventExist = false;
        boolean isPurchased = false;
        boolean isUser1Exist = false;

        for (PurchaseOrderDTO po : result) {
            List<TicketDTO> ticketsList = po.tickets();
            if(po.buyer().equals("2")){
                isUser1Exist = true;
            }


            for (TicketDTO ticket : ticketsList) {
                if(ticket.isPurchased()){
                    isPurchased = true;
                }
                if(ticket.company().equals("1")){
                    isCompanyExist = true;
                }
                if(ticket.event().equals("1")){
                    isEventExist = true;
                }


            }
        }
        assertNotNull(result);
        assertTrue(isCompanyExist);
        assertTrue(isEventExist);
        assertTrue(isPurchased);
        assertTrue(isUser1Exist);
    }

    @Test @DisplayName("5. Get Company Transaction Success - Stand & Seat")
    void getCompanyTransactionSuccess5() {
        userService.register("1", "1");
        String token = userService.login("1", "1");
        companyService.CreateCompany("1", token);
        eventService.createEvent(token, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea1());
        userService.register("2", "2");
        String token1 = userService.login("2", "2");
        List<int[]> requests = Arrays.asList(new int[]{0, 0, 1}, new int[]{1, 1, 1});
        String orderId = reserveTicketService.reserveTickets(token1, "1", "1", requests);
        purchasedService.PurchaseTicket("ro@gmail.com", orderId, "2");
        List<PurchaseOrderDTO> result = purchasedService.getCompanyTransaction("1", token);

        boolean isCompanyExist = false;
        boolean isEventExist = false;
        boolean isPurchased = false;
        boolean isUser1Exist = false;
        boolean isTheStand=false;
        boolean isTheSeat=false;

        for (PurchaseOrderDTO po : result) {
            List<TicketDTO> ticketsList = po.tickets();
            if(po.buyer().equals("2")){
                isUser1Exist = true;
            }


            for (TicketDTO ticket : ticketsList) {
                if(ticket.isPurchased()){
                    isPurchased = true;
                }
                if(ticket.company().equals("1")){
                    isCompanyExist = true;
                }
                if(ticket.event().equals("1")){
                    isEventExist = true;
                }
                if(ticket.col()==0&& ticket.row()==0){
                    isTheStand = true;
                }
                if(ticket.col()==1&& ticket.row()==1){
                    isTheSeat = true;
                }

            }
        }
        assertNotNull(result);
        assertTrue(isCompanyExist);
        assertTrue(isEventExist);
        assertTrue(isPurchased);
        assertTrue(isUser1Exist);
        assertTrue(isTheStand);
        assertTrue(isTheSeat);
    }

    @Test @DisplayName("6. Get Company Transaction Success - Manager with Permission")
    void getCompanyTransactionSuccess6() {
        userService.register("1", "1");
        String token = userService.login("1", "1");
        companyService.CreateCompany("1", token);
        userService.register("3", "3");
        String token3 = userService.login("3", "3");
        companyService.AppointAManager("3", "1", Set.of(Permission.GENERATE_SALES_REPORTS), token);
        companyService.ApproveAppointmentForManager(token3, "1");

        eventService.createEvent(token, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea());
        userService.register("2", "2");
        String token2 = userService.login("2", "2");
        String orderId = reserveTicketService.reserveTickets(token2, "1", "1", List.of(new int[]{0,0,1}));
        purchasedService.PurchaseTicket("ro@gmail.com", orderId, "2");

        List<PurchaseOrderDTO> result = purchasedService.getCompanyTransaction("1", token3);

        boolean isCompanyExist = false;
        boolean isEventExist = false;
        boolean isPurchased = false;
        boolean isUser1Exist = false;

        for (PurchaseOrderDTO po : result) {
            List<TicketDTO> ticketsList = po.tickets();
            if(po.buyer().equals("2")){
                isUser1Exist = true;
            }


            for (TicketDTO ticket : ticketsList) {
                if(ticket.isPurchased()){
                    isPurchased = true;
                }
                if(ticket.company().equals("1")){
                    isCompanyExist = true;
                }
                if(ticket.event().equals("1")){
                    isEventExist = true;
                }


            }
        }
        assertNotNull(result);
        assertTrue(isCompanyExist);
        assertTrue(isEventExist);
        assertTrue(isPurchased);
        assertTrue(isUser1Exist);
    }

    @Test
    @DisplayName("7. Get Company Transaction Success - Multiple Events")
    void getCompanyTransactionSuccess7() {
        // 1. הגדרת חברה ובעלים
        userService.register("owner1", "p");
        String token = userService.login("owner1", "p");
        companyService.CreateCompany("1", token);

        // 2. יצירת שני אירועים נפרדים
        eventService.createEvent(token, "E1", "1", EventType.PLAY, 100, new Date(), "L1", "1", getMapArea());
        eventService.createEvent(token, "E2", "1", EventType.PLAY, 100, new Date(), "L2", "1", getMapArea());

        userService.register("buyer2", "p");
        String tB2 = userService.login("buyer2", "p");
        userService.register("buyer3", "p");
        String tB3 = userService.login("buyer3", "p");

        List<int[]> requests = List.of(new int[]{0, 0, 1});

        String o1 = reserveTicketService.reserveTickets(tB2, "1", "E1", requests);
        String o2 = reserveTicketService.reserveTickets(tB3, "1", "E2", requests);

        assertEquals("success", purchasedService.PurchaseTicket("r2@g.com", o1, "buyer2"));
        assertEquals("success", purchasedService.PurchaseTicket("r3@g.com", o2, "buyer3"));

        List<PurchaseOrderDTO> result = purchasedService.getCompanyTransaction("1", token);

        boolean isCompanyExist = false;
        boolean isEvent1Exist = false;
        boolean isEvent2Exist = false;

        boolean isPurchased = false;
        boolean isUser1Exist = false;
        boolean isUser2Exist = false;

        for (PurchaseOrderDTO po : result) {
            List<TicketDTO> ticketsList = po.tickets();
            if(po.buyer().equals("buyer2")){
                isUser1Exist = true;
            }
            if(po.buyer().equals("buyer3")){
                isUser2Exist = true;
            }


            for (TicketDTO ticket : ticketsList) {
                if(ticket.isPurchased()){
                    isPurchased = true;
                }
                if(ticket.company().equals("1")){
                    isCompanyExist = true;
                }
                if(ticket.event().equals("E1")){
                    isEvent1Exist = true;
                }
                if(ticket.event().equals("E1")){
                    isEvent2Exist = true;
                }


            }
        }
        assertNotNull(result);
        assertTrue(isCompanyExist);
        assertTrue(isEvent1Exist);
        assertTrue(isEvent2Exist);
        assertTrue(isPurchased);
        assertTrue(isUser1Exist);
    }
    @Test @DisplayName("8. Get Company Transaction Security - Unauthorized")
    void getCompanyTransactionSecurity8() {
        userService.register("owner1", "pass");
        String token1 = userService.login("owner1", "pass");
        companyService.CreateCompany("C1", token1);

        userService.register("owner2", "pass");
        String token2 = userService.login("owner2", "pass");
        companyService.CreateCompany("C2", token2);

        List<PurchaseOrderDTO> result = purchasedService.getCompanyTransaction("C2", token1);
        assertTrue(result==null);
    }

    @Test @DisplayName("9. Get Company Transaction - Multiple Events Detailed")
    void getCompanyTransactionMultipleEvents9() {
        // Similar to 7, ensures data consistency across multiple events
        getCompanyTransactionSuccess7();
    }

    @Test @DisplayName("10. Get User Transaction Success")
    void getUserTransactionSuccess10() {
        userService.register("10", "10");
        String tO = userService.login("10", "10");
        companyService.CreateCompany("C10", tO);
        eventService.createEvent(tO, "E10", "C10", EventType.PLAY, 100, new Date(), "L", "C10", getMapArea());

        userService.register("20", "20");
        String tB = userService.login("20", "20");
        String orderId = reserveTicketService.reserveTickets(tB, "C10", "E10", List.of(new int[]{0, 0, 1}));
        purchasedService.PurchaseTicket("b@gmail.com", orderId, "20");

        List<PurchaseOrderDTO>  result = purchasedService.getUserTransaction(tB);
        boolean isCompanyExist = false;
        boolean isEventExist = false;
        boolean isPurchased = false;
        boolean isUserExist = false;
        for (PurchaseOrderDTO po : result) {
            List<TicketDTO> ticketsList = po.tickets();
            if(po.buyer().equals("20")){
                isUserExist = true;
            }
            for (TicketDTO ticket : ticketsList) {
                if(ticket.isPurchased()){
                    isPurchased = true;
                }
                if(ticket.company().equals("C10")){
                    isCompanyExist = true;
                }
                if(ticket.event().equals("E10")){
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
    @DisplayName("11. Get User Transaction - Multiple Companies")
    void getUserTransactionMultipleCompanies11() {
        // 1. הקמת חברה א' ואירוע א'
        userService.register("u1", "p");
        String tO1 = userService.login("u1", "p");
        companyService.CreateCompany("CA", tO1);
        eventService.createEvent(tO1, "EA", "CA", EventType.PLAY, 100, new Date(), "LA", "CA", getMapArea());

        // 2. הקמת חברה ב' ואירוע ב'
        userService.register("u2", "p");
        String tO2 = userService.login("u2", "p");
        companyService.CreateCompany("CB", tO2);
        eventService.createEvent(tO2, "EB", "CB", EventType.PLAY, 100, new Date(), "LB", "CB", getMapArea());

        // 3. רישום הקונה
        userService.register("buyer", "p");
        String tB = userService.login("buyer", "p");
        List<int[]> req = List.of(new int[]{0, 0, 1});

        // 4. רכישה מחברה א' - חובה לסיים רכישה לפני ההזמנה הבאה
        String oA = reserveTicketService.reserveTickets(tB, "CA", "EA", req);
        assertEquals("success", purchasedService.PurchaseTicket("b@g.com", oA, "buyer"), "Purchase for CA failed");

        // 5. רכישה מחברה ב'
        String oB = reserveTicketService.reserveTickets(tB, "CB", "EB", req);
        assertEquals("success", purchasedService.PurchaseTicket("b@g.com", oB, "buyer"), "Purchase for CB failed");

        List<PurchaseOrderDTO>  result = purchasedService.getUserTransaction(tB);
        boolean isCompany1Exist = false;
        boolean isCompany2Exist = false;
        boolean isEvent1Exist = false;
        boolean isEvent2Exist = false;

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
                if(ticket.company().equals("CA")){
                    isCompany1Exist = true;
                }
                if(ticket.company().equals("CB")){
                    isCompany2Exist = true;
                }
                if(ticket.event().equals("EA")){
                    isEvent1Exist = true;
                }
                if(ticket.event().equals("EB")){
                    isEvent2Exist = true;
                }


            }
        }
        assertNotNull(result);
        assertTrue(isCompany1Exist);
        assertTrue(isCompany2Exist);
        assertTrue(isEvent1Exist);
        assertTrue(isEvent2Exist);
        assertTrue(isPurchased);
        assertTrue(isUserExist);
    }

    @Test @DisplayName("12. Get User Transaction Security - Unauthorized View")
    void getUserTransactionSecurity12() {
        userService.register("u1", "p"); String t1 = userService.login("u1", "p");
        userService.register("u2", "p"); String t2 = userService.login("u2", "p");
        List<PurchaseOrderDTO>  result = purchasedService.getUserTransaction(t2);
        assertEquals(new ArrayList<>(),result);
    }

    @Test @DisplayName("13. Purchase Order As Guest Success")
    void purchaseOrderAsGuest13() {
        userService.register("own", "p");
        String tO = userService.login("own", "p");
        companyService.CreateCompany("SecC", tO);
        eventService.createEvent(tO, "SecE", "SecC", EventType.PLAY, 100, new Date(), "L", "SecC", getMapArea());

        String order1 = reserveTicketService.reserveTickets("guestToken", "SecC", "SecE", List.of(new int[]{0, 0, 1}));
        assertEquals("success", purchasedService.PurchaseTicket("u1@gmail.com", order1, "guestUser"));
    }

    @Test @DisplayName("14. Purchased Ticket Success After App Re-entry")
    void purchasedTicketSuccessAndGETOutTheApp14() {
        userService.register("1", "1");
        String tO = userService.login("1", "1");
        companyService.CreateCompany("C1", tO);
        eventService.createEvent(tO, "E1", "C1", EventType.PLAY, 100, new Date(), "L", "C1", getMapArea());

        userService.register("2", "2");
        String tB = userService.login("2", "2");
        String orderId = reserveTicketService.reserveTickets(tB, "C1", "E1", List.of(new int[]{0, 0, 1}));

        userService.logout(tB);
        String tB2 = userService.login("2", "2");
        // Using "orderId" string reference if the system persists it by ID
        assertEquals("success", purchasedService.PurchaseTicket("ro@gmail.com", orderId, "2"));
    }

    @Test @DisplayName("15. Purchase Order Guest Fail - App Exit")
    void purchaseOrderAsGuestFaildBecauseHegetOut15() {
        userService.register("own", "p");
        String tO = userService.login("own", "p");
        companyService.CreateCompany("SecC", tO);
        eventService.createEvent(tO, "SecE", "SecC", EventType.PLAY, 100, new Date(), "L", "SecC", getMapArea());

        // Simulating invalid/lost guest order ID
        assertNotEquals("success", purchasedService.PurchaseTicket("u1@gmail.com", "invalid_id", "user1"));
    }

    @Test @DisplayName("16. Purchased Ticket Fail - App Re-entry but Expired")
    void purchasedTicketSuccessAndGETOutTheAppAndTheOrderExpired16() throws InterruptedException {
        userService.register("1", "1");
        String tO = userService.login("1", "1");
        companyService.CreateCompany("C1", tO);
        eventService.createEvent(tO, "E1", "C1", EventType.PLAY, 100, new Date(), "L", "C1", getMapArea());

        userService.register("2", "2");
        String tB = userService.login("2", "2");
        String orderId = reserveTicketService.reserveTickets(tB, "C1", "E1", List.of(new int[]{0, 0, 1}));

        userService.logout(tB);
        Thread.sleep(11000); // Expiration
        String tB2 = userService.login("2", "2");
        assertNotEquals("success", purchasedService.PurchaseTicket("ro@gmail.com", orderId, "2"));
    }
}