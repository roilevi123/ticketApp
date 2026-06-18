package AcceptanceTests;

import com.ticketing.ticketapp.Appliction.*;
import com.ticketing.ticketapp.Domain.AdminAggregate.iAdminRepository;
import com.ticketing.ticketapp.Domain.Company.iCompanyRepository;
import com.ticketing.ticketapp.Domain.Discount.iDiscountPolicyRepository;
import com.ticketing.ticketapp.Domain.Event.EventType;
import com.ticketing.ticketapp.Domain.Event.MapArea;
import com.ticketing.ticketapp.Domain.Event.iEventRepository;
import com.ticketing.ticketapp.Domain.Order.IActiveOrderRepository;
import com.ticketing.ticketapp.Domain.OwnerManagerTree.iTreeOfRoleRepository;
import com.ticketing.ticketapp.Domain.PurchasePolicy.PurchasePolicyDTO;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@SpringBootTest(classes = com.ticketing.ticketapp.TicketappApplication.class)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Transactional
@DisplayName("Purchase Policy Acceptance Tests")
public class PurchasePolicyAcceptanceTests {

    @Autowired private JpaPurchasePolicyRepository jpaPurchasePolicyRepository;
    @Autowired private IUserRepository userRepository;
    @Autowired private iCompanyRepository companyRepository;
    @Autowired private iEventRepository eventRepository;
    @Autowired private iQueueRepository queueRepository;
    @Autowired private iTreeOfRoleRepository treeOfRoleRepository;
    @Autowired private IActiveOrderRepository activeOrderRepository;
    @Autowired private iTicketRepository ticketRepository;
    @Autowired private iPurchasedOrderRepository purchasedOrderRepository;
    @Autowired private TokenService tokenService;
    @Autowired private com.ticketing.ticketapp.Domain.Discount.JpaDiscountPolicyRepository jpaDiscountPolicyRepository;

    private UserService userService;
    private CompanyService companyService;
    private EventService eventService;
    private OrderService reserveService;
    private PurchasePolicyService policyService;
    private AdminService adminService;

    @BeforeEach
    void setUp() {
        iPurchasePolicyRepository purchasePolicyRepository = new com.ticketing.ticketapp.Infastructure.PurchasePolicyRepositoryAdapter(jpaPurchasePolicyRepository);
        iDiscountPolicyRepository discountPolicyRepository = new com.ticketing.ticketapp.Infastructure.DataBaseInterface.DiscountPolicyRepositoryAdapter(jpaDiscountPolicyRepository);

        INotifier notifierMock = mock(INotifier.class);
        IPasswordEncoder passwordEncoder = new PasswordEncoderImpl();

        this.userService = new UserService(passwordEncoder, userRepository, tokenService, new NotificationRepositoryImpl(), notifierMock, treeOfRoleRepository);
        this.companyService = new CompanyService(companyRepository, userRepository, treeOfRoleRepository, tokenService, notifierMock);
        this.eventService = new EventService(companyRepository, eventRepository, tokenService, treeOfRoleRepository, ticketRepository, queueRepository, purchasedOrderRepository, userRepository, notifierMock, discountPolicyRepository);
        this.reserveService = new OrderService(activeOrderRepository, tokenService, ticketRepository, userRepository, purchasePolicyRepository, notifierMock, eventRepository, mock(LotteryService.class));
        this.policyService = new PurchasePolicyService(purchasePolicyRepository, tokenService, userRepository);

        iAdminRepository adminRepository = new AdminRepositoryImpl() {
            @Override
            public boolean isAdmin(String userID) { return userID.equals("admin"); }
        };
        this.adminService = new AdminService(treeOfRoleRepository, companyRepository, adminRepository, userRepository, purchasedOrderRepository, ticketRepository, eventRepository, tokenService, new NotifierImpl(new Broadcaster(new NotificationRepositoryImpl())), activeOrderRepository);
    }

    private boolean isValidOrderId(String str) { return str != null && !str.isBlank(); }
    private String gt() { return tokenService.generateGuestToken(); }

    private MapArea[][] getMap() {
        MapArea[][] map = new MapArea[10][10];
        for (int i = 0; i < 10; i++) for (int j = 0; j < 10; j++) map[i][j] = MapArea.SEAT;
        return map;
    }

    private String regAndSetup(String adminName, String compName, String eventName, int minAge) {
        userService.register(gt(), adminName, "p", 30, adminName + "@test.com");
        String token = userService.login(gt(), adminName, "p").getData();
        companyService.CreateCompany(compName, token);
        eventService.createEvent(token, eventName, eventName, EventType.PLAY, 100, new Date(), "Loc", compName, getMap());
        if (minAge > 0) policyService.createAgeLimitPolicy(token, eventName, PurchaseTargetType.EVENT, minAge);
        return token;
    }

    private String quickReg(String name, int age) {
        userService.register(gt(), name, "p", age, name + "@test.com");
        return userService.login(gt(), name, "p").getData();
    }
    @Test @DisplayName("1. Fail - Underage for Event Policy")
    void underageForEvent() {
        regAndSetup("admin", "C1", "E1", 18);
        String userToken = quickReg("kid", 12);
        Response<String> result = reserveService.reserveTickets(userToken, "C1", "E1", List.of(new int[]{0, 0, 1}), null);
        assertTrue(result.isError());
        assertTrue(result.getMessage().contains("Doesn't stand in Event Purchase Policy"));
    }

    @Test @DisplayName("2. Success - Meets Age Policy")
    void meetsAgePolicy() {
        regAndSetup("admin", "C1", "E1", 18);
        String userToken = quickReg("adult", 25);
        Response<String> result = reserveService.reserveTickets(userToken, "C1", "E1", List.of(new int[]{1, 1, 1}), null);
        assertTrue(isValidOrderId(result.getData()), "Expected valid Order ID but got: " + result.getData());
    }

    @Test @DisplayName("3. Fail - Exceeds Max Quantity")
    void exceedsMaxQuantity() {
        String admin = regAndSetup("admin", "C1", "E1", 0);
        policyService.createQuantityLimitPolicy(admin, "E1", PurchaseTargetType.EVENT, 0, 2);
        String user = quickReg("u1", 20);
        Response<String> result = reserveService.reserveTickets(user, "C1", "E1", List.of(new int[]{0, 0, 5}), null);
        assertTrue(result.isError());
    }

    @Test @DisplayName("4. Fail - Below Min Quantity")
    void belowMinQuantity() {
        String admin = regAndSetup("admin", "C1", "E1", 0);
        policyService.createQuantityLimitPolicy(admin, "E1", PurchaseTargetType.EVENT, 5, 10);
        String user = quickReg("u1", 20);
        Response<String> result = reserveService.reserveTickets(user, "C1", "E1", List.of(new int[]{0, 0, 2}), null);
        assertTrue(result.isError());
    }

    @Test @DisplayName("5. Fail - Company Level Age Policy")
    void companyAgePolicyFail() {
        String admin = regAndSetup("admin", "C1", "E1", 0);
        policyService.createAgeLimitPolicy(admin, "C1", PurchaseTargetType.COMPANY, 21);
        String userToken = quickReg("u1", 19);
        Response<String> result = reserveService.reserveTickets(userToken, "C1", "E1", List.of(new int[]{0, 0, 1}), null);
        assertTrue(result.isError());
        assertTrue(result.getMessage().contains("Company Purchase Policy"));
    }

    @Test @DisplayName("6. Success - Composite AND Policy")
    void compositeAndSuccess() {
        String admin = regAndSetup("admin", "C1", "E1", 0);
        String p1 = policyService.createAgeLimitPolicy(admin, "E1", PurchaseTargetType.EVENT, 18).getData();
        String p2 = policyService.createQuantityLimitPolicy(admin, "E1", PurchaseTargetType.EVENT, 1, 10).getData();
        policyService.createAndPolicy(admin, "E1", PurchaseTargetType.EVENT, Arrays.asList(p1, p2));

        String user = quickReg("u1", 20);
        Response<String> result = reserveService.reserveTickets(user, "C1", "E1", List.of(new int[]{2, 2, 1}), null);
        assertTrue(isValidOrderId(result.getData()));
    }

    @Test @DisplayName("7. Fail - Composite OR Policy Out of Range")
    void compositeOrFail() {
        String admin = regAndSetup("admin", "C1", "E1", 0);
        String p1 = policyService.createQuantityLimitPolicy(admin, "E1", PurchaseTargetType.EVENT, 1, 2).getData();
        String p2 = policyService.createQuantityLimitPolicy(admin, "E1", PurchaseTargetType.EVENT, 10, 20).getData();
        policyService.createOrPolicy(admin, "E1", PurchaseTargetType.EVENT, Arrays.asList(p1, p2));

        String user = quickReg("u1", 20);
        Response<String> result = reserveService.reserveTickets(user, "C1", "E1", List.of(new int[]{0, 0, 5}), null);
        assertTrue(result.isError());
    }

    @Test @DisplayName("8. Success - Nested Complex Policy")
    void nestedPolicySuccess() {
        String admin = regAndSetup("admin", "C1", "E1", 0);
        String pAge = policyService.createAgeLimitPolicy(admin, "E1", PurchaseTargetType.EVENT, 16).getData();
        String pQty = policyService.createQuantityLimitPolicy(admin, "E1", PurchaseTargetType.EVENT, 1, 1).getData();
        policyService.createOrPolicy(admin, "E1", PurchaseTargetType.EVENT, Arrays.asList(pAge, pQty));

        String user = quickReg("u1", 10);
        Response<String> result = reserveService.reserveTickets(user, "C1", "E1", List.of(new int[]{3, 3, 1}), null);
        assertTrue(isValidOrderId(result.getData()));
    }

    @Test @DisplayName("9. Fail - Guest User on Age Policy")
    void guestUserAgeFail() {
        regAndSetup("admin", "C1", "E1", 18);
        Response<String> result = reserveService.reserveTickets(gt(), "C1", "E1", List.of(new int[]{0, 0, 1}), null);
        assertTrue(isValidOrderId(result.getData()));
    }

    @Test @DisplayName("10. Fail - Multiple Requests Total Quantity")
    void multipleRequestsFail() {
        String admin = regAndSetup("admin", "C1", "E1", 0);
        policyService.createQuantityLimitPolicy(admin, "E1", PurchaseTargetType.EVENT, 0, 3);
        String user = quickReg("u1", 20);
        List<int[]> reqs = List.of(new int[]{4, 4, 2}, new int[]{5, 5, 2});
        Response<String> result = reserveService.reserveTickets(user, "C1", "E1", reqs, null);
        assertTrue(result.isError());
    }

    @Test @DisplayName("21. Success - Get Single Event Policy DTO")
    void getSinglePolicyDTO() {
        String admin = regAndSetup("admin", "C1", "E1", 18);
        List<PurchasePolicyDTO> policies = policyService.getPoliciesForEventAndCompany(admin, "E1", "C1").getData();

        assertEquals(1, policies.size());
        assertTrue(policies.get(0).description().contains("18"));
        assertEquals("EVENT", policies.get(0).type());
    }

    @Test @DisplayName("22. Success - Get Multiple Policies DTO (Event + Company)")
    void getMultiplePoliciesDTO() {
        String admin = regAndSetup("admin", "C1", "E1", 18);

        String pAgeEventId = policyService.getPoliciesForEventAndCompany(admin, "E1", "C1").getData().get(0).id();

        String pQtyEventId = policyService.createQuantityLimitPolicy(admin, "E1", PurchaseTargetType.EVENT, 1, 5).getData();

        policyService.createAndPolicy(admin, "E1", PurchaseTargetType.EVENT, Arrays.asList(pAgeEventId, pQtyEventId));

        policyService.createAgeLimitPolicy(admin, "C1", PurchaseTargetType.COMPANY, 10);

        List<PurchasePolicyDTO> policies = policyService.getPoliciesForEventAndCompany(admin, "E1", "C1").getData();

        assertEquals(2, policies.size());
    }

    @Test @DisplayName("23. Success - DTO Description After Composite AND Creation")
    void dtoDescriptionAfterAndComposite() {
        String admin = regAndSetup("admin", "C1", "E1", 0);
        String p1 = policyService.createAgeLimitPolicy(admin, "E1", PurchaseTargetType.EVENT, 18).getData();
        String p2 = policyService.createQuantityLimitPolicy(admin, "E1", PurchaseTargetType.EVENT, 1, 2).getData();

        policyService.createAndPolicy(admin, "E1", PurchaseTargetType.EVENT, Arrays.asList(p1, p2));

        List<PurchasePolicyDTO> policies = policyService.getPoliciesForEventAndCompany(admin, "E1", "C1").getData();

        assertEquals(1, policies.size());
        String desc = policies.get(0).description();
        assertTrue(desc.contains("AND") || desc.contains("&&") || desc.toLowerCase().contains("combined"));
        assertTrue(desc.contains("18") && desc.contains("1") && desc.contains("2"));
    }

    @Test @DisplayName("24. Success - DTO Description After Composite OR Creation")
    void dtoDescriptionAfterOrComposite() {
        String admin = regAndSetup("admin", "C1", "E1", 0);
        String p1 = policyService.createAgeLimitPolicy(admin, "E1", PurchaseTargetType.EVENT, 60).getData();
        String p2 = policyService.createQuantityLimitPolicy(admin, "E1", PurchaseTargetType.EVENT, 1, 1).getData();

        policyService.createOrPolicy(admin, "E1", PurchaseTargetType.EVENT, Arrays.asList(p1, p2));

        List<PurchasePolicyDTO> policies = policyService.getPoliciesForEventAndCompany(admin, "E1", "C1").getData();

        String desc = policies.get(0).description();
        assertTrue(desc.contains("OR") || desc.contains("||") || desc.toLowerCase().contains("either"));
    }

    @Test @DisplayName("25. Success - Get Policies for Event with No Policies")
    void getPoliciesEmptyList() {
        String admin = regAndSetup("admin", "C1", "E1", 0);

        List<PurchasePolicyDTO> policies = policyService.getPoliciesForEventAndCompany(admin, "E1", "C1").getData();
        assertTrue(policies.isEmpty());
    }

    @Test @DisplayName("26. Success - Filter Policies by Company Only")
    void getPoliciesFilteringByCompany() {
        String admin = regAndSetup("admin", "C1", "E1", 0);
        policyService.createAgeLimitPolicy(admin, "C1", PurchaseTargetType.COMPANY, 21);

        companyService.CreateCompany("C2", admin);
        eventService.createEvent(admin, "E2", "E2", EventType.PLAY, 100, new Date(), "Loc", "C2", getMap());
        policyService.createAgeLimitPolicy(admin, "E2", PurchaseTargetType.EVENT, 50);

        List<PurchasePolicyDTO> policies = policyService.getPoliciesForEventAndCompany(admin, "E1", "C1").getData();

        assertEquals(1, policies.size());
        assertEquals("C1", policies.get(0).targetId());
    }

    @Test @DisplayName("27. Success - Verify Recursive DTO Description (Nested Composites)")
    void recursiveCompositeDescriptionDTO() {
        String admin = regAndSetup("admin", "C1", "E1", 0);
        String p1 = policyService.createAgeLimitPolicy(admin, "E1", PurchaseTargetType.EVENT, 18).getData();
        String p2 = policyService.createQuantityLimitPolicy(admin, "E1", PurchaseTargetType.EVENT, 1, 1).getData();
        String andId = policyService.createAndPolicy(admin, "E1", PurchaseTargetType.EVENT, Arrays.asList(p1, p2)).getData();

        String p3 = policyService.createAgeLimitPolicy(admin, "E1", PurchaseTargetType.EVENT, 65).getData();
        policyService.createOrPolicy(admin, "E1", PurchaseTargetType.EVENT, Arrays.asList(andId, p3));

        List<PurchasePolicyDTO> policies = policyService.getPoliciesForEventAndCompany(admin, "E1", "C1").getData();

        assertEquals(1, policies.size());
        String desc = policies.get(0).description();
        assertTrue(desc.contains("18") && desc.contains("1") && desc.contains("65"));
    }

    @Test @DisplayName("28. Fail - Get Policies with Invalid Token")
    void getPoliciesInvalidToken() {
        regAndSetup("admin", "C1", "E1", 18);
        Response<List<PurchasePolicyDTO>> resp = policyService.getPoliciesForEventAndCompany("invalid_token", "E1", "C1");
        assertTrue(resp.isError());
    }

    @Test @DisplayName("29. Success - DTO Fields Consistency")
    void dtoFieldsIntegrity() {
        String admin = regAndSetup("admin", "C1", "E1", 25);
        List<PurchasePolicyDTO> policies = policyService.getPoliciesForEventAndCompany(admin, "E1", "C1").getData();

        PurchasePolicyDTO dto = policies.get(0);
        assertNotNull(dto.id());
        assertEquals("E1", dto.targetId());
        assertEquals("EVENT", dto.type());
        assertNotNull(dto.description());
    }

    @Test @DisplayName("30. Success - Multiple Concurrent Policies DTO")
    void multipleConcurrentPoliciesDTO() {
        String admin = regAndSetup("admin", "C1", "E1", 0);

        String p1 = policyService.createAgeLimitPolicy(admin, "E1", PurchaseTargetType.EVENT, 18).getData();
        String p2 = policyService.createQuantityLimitPolicy(admin, "E1", PurchaseTargetType.EVENT, 1, 10).getData();

        policyService.createAndPolicy(admin, "E1", PurchaseTargetType.EVENT, Arrays.asList(p1, p2));

        policyService.createAgeLimitPolicy(admin, "C1", PurchaseTargetType.COMPANY, 12);

        List<PurchasePolicyDTO> policies = policyService.getPoliciesForEventAndCompany(admin, "E1", "C1").getData();

        assertEquals(2, policies.size());

        boolean hasEvent = policies.stream().anyMatch(p -> p.type().equals("EVENT"));
        boolean hasCompany = policies.stream().anyMatch(p -> p.type().equals("COMPANY"));

        assertTrue(hasEvent, "Missing EVENT policy");
        assertTrue(hasCompany, "Missing COMPANY policy");
    }

    @Test
    void CreatecreateQuantityLimitPolicyInValidToken() {
        Response<String> a = policyService.createQuantityLimitPolicy("a", "C1", PurchaseTargetType.EVENT, 18, 100);
        assertTrue(a.isError());
    }

    @Test
    void createAndPolicyInValidToken() {
        Response<String> a = policyService.createAndPolicy("a", "C1", PurchaseTargetType.EVENT, new ArrayList<>());
        assertTrue(a.isError());
    }

    @Test
    void createOrPolicyInValidToken() {
        Response<String> a = policyService.createOrPolicy("a", "C1", PurchaseTargetType.EVENT, new ArrayList<>());
        assertTrue(a.isError());
    }


    @Test @DisplayName("31. Fail - Create Age Limit Policy When User Is Suspended")
    void createAgeLimitPolicy_SuspendedUser_ReturnsError() {
        String adminToken = regAndSetup("admin_policy", "C1", "E1", 0);
        String adminUserId = tokenService.extractUserId(adminToken);

        adminService.suspendUser(adminUserId, "admin", 7);

        Response<String> response = policyService.createAgeLimitPolicy(adminToken, "E1", PurchaseTargetType.EVENT, 18);

        assertTrue(response.isError());
        assertEquals("User is suspended", response.getMessage());
    }

    @Test @DisplayName("32. Fail - Create Quantity Limit Policy When User Is Suspended")
    void createQuantityLimitPolicy_SuspendedUser_ReturnsError() {
        String adminToken = regAndSetup("admin_policy2", "C1", "E1", 0);
        String adminUserId = tokenService.extractUserId(adminToken);

        adminService.suspendUser(adminUserId, "admin", 7);

        Response<String> response = policyService.createQuantityLimitPolicy(adminToken, "E1", PurchaseTargetType.EVENT, 1, 10);

        assertTrue(response.isError());
        assertEquals("User is suspended", response.getMessage());
    }

    @Test @DisplayName("33. Fail - Create AND Policy When User Is Suspended")
    void createAndPolicy_SuspendedUser_ReturnsError() {
        String adminToken = regAndSetup("admin_policy3", "C1", "E1", 0);

        String p1 = policyService.createAgeLimitPolicy(adminToken, "E1", PurchaseTargetType.EVENT, 18).getData();
        String p2 = policyService.createQuantityLimitPolicy(adminToken, "E1", PurchaseTargetType.EVENT, 1, 5).getData();

        String adminUserId = tokenService.extractUserId(adminToken);
        adminService.suspendUser(adminUserId, "admin", 7);

        Response<String> response = policyService.createAndPolicy(adminToken, "E1", PurchaseTargetType.EVENT, Arrays.asList(p1, p2));

        assertTrue(response.isError());
        assertEquals("User is suspended", response.getMessage());
    }

    @Test @DisplayName("34. Fail - Create OR Policy When User Is Suspended")
    void createOrPolicy_SuspendedUser_ReturnsError() {
        String adminToken = regAndSetup("admin_policy4", "C1", "E1", 0);

        String p1 = policyService.createAgeLimitPolicy(adminToken, "E1", PurchaseTargetType.EVENT, 18).getData();
        String p2 = policyService.createQuantityLimitPolicy(adminToken, "E1", PurchaseTargetType.EVENT, 1, 5).getData();

        String adminUserId = tokenService.extractUserId(adminToken);
        adminService.suspendUser(adminUserId, "admin", 7);

        Response<String> response = policyService.createOrPolicy(adminToken, "E1", PurchaseTargetType.EVENT, Arrays.asList(p1, p2));

        assertTrue(response.isError());
        assertEquals("User is suspended", response.getMessage());
    }

}
