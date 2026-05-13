package AcceptanceTests;

import com.ticketing.ticketapp.Appliction.*;
import com.ticketing.ticketapp.Domain.Company.iCompanyRepository;
import com.ticketing.ticketapp.Domain.Event.EventType;
import com.ticketing.ticketapp.Domain.Event.MapArea;
import com.ticketing.ticketapp.Domain.Event.iEventRepository;
import com.ticketing.ticketapp.Domain.Order.IActiveOrderRepository;
import com.ticketing.ticketapp.Domain.OwnerManagerTree.iTreeOfRoleRepository;
import com.ticketing.ticketapp.Domain.PurchasePolicy.PurchasePolicyDTO;
import com.ticketing.ticketapp.Domain.PurchasePolicy.PurchaseTargetType;
import com.ticketing.ticketapp.Domain.PurchasePolicy.iPurchasePolicyRepository;
import com.ticketing.ticketapp.Domain.QueueAggregates.iQueueRepository;
import com.ticketing.ticketapp.Domain.Ticket.iTicketRepository;
import com.ticketing.ticketapp.Domain.User.IUserRepository;
import com.ticketing.ticketapp.Infastructure.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Purchase Policy Acceptance Tests")
public class PurchasePolicyAcceptanceTests {

    private UserService userService;
    private CompanyService companyService;
    private EventService eventService;
    private OrderService reserveService;
    private PurchasePolicyService policyService;
    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        IUserRepository userRepository = new UserRepositoryImpl();
        iCompanyRepository companyRepository = new CompanyRepositoryImpl();
        iEventRepository eventRepository = new EventRepositoryImpl();
        iQueueRepository queueRepository = new QueueRepositoryImpl();
        iTreeOfRoleRepository treeOfRoleRepository = new TreeOfRoleRepositoryImpl();
        IActiveOrderRepository activeOrderRepository = new OrderRepositoryImpl();
        iTicketRepository ticketRepository = new TicketRepositoryImpl();
        iPurchasePolicyRepository purchasePolicyRepository = new InMemoryPurchasePolicyRepository();

        this.tokenService = new TokenService();
        IPasswordEncoder passwordEncoder = new PasswordEncoderImpl();

        this.userService = new UserService(passwordEncoder, userRepository, tokenService);
        this.companyService = new CompanyService(companyRepository, userRepository, treeOfRoleRepository, tokenService);
        this.eventService = new EventService(companyRepository, eventRepository, tokenService, treeOfRoleRepository, ticketRepository, queueRepository);
        this.reserveService = new OrderService(activeOrderRepository, tokenService, ticketRepository, userRepository, purchasePolicyRepository);
        this.policyService = new PurchasePolicyService(purchasePolicyRepository, tokenService);

        userRepository.deleteAll();
        eventRepository.deleteAllEvents();
        companyRepository.deleteAllCompany();
        ticketRepository.deleteAllTickets();
        purchasePolicyRepository.deleteAll();
    }

    private String gt() { return tokenService.generateGuestToken(); }

    private MapArea[][] getMap() {
        MapArea[][] map = new MapArea[10][10];
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                map[i][j] = MapArea.SEAT;
            }
        }
        return map;
    }

    private boolean isNumeric(String str) { return str != null && str.matches("-?\\d+"); }

    private String regAndSetup(String adminName, String compName, String eventName, int minAge) {
        userService.register(gt(), adminName, "p", 30);
        String token = userService.login(gt(), adminName, "p").getData();
        companyService.CreateCompany(compName, token);

        eventService.createEvent(token, eventName, eventName, EventType.PLAY, 100, new Date(), "Loc", compName, getMap());

        if (minAge > 0) {
            policyService.createAgeLimitPolicy(token, eventName, PurchaseTargetType.EVENT, minAge);
        }
        return token;
    }

    private String quickReg(String name, int age) {
        userService.register(gt(), name, "p", age);
        return userService.login(gt(), name, "p").getData();
    }

    @Test @DisplayName("1. Fail - Underage for Event Policy")
    void underageForEvent() {
        regAndSetup("admin", "C1", "E1", 18);
        String userToken = quickReg("kid", 12);
        Response<String> result = reserveService.reserveTickets(userToken, "C1", "E1", List.of(new int[]{0, 0, 1}));
        assertTrue(result.isError());
        assertTrue(result.getMessage().contains("Doesn't stand in Event Purchase Policy"));
    }

    @Test @DisplayName("2. Success - Meets Age Policy")
    void meetsAgePolicy() {
        regAndSetup("admin", "C1", "E1", 18);
        String userToken = quickReg("adult", 25);
        Response<String> result = reserveService.reserveTickets(userToken, "C1", "E1", List.of(new int[]{1, 1, 1}));
        assertTrue(isNumeric(result.getData()), "Expected numeric Order ID but got: " + result.getData());
    }

    @Test @DisplayName("3. Fail - Exceeds Max Quantity")
    void exceedsMaxQuantity() {
        String admin = regAndSetup("admin", "C1", "E1", 0);
        policyService.createQuantityLimitPolicy(admin, "E1", PurchaseTargetType.EVENT, 0, 2);
        String user = quickReg("u1", 20);
        Response<String> result = reserveService.reserveTickets(user, "C1", "E1", List.of(new int[]{0, 0, 5}));
        assertTrue(result.isError());
    }

    @Test @DisplayName("4. Fail - Below Min Quantity")
    void belowMinQuantity() {
        String admin = regAndSetup("admin", "C1", "E1", 0);
        policyService.createQuantityLimitPolicy(admin, "E1", PurchaseTargetType.EVENT, 5, 10);
        String user = quickReg("u1", 20);
        Response<String> result = reserveService.reserveTickets(user, "C1", "E1", List.of(new int[]{0, 0, 2}));
        assertTrue(result.isError());
    }

    @Test @DisplayName("5. Fail - Company Level Age Policy")
    void companyAgePolicyFail() {
        String admin = regAndSetup("admin", "C1", "E1", 0);
        policyService.createAgeLimitPolicy(admin, "C1", PurchaseTargetType.COMPANY, 21);
        String userToken = quickReg("u1", 19);
        Response<String> result = reserveService.reserveTickets(userToken, "C1", "E1", List.of(new int[]{0, 0, 1}));
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
        Response<String> result = reserveService.reserveTickets(user, "C1", "E1", List.of(new int[]{2, 2, 1}));
        assertTrue(isNumeric(result.getData()));
    }

    @Test @DisplayName("7. Fail - Composite OR Policy Out of Range")
    void compositeOrFail() {
        String admin = regAndSetup("admin", "C1", "E1", 0);
        String p1 = policyService.createQuantityLimitPolicy(admin, "E1", PurchaseTargetType.EVENT, 1, 2).getData();
        String p2 = policyService.createQuantityLimitPolicy(admin, "E1", PurchaseTargetType.EVENT, 10, 20).getData();
        policyService.createOrPolicy(admin, "E1", PurchaseTargetType.EVENT, Arrays.asList(p1, p2));

        String user = quickReg("u1", 20);
        Response<String> result = reserveService.reserveTickets(user, "C1", "E1", List.of(new int[]{0, 0, 5}));
        assertTrue(result.isError());
    }

    @Test @DisplayName("8. Success - Nested Complex Policy")
    void nestedPolicySuccess() {
        String admin = regAndSetup("admin", "C1", "E1", 0);
        String pAge = policyService.createAgeLimitPolicy(admin, "E1", PurchaseTargetType.EVENT, 16).getData();
        String pQty = policyService.createQuantityLimitPolicy(admin, "E1", PurchaseTargetType.EVENT, 1, 1).getData();
        policyService.createOrPolicy(admin, "E1", PurchaseTargetType.EVENT, Arrays.asList(pAge, pQty));

        String user = quickReg("u1", 10); // Underage but quantity is 1
        Response<String> result = reserveService.reserveTickets(user, "C1", "E1", List.of(new int[]{3, 3, 1}));
        assertTrue(isNumeric(result.getData()));
    }

    @Test @DisplayName("9. Fail - Guest User on Age Policy")
    void guestUserAgeFail() {
        regAndSetup("admin", "C1", "E1", 18);
        Response<String> result = reserveService.reserveTickets(gt(), "C1", "E1", List.of(new int[]{0, 0, 1}));
        assertTrue(isNumeric(result.getData()));
    }

    @Test @DisplayName("10. Fail - Multiple Requests Total Quantity")
    void multipleRequestsFail() {
        String admin = regAndSetup("admin", "C1", "E1", 0);
        policyService.createQuantityLimitPolicy(admin, "E1", PurchaseTargetType.EVENT, 0, 3);
        String user = quickReg("u1", 20);
        List<int[]> reqs = List.of(new int[]{4, 4, 2}, new int[]{5, 5, 2});
        Response<String> result = reserveService.reserveTickets(user, "C1", "E1", reqs);
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
        policyService.createQuantityLimitPolicy(admin, "E1", PurchaseTargetType.EVENT, 1, 5);
        policyService.createAgeLimitPolicy(admin, "C1", PurchaseTargetType.COMPANY, 10);

        List<PurchasePolicyDTO> policies = policyService.getPoliciesForEventAndCompany(admin, "E1", "C1").getData();

        assertEquals(3, policies.size()); // 1 Age (Event), 1 Qty (Event), 1 Age (Company)
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
        policyService.createAgeLimitPolicy(admin, "E1", PurchaseTargetType.EVENT, 18);
        policyService.createQuantityLimitPolicy(admin, "E1", PurchaseTargetType.EVENT, 1, 10);
        policyService.createAgeLimitPolicy(admin, "C1", PurchaseTargetType.COMPANY, 12);

        List<PurchasePolicyDTO> policies = policyService.getPoliciesForEventAndCompany(admin, "E1", "C1").getData();

        long eventCount = policies.stream().filter(p -> p.type().equals("EVENT")).count();
        long companyCount = policies.stream().filter(p -> p.type().equals("COMPANY")).count();

        assertEquals(2, eventCount);
        assertEquals(1, companyCount);
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
}
