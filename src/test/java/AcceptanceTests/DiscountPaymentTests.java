package AcceptanceTests;

import com.ticketing.ticketapp.Appliction.*;
import com.ticketing.ticketapp.Domain.Company.iCompanyRepository;
import com.ticketing.ticketapp.Domain.Discount.ConditionalDiscount;
import com.ticketing.ticketapp.Domain.Discount.DiscountPolicyDTO;
import com.ticketing.ticketapp.Domain.Discount.DiscountTargetType;
import com.ticketing.ticketapp.Domain.Discount.iDiscountPolicyRepository;
import com.ticketing.ticketapp.Domain.Event.EventType;
import com.ticketing.ticketapp.Domain.Event.MapArea;
import com.ticketing.ticketapp.Domain.Event.iEventRepository;
import com.ticketing.ticketapp.Domain.Order.IActiveOrderRepository;
import com.ticketing.ticketapp.Domain.OwnerManagerTree.iTreeOfRoleRepository;
import com.ticketing.ticketapp.Domain.PurchasePolicy.PurchaseTargetType;
import com.ticketing.ticketapp.Domain.PurchasePolicy.iPurchasePolicyRepository;
import com.ticketing.ticketapp.Domain.PurchasedOrderAggregate.iPurchasedOrderRepository;
import com.ticketing.ticketapp.Domain.QueueAggregates.iQueueRepository;
import com.ticketing.ticketapp.Domain.Ticket.iTicketRepository;
import com.ticketing.ticketapp.Domain.User.IUserRepository;
import com.ticketing.ticketapp.Infastructure.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Discount & Payment Price Acceptance Tests")
public class DiscountPaymentTests {

    private UserService userService;
    private CompanyService companyService;
    private EventService eventService;
    private OrderService reserveTicketService;
    private PurchasedService purchasedService;
    private TokenService tokenService;
    private IPaymentService paymentServiceSpy;
    private DiscountService discountService;
    private iDiscountPolicyRepository discountRepo;

    @BeforeEach
    void setUp() {
        IUserRepository userRepository = new UserRepositoryImpl();
        iCompanyRepository companyRepository = new CompanyRepositoryImpl();
        iEventRepository eventRepository = new EventRepositoryImpl();
        iQueueRepository queueRepository = new QueueRepositoryImpl();
        iTreeOfRoleRepository treeOfRoleRepository = new TreeOfRoleRepositoryImpl();
        IActiveOrderRepository activeOrderRepository = new OrderRepositoryImpl();
        iTicketRepository ticketRepository = new TicketRepositoryImpl();
        iPurchasedOrderRepository purchasedOrderRepository = new PurchasedOrderRepositoryImpl();
        this.discountRepo = new InMemoryDiscountPolicyRepository();
        iPurchasePolicyRepository purchasePolicyRepository=new InMemoryPurchasePolicyRepository();

        this.tokenService = new TokenService();
        IPasswordEncoder passwordEncoder = new PasswordEncoderImpl();
        ISupplyService supplyService = new SupplyServiceMock();
        IBarcodeGenerator barcodeGenerator = new BarcodeGeneratorMock();
        this.paymentServiceSpy = spy(new PaymentServiceMock());

        this.userService = new UserService(passwordEncoder, userRepository, tokenService);
        this.companyService = new CompanyService(companyRepository, userRepository, treeOfRoleRepository, tokenService);
        this.eventService = new EventService(companyRepository, eventRepository, tokenService, treeOfRoleRepository, ticketRepository, queueRepository);
        this.reserveTicketService = new OrderService(activeOrderRepository, tokenService, ticketRepository,userRepository,purchasePolicyRepository);

        this.purchasedService = new PurchasedService(
                activeOrderRepository, ticketRepository, purchasedOrderRepository,
                supplyService, paymentServiceSpy, barcodeGenerator,
                tokenService, treeOfRoleRepository, discountRepo
        );

        this.discountService = new DiscountService(discountRepo, tokenService, purchasedService);

        activeOrderRepository.deleteAllActiveOrders();
        eventRepository.deleteAllEvents();
        userRepository.deleteAll();
        discountRepo.deleteAll();
    }

    private String setupEventAndGetToken(String owner, String company, String event, double price) {
        String guestToken = tokenService.generateGuestToken();
        userService.register(guestToken, owner, "password",10);
        String token = userService.login(guestToken, owner, "password");
        companyService.CreateCompany(company, token);
        eventService.createEvent(token, event, company, EventType.PLAY, price, new Date(), "Location", company, getLargeMap());
        return token;
    }

    private String registerAndLoginBuyer(String name) {
        String guestToken = tokenService.generateGuestToken();
        userService.register(guestToken, name, "password",10);
        return userService.login(guestToken, name, "password");
    }

    private MapArea[][] getLargeMap() {
        MapArea[][] map = new MapArea[5][5];
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                map[i][j] = MapArea.SEAT;
            }
        }
        return map;
    }

    @Test
    @DisplayName("1. Simple Percentage Discount Verification")
    void testSimpleDiscount() {
        String ownerToken = setupEventAndGetToken("o1", "C1", "E1", 100.0);
        discountService.createSimpleDiscount(ownerToken, "E1", DiscountTargetType.EVENT, 10.0, "C1");

        String buyerToken = registerAndLoginBuyer("b1");
        String orderId = reserveTicketService.reserveTickets(buyerToken, "C1", "E1", List.of(new int[]{0, 0, 1}));

        purchasedService.PurchaseTicket("b1@g.com", orderId, "b1", "none");

        verify(paymentServiceSpy, times(1)).processPayment("b1@g.com", 90.0);
    }

    @Test
    @DisplayName("2. Coupon Code Discount Verification")
    void testCouponDiscount() {
        String ownerToken = setupEventAndGetToken("o2", "C2", "E2", 200.0);
        discountService.createCouponDiscount(ownerToken, "E2", DiscountTargetType.EVENT, "PROMO50", 50.0, "C2");

        String buyerToken = registerAndLoginBuyer("b2");
        String orderId = reserveTicketService.reserveTickets(buyerToken, "C2", "E2", List.of(new int[]{0, 0, 1}));

        purchasedService.PurchaseTicket("b2@g.com", orderId, "b2", "PROMO50");

        verify(paymentServiceSpy, times(1)).processPayment("b2@g.com", 100.0);
    }

    @Test
    @DisplayName("3. Quantity Based Discount Verification")
    void testQuantityDiscount() {
        String ownerToken = setupEventAndGetToken("o3", "C3", "E3", 100.0);
        discountService.createQuantityDiscount(ownerToken, "E3", DiscountTargetType.EVENT, 20.0, 2, "C3");

        String buyerToken = registerAndLoginBuyer("b3");
        List<int[]> seats = List.of(new int[]{0, 0, 1}, new int[]{0, 1, 1});
        String orderId = reserveTicketService.reserveTickets(buyerToken, "C3", "E3", seats);

        purchasedService.PurchaseTicket("b3@g.com", orderId, "b3", "none");

        verify(paymentServiceSpy, times(1)).processPayment("b3@g.com", 160.0);
    }

    @Test
    @DisplayName("4. Additive Multiple Discounts Verification")
    void testSummedDiscounts() {
        String ownerToken = setupEventAndGetToken("o4", "C4", "E4", 100.0);

        String d1 = discountService.createSimpleDiscount(ownerToken, "E4", DiscountTargetType.EVENT, 10.0, "C4");
        String d2 = discountService.createCouponDiscount(ownerToken, "E4", DiscountTargetType.EVENT, "PLUS5", 5.0, "C4");

        discountService.createSumDiscountPolicy(ownerToken, "E4", DiscountTargetType.EVENT, List.of(d1, d2), "C4");

        String buyerToken = registerAndLoginBuyer("b4");
        String orderId = reserveTicketService.reserveTickets(buyerToken, "C4", "E4", List.of(new int[]{0, 0, 1}));

        purchasedService.PurchaseTicket("b4@g.com", orderId, "b4", "PLUS5");

        verify(paymentServiceSpy, times(1)).processPayment("b4@g.com", 85.0);
    }

    @Test
    @DisplayName("5. Maximum Discount Logic Verification")
    void testMaxDiscountSelection() {
        String ownerToken = setupEventAndGetToken("o5", "C5", "E5", 100.0);

        String d1 = discountService.createSimpleDiscount(ownerToken, "E5", DiscountTargetType.EVENT, 15.0, "C5");
        String d2 = discountService.createSimpleDiscount(ownerToken, "E5", DiscountTargetType.EVENT, 30.0, "C5");

        discountService.createMaxDiscountPolicy(ownerToken, "E5", DiscountTargetType.EVENT, List.of(d1, d2), "C5");

        String buyerToken = registerAndLoginBuyer("b5");
        String orderId = reserveTicketService.reserveTickets(buyerToken, "C5", "E5", List.of(new int[]{0, 0, 1}));

        purchasedService.PurchaseTicket("b5@g.com", orderId, "b5", "none");

        verify(paymentServiceSpy, times(1)).processPayment("b5@g.com", 70.0);
    }

    @Test
    @DisplayName("6. Time Limited Discount Expiration Verification")
    void testExpiredDiscount() {
        String ownerToken = setupEventAndGetToken("o6", "C6", "E6", 100.0);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MINUTE, -10);

        discountService.createTimeLimitedDiscount(ownerToken, "E6", DiscountTargetType.EVENT, 50.0, cal.getTime(), "C6");

        String buyerToken = registerAndLoginBuyer("b6");
        String orderId = reserveTicketService.reserveTickets(buyerToken, "C6", "E6", List.of(new int[]{0, 0, 1}));

        purchasedService.PurchaseTicket("b6@g.com", orderId, "b6", "none");

        verify(paymentServiceSpy, times(1)).processPayment("b6@g.com", 100.0);
    }

    @Test
    @DisplayName("7. Invalid Coupon Code Behavior Verification")
    void testInvalidCouponCode() {
        String ownerToken = setupEventAndGetToken("o7", "C7", "E7", 100.0);
        discountService.createCouponDiscount(ownerToken, "E7", DiscountTargetType.EVENT, "REAL_CODE", 20.0, "C7");

        String buyerToken = registerAndLoginBuyer("b7");
        String orderId = reserveTicketService.reserveTickets(buyerToken, "C7", "E7", List.of(new int[]{0, 0, 1}));

        purchasedService.PurchaseTicket("b7@g.com", orderId, "b7", "FAKE_CODE");

        verify(paymentServiceSpy, times(1)).processPayment("b7@g.com", 100.0);
    }
    @Test
    @DisplayName("1. Verify Simple Discount Creation and Persistence")
    void testSimpleDiscountStorage() {
        String ownerToken = setupEventAndGetToken("o1", "C1", "E1", 100.0);
        String id = discountService.createSimpleDiscount(ownerToken, "E1", DiscountTargetType.EVENT, 10.0, "C1");

        assertNotNull(id);
        assertNotNull(discountRepo.getPolicy(id));
        assertEquals(10.0, ((ConditionalDiscount)discountRepo.getPolicy(id).getRoot()).getPercentage());
    }

    @Test
    @DisplayName("2. Verify Coupon Discount Description Contains Code")
    void testCouponDescriptionLogic() {
        String ownerToken = setupEventAndGetToken("o2", "C2", "E2", 100.0);
        discountService.createCouponDiscount(ownerToken, "E2", DiscountTargetType.EVENT, "SAVE50", 50.0, "C2");

        List<DiscountPolicyDTO> discounts = discountService.getDiscountsForEventAndCompany(ownerToken, "E2", "C2");

        assertTrue(discounts.get(0).description().contains("SAVE50"));
    }

    @Test
    @DisplayName("3. Verify Quantity Discount Description Contains Min Quantity")
    void testQuantityDescriptionLogic() {
        String ownerToken = setupEventAndGetToken("o3", "C3", "E3", 100.0);
        discountService.createQuantityDiscount(ownerToken, "E3", DiscountTargetType.EVENT, 20.0, 5, "C3");

        List<DiscountPolicyDTO> discounts = discountService.getDiscountsForEventAndCompany(ownerToken, "E3", "C3");

        assertTrue(discounts.get(0).description().contains("quantity >= 5"));
    }

    @Test
    @DisplayName("4. Verify Composite Sum Discount Deletes Originals")
    void testSumDiscountCleanup() {
        String ownerToken = setupEventAndGetToken("o4", "C4", "E4", 100.0);
        String d1 = discountService.createSimpleDiscount(ownerToken, "E4", DiscountTargetType.EVENT, 10.0, "C4");
        String d2 = discountService.createSimpleDiscount(ownerToken, "E4", DiscountTargetType.EVENT, 5.0, "C4");

        discountService.createSumDiscountPolicy(ownerToken, "E4", DiscountTargetType.EVENT, List.of(d1, d2), "C4");

        assertNull(discountRepo.getPolicy(d1), "Original policy d1 should be deleted after composite creation");
        assertNull(discountRepo.getPolicy(d2), "Original policy d2 should be deleted after composite creation");
    }

    @Test
    @DisplayName("5. Verify Max Discount Description Recursion")
    void testMaxDiscountDescriptionRecursion() {
        String ownerToken = setupEventAndGetToken("o5", "C5", "E5", 100.0);
        String d1 = discountService.createSimpleDiscount(ownerToken, "E5", DiscountTargetType.EVENT, 15.0, "C5");
        discountService.createMaxDiscountPolicy(ownerToken, "E5", DiscountTargetType.EVENT, List.of(d1), "C5");

        List<DiscountPolicyDTO> discounts = discountService.getDiscountsForEventAndCompany(ownerToken, "E5", "C5");

        assertTrue(discounts.get(0).description().contains("Best of"));
        assertTrue(discounts.get(0).description().contains("15.0%"));
    }

    @Test
    @DisplayName("6. Unauthorized User Creation Returns Null")
    void testUnauthorizedCreationReturnsNull() {
        String ownerToken = setupEventAndGetToken("o6", "C6", "E6", 100.0);
        String guestToken = tokenService.generateGuestToken(); // Not authorized for C6

        String id = discountService.createSimpleDiscount(guestToken, "E6", DiscountTargetType.EVENT, 10.0, "C6");

        assertNull(id);
    }

    @Test
    @DisplayName("7. Verify Time Limited Discount Description Format")
    void testTimeLimitedDescription() {
        String ownerToken = setupEventAndGetToken("o7", "C7", "E7", 100.0);
        Date deadline = new Date();
        discountService.createTimeLimitedDiscount(ownerToken, "E7", DiscountTargetType.EVENT, 30.0, deadline, "C7");

        List<DiscountPolicyDTO> discounts = discountService.getDiscountsForEventAndCompany(ownerToken, "E7", "C7");

        assertTrue(discounts.get(0).description().contains("purchase date before"));
    }

    @Test
    @DisplayName("8. Retrieve Multiple Active Discounts for One Event")
    void testGetMultipleActiveDiscounts() {
        String ownerToken = setupEventAndGetToken("o8", "C8", "E8", 100.0);
        discountService.createSimpleDiscount(ownerToken, "E8", DiscountTargetType.EVENT, 10.0, "C8");
        discountService.createSimpleDiscount(ownerToken, "E8", DiscountTargetType.EVENT, 20.0, "C8");

        List<DiscountPolicyDTO> discounts = discountService.getDiscountsForEventAndCompany(ownerToken, "E8", "C8");

        assertEquals(2, discounts.size());
    }

    @Test
    @DisplayName("9. Ensure Composite Sum Description Formatting")
    void testSumDescriptionFormatting() {
        String ownerToken = setupEventAndGetToken("o9", "C9", "E9", 100.0);
        String d1 = discountService.createSimpleDiscount(ownerToken, "E9", DiscountTargetType.EVENT, 10.0, "C9");
        discountService.createSumDiscountPolicy(ownerToken, "E9", DiscountTargetType.EVENT, List.of(d1), "C9");

        List<DiscountPolicyDTO> discounts = discountService.getDiscountsForEventAndCompany(ownerToken, "E9", "C9");

        assertTrue(discounts.get(0).description().startsWith("Combined:"));
    }

    @Test
    @DisplayName("10. Verify Target ID and Type Integrity in DTO")
    void testDtoIntegrity() {
        String ownerToken = setupEventAndGetToken("o10", "C10", "E10", 100.0);
        discountService.createSimpleDiscount(ownerToken, "E10", DiscountTargetType.EVENT, 10.0, "C10");

        List<DiscountPolicyDTO> discounts = discountService.getDiscountsForEventAndCompany(ownerToken, "E10", "C10");

        assertEquals("E10", discounts.get(0).targetId());
        assertEquals("EVENT", discounts.get(0).type());
    }
    void createQuantityDiscountInValidToken() {
        String a=discountService.createQuantityDiscount("a", "C1",DiscountTargetType.EVENT,1,10,"");
        assertEquals(null,a);

    }
    @Test
    void createAndPolicyInValidToken() {
        String a=discountService.createTimeLimitedDiscount("a", "C1",DiscountTargetType.EVENT,1,null,null);
        assertEquals(null,a);

    }
    @Test
    void createOrPolicyInValidToken() {
        String a=discountService.createCouponDiscount("a", "C1",null,null,1,null);
        assertEquals(null,a);

    }
    @Test
    void createAndPolicyInValidToken2() {
        String ownerToken = setupEventAndGetToken("o10", "C10", "E10", 100.0);

        String a=discountService.createMaxDiscountPolicy(ownerToken, "C1",null,null,null);
        assertEquals(null,a);
    }
    @Test
    void getDiscountsForEventAndCompanyInValidToken3() {
        String ownerToken = setupEventAndGetToken("o10", "C10", "E10", 100.0);

        List<DiscountPolicyDTO> a=discountService.getDiscountsForEventAndCompany(ownerToken,"1","1");
        assertEquals(new ArrayList<>(),a);
    }
}
