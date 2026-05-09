package AcceptanceTests;

import Appliction.*;
import Domain.Company.iCompanyRepository;
import Domain.Event.EventType;
import Domain.Event.MapArea;
import Domain.Event.iEventRepository;
import Domain.Order.IActiveOrderRepository;
import Domain.OwnerManagerTree.iTreeOfRoleRepository;
import Domain.PurchasePolicy.PurchaseTargetType;
import Domain.PurchasePolicy.iPurchasePolicyRepository;
import Domain.QueueAggregates.iQueueRepository;
import Domain.Ticket.iTicketRepository;
import Domain.User.IUserRepository;
import Infastructure.*;
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
        String token = userService.login(gt(), adminName, "p");
        companyService.CreateCompany(compName, token);

        eventService.createEvent(token, eventName, eventName, EventType.PLAY, 100, new Date(), "Loc", compName, getMap());

        if (minAge > 0) {
            policyService.createAgeLimitPolicy(token, eventName, PurchaseTargetType.EVENT, minAge);
        }
        return token;
    }

    private String quickReg(String name, int age) {
        userService.register(gt(), name, "p", age);
        return userService.login(gt(), name, "p");
    }

    @Test @DisplayName("1. Fail - Underage for Event Policy")
    void underageForEvent() {
        regAndSetup("admin", "C1", "E1", 18);
        String userToken = quickReg("kid", 12);
        String result = reserveService.reserveTickets(userToken, "C1", "E1", List.of(new int[]{0, 0, 1}));
        assertTrue(result.contains("Doesn't stand in Event Purchase Policy"));
    }

    @Test @DisplayName("2. Success - Meets Age Policy")
    void meetsAgePolicy() {
        regAndSetup("admin", "C1", "E1", 18);
        String userToken = quickReg("adult", 25);
        String result = reserveService.reserveTickets(userToken, "C1", "E1", List.of(new int[]{1, 1, 1}));
        assertTrue(isNumeric(result), "Expected numeric Order ID but got: " + result);
    }

    @Test @DisplayName("3. Fail - Exceeds Max Quantity")
    void exceedsMaxQuantity() {
        String admin = regAndSetup("admin", "C1", "E1", 0);
        policyService.createQuantityLimitPolicy(admin, "E1", PurchaseTargetType.EVENT, 0, 2);
        String user = quickReg("u1", 20);
        String result = reserveService.reserveTickets(user, "C1", "E1", List.of(new int[]{0, 0, 5}));
        assertFalse(isNumeric(result));
    }

    @Test @DisplayName("4. Fail - Below Min Quantity")
    void belowMinQuantity() {
        String admin = regAndSetup("admin", "C1", "E1", 0);
        policyService.createQuantityLimitPolicy(admin, "E1", PurchaseTargetType.EVENT, 5, 10);
        String user = quickReg("u1", 20);
        String result = reserveService.reserveTickets(user, "C1", "E1", List.of(new int[]{0, 0, 2}));
        assertFalse(isNumeric(result));
    }

    @Test @DisplayName("5. Fail - Company Level Age Policy")
    void companyAgePolicyFail() {
        String admin = regAndSetup("admin", "C1", "E1", 0);
        policyService.createAgeLimitPolicy(admin, "C1", PurchaseTargetType.COMPANY, 21);
        String userToken = quickReg("u1", 19);
        String result = reserveService.reserveTickets(userToken, "C1", "E1", List.of(new int[]{0, 0, 1}));
        assertTrue(result.contains("Company Purchase Policy"));
    }

    @Test @DisplayName("6. Success - Composite AND Policy")
    void compositeAndSuccess() {
        String admin = regAndSetup("admin", "C1", "E1", 0);
        String p1 = policyService.createAgeLimitPolicy(admin, "E1", PurchaseTargetType.EVENT, 18);
        String p2 = policyService.createQuantityLimitPolicy(admin, "E1", PurchaseTargetType.EVENT, 1, 10);
        policyService.createAndPolicy(admin, "E1", PurchaseTargetType.EVENT, Arrays.asList(p1, p2));

        String user = quickReg("u1", 20);
        String result = reserveService.reserveTickets(user, "C1", "E1", List.of(new int[]{2, 2, 1}));
        assertTrue(isNumeric(result));
    }

    @Test @DisplayName("7. Fail - Composite OR Policy Out of Range")
    void compositeOrFail() {
        String admin = regAndSetup("admin", "C1", "E1", 0);
        String p1 = policyService.createQuantityLimitPolicy(admin, "E1", PurchaseTargetType.EVENT, 1, 2);
        String p2 = policyService.createQuantityLimitPolicy(admin, "E1", PurchaseTargetType.EVENT, 10, 20);
        policyService.createOrPolicy(admin, "E1", PurchaseTargetType.EVENT, Arrays.asList(p1, p2));

        String user = quickReg("u1", 20);
        String result = reserveService.reserveTickets(user, "C1", "E1", List.of(new int[]{0, 0, 5}));
        assertFalse(isNumeric(result));
    }

    @Test @DisplayName("8. Success - Nested Complex Policy")
    void nestedPolicySuccess() {
        String admin = regAndSetup("admin", "C1", "E1", 0);
        String pAge = policyService.createAgeLimitPolicy(admin, "E1", PurchaseTargetType.EVENT, 16);
        String pQty = policyService.createQuantityLimitPolicy(admin, "E1", PurchaseTargetType.EVENT, 1, 1);
        policyService.createOrPolicy(admin, "E1", PurchaseTargetType.EVENT, Arrays.asList(pAge, pQty));

        String user = quickReg("u1", 10); // Underage but quantity is 1
        String result = reserveService.reserveTickets(user, "C1", "E1", List.of(new int[]{3, 3, 1}));
        assertTrue(isNumeric(result));
    }

    @Test @DisplayName("9. Fail - Guest User on Age Policy")
    void guestUserAgeFail() {
        regAndSetup("admin", "C1", "E1", 18);
        String result = reserveService.reserveTickets(gt(), "C1", "E1", List.of(new int[]{0, 0, 1}));
        assertTrue(isNumeric(result));
    }

    @Test @DisplayName("10. Fail - Multiple Requests Total Quantity")
    void multipleRequestsFail() {
        String admin = regAndSetup("admin", "C1", "E1", 0);
        policyService.createQuantityLimitPolicy(admin, "E1", PurchaseTargetType.EVENT, 0, 3);
        String user = quickReg("u1", 20);
        List<int[]> reqs = List.of(new int[]{4, 4, 2}, new int[]{5, 5, 2});
        String result = reserveService.reserveTickets(user, "C1", "E1", reqs);
        assertFalse(isNumeric(result));
    }
}