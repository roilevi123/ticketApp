package com.ticketing.ticketapp.Config;

import com.ticketing.ticketapp.Appliction.CompanyService;
import com.ticketing.ticketapp.Appliction.EventService;
import com.ticketing.ticketapp.Appliction.UserService;
import com.ticketing.ticketapp.Domain.Event.EventType;
import com.ticketing.ticketapp.Domain.Event.MapArea;
import com.ticketing.ticketapp.Domain.AdminAggregate.iAdminRepository;
import com.ticketing.ticketapp.Domain.PurchasedOrderAggregate.iPurchasedOrderRepository;
import com.ticketing.ticketapp.Domain.Ticket.Ticket;
import com.ticketing.ticketapp.Domain.Ticket.iTicketRepository;
import com.ticketing.ticketapp.Infastructure.TokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final UserService userService;
    private final CompanyService companyService;
    private final EventService eventService;
    private final TokenService tokenService;
    private final iTicketRepository ticketRepository;
    private final iPurchasedOrderRepository purchasedOrderRepository;
    private final iAdminRepository adminRepository;

    public DataInitializer(UserService userService, CompanyService companyService,
                           EventService eventService, TokenService tokenService,
                           iTicketRepository ticketRepository,
                           iPurchasedOrderRepository purchasedOrderRepository,
                           iAdminRepository adminRepository) {
        this.userService = userService;
        this.companyService = companyService;
        this.eventService = eventService;
        this.tokenService = tokenService;
        this.ticketRepository = ticketRepository;
        this.purchasedOrderRepository = purchasedOrderRepository;
        this.adminRepository = adminRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            String guestToken = tokenService.generateGuestToken();
            userService.register(guestToken, "admin", "admin123", 30, "admin@bgu.ac.il");
            String guestToken2 = tokenService.generateGuestToken();
            userService.register(guestToken2, "koren_manager", "koren123", 25, "koren@bgu.ac.il");
            String loginToken = tokenService.generateGuestToken();
            String adminToken = userService.login(loginToken, "admin", "admin123").getData();
            adminRepository.addAdmin(tokenService.extractUserId(adminToken));

            companyService.CreateCompany("BGU Events", adminToken);

            MapArea[][] map = makeMap(8, 10);

            createEvent(adminToken, "Rock Night Live", "The Rolling Stones",
                    EventType.LIVE_PERFORMANCE, 89.99, daysFromNow(14), "BGU Amphitheater", map);

            createEvent(adminToken, "Classical Evening", "BGU Philharmonic",
                    EventType.LIVE_PERFORMANCE, 45.00, daysFromNow(21), "Marcus Family Campus Hall", map);

            createEvent(adminToken, "Hamlet", "BGU Theater Ensemble",
                    EventType.PLAY, 35.00, daysFromNow(7), "Soroka Cultural Center", map);

            createEvent(adminToken, "A Midsummer Night", "Shakespeare Co.",
                    EventType.PLAY, 30.00, daysFromNow(30), "Beer-Sheva Municipal Theater", map);

            createEvent(adminToken, "Spring Music Festival", "Various Artists",
                    EventType.FESTIVAL, 0.0, daysFromNow(45), "BGU Central Park", map);

            createEvent(adminToken, "Tech & Society Summit", "Prof. Alon Cohen",
                    EventType.CONFERENCE, 20.00, daysFromNow(10), "Engineering Building A", map);

            createEvent(adminToken, "AI Research Conference", "Dr. Yael Stern",
                    EventType.CONFERENCE, 15.00, daysFromNow(60), "Alon Building, Room 201", map);

            // Seed purchased tickets for the admin user so "My Tickets" has test data
            String adminUserId = tokenService.extractUserId(adminToken);
            seedPurchase(adminUserId, "BGU Events", "Rock Night Live",   2);
            seedPurchase(adminUserId, "BGU Events", "Hamlet",            1);
            seedPurchase(adminUserId, "BGU Events", "Classical Evening", 3);

            logger.info("DataInitializer: seeded 7 events and 3 purchase orders successfully");
        } catch (Exception e) {
            logger.error("DataInitializer failed: {}", e.getMessage());
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

}
