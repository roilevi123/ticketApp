package com.ticketing.ticketapp.Config;

import com.ticketing.ticketapp.Appliction.CompanyService;
import com.ticketing.ticketapp.Appliction.EventService;
import com.ticketing.ticketapp.Appliction.UserService;
import com.ticketing.ticketapp.Domain.Event.EventType;
import com.ticketing.ticketapp.Domain.Event.MapArea;
import com.ticketing.ticketapp.Infastructure.TokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import java.util.Set;
import java.util.Calendar;
import java.util.Date;

@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final UserService userService;
    private final CompanyService companyService;
    private final EventService eventService;
    private final TokenService tokenService;

    public DataInitializer(UserService userService, CompanyService companyService,
                           EventService eventService, TokenService tokenService) {
        this.userService = userService;
        this.companyService = companyService;
        this.eventService = eventService;
        this.tokenService = tokenService;
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
            userService.register(guestToken2, "koren1", "koren1234", 25, "koren@bgu.ac.il");

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

            logger.info("DataInitializer: seeded 7 events successfully");
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

    private Date daysFromNow(int days) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, days);
        return cal.getTime();
    }
}
