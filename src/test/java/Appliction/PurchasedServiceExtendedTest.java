package Appliction;

import com.ticketing.ticketapp.Appliction.*;
import com.ticketing.ticketapp.Domain.Discount.*;
import com.ticketing.ticketapp.Domain.Order.IActiveOrderRepository;
import com.ticketing.ticketapp.Domain.OwnerManagerTree.Manager;
import com.ticketing.ticketapp.Domain.OwnerManagerTree.Owner;
import com.ticketing.ticketapp.Domain.OwnerManagerTree.Permission;
import com.ticketing.ticketapp.Domain.OwnerManagerTree.iTreeOfRoleRepository;
import com.ticketing.ticketapp.Domain.PurchasedOrderAggregate.PurchaseOrder;
import com.ticketing.ticketapp.Domain.PurchasedOrderAggregate.iPurchasedOrderRepository;
import com.ticketing.ticketapp.Domain.Ticket.Ticket;
import com.ticketing.ticketapp.Domain.Ticket.iTicketRepository;
import com.ticketing.ticketapp.Domain.User.IUserRepository;
import com.ticketing.ticketapp.Domain.User.User;
import com.ticketing.ticketapp.Domain.payment.CreditCardDetails;
import com.ticketing.ticketapp.Infastructure.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PurchasedServiceExtendedTest {

    @Mock private iPurchasedOrderRepository purchasedOrderRepository;
    @Mock private ISupplyService supplyService;
    @Mock private IPaymentService paymentService;
    @Mock private IBarcodeGenerator barcodeGenerator;
    @Mock private IExternalTicketService externalTicketService;
    @Mock private TokenService tokenService;
    @Mock private iTreeOfRoleRepository treeOfRoleRepository;
    @Mock private IUserRepository userRepository;
    @Mock private INotifier notifier;

    private iDiscountPolicyRepository discountPolicyRepository;
    private PurchasedService purchasedService;

    private final String EMAIL = "test@example.com";
    private final String USERNAME = "user1";
    private final String COMPANY = "companyX";
    private final String EVENT = "eventY";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        discountPolicyRepository = spy(new InMemoryDiscountPolicyRepository());

        when(externalTicketService.issueTicket(anyString(), anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn("TIX-test-123");
    }

    private PurchasedService buildService(
            IActiveOrderRepository orderRepo,
            iTicketRepository ticketRepo
    ) {
        return new PurchasedService(
                orderRepo,
                ticketRepo,
                purchasedOrderRepository,
                supplyService,
                paymentService,
                barcodeGenerator,
                tokenService,
                treeOfRoleRepository,
                discountPolicyRepository,
                userRepository,
                notifier,
                externalTicketService
        );
    }

    private CreditCardDetails createCreditCardDetails() {
        return new CreditCardDetails(
                "0000000000000000",
                "12",
                "2030",
                "System Check",
                "000",
                "00000000"
        );
    }

    @Test
    void purchaseTicket_SoldOut_NotifiesOwnersAndManagers() throws Exception {
        TicketRepositoryImpl ticketRepoSpy = spy(new TicketRepositoryImpl());
        OrderRepositoryImpl orderRepoSpy = spy(new OrderRepositoryImpl());

        purchasedService = buildService(orderRepoSpy, ticketRepoSpy);

        ticketRepoSpy.storeTicket(0, 0, EVENT, COMPANY, 100);
        String ticketId = ticketRepoSpy.getTicketsForEvent(COMPANY, EVENT).get(0).getId();

        Date futureDate = new Date(System.currentTimeMillis() + 1_000_000);
        String orderId = orderRepoSpy.store(COMPANY, EVENT, List.of(ticketId), USERNAME, futureDate);

        when(paymentService.processPayment(createCreditCardDetails(), 100.0, "USD"))
                .thenReturn(100);

        when(barcodeGenerator.generateBarcode(anyString(), anyString()))
                .thenReturn("barcode");

        User ownerUser = new User("ownerName", "pass", 40);
        User managerUser = new User("managerName", "pass", 35);

        Owner owner = new Owner("ownerUsername", COMPANY, "root");
        Manager manager = new Manager("managerUsername", COMPANY, Set.of(), "ownerUsername");

        when(treeOfRoleRepository.getAllOwnersByCompany(COMPANY))
                .thenReturn(List.of(owner));

        when(treeOfRoleRepository.getAllManagersByCompany(COMPANY))
                .thenReturn(List.of(manager));

        when(userRepository.getUserByUsername("ownerUsername"))
                .thenReturn(ownerUser);

        when(userRepository.getUserByUsername("managerUsername"))
                .thenReturn(managerUser);

        when(tokenService.validateToken(USERNAME))
                .thenReturn(false);

        purchasedService.PurchaseTicket(
                EMAIL,
                orderId,
                USERNAME,
                "none",
                createCreditCardDetails()
        );

        verify(notifier, times(2)).notifyUser(
                any(),
                eq("Event Sold Out"),
                anyString()
        );
    }

    @Test
    void purchaseTicket_NullUserId_NoPurchaseNotification() throws Exception {
        TicketRepositoryImpl ticketRepoSpy = spy(new TicketRepositoryImpl());
        OrderRepositoryImpl orderRepoSpy = spy(new OrderRepositoryImpl());

        purchasedService = buildService(orderRepoSpy, ticketRepoSpy);

        ticketRepoSpy.storeTicket(0, 0, EVENT, COMPANY, 100);
        String ticketId = ticketRepoSpy.getTicketsForEvent(COMPANY, EVENT).get(0).getId();

        Date futureDate = new Date(System.currentTimeMillis() + 1_000_000);

        String orderId = orderRepoSpy.store(
                COMPANY,
                EVENT,
                List.of(ticketId),
                null,
                futureDate
        );

        when(paymentService.processPayment(createCreditCardDetails(), 100.0, "USD"))
                .thenReturn(100);

        when(barcodeGenerator.generateBarcode(anyString(), anyString()))
                .thenReturn("barcode");

        when(tokenService.validateToken(USERNAME))
                .thenReturn(false);

        purchasedService.PurchaseTicket(
                EMAIL,
                orderId,
                USERNAME,
                "none",
                createCreditCardDetails()
        );

        verify(notifier, never())
                .notifyUser(any(), eq("Purchase Successful"), any());
    }

    @Test
    void getPriceAfterDiscounts_NoPolicies_ReturnsOriginalPrice() {
        purchasedService = buildService(
                new OrderRepositoryImpl(),
                new TicketRepositoryImpl()
        );

        PurchaseContext ctx = new PurchaseContext(1, "NONE", new Date());

        double result = purchasedService.getPriceAfterDiscounts(
                EVENT,
                COMPANY,
                100.0,
                ctx
        );

        assertEquals(100.0, result, 0.001);
    }

    @Test
    void getPriceAfterDiscounts_EventPolicyOnly_AppliesDiscount() {
        discountPolicyRepository.save(
                new DiscountPolicy(
                        "p1",
                        EVENT,
                        DiscountTargetType.EVENT,
                        new CouponDiscount("1", "SAVE10", 10.0)
                )
        );

        purchasedService = buildService(
                new OrderRepositoryImpl(),
                new TicketRepositoryImpl()
        );

        PurchaseContext ctx = new PurchaseContext(1, "SAVE10", new Date());

        double result = purchasedService.getPriceAfterDiscounts(
                EVENT,
                COMPANY,
                100.0,
                ctx
        );

        assertEquals(90.0, result, 0.001);
    }

    @Test
    void getPriceAfterDiscounts_CompanyPolicyOnly_AppliesDiscount() {
        discountPolicyRepository.save(
                new DiscountPolicy(
                        "p2",
                        COMPANY,
                        DiscountTargetType.COMPANY,
                        new CouponDiscount("1", "CORP20", 20.0)
                )
        );

        purchasedService = buildService(
                new OrderRepositoryImpl(),
                new TicketRepositoryImpl()
        );

        PurchaseContext ctx = new PurchaseContext(1, "CORP20", new Date());

        double result = purchasedService.getPriceAfterDiscounts(
                EVENT,
                COMPANY,
                100.0,
                ctx
        );

        assertEquals(80.0, result, 0.001);
    }

    @Test
    void getPriceAfterDiscounts_BothPolicies_MaxDiscountApplied() {
        discountPolicyRepository.save(
                new DiscountPolicy(
                        "p1",
                        EVENT,
                        DiscountTargetType.EVENT,
                        new CouponDiscount("1", "COUPON10", 10.0)
                )
        );

        discountPolicyRepository.save(
                new DiscountPolicy(
                        "p2",
                        COMPANY,
                        DiscountTargetType.COMPANY,
                        new CouponDiscount("2", "CORP30", 30.0)
                )
        );

        purchasedService = buildService(
                new OrderRepositoryImpl(),
                new TicketRepositoryImpl()
        );

        PurchaseContext ctx = new PurchaseContext(1, "CORP30", new Date());

        double result = purchasedService.getPriceAfterDiscounts(
                EVENT,
                COMPANY,
                100.0,
                ctx
        );

        assertEquals(70.0, result, 0.001);
    }

//    @Test
//    void getSubTreeSalesReport_WithSubtreeOrders_ReturnsCorrectReport() {
//        TicketRepositoryImpl ticketRepoSpy = spy(new TicketRepositoryImpl());
//
//        purchasedService = buildService(
//                new OrderRepositoryImpl(),
//                ticketRepoSpy
//        );
//
//        when(tokenService.validateToken(USERNAME))
//                .thenReturn(true);
//
//        when(tokenService.extractUserId(USERNAME))
//                .thenReturn(USERNAME);
//
//        when(treeOfRoleRepository.exitsOwner(USERNAME, COMPANY))
//                .thenReturn(true);
//
//        Owner downlineOwner = new Owner("downlineUser", COMPANY, USERNAME);
//        downlineOwner.acceptAppointment();
//
//        when(treeOfRoleRepository.getAllOwnersByCompany(COMPANY))
//                .thenReturn(List.of(downlineOwner));
//
//        when(treeOfRoleRepository.getAllManagersByCompany(COMPANY))
//                .thenReturn(List.of());
//
//        PurchaseOrder subtreeOrder =
//                new PurchaseOrder(COMPANY, EVENT, List.of("T1"), "downlineUser", "O1");
//
//        PurchaseOrder selfOrder =
//                new PurchaseOrder(COMPANY, EVENT, List.of("T2"), USERNAME, "O2");
//
//        PurchaseOrder otherOrder =
//                new PurchaseOrder(COMPANY, EVENT, List.of("T3"), "stranger", "O3");
//
//        when(purchasedOrderRepository.getPurchasedOrdersForCompany(COMPANY))
//                .thenReturn(List.of(subtreeOrder, selfOrder, otherOrder));
//
//        Ticket t1 = new Ticket(0, 0, EVENT, COMPANY, "T1", 100.0);
//        Ticket t2 = new Ticket(1, 1, EVENT, COMPANY, "T2", 50.0);
//
//        when(ticketRepoSpy.getTickets(List.of("T1")))
//                .thenReturn(List.of(t1));
//
//        when(ticketRepoSpy.getTickets(List.of("T2")))
//                .thenReturn(List.of(t2));
//
//        when(ticketRepoSpy.getTickets(List.of("T3")))
//                .thenReturn(List.of());
//
//        var result = purchasedService.getSubTreeSalesReport(USERNAME, COMPANY);
//
//        assertTrue(result.isSuccess());
//        assertEquals(150.0, result.getData().getTotalRevenue(), 0.001);
//        assertEquals(2, result.getData().getTotalTicketsSold());
//        assertEquals(2, result.getData().getOrders().size());
//    }

//    @Test
//    void getSubTreeSalesReport_AuthorizedManager_WithDownlineOrders_ReturnsReport() {
//        TicketRepositoryImpl ticketRepoSpy = spy(new TicketRepositoryImpl());
//
//        purchasedService = buildService(
//                new OrderRepositoryImpl(),
//                ticketRepoSpy
//        );
//
//        when(tokenService.validateToken(USERNAME))
//                .thenReturn(true);
//
//        when(tokenService.extractUserId(USERNAME))
//                .thenReturn(USERNAME);
//
//        when(treeOfRoleRepository.exitsOwner(USERNAME, COMPANY))
//                .thenReturn(false);
//
//        when(treeOfRoleRepository.ManagerPermitToSeeTransactions(USERNAME, COMPANY))
//                .thenReturn(true);
//
//        Manager downlineMgr = new Manager(
//                "subManager",
//                COMPANY,
//                Set.of(Permission.VIEW_PURCHASE_HISTORY),
//                USERNAME
//        );
//
//        when(treeOfRoleRepository.getAllOwnersByCompany(COMPANY))
//                .thenReturn(List.of());
//
//        when(treeOfRoleRepository.getAllManagersByCompany(COMPANY))
//                .thenReturn(List.of(downlineMgr));
//
//        PurchaseOrder downlineOrder =
//                new PurchaseOrder(COMPANY, EVENT, List.of("T1"), "subManager", "O1");
//
//        when(purchasedOrderRepository.getPurchasedOrdersForCompany(COMPANY))
//                .thenReturn(List.of(downlineOrder));
//
//        Ticket t1 = new Ticket(0, 0, EVENT, COMPANY, "T1", 75.0);
//
//        when(ticketRepoSpy.getTickets(List.of("T1")))
//                .thenReturn(List.of(t1));
//
//        var result = purchasedService.getSubTreeSalesReport(USERNAME, COMPANY);
//
//        assertTrue(result.isSuccess());
//        assertEquals(1, result.getData().getOrders().size());
//        assertEquals(75.0, result.getData().getTotalRevenue(), 0.001);
//    }
}