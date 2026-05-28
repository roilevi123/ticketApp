package com.ticketing.ticketapp.Config;

import com.ticketing.ticketapp.Appliction.CompanyService;
import com.ticketing.ticketapp.Appliction.EventService;
import com.ticketing.ticketapp.Appliction.LotteryService;
import com.ticketing.ticketapp.Appliction.UserService;
import com.ticketing.ticketapp.Domain.Event.EventType;
import com.ticketing.ticketapp.Domain.Event.MapArea;
import com.ticketing.ticketapp.Domain.AdminAggregate.iAdminRepository;
import com.ticketing.ticketapp.Domain.Notification.INotificationRepository;
import com.ticketing.ticketapp.Infastructure.TokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Calendar;
import java.util.Date;

@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final UserService   userService;
    private final CompanyService companyService;
    private final EventService  eventService;
    private final LotteryService lotteryService;
    private final TokenService  tokenService;
    private final iAdminRepository adminRepository;
    private final INotificationRepository notificationRepository;

    public DataInitializer(UserService userService, CompanyService companyService,
                           EventService eventService, LotteryService lotteryService,
                           TokenService tokenService,
                           iAdminRepository adminRepository,
                           INotificationRepository notificationRepository) {
        this.userService = userService;
        this.companyService = companyService;
        this.eventService = eventService;
        this.lotteryService = lotteryService;
        this.tokenService = tokenService;
        this.adminRepository = adminRepository;
        this.notificationRepository = notificationRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            String guestToken = tokenService.generateGuestToken();
            userService.register(guestToken, "admin", "admin123", 30, "admin@bgu.ac.il");
            String guestToken2 = tokenService.generateGuestToken();
            userService.register(guestToken2, "koren_manager", "koren123", 25, "koren@bgu.ac.il");
            userService.register(tokenService.generateGuestToken(), "s", "s", 25, "s@test.com");
            userService.register(tokenService.generateGuestToken(), "d", "d", 25, "d@test.com");
            String loginToken = tokenService.generateGuestToken();
            String adminToken = userService.login(loginToken, "admin", "admin123").getData();
            adminRepository.addAdmin(tokenService.extractUserId(adminToken));

            companyService.CreateCompany("BGU Events", adminToken);

            MapArea[][] seatedMap = makeMap(8, 10);
            MapArea[][] gaMap     = makeGAMap(5, 100);

            createEvent(adminToken, "Rock Night Live", "The Rolling Stones",
                    EventType.LIVE_PERFORMANCE, 89.99, daysFromNow(14), "BGU Amphitheater", seatedMap);

            createEvent(adminToken, "Classical Evening", "BGU Philharmonic",
                    EventType.LIVE_PERFORMANCE, 45.00, daysFromNow(21), "Marcus Family Campus Hall", seatedMap);

            createEvent(adminToken, "Hamlet", "BGU Theater Ensemble",
                    EventType.PLAY, 35.00, daysFromNow(7), "Soroka Cultural Center", seatedMap);

            createEvent(adminToken, "A Midsummer Night", "Shakespeare Co.",
                    EventType.PLAY, 30.00, daysFromNow(30), "Beer-Sheva Municipal Theater", seatedMap);

            createEvent(adminToken, "Spring Music Festival", "Various Artists",
                    EventType.FESTIVAL, 0.0, daysFromNow(45), "BGU Central Park", gaMap);

            createEvent(adminToken, "Tech & Society Summit", "Prof. Alon Cohen",
                    EventType.CONFERENCE, 20.00, daysFromNow(10), "Engineering Building A", seatedMap);

            createEvent(adminToken, "AI Research Conference", "Dr. Yael Stern",
                    EventType.CONFERENCE, 15.00, daysFromNow(60), "Alon Building, Room 201", seatedMap);

            // ── High-demand lottery event ──────────────────────────────────────
            // Lottery window closes in 5 minutes so it is easy to test locally.
            createEvent(adminToken, "Coldplay World Tour", "Coldplay",
                    EventType.LIVE_PERFORMANCE, 149.99, daysFromNow(90), "BGU Arena", seatedMap);
            Date lotteryEnd = minutesFromNow(5);
            lotteryService.configureLottery(adminToken, "BGU Events", "Coldplay World Tour",
                    null, lotteryEnd, 50);
            logger.info("DataInitializer: seeded high-demand lottery event 'Coldplay World Tour' " +
                    "(lottery closes in 5 minutes, 50 winners)");

            // Seed personal notifications for admin
            String adminUserId = tokenService.extractUserId(adminToken);
            notificationRepository.save(adminUserId, "{\"title\":\"System maintenance scheduled for Sunday 02:00–04:00\",\"message\":\"The ticketing platform will be briefly unavailable during this window.\"}");
            notificationRepository.save(adminUserId, "{\"title\":\"Welcome back, Admin\",\"message\":\"You have full access to the admin dashboard and all system controls.\"}");

            // Seed complaint-style notifications for admin (stored under SYSTEM_ADMIN)
            notificationRepository.save("SYSTEM_ADMIN", "{\"title\":\"Complaint from koren_manager\",\"message\":\"[Double charge on Classical Evening] I was charged twice for the Classical Evening tickets. Please investigate.\"}");
            notificationRepository.save("SYSTEM_ADMIN", "{\"title\":\"Complaint from koren_manager\",\"message\":\"[Seat map showing wrong availability] The seat map for Rock Night Live is showing incorrect availability.\"}");

            // Mark the older personal notification as read
            var adminNotifs = notificationRepository.getAll(adminUserId);
            if (adminNotifs.size() >= 1) notificationRepository.markAsRead(adminUserId, adminNotifs.get(adminNotifs.size() - 1).getId());

            logger.info("DataInitializer: seeded 8 events and notifications successfully");
        } catch (Exception e) {
            logger.error("DataInitializer failed: {}", e.getMessage());
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

    private MapArea[][] makeGAMap(int rows, int cols) {
        MapArea[][] map = new MapArea[rows][cols];
        for (int i = 0; i < rows; i++)
            for (int j = 0; j < cols; j++)
                map[i][j] = MapArea.STAND;
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
