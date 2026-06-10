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
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@org.springframework.boot.test.context.SpringBootTest
@org.springframework.test.context.ContextConfiguration(classes = com.ticketing.ticketapp.TicketappApplication.class)
@org.springframework.boot.autoconfigure.domain.EntityScan(basePackages = "com.ticketing.ticketapp")
@org.springframework.data.jpa.repository.config.EnableJpaRepositories(basePackages = "com.ticketing.ticketapp")
@org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase(replace = org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE)
class PurchasedServiceExtendedTest {

    @Mock private iPurchasedOrderRepository purchasedOrderRepository;
    @Mock private ISupplyService supplyService;
    @Mock private IPaymentService paymentService;
    @Mock private IBarcodeGenerator barcodeGenerator;
    @MockBean IExternalTicketService externalTicketService;
    @Mock private TokenService tokenService;
    @Mock private iTreeOfRoleRepository treeOfRoleRepository;
    @Mock private IUserRepository userRepository;
    @Mock private INotifier notifier;

    @org.springframework.beans.factory.annotation.Autowired
    private com.ticketing.ticketapp.Domain.Discount.JpaDiscountPolicyRepository jpaDiscountPolicyRepository;

    private PurchasedService purchasedService;

    private final String EMAIL = "test@example.com";
    private final String USERNAME = "user1";
    private final String COMPANY = "companyX";
    private final String EVENT = "eventY";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        jpaDiscountPolicyRepository.deleteAll();
        when(externalTicketService.issueTicket(anyString(), anyString(), anyString(), anyInt(), anyInt())).thenReturn("TIX-test-123");
    }

    private PurchasedService buildService(IActiveOrderRepository orderRepo,
                                          iTicketRepository ticketRepo,
                                          iDiscountPolicyRepository discountRepo) {
        return new PurchasedService(orderRepo, ticketRepo, purchasedOrderRepository,
                supplyService, paymentService, barcodeGenerator, tokenService,
                treeOfRoleRepository, discountRepo, userRepository, notifier, externalTicketService);
    }

    // ── PurchaseTicket: sold-out → notifies owners and managers ──────────────
    private CreditCardDetails createCreditCardDetails() {
        return      new CreditCardDetails(
                "0000000000000000", // card_number
                "12",               // month
                "2030",             // year
                "System Check",     // holder
                "000",              // cvv
                "00000000"          // id
        );
    }
    @Test
    void purchaseTicket_SoldOut_NotifiesOwnersAndManagers() throws Exception {
        TicketRepositoryImpl ticketRepoSpy = spy(new TicketRepositoryImpl());
        OrderRepositoryImpl orderRepoSpy = spy(new OrderRepositoryImpl());
        purchasedService = buildService(orderRepoSpy, ticketRepoSpy, spy(new com.ticketing.ticketapp.Infastructure.DataBaseInterface.DiscountPolicyRepositoryAdapter(jpaDiscountPolicyRepository)));

        ticketRepoSpy.storeTicket(0, 0, EVENT, COMPANY, 100);
        String ticketId = ticketRepoSpy.getTicketsForEvent(COMPANY, EVENT).get(0).getId();
        Date futureDate = new Date(System.currentTimeMillis() + 1_000_000);
        String orderId = orderRepoSpy.store(COMPANY, EVENT, List.of(ticketId), USERNAME, futureDate);

        when(paymentService.processPayment(createCreditCardDetails(), 100.0,"USD")).thenReturn(100);
        when(barcodeGenerator.generateBarcode(anyString(), anyString())).thenReturn("barcode");

        User ownerUser = new User("ownerName", "pass", 40);
        User managerUser = new User("managerName", "pass", 35);
        Owner owner = new Owner("ownerUsername", COMPANY, "root");
        Manager manager = new Manager("managerUsername", COMPANY, Set.of(), "ownerUsername");

        when(treeOfRoleRepository.getAllOwnersByCompany(COMPANY)).thenReturn(List.of(owner));
        when(treeOfRoleRepository.getAllManagersByCompany(COMPANY)).thenReturn(List.of(manager));
        when(userRepository.getUserByUsername("ownerUsername")).thenReturn(ownerUser);
        when(userRepository.getUserByUsername("managerUsername")).thenReturn(managerUser);

        // token validation fails → order is fetched by orderId (not by userId)
        when(tokenService.validateToken(USERNAME)).thenReturn(false);

        purchasedService.PurchaseTicket(EMAIL, orderId, USERNAME, "none",createCreditCardDetails());

        verify(notifier).notifyUser(eq(ownerUser.getID()), eq("Event Sold Out"), anyString());
        verify(notifier).notifyUser(eq(managerUser.getID()), eq("Event Sold Out"), anyString());
    }

    // ── PurchaseTicket: null userId → no "Purchase Successful" notification ───

    @Test
    void purchaseTicket_NullUserId_NoPurchaseNotification() throws Exception {
        TicketRepositoryImpl ticketRepoSpy = spy(new TicketRepositoryImpl());
        OrderRepositoryImpl orderRepoSpy = spy(new OrderRepositoryImpl());
        purchasedService = buildService(orderRepoSpy, ticketRepoSpy, spy(new com.ticketing.ticketapp.Infastructure.DataBaseInterface.DiscountPolicyRepositoryAdapter(jpaDiscountPolicyRepository)));

        ticketRepoSpy.storeTicket(0, 0, EVENT, COMPANY, 100);
        String ticketId = ticketRepoSpy.getTicketsForEvent(COMPANY, EVENT).get(0).getId();
        Date futureDate = new Date(System.currentTimeMillis() + 1_000_000);
        // Guest order: userId = null
        String orderId = orderRepoSpy.store(COMPANY, EVENT, List.of(ticketId), null, futureDate);

        when(paymentService.processPayment(createCreditCardDetails(), 100.0,"USD")).thenReturn(100);
        when(barcodeGenerator.generateBarcode(anyString(), anyString())).thenReturn("barcode");
        when(tokenService.validateToken(USERNAME)).thenReturn(false);

        purchasedService.PurchaseTicket(EMAIL, orderId, USERNAME, "none",createCreditCardDetails());

        verify(notifier, never()).notifyUser(any(), eq("Purchase Successful"), any());
    }

    // ── getPriceAfterDiscounts ────────────────────────────────────────────────

    @Test
    void getPriceAfterDiscounts_NoPolicies_ReturnsOriginalPrice() {
        purchasedService = buildService(new OrderRepositoryImpl(), new TicketRepositoryImpl(), new com.ticketing.ticketapp.Infastructure.DataBaseInterface.DiscountPolicyRepositoryAdapter(jpaDiscountPolicyRepository));

        PurchaseContext ctx = new PurchaseContext(1, "NONE", new Date());
        double result = purchasedService.getPriceAfterDiscounts(EVENT, COMPANY, 100.0, ctx);

        assertEquals(100.0, result, 0.001);
    }

    @Test
    void getPriceAfterDiscounts_EventPolicyOnly_AppliesDiscount() {
        com.ticketing.ticketapp.Infastructure.DataBaseInterface.DiscountPolicyRepositoryAdapter discountRepo = new com.ticketing.ticketapp.Infastructure.DataBaseInterface.DiscountPolicyRepositoryAdapter(jpaDiscountPolicyRepository);
        discountRepo.save(new DiscountPolicy("p1", EVENT, DiscountTargetType.EVENT,
                new CouponDiscount("1","SAVE10", 10.0)));
        purchasedService = buildService(new OrderRepositoryImpl(), new TicketRepositoryImpl(), discountRepo);

        PurchaseContext ctx = new PurchaseContext(1, "SAVE10", new Date());
        double result = purchasedService.getPriceAfterDiscounts(EVENT, COMPANY, 100.0, ctx);

        assertEquals(90.0, result, 0.001);
    }

    @Test
    void getPriceAfterDiscounts_CompanyPolicyOnly_AppliesDiscount() {
        com.ticketing.ticketapp.Infastructure.DataBaseInterface.DiscountPolicyRepositoryAdapter discountRepo = new com.ticketing.ticketapp.Infastructure.DataBaseInterface.DiscountPolicyRepositoryAdapter(jpaDiscountPolicyRepository);
        discountRepo.save(new DiscountPolicy("p2", COMPANY, DiscountTargetType.COMPANY,
                new CouponDiscount("1","CORP20", 20.0)));
        purchasedService = buildService(new OrderRepositoryImpl(), new TicketRepositoryImpl(), discountRepo);

        PurchaseContext ctx = new PurchaseContext(1, "CORP20", new Date());
        double result = purchasedService.getPriceAfterDiscounts(EVENT, COMPANY, 100.0, ctx);

        assertEquals(80.0, result, 0.001);
    }

    @Test
    void getPriceAfterDiscounts_BothPolicies_MaxDiscountApplied() {
        com.ticketing.ticketapp.Infastructure.DataBaseInterface.DiscountPolicyRepositoryAdapter discountRepo = new com.ticketing.ticketapp.Infastructure.DataBaseInterface.DiscountPolicyRepositoryAdapter(jpaDiscountPolicyRepository);
        discountRepo.save(new DiscountPolicy("p1", EVENT, DiscountTargetType.EVENT,
                new CouponDiscount("1","COUPON10", 10.0)));
        discountRepo.save(new DiscountPolicy("p2", COMPANY, DiscountTargetType.COMPANY,
                new CouponDiscount("2","CORP30", 30.0)));
        purchasedService = buildService(new OrderRepositoryImpl(), new TicketRepositoryImpl(), discountRepo);

        PurchaseContext ctx = new PurchaseContext(1, "CORP30", new Date());
        double result = purchasedService.getPriceAfterDiscounts(EVENT, COMPANY, 100.0, ctx);

        assertEquals(70.0, result, 0.001);
    }

    // ── getSubTreeSalesReport: orders matching subtree ────────────────────────

    @Test
    void getSubTreeSalesReport_WithSubtreeOrders_ReturnsCorrectReport() {
        TicketRepositoryImpl ticketRepoSpy = spy(new TicketRepositoryImpl());
        purchasedService = buildService(new OrderRepositoryImpl(), ticketRepoSpy, new com.ticketing.ticketapp.Infastructure.DataBaseInterface.DiscountPolicyRepositoryAdapter(jpaDiscountPolicyRepository));

        when(tokenService.validateToken(USERNAME)).thenReturn(true);
        when(tokenService.extractUserId(USERNAME)).thenReturn(USERNAME);
        when(treeOfRoleRepository.exitsOwner(USERNAME, COMPANY)).thenReturn(true);

        Owner downlineOwner = new Owner("downlineUser", COMPANY, USERNAME);
        downlineOwner.acceptAppointment();

        when(treeOfRoleRepository.getAllOwnersByCompany(COMPANY)).thenReturn(List.of(downlineOwner));
        when(treeOfRoleRepository.getAllManagersByCompany(COMPANY)).thenReturn(List.of());

        PurchaseOrder subtreeOrder = new PurchaseOrder(COMPANY, EVENT, List.of("T1"), "downlineUser", "O1");
        PurchaseOrder selfOrder    = new PurchaseOrder(COMPANY, EVENT, List.of("T2"), USERNAME, "O2");
        PurchaseOrder otherOrder   = new PurchaseOrder(COMPANY, EVENT, List.of("T3"), "stranger", "O3");

        when(purchasedOrderRepository.getPurchasedOrdersForCompany(COMPANY))
                .thenReturn(List.of(subtreeOrder, selfOrder, otherOrder));

        Ticket t1 = new Ticket(0, 0, EVENT, COMPANY, "T1", 100.0);
        Ticket t2 = new Ticket(1, 1, EVENT, COMPANY, "T2", 50.0);
        when(ticketRepoSpy.getTickets(List.of("T1"))).thenReturn(List.of(t1));
        when(ticketRepoSpy.getTickets(List.of("T2"))).thenReturn(List.of(t2));
        when(ticketRepoSpy.getTickets(List.of("T3"))).thenReturn(List.of());

        var result = purchasedService.getSubTreeSalesReport(USERNAME, COMPANY);

        assertTrue(result.isSuccess());
        assertEquals(150.0, result.getData().getTotalRevenue(), 0.001);
        assertEquals(2, result.getData().getTotalTicketsSold());
        assertEquals(2, result.getData().getOrders().size());
    }

    @Test
    void getSubTreeSalesReport_AuthorizedManager_WithDownlineOrders_ReturnsReport() {
        TicketRepositoryImpl ticketRepoSpy = spy(new TicketRepositoryImpl());
        purchasedService = buildService(new OrderRepositoryImpl(), ticketRepoSpy, new com.ticketing.ticketapp.Infastructure.DataBaseInterface.DiscountPolicyRepositoryAdapter(jpaDiscountPolicyRepository));

        when(tokenService.validateToken(USERNAME)).thenReturn(true);
        when(tokenService.extractUserId(USERNAME)).thenReturn(USERNAME);
        when(treeOfRoleRepository.exitsOwner(USERNAME, COMPANY)).thenReturn(false);
        when(treeOfRoleRepository.ManagerPermitToSeeTransactions(USERNAME, COMPANY)).thenReturn(true);

        Manager downlineMgr = new Manager("subManager", COMPANY,
                Set.of(Permission.VIEW_PURCHASE_HISTORY), USERNAME);

        when(treeOfRoleRepository.getAllOwnersByCompany(COMPANY)).thenReturn(List.of());
        when(treeOfRoleRepository.getAllManagersByCompany(COMPANY)).thenReturn(List.of(downlineMgr));

        PurchaseOrder downlineOrder = new PurchaseOrder(COMPANY, EVENT, List.of("T1"), "subManager", "O1");
        when(purchasedOrderRepository.getPurchasedOrdersForCompany(COMPANY))
                .thenReturn(List.of(downlineOrder));
        Ticket t1 = new Ticket(0, 0, EVENT, COMPANY, "T1", 75.0);
        when(ticketRepoSpy.getTickets(List.of("T1"))).thenReturn(List.of(t1));

        var result = purchasedService.getSubTreeSalesReport(USERNAME, COMPANY);

        assertTrue(result.isSuccess());
        assertEquals(1, result.getData().getOrders().size());
        assertEquals(75.0, result.getData().getTotalRevenue(), 0.001);
    }
}