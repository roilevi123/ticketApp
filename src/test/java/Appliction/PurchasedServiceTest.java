package Appliction;

import com.ticketing.ticketapp.Appliction.*;

import com.ticketing.ticketapp.Domain.Discount.iDiscountPolicyRepository;
import com.ticketing.ticketapp.Domain.Order.ActiveOrder;
import com.ticketing.ticketapp.Domain.Order.IActiveOrderRepository;
import com.ticketing.ticketapp.Domain.OwnerManagerTree.iTreeOfRoleRepository;
import com.ticketing.ticketapp.Domain.PurchasedOrderAggregate.PurchaseOrder;
import com.ticketing.ticketapp.Domain.PurchasedOrderAggregate.PurchaseOrderDTO;
import com.ticketing.ticketapp.Domain.PurchasedOrderAggregate.PurchaseOrderException;
import com.ticketing.ticketapp.Infastructure.ExternalServiceException;
import com.ticketing.ticketapp.Domain.PurchasedOrderAggregate.iPurchasedOrderRepository;

import com.ticketing.ticketapp.Domain.Ticket.Ticket;
import com.ticketing.ticketapp.Domain.Ticket.iTicketRepository;
import com.ticketing.ticketapp.Domain.payment.CreditCardDetails;
import com.ticketing.ticketapp.Infastructure.InMemoryDiscountPolicyRepository;
import com.ticketing.ticketapp.Infastructure.OrderRepositoryImpl;
import com.ticketing.ticketapp.Infastructure.TicketRepositoryImpl;
import com.ticketing.ticketapp.Infastructure.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@org.springframework.boot.test.context.SpringBootTest
@org.springframework.test.context.ContextConfiguration(classes = com.ticketing.ticketapp.TicketappApplication.class)
@org.springframework.boot.autoconfigure.domain.EntityScan(basePackages = "com.ticketing.ticketapp")
@org.springframework.data.jpa.repository.config.EnableJpaRepositories(basePackages = "com.ticketing.ticketapp")
@org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase(replace = org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE)
class PurchasedServiceTest {

    @Mock
    private IActiveOrderRepository orderRepository;
    @Mock
    private iTicketRepository ticketRepository;
    @Mock
    private iPurchasedOrderRepository purchasedOrderRepository;
    @Mock
    private ISupplyService supplyService;
    @Mock
    private IPaymentService paymentService;
    @Mock
    private IBarcodeGenerator barcodeGenerator;
    @Mock
    private TokenService tokenService;
    @Mock
    private iTreeOfRoleRepository treeOfRoleRepository;
    @Mock
    private com.ticketing.ticketapp.Domain.User.IUserRepository userRepository;
    @Mock
    private INotifier notifier;
    @MockBean
    IExternalTicketService externalTicketService;

    @org.springframework.beans.factory.annotation.Autowired
    private com.ticketing.ticketapp.Domain.Discount.JpaDiscountPolicyRepository jpaDiscountPolicyRepository;

    @InjectMocks
    private PurchasedService purchasedService;

    private final String EMAIL = "test@example.com";
    private final String ORDER_ID = "ord123";
    private final String USERNAME = "user1";
    private final String COMPANY = "companyX";
    private final String EVENT = "eventY";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        jpaDiscountPolicyRepository.deleteAll();
    }


    @Test
    void purchaseTicket_Success_WithSpyAndStateCheck() throws Exception {
        iTicketRepository ticketRepoSpy = spy(new TicketRepositoryImpl());
        IActiveOrderRepository orderRepoSpy = spy(new OrderRepositoryImpl());
        iDiscountPolicyRepository discountPolicyRepository = spy(new com.ticketing.ticketapp.Infastructure.DataBaseInterface.DiscountPolicyRepositoryAdapter(jpaDiscountPolicyRepository));

        purchasedService = new PurchasedService(
                orderRepoSpy,
                ticketRepoSpy,
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

        Date futureDate = new Date(System.currentTimeMillis() + 1000000);

        ticketRepoSpy.storeTicket(0, 0, EVENT, COMPANY, 100);
        Ticket originalTicket = ticketRepoSpy.getTicketsForEvent(COMPANY, EVENT).get(0);
        String ticketId = originalTicket.getId();

        String orderId = orderRepoSpy.store(COMPANY, EVENT, List.of(ticketId), USERNAME, futureDate);

        when(paymentService.processPayment(createCreditCardDetails(), 100.0,"USD")).thenReturn(100);
        when(externalTicketService.issueTicket(anyString(), anyString(), anyString(), anyInt(), anyInt())).thenReturn("TIX-test-123");

        purchasedService.PurchaseTicket(EMAIL, orderId, USERNAME, "none",createCreditCardDetails());

        Ticket ticketAfterPurchase = ticketRepoSpy.getTicketById(ticketId);

        assertNotNull(ticketAfterPurchase);
        assertTrue(ticketAfterPurchase.isPurchased());
        assertEquals(2, ticketAfterPurchase.getVersion());

        ActiveOrder activeOrderAfter = orderRepoSpy.getOrder(USERNAME);
        assertNull(activeOrderAfter);

        verify(purchasedOrderRepository).StorePurchasedOrder(
                eq(COMPANY),
                eq(EVENT),
                anyList(),
                eq(USERNAME),
                eq(orderId),
                anyList()
        );

        verify(orderRepoSpy).delete(orderId);
    }
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
    void purchaseTicket_Failure_PaymentDeclined() throws Exception {
        iTicketRepository ticketRepoSpy = spy(new TicketRepositoryImpl());
        IActiveOrderRepository orderRepoSpy = spy(new OrderRepositoryImpl());
        iDiscountPolicyRepository discountPolicyRepository = spy(new com.ticketing.ticketapp.Infastructure.DataBaseInterface.DiscountPolicyRepositoryAdapter(jpaDiscountPolicyRepository));

        purchasedService = new PurchasedService(
                orderRepoSpy,
                ticketRepoSpy,
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

        Date futureDate = new Date(System.currentTimeMillis() + 1000000);
        ticketRepoSpy.storeTicket(0, 0, EVENT, COMPANY, 100);
        String ticketId = ticketRepoSpy.getTicketsForEvent(COMPANY, EVENT).get(0).getId();
        String orderId = orderRepoSpy.store(COMPANY, EVENT, List.of(ticketId), USERNAME, futureDate);

        doThrow(new ExternalServiceException("Payment service returned error: -1"))
                .when(paymentService).processPayment(createCreditCardDetails(), 100.0, "USD");

        PurchaseOrderException exception = assertThrows(PurchaseOrderException.class, () -> {
            purchasedService.PurchaseTicket(EMAIL, orderId, USERNAME, "none",createCreditCardDetails());
        });
        assertEquals("External service error: Payment service returned error: -1", exception.getMessage());
        verify(paymentService).processPayment(createCreditCardDetails(), 100.0,"USD");
        verify(supplyService, never()).supplyToEmail(anyString(), anyString());

        Ticket ticketAfterFailedPurchase = ticketRepoSpy.getTicketById(ticketId);
        assertFalse(ticketAfterFailedPurchase.isPurchased());
        assertNotNull(orderRepoSpy.getOrder(USERNAME));
        verify(orderRepoSpy, never()).delete(anyString());
    }

    @Test
    void purchaseTicket_Failure_OrderExpired() throws Exception {
        iTicketRepository ticketRepoSpy = spy(new TicketRepositoryImpl());
        IActiveOrderRepository orderRepoSpy = spy(new OrderRepositoryImpl());
        iDiscountPolicyRepository discountPolicyRepository = spy(new com.ticketing.ticketapp.Infastructure.DataBaseInterface.DiscountPolicyRepositoryAdapter(jpaDiscountPolicyRepository));

        purchasedService = new PurchasedService(
                orderRepoSpy,
                ticketRepoSpy,
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

        Date pastDate = new Date(System.currentTimeMillis() - 1000000);
        ticketRepoSpy.storeTicket(0, 0, EVENT, COMPANY, 100);
        String ticketId = ticketRepoSpy.getTicketsForEvent(COMPANY, EVENT).get(0).getId();
        String orderId = orderRepoSpy.store(COMPANY, EVENT, List.of(ticketId), USERNAME, pastDate);

        PurchaseOrderException exception = assertThrows(PurchaseOrderException.class, () -> {
            purchasedService.PurchaseTicket(EMAIL, orderId, USERNAME, "none",createCreditCardDetails());
        });

        assertEquals("Order expired or not found", exception.getMessage());
        verify(paymentService, never()).processPayment(any(CreditCardDetails.class), anyDouble(),eq("USD"));
        assertNotNull(orderRepoSpy.getOrder(USERNAME));
        verify(orderRepoSpy, never()).delete(anyString());
    }

    @Test
    void purchaseTicket_Failure_OrderNotExist() throws Exception {
        iTicketRepository ticketRepoSpy = spy(new TicketRepositoryImpl());
        IActiveOrderRepository orderRepoSpy = spy(new OrderRepositoryImpl());
        iDiscountPolicyRepository discountPolicyRepository = spy(new com.ticketing.ticketapp.Infastructure.DataBaseInterface.DiscountPolicyRepositoryAdapter(jpaDiscountPolicyRepository));

        purchasedService = new PurchasedService(
                orderRepoSpy,
                ticketRepoSpy,
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

        Date pastDate = new Date(System.currentTimeMillis() - 1000000);
        ticketRepoSpy.storeTicket(0, 0, EVENT, COMPANY, 100);
        String ticketId = ticketRepoSpy.getTicketsForEvent(COMPANY, EVENT).get(0).getId();
        String orderId = orderRepoSpy.store(COMPANY, EVENT, List.of(ticketId), USERNAME, pastDate);

        PurchaseOrderException exception = assertThrows(PurchaseOrderException.class, () -> {
            purchasedService.PurchaseTicket(EMAIL, "orderId", USERNAME, "none",createCreditCardDetails());
        });
        assertEquals("Order expired or not found", exception.getMessage());

        verify(paymentService, never()).processPayment(any(CreditCardDetails.class), anyDouble(),eq("USD"));
        assertNotNull(orderRepoSpy.findById(orderId));
        verify(orderRepoSpy, never()).delete(anyString());
    }

    @Test
    void purchaseTicket_RefundOnSupplyFailure() throws Exception {
        iTicketRepository ticketRepoSpy = spy(new TicketRepositoryImpl());
        IActiveOrderRepository orderRepoSpy = spy(new OrderRepositoryImpl());
        iDiscountPolicyRepository discountPolicyRepository = spy(new com.ticketing.ticketapp.Infastructure.DataBaseInterface.DiscountPolicyRepositoryAdapter(jpaDiscountPolicyRepository));

        purchasedService = new PurchasedService(
                orderRepoSpy,
                ticketRepoSpy,
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

        Date futureDate = new Date(System.currentTimeMillis() + 1000000);
        ticketRepoSpy.storeTicket(0, 0, EVENT, COMPANY, 100);
        String ticketId = ticketRepoSpy.getTicketsForEvent(COMPANY, EVENT).get(0).getId();
        String orderId = orderRepoSpy.store(COMPANY, EVENT, List.of(ticketId), USERNAME, futureDate);

        when(paymentService.processPayment(createCreditCardDetails(), 100.0,"USD")).thenReturn(100);
        when(externalTicketService.issueTicket(anyString(), anyString(), anyString(), anyInt(), anyInt())).thenReturn("TIX-test-123");
        doThrow(new RuntimeException("Generator Error")).when(supplyService).supplyToEmail(anyString(), anyString());

        PurchaseOrderException exception = assertThrows(PurchaseOrderException.class, () -> {
            purchasedService.PurchaseTicket(EMAIL, orderId, USERNAME, "none",createCreditCardDetails());
        });
        
        assertEquals("Failed during save or supply: Generator Error", exception.getMessage());
        verify(paymentService).refund(100);
        assertNotNull(orderRepoSpy.getOrder(USERNAME));
        verify(orderRepoSpy, never()).delete(USERNAME);
    }

    @Test
    void purchaseTicket_Success_WithSpyAndStateCheckAsLogoutUser() throws Exception {
        iTicketRepository ticketRepoSpy = spy(new TicketRepositoryImpl());
        IActiveOrderRepository orderRepoSpy = spy(new OrderRepositoryImpl());
        iDiscountPolicyRepository discountPolicyRepository = spy(new com.ticketing.ticketapp.Infastructure.DataBaseInterface.DiscountPolicyRepositoryAdapter(jpaDiscountPolicyRepository));

        purchasedService = new PurchasedService(
                orderRepoSpy,
                ticketRepoSpy,
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

        Date futureDate = new Date(System.currentTimeMillis() + 1000000);

        ticketRepoSpy.storeTicket(0, 0, EVENT, COMPANY, 100);
        Ticket originalTicket = ticketRepoSpy.getTicketsForEvent(COMPANY, EVENT).get(0);
        String ticketId = originalTicket.getId();

        String orderId = orderRepoSpy.store(COMPANY, EVENT, List.of(ticketId), USERNAME, futureDate);

        when(paymentService.processPayment(createCreditCardDetails(), 100.0,"USD")).thenReturn(100);
        when(externalTicketService.issueTicket(anyString(), anyString(), anyString(), anyInt(), anyInt())).thenReturn("TIX-test-123");
        when(tokenService.validateToken(anyString())).thenReturn(true);
        when(tokenService.extractUserId(anyString())).thenReturn(USERNAME);
        purchasedService.PurchaseTicket(EMAIL, "", USERNAME, "none",createCreditCardDetails());

        Ticket ticketAfterPurchase = ticketRepoSpy.getTicketById(ticketId);

        assertNotNull(ticketAfterPurchase);
        assertTrue(ticketAfterPurchase.isPurchased());
        assertEquals(2, ticketAfterPurchase.getVersion());

        ActiveOrder activeOrderAfter = orderRepoSpy.getOrder(USERNAME);
        assertNull(activeOrderAfter);

        verify(purchasedOrderRepository).StorePurchasedOrder(
                eq(COMPANY),
                eq(EVENT),
                anyList(),
                eq(USERNAME),
                eq(orderId),
                anyList()
        );

        verify(orderRepoSpy).delete(orderId);
    }

    @Test
    void isAuthorized_Owner_ReturnsTrue() {
        when(treeOfRoleRepository.exitsOwner(USERNAME, COMPANY)).thenReturn(true);

        assertTrue(purchasedService.isAuthorized(COMPANY, USERNAME));
    }

    @Test
    void isAuthorized_ManagerWithPermission_ReturnsTrue() {
        when(treeOfRoleRepository.exitsOwner(USERNAME, COMPANY)).thenReturn(false);
        when(treeOfRoleRepository.ManagerPermitToSeeTransactions(USERNAME, COMPANY)).thenReturn(true);

        assertTrue(purchasedService.isAuthorized(COMPANY, USERNAME));
    }

    @Test
    void getCompanyTransaction_Success() {
        String token = "valid_token";
        String manager = "manager1";
        PurchaseOrder po = new PurchaseOrder(COMPANY, EVENT, List.of("T1"), USERNAME, ORDER_ID);

        when(tokenService.validateToken(token)).thenReturn(true);
        when(tokenService.extractUserId(token)).thenReturn(manager);
        when(treeOfRoleRepository.exitsOwner(manager, COMPANY)).thenReturn(true);
        when(purchasedOrderRepository.getPurchasedOrdersForCompany(COMPANY)).thenReturn(List.of(po));
        when(ticketRepository.getTicketsDescription(anyList())).thenReturn("Ticket Info");

        Response<List<PurchaseOrderDTO>> result = purchasedService.getCompanyTransaction(COMPANY, token);

        assertTrue(result.isSuccess());
        assertTrue(result.getData().get(0).buyer().equals(USERNAME));
    }

    @Test
    void getCompanyTransaction_Failure_NotAuthorized() {
        String token = "valid_token";
        String user = "user_without_perms";

        when(tokenService.validateToken(token)).thenReturn(true);
        when(tokenService.extractUserId(token)).thenReturn(user);
        when(treeOfRoleRepository.exitsOwner(user, COMPANY)).thenReturn(false);
        when(treeOfRoleRepository.ManagerPermitToSeeTransactions(user, COMPANY)).thenReturn(false);

        Response<List<PurchaseOrderDTO>> result = purchasedService.getCompanyTransaction(COMPANY, token);

        assertTrue(result.isError());
        verify(purchasedOrderRepository, never()).getPurchasedOrdersForCompany(anyString());
    }

    @Test
    void getUserTransaction_Success() {
        String token = "valid_token";
        String buyer = "buyer1";
        PurchaseOrder po = new PurchaseOrder(COMPANY, EVENT, List.of("T1"), buyer, ORDER_ID);

        when(tokenService.validateToken(token)).thenReturn(true);
        when(tokenService.extractUserId(token)).thenReturn(buyer);
        when(purchasedOrderRepository.getPurchasedOrdersForUser(buyer)).thenReturn(List.of(po));
        when(ticketRepository.getTicketsDescription(anyList())).thenReturn("Seat 10");

        Response<List<PurchaseOrderDTO>> result = purchasedService.getUserTransaction(token);

        assertTrue(result.isSuccess());
        assertTrue(result.getData().get(0).buyer().equals(buyer));
    }

    @Test
    void getUserTransaction_InvalidToken() {
        String token = "invalid_token";
        when(tokenService.validateToken(token)).thenReturn(false);

        Response<List<PurchaseOrderDTO>> result = purchasedService.getUserTransaction(token);

        assertTrue(result.isError());
        verify(purchasedOrderRepository, never()).getPurchasedOrdersForUser(anyString());
    }

    @Test
    void getSubTreeSalesReport_InvalidToken_ReturnsError() {
        when(tokenService.validateToken("bad_token")).thenReturn(false);

        var result = purchasedService.getSubTreeSalesReport("bad_token", COMPANY);

        assertTrue(result.isError());
    }

    @Test
    void getSubTreeSalesReport_NotAuthorized_ReturnsError() {
        when(tokenService.validateToken(USERNAME)).thenReturn(true);
        when(tokenService.extractUserId(USERNAME)).thenReturn(USERNAME);
        when(treeOfRoleRepository.exitsOwner(USERNAME, COMPANY)).thenReturn(false);
        when(treeOfRoleRepository.ManagerPermitToSeeTransactions(USERNAME, COMPANY)).thenReturn(false);

        var result = purchasedService.getSubTreeSalesReport(USERNAME, COMPANY);

        assertTrue(result.isError());
    }

    @Test
    void getSubTreeSalesReport_AsOwner_ReturnsReport() {
        when(tokenService.validateToken(USERNAME)).thenReturn(true);
        when(tokenService.extractUserId(USERNAME)).thenReturn(USERNAME);
        when(treeOfRoleRepository.exitsOwner(USERNAME, COMPANY)).thenReturn(true);
        when(treeOfRoleRepository.getAllOwnersByCompany(COMPANY)).thenReturn(List.of());
        when(treeOfRoleRepository.getAllManagersByCompany(COMPANY)).thenReturn(List.of());
        when(purchasedOrderRepository.getPurchasedOrdersForCompany(COMPANY)).thenReturn(List.of());

        var result = purchasedService.getSubTreeSalesReport(USERNAME, COMPANY);

        assertTrue(result.isSuccess());
        assertEquals(0.0, result.getData().getTotalRevenue(), 0.001);
        assertEquals(0, result.getData().getTotalTicketsSold());
    }

    @Test
    void purchaseTicket_Failure_UserSuspended() throws Exception {
        iTicketRepository ticketRepoSpy = spy(new TicketRepositoryImpl());
        IActiveOrderRepository orderRepoSpy = spy(new OrderRepositoryImpl());
        iDiscountPolicyRepository discountPolicyRepository = spy(new com.ticketing.ticketapp.Infastructure.DataBaseInterface.DiscountPolicyRepositoryAdapter(jpaDiscountPolicyRepository));

        purchasedService = new PurchasedService(
                orderRepoSpy,
                ticketRepoSpy,
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

        String validToken = "valid_suspended_user_token";
        Date futureDate = new Date(System.currentTimeMillis() + 1000000);

        ticketRepoSpy.storeTicket(0, 0, EVENT, COMPANY, 100);
        String ticketId = ticketRepoSpy.getTicketsForEvent(COMPANY, EVENT).get(0).getId();
        String orderId = orderRepoSpy.store(COMPANY, EVENT, List.of(ticketId), USERNAME, futureDate);

        when(tokenService.validateToken(validToken)).thenReturn(true);
        when(tokenService.extractUserId(validToken)).thenReturn(USERNAME);
        when(tokenService.extractUsername(validToken)).thenReturn(USERNAME);

        when(userRepository.isUserSuspendedNow(USERNAME)).thenReturn(true);

        PurchaseOrderException exception = assertThrows(PurchaseOrderException.class, () -> {
            purchasedService.PurchaseTicket(EMAIL, orderId, validToken, "none",createCreditCardDetails());
        });
        assertEquals("User is suspended", exception.getMessage());
        verify(paymentService, never()).processPayment(any(CreditCardDetails.class), anyDouble(),eq("USD"));
        verify(supplyService, never()).supplyToEmail(anyString(), anyString());
        assertNotNull(orderRepoSpy.findById(orderId));
    }
}