package com.ticketing.ticketapp.Config;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import com.ticketing.ticketapp.Appliction.*;
import com.ticketing.ticketapp.Domain.Event.EventType;
import com.ticketing.ticketapp.Domain.Event.MapArea;
import com.ticketing.ticketapp.Domain.AdminAggregate.iAdminRepository;
import com.ticketing.ticketapp.Domain.Notification.INotificationRepository;
import com.ticketing.ticketapp.Domain.PurchasedOrderAggregate.iPurchasedOrderRepository;
import com.ticketing.ticketapp.Domain.Ticket.Ticket;
import com.ticketing.ticketapp.Domain.Ticket.iTicketRepository;
import com.ticketing.ticketapp.Infastructure.TokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import com.ticketing.ticketapp.Domain.Discount.DiscountTargetType;
import com.ticketing.ticketapp.Domain.PurchasePolicy.PurchaseTargetType;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Arrays;
import java.util.stream.Collectors;
@Component
@Profile("!test")
@ConditionalOnProperty(
        name = "initial.state.enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class DataInitializer implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);
    private final Map<String, String> tokens = new HashMap<>();
    private final Map<String, String> ids = new HashMap<>();
    private final UserService   userService;
    private final CompanyService companyService;
    private final EventService  eventService;
    private final LotteryService lotteryService;
    private final TokenService  tokenService;
    private final iTicketRepository ticketRepository;
    private final iPurchasedOrderRepository purchasedOrderRepository;

    private final iAdminRepository adminRepository;
    private final INotificationRepository notificationRepository;
    private final DiscountService discountService;
    private final PurchasePolicyService purchasePolicyService;
    @Value("${initial.state.file:initial-state.txt}")
    private String initialStateFile;
    @Value("${initial.state.skipExisting:true}")
    private boolean skipExisting;
    @Value("${initial.state.reset:false}")
    private boolean resetBeforeInit;
    @Value("${initial.state.enabled:false}")
    private boolean initialStateEnabled;
    public DataInitializer(UserService userService, CompanyService companyService,
                           EventService eventService, LotteryService lotteryService,
                           TokenService tokenService,
                           iTicketRepository ticketRepository,
                           iPurchasedOrderRepository purchasedOrderRepository,
                           iAdminRepository adminRepository,
                           INotificationRepository notificationRepository,
                           DiscountService discountService,PurchasePolicyService purchasePolicyService) {
        this.userService = userService;
        this.companyService = companyService;
        this.eventService = eventService;
        this.lotteryService = lotteryService;
        this.tokenService = tokenService;
        this.ticketRepository = ticketRepository;
        this.purchasedOrderRepository = purchasedOrderRepository;
        this.adminRepository = adminRepository;
        this.notificationRepository = notificationRepository;
        this.discountService = discountService;
        this.purchasePolicyService = purchasePolicyService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!initialStateEnabled) {
            logger.info("DataInitializer: disabled, skipping initial-state config");
            return;
        }

        try {
            if (resetBeforeInit) {
                resetDatabase();
            }

            initializeFromConfig(initialStateFile);
            logger.info("DataInitializer: initial state loaded successfully");

        } catch (Exception e) {
            logger.error("DataInitializer failed: {}", e.getMessage());
            throw new RuntimeException("Initial state failed", e);
        }
    }
    private void resetDatabase() {
        logger.warn("Initial-state: resetting database before initialization");

        purchasedOrderRepository.deleteAll();
        ticketRepository.deleteAllTickets();
        eventService.deleteAllEvents();
        companyService.deleteAll();
        userService.deleteAll();
        adminRepository.deleteAll();
        discountService.deleteAllPolicy();
        purchasePolicyService.deleteAll();

        logger.warn("Initial-state: database reset completed");
    }
    private void initializeFromConfig(String filePath) throws Exception {
        InputStream inputStream =
                getClass().getClassLoader().getResourceAsStream(filePath);

        if (inputStream == null) {
            throw new RuntimeException("File not found: " + filePath);
        }

        List<String> lines = new BufferedReader(
                new InputStreamReader(inputStream)
        ).lines().toList();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();

            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            String[] p = line.split("\\s+");

            try {
                executeAction(p);
            } catch (Exception e) {
                if (skipExisting && isAlreadyExistsError(e)) {
                    logger.warn("Skipping existing data at line {}: {}", i + 1, line);
                    continue;
                }

                throw new RuntimeException(
                        "Initial-state failed at line " + (i + 1) + ": " + line + " | " + e.getMessage(),
                        e
                );
            }
        }
    }
    private boolean isAlreadyExistsError(Exception e) {
        String msg = e.getMessage();

        if (msg == null) {
            return false;
        }

        msg = msg.toLowerCase();

        return msg.contains("already exists")
                || msg.contains("duplicate")
                || msg.contains("constraint")
                || msg.contains("could not execute statement")
                || msg.contains("event already exists")
                || msg.contains("user already exists")
                || msg.contains("company already exists");
    }
    private void executeAction(String[] p) throws Exception {
        String action = p[0];

        switch (action) {

            case "register": {
                // register username password age email
                require(p, 5);

                Response<String> res = userService.register(
                        tokenService.generateGuestToken(),
                        p[1],
                        p[2],
                        Integer.parseInt(p[3]),
                        p[4]
                );

                assertSuccess(res);
                break;
            }

            case "login": {
                // login username password
                require(p, 3);

                Response<String> res = userService.login(
                        tokenService.generateGuestToken(),
                        p[1],
                        p[2]
                );

                assertSuccess(res);
                tokens.put(p[1], res.getData());
                break;
            }
            case "registerAdmin": {
                require(p, 5);

                String username = p[1];
                String password = p[2];
                int age = Integer.parseInt(p[3]);
                String email = p[4];

                Response<String> registerRes = userService.register(
                        tokenService.generateGuestToken(),
                        username,
                        password,
                        age,
                        email
                );

                if (!registerRes.isSuccess() && !isAlreadyExistsMessage(registerRes.getMessage())) {
                    assertSuccess(registerRes);
                }

                Response<String> loginRes = userService.login(
                        tokenService.generateGuestToken(),
                        username,
                        password
                );

                assertSuccess(loginRes);

                String adminToken = loginRes.getData();
                tokens.put(username, adminToken);

                String adminId = tokenService.extractUserId(adminToken);
                adminRepository.addAdmin(adminId);

                break;
            }

            case "create-company": {
                // create-company username companyName
                require(p, 3);

                Response<String> res = companyService.CreateCompany(
                        text(p[2]),
                        tokenOf(p[1])
                );

                assertSuccess(res);

                // CreateCompany מחזיר company/founder token
                tokens.put(p[1], res.getData());
                break;
            }

            case "appoint-manager": {
                // appoint-manager appointerUsername managerUsername companyName
                require(p, 4);

                Response<String> res = companyService.AppointAManager(
                        p[2],
                        text(p[3]),
                        Set.of(),
                        tokenOf(p[1])
                );

                assertSuccess(res);
                break;
            }

            case "approve-manager": {
                // approve-manager username companyName
                require(p, 3);

                Response<String> res = companyService.ApproveAppointmentForManager(
                        tokenOf(p[1]),
                        text(p[2])
                );

                assertSuccess(res);
                break;
            }

            case "appoint-owner": {
                // appoint-owner appointerUsername ownerUsername companyName
                require(p, 4);

                Response<String> res = companyService.AppointOwner(
                        p[2],
                        text(p[3]),
                        tokenOf(p[1])
                );

                assertSuccess(res);
                break;
            }

            case "approve-owner": {
                // approve-owner username companyName
                require(p, 3);

                Response<String> res = companyService.ApproveAppointmentForOwner(
                        tokenOf(p[1]),
                        text(p[2])
                );

                assertSuccess(res);
                break;
            }

            case "create-event": {
                // create-event username companyName eventName artistName type price location daysFromNow
                require(p, 11);

                Response<String> res = eventService.createEvent(
                        tokenOf(p[1]),
                        text(p[3]),
                        text(p[4]),
                        EventType.valueOf(p[5]),
                        Double.parseDouble(p[6]),
                        daysFromNow(Integer.parseInt(p[8])),
                        text(p[7]),
                        text(p[2]),
                        makeMap(Integer.parseInt(p[9]), Integer.parseInt(p[10]))
                );

                assertSuccess(res);
                break;
            }

            case "configure-lottery": {
                // configure-lottery username companyName eventName minutesFromNow maxWinners
                require(p, 6);

                Response<String> res = lotteryService.configureLottery(
                        tokenOf(p[1]),
                        text(p[2]),
                        text(p[3]),
                        null,
                        minutesFromNow(Integer.parseInt(p[4])),
                        Integer.parseInt(p[5])
                );

                assertSuccess(res);
                break;
            }

            case "discount-simple": {
                // discount-simple username targetId targetType percentage companyName saveAs
                require(p, 7);

                Response<String> res = discountService.createSimpleDiscount(
                        tokenOf(p[1]),
                        text(p[2]),
                        DiscountTargetType.valueOf(p[3]),
                        Double.parseDouble(p[4]),
                        text(p[5])
                );

                assertSuccess(res);
                ids.put(p[6], res.getData());
                break;
            }

            case "discount-quantity": {
                // discount-quantity username targetId targetType percentage minQuantity companyName saveAs
                require(p, 8);

                Response<String> res = discountService.createQuantityDiscount(
                        tokenOf(p[1]),
                        text(p[2]),
                        DiscountTargetType.valueOf(p[3]),
                        Double.parseDouble(p[4]),
                        Integer.parseInt(p[5]),
                        text(p[6])
                );

                assertSuccess(res);
                ids.put(p[7], res.getData());
                break;
            }

            case "discount-coupon": {
                // discount-coupon username targetId targetType code percentage companyName saveAs
                require(p, 8);

                Response<String> res = discountService.createCouponDiscount(
                        tokenOf(p[1]),
                        text(p[2]),
                        DiscountTargetType.valueOf(p[3]),
                        p[4],
                        Double.parseDouble(p[5]),
                        text(p[6])
                );

                assertSuccess(res);
                ids.put(p[7], res.getData());
                break;
            }

            case "discount-sum": {
                // discount-sum username targetId targetType companyName saveAs id1,id2,id3
                require(p, 7);

                Response<String> res = discountService.createSumDiscountPolicy(
                        tokenOf(p[1]),
                        text(p[2]),
                        DiscountTargetType.valueOf(p[3]),
                        idList(p[6]),
                        text(p[4])
                );

                assertSuccess(res);
                ids.put(p[5], res.getData());
                break;
            }

            case "discount-max": {
                // discount-max username targetId targetType companyName saveAs id1,id2,id3
                require(p, 7);

                Response<String> res = discountService.createMaxDiscountPolicy(
                        tokenOf(p[1]),
                        text(p[2]),
                        DiscountTargetType.valueOf(p[3]),
                        idList(p[6]),
                        text(p[4])
                );

                assertSuccess(res);
                ids.put(p[5], res.getData());
                break;
            }

            case "policy-age": {
                require(p, 6);

                Response<String> res = purchasePolicyService.createAgeLimitPolicy(
                        tokenOf(p[1]),
                        text(p[2]),
                        PurchaseTargetType.valueOf(p[3]),
                        Integer.parseInt(p[4])
                );

                assertSuccess(res);
                ids.put(p[5], res.getData());
                break;
            }

            case "policy-quantity": {
                require(p, 7);

                Response<String> res = purchasePolicyService.createQuantityLimitPolicy(
                        tokenOf(p[1]),
                        text(p[2]),
                        PurchaseTargetType.valueOf(p[3]),
                        Integer.parseInt(p[4]),
                        Integer.parseInt(p[5])
                );

                assertSuccess(res);
                ids.put(p[6], res.getData());
                break;
            }

            case "policy-and": {
                require(p, 6);

                Response<String> res = purchasePolicyService.createAndPolicy(
                        tokenOf(p[1]),
                        text(p[2]),
                        PurchaseTargetType.valueOf(p[3]),
                        idList(p[5])
                );

                assertSuccess(res);
                ids.put(p[4], res.getData());
                break;
            }

            case "policy-or": {
                require(p, 6);

                Response<String> res = purchasePolicyService.createOrPolicy(
                        tokenOf(p[1]),
                        text(p[2]),
                        PurchaseTargetType.valueOf(p[3]),
                        idList(p[5])
                );

                assertSuccess(res);
                ids.put(p[4], res.getData());
                break;
            }

            default:
                throw new IllegalArgumentException("Unknown action: " + action);
        }
    }
    private void require(String[] p, int expected) {
        if (p.length != expected) {
            throw new IllegalArgumentException(
                    "Expected " + (expected - 1) + " arguments, got " + (p.length - 1)
            );
        }
    }

    private String tokenOf(String username) {
        String token = tokens.get(username);
        if (token == null) {
            throw new IllegalArgumentException("User is not logged in: " + username);
        }
        return token;
    }

    private String text(String value) {
        return value.replace("_", " ");
    }

    private List<String> idList(String csv) {
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .map(id -> ids.getOrDefault(id, id))
                .collect(Collectors.toList());
    }
    private boolean isAlreadyExistsMessage(String msg) {
        if (msg == null) return false;
        msg = msg.toLowerCase();

        return msg.contains("already exists")
                || msg.contains("user already exists")
                || msg.contains("username already exists");
    }
    private void assertSuccess(Response<?> res) {
        if (res == null || !res.isSuccess()) {
            throw new RuntimeException(
                    res == null ? "Response is null" : res.getMessage()
            );
        }
    }
    private void seedPurchase(String buyerID, String company, String event, int count) {
        List<Ticket> available = ticketRepository.getAvailableTicketsByEventAndCompany(company, event);
        List<String> ticketIds = new ArrayList<>();
        int taken = 0;
        for (Ticket ticket : available) {
            if (taken >= count) break;
            ticket.purchase();
            ticketIds.add(ticket.getId());
            taken++;
        }
        if (!ticketIds.isEmpty()) {
            purchasedOrderRepository.StorePurchasedOrder(company, event, ticketIds, buyerID, UUID.randomUUID().toString());
        }
    }

    private void createEvent(String token, String name, String artist,
                             EventType type, double price, Date date,
                             String location, MapArea[][] map) {
        eventService.createEvent(token, name, artist, type, price, date, location, "BGU Events", map);
    }

    private MapArea[][] makeMap(int rows, int cols) {
        MapArea[][] map = new MapArea[rows][cols];
        for (int i = 0; i < rows; i++)
            for (int j = 0; j < cols; j++)
                map[i][j] = MapArea.SEAT;
        return map;
    }

    private Date daysFromNow(int days) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, days);
        return cal.getTime();
    }

    private Date minutesFromNow(int minutes) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MINUTE, minutes);
        return cal.getTime();
    }


}
