package com.ticketing.ticketapp.Config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketing.ticketapp.Appliction.CompanyService;
import com.ticketing.ticketapp.Appliction.EventService;
import com.ticketing.ticketapp.Appliction.LotteryService;
import com.ticketing.ticketapp.Appliction.Response;
import com.ticketing.ticketapp.Appliction.UserService;
import com.ticketing.ticketapp.Domain.AdminAggregate.iAdminRepository;
import com.ticketing.ticketapp.Domain.Company.iCompanyRepository;
import com.ticketing.ticketapp.Domain.Event.EventType;
import com.ticketing.ticketapp.Domain.Event.MapArea;
import com.ticketing.ticketapp.Domain.Notification.INotificationRepository;
import com.ticketing.ticketapp.Domain.Notification.Notification;
import com.ticketing.ticketapp.Domain.PurchasedOrderAggregate.iPurchasedOrderRepository;
import com.ticketing.ticketapp.Domain.Ticket.Ticket;
import com.ticketing.ticketapp.Domain.Ticket.iTicketRepository;
import com.ticketing.ticketapp.Domain.User.IUserRepository;
import com.ticketing.ticketapp.Infastructure.TokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Seeds initial system state by replaying the use-case commands listed in
 * init-state.json (classpath), instead of hardcoding data here.
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);
    private static final String INIT_STATE_RESOURCE = "init-state.json";
    private static final String SYSTEM_ADMIN = "SYSTEM_ADMIN";

    private final UserService userService;
    private final CompanyService companyService;
    private final EventService eventService;
    private final LotteryService lotteryService;
    private final TokenService tokenService;
    private final iTicketRepository ticketRepository;
    private final iPurchasedOrderRepository purchasedOrderRepository;
    private final iAdminRepository adminRepository;
    private final INotificationRepository notificationRepository;
    private final IUserRepository userRepository;
    private final iCompanyRepository companyRepository;
    private final ObjectMapper objectMapper;

    // Session token per username, populated as register_user commands run, so
    // later commands (create_event, seed_purchase, ...) can act as that user.
    private final Map<String, String> tokenByUsername = new HashMap<>();

    public DataInitializer(UserService userService, CompanyService companyService,
                            EventService eventService, LotteryService lotteryService,
                            TokenService tokenService,
                            iTicketRepository ticketRepository,
                            iPurchasedOrderRepository purchasedOrderRepository,
                            iAdminRepository adminRepository,
                            INotificationRepository notificationRepository,
                            IUserRepository userRepository,
                            iCompanyRepository companyRepository,
                            ObjectMapper objectMapper) {
        this.userService = userService;
        this.companyService = companyService;
        this.eventService = eventService;
        this.lotteryService = lotteryService;
        this.tokenService = tokenService;
        this.ticketRepository = ticketRepository;
        this.purchasedOrderRepository = purchasedOrderRepository;
        this.adminRepository = adminRepository;
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (alreadyInitialized()) {
            logger.info("Database already initialized. Skipping {}.", INIT_STATE_RESOURCE);
            return;
        }
        try {
            List<InitCommand> commands = loadCommands();
            for (InitCommand command : commands) {
                execute(command);
            }
            logger.info("DataInitializer: executed {} commands from {}", commands.size(), INIT_STATE_RESOURCE);
        } catch (Exception e) {
            logger.error("DataInitializer failed: {}", e.getMessage(), e);
        }
    }

    // Re-running init-state.json against a database that already has this data
    // would hit unique-constraint violations and crash startup, so bail out early.
    private boolean alreadyInitialized() {
        return userRepository.usernameExists("admin") || companyRepository.getCompany("BGU Events") != null;
    }

    private List<InitCommand> loadCommands() throws Exception {
        try (InputStream in = new ClassPathResource(INIT_STATE_RESOURCE).getInputStream()) {
            return objectMapper.readValue(in, new TypeReference<List<InitCommand>>() {});
        }
    }

    private void execute(InitCommand cmd) throws Exception {
        switch (cmd.getAction()) {
            case "register_user" -> registerUser(cmd);
            case "grant_admin" -> grantAdmin(cmd);
            case "create_company" -> createCompany(cmd);
            case "create_event" -> createEvent(cmd);
            case "configure_lottery" -> configureLottery(cmd);
            case "seed_purchase" -> seedPurchase(cmd);
            case "seed_notification" -> seedNotification(cmd);
            case "mark_last_notification_read" -> markLastNotificationRead(cmd);
            default -> logger.warn("Unknown init-state action: {}", cmd.getAction());
        }
    }

    private void registerUser(InitCommand cmd) {
        String registerToken = tokenService.generateGuestToken();
        userService.register(registerToken, cmd.getUsername(), cmd.getPassword(), cmd.getAge(), cmd.getEmail());

        String loginToken = tokenService.generateGuestToken();
        Response<String> loginResponse = userService.login(loginToken, cmd.getUsername(), cmd.getPassword());
        if (loginResponse.isSuccess()) {
            tokenByUsername.put(cmd.getUsername(), loginResponse.getData());
        } else {
            logger.warn("DataInitializer: could not log in newly registered user '{}': {}",
                    cmd.getUsername(), loginResponse.getMessage());
        }
    }

    private void grantAdmin(InitCommand cmd) {
        String token = requireToken(cmd.getUsername());
        adminRepository.addAdmin(tokenService.extractUserId(token));
    }

    private void createCompany(InitCommand cmd) {
        String founderToken = requireToken(cmd.getFounderUsername());
        companyService.CreateCompany(cmd.getCompany(), founderToken);
    }

    private void createEvent(InitCommand cmd) {
        String token = requireToken(cmd.getCreatorUsername());
        EventType type = EventType.valueOf(cmd.getEventType());
        MapArea[][] map = makeMap(cmd.getMapRows(), cmd.getMapCols());
        Date date = daysFromNow(cmd.getEventDateDaysFromNow());
        eventService.createEvent(token, cmd.getName(), cmd.getArtist(), type, cmd.getPrice(),
                date, cmd.getLocation(), cmd.getCompany(), map);
    }

    private void configureLottery(InitCommand cmd) {
        String token = requireToken(cmd.getCreatorUsername());
        Date endDate = minutesFromNow(cmd.getLotteryEndMinutesFromNow());
        lotteryService.configureLottery(token, cmd.getCompany(), cmd.getEvent(), null, endDate, cmd.getMaxWinners());
        logger.info("DataInitializer: configured lottery for '{}' (closes in {} minutes, {} winners)",
                cmd.getEvent(), cmd.getLotteryEndMinutesFromNow(), cmd.getMaxWinners());
    }

    private void seedPurchase(InitCommand cmd) {
        String buyerToken = requireToken(cmd.getBuyerUsername());
        String buyerId = tokenService.extractUserId(buyerToken);

        List<Ticket> available = ticketRepository.getAvailableTicketsByEventAndCompany(cmd.getCompany(), cmd.getEvent());
        List<String> ticketIds = new ArrayList<>();
        int taken = 0;
        for (Ticket ticket : available) {
            if (taken >= cmd.getCount()) break;
            ticket.purchase();
            ticketIds.add(ticket.getId());
            taken++;
        }
        if (!ticketIds.isEmpty()) {
            purchasedOrderRepository.StorePurchasedOrder(cmd.getCompany(), cmd.getEvent(), ticketIds, buyerId, UUID.randomUUID().toString());
        }
    }

    private void seedNotification(InitCommand cmd) throws Exception {
        String recipientId = resolveNotificationRecipient(cmd.getRecipient());
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("title", cmd.getTitle());
        payload.put("message", cmd.getMessage());
        notificationRepository.save(recipientId, objectMapper.writeValueAsString(payload));
    }

    private void markLastNotificationRead(InitCommand cmd) {
        String recipientId = resolveNotificationRecipient(cmd.getRecipient());
        List<Notification> notifications = notificationRepository.getAll(recipientId);
        if (!notifications.isEmpty()) {
            notificationRepository.markAsRead(recipientId, notifications.get(notifications.size() - 1).getId());
        }
    }

    private String resolveNotificationRecipient(String recipient) {
        if (SYSTEM_ADMIN.equals(recipient)) {
            return SYSTEM_ADMIN;
        }
        return tokenService.extractUserId(requireToken(recipient));
    }

    private String requireToken(String username) {
        String token = tokenByUsername.get(username);
        if (token == null) {
            throw new IllegalStateException("No session for user '" + username
                    + "'. Add a register_user command for this user earlier in " + INIT_STATE_RESOURCE + ".");
        }
        return token;
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
