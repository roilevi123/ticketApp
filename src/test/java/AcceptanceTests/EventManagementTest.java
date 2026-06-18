package AcceptanceTests;

import com.ticketing.ticketapp.Appliction.*;
import com.ticketing.ticketapp.Domain.AdminAggregate.iAdminRepository;
import com.ticketing.ticketapp.Domain.Company.iCompanyRepository;
import com.ticketing.ticketapp.Domain.Event.EventType;
import com.ticketing.ticketapp.Domain.Event.MapArea;
import com.ticketing.ticketapp.Domain.Event.iEventRepository;
import com.ticketing.ticketapp.Domain.Order.IActiveOrderRepository;
import com.ticketing.ticketapp.Domain.OwnerManagerTree.Permission;
import com.ticketing.ticketapp.Domain.OwnerManagerTree.iTreeOfRoleRepository;
import com.ticketing.ticketapp.Domain.PurchasedOrderAggregate.iPurchasedOrderRepository;
import com.ticketing.ticketapp.Domain.Discount.iDiscountPolicyRepository;
import com.ticketing.ticketapp.Domain.QueueAggregates.iQueueRepository;
import com.ticketing.ticketapp.Domain.Ticket.iTicketRepository;
import com.ticketing.ticketapp.Domain.User.IUserRepository;
import com.ticketing.ticketapp.Infastructure.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = com.ticketing.ticketapp.TicketappApplication.class)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Transactional
@DisplayName("Complete Event Management Acceptance Tests")
public class EventManagementTest {

    @Autowired
    private IUserRepository userRepository;
    @Autowired private iCompanyRepository companyRepository;
    @Autowired private iEventRepository eventRepository;
    @Autowired private iQueueRepository queueRepository;
    @Autowired private iTreeOfRoleRepository treeOfRoleRepository;
    @Autowired private IActiveOrderRepository activeOrderRepository;
    @Autowired private iTicketRepository ticketRepository;
    @Autowired private iPurchasedOrderRepository purchasedOrderRepository;
    @Autowired private iAdminRepository adminRepository;
    @Autowired private TokenService tokenService;

    private UserService userService;
    private CompanyService companyService;
    private EventService eventService;
    private AdminService adminService;

    @BeforeEach
    void setUp() {
        INotifier notifierMock = mock(INotifier.class);
        IPasswordEncoder passwordEncoder = new PasswordEncoderImpl();

        this.userService = new UserService(passwordEncoder, userRepository, tokenService, new NotificationRepositoryImpl(), notifierMock, treeOfRoleRepository);
        this.companyService = new CompanyService(companyRepository, userRepository, treeOfRoleRepository, tokenService, notifierMock);
        this.eventService = new EventService(companyRepository, eventRepository, tokenService, treeOfRoleRepository, ticketRepository, queueRepository, purchasedOrderRepository, userRepository, notifierMock, mock(iDiscountPolicyRepository.class));

        this.adminService = new AdminService(treeOfRoleRepository, companyRepository, adminRepository, userRepository, purchasedOrderRepository, ticketRepository, eventRepository, tokenService, new NotifierImpl(new Broadcaster(new NotificationRepositoryImpl())), activeOrderRepository);

        activeOrderRepository.deleteAllActiveOrders();
        eventRepository.deleteAllEvents();
        treeOfRoleRepository.deleteAllRoles();
        companyRepository.deleteAllCompany();
        purchasedOrderRepository.deleteAll();
        queueRepository.deleteAll();
        ticketRepository.deleteAllTickets();
        userRepository.deleteAll();
        adminRepository.deleteAll();
        adminRepository.addAdmin("admin");
        tokenService.clearAllData();
    }

    private String gt() {
        return tokenService.generateGuestToken();
    }

    private void reg(String username, String password) {
        userService.register(gt(), username, password, 10, username + "@test.com");
    }

    private String log(String username, String password) {
        return userService.login(gt(), username, password).getData();
    }

    private MapArea[][] getMapArea() {
        MapArea[][] map = new MapArea[10][10];
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                map[i][j] = MapArea.SEAT;
            }
        }
        return map;
    }

    // --- Create Event Tests ---

    @Test
    @DisplayName("1. Create Event As Founder - Success")
    void createEventAsFounderSuccess1() {
        reg("1", "1");
        String token = log("1", "1");
        companyService.CreateCompany("1", token);
        assertTrue(eventService.createEvent(token, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea()).isSuccess());
    }

    @Test
    @DisplayName("2. Create Event As Manager - Success")
    void createEventAsManagerSuccess2() {
        reg("1", "1");
        String token = log("1", "1");
        reg("2", "2");
        String token2 = log("2", "2");
        companyService.CreateCompany("1", token);
        Set<Permission> permissions = new HashSet<>();
        permissions.add(Permission.MANAGE_INVENTORY);
        companyService.AppointAManager("2", "1", permissions, token);
        companyService.ApproveAppointmentForManager(token2, "1");
        assertTrue(eventService.createEvent(token2, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea()).isSuccess());
    }

    @Test
    @DisplayName("3. Create Event As Owner - Success")
    void createEventAsOwnerSuccess3() {
        reg("1", "1");
        String token = log("1", "1");
        reg("2", "2");
        String token2 = log("2", "2");
        companyService.CreateCompany("1", token);
        companyService.AppointOwner("2", "1", token);
        companyService.ApproveAppointmentForOwner(token2, "1");
        assertTrue(eventService.createEvent(token2, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea()).isSuccess());
    }

    @Test
    @DisplayName("4. Create Event - Fail (Not Part of Company)")
    void createEventFailedNotAuthorizedNotPartOfTheCompany4() {
        reg("1", "1");
        String token = log("1", "1");
        reg("2", "2");
        String token2 = log("2", "2");
        companyService.CreateCompany("1", token);
        assertTrue(eventService.createEvent(token2, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea()).isError());
    }

    @Test
    @DisplayName("5. Create Event - Fail (Pending Ownership Approval)")
    void createEventFailedNotAuthorizedNotAprroveTheOwnerShip5() {
        reg("1", "1");
        String token = log("1", "1");
        reg("2", "2");
        String token2 = log("2", "2");
        companyService.CreateCompany("1", token);
        companyService.AppointOwner("2", "1", token);
        assertTrue(eventService.createEvent(token2, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea()).isError());
    }

    @Test
    @DisplayName("6. Create Event - Fail (No MANAGE_INVENTORY Permission)")
    void createEventFailedNotAuthorizedNotHaveTheRightPermission6() {
        reg("1", "1");
        String token = log("1", "1");
        reg("2", "2");
        String token2 = log("2", "2");
        companyService.CreateCompany("1", token);
        Set<Permission> permissions = new HashSet<>();
        permissions.add(Permission.CHANGE_POLICIES);
        companyService.AppointAManager("2", "1", permissions, token);
        companyService.ApproveAppointmentForManager(token2, "1");
        assertTrue(eventService.createEvent(token2, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea()).isError());
    }

    // --- Delete Event Tests ---

    @Test
    @DisplayName("7. Delete Event As Founder - Success")
    void deleteEventAsFounderSuccess7() {
        reg("1", "1");
        String token = log("1", "1");
        companyService.CreateCompany("1", token);

        eventService.createEvent(token, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea());

        var eventsResponse = eventService.getCompanyEvents(token, "1");
        String actualEventId = eventsResponse.getData().get(0).eventId();

        assertTrue(eventService.deleteEvent(actualEventId, "1", token).isSuccess());
    }

    @Test
    @DisplayName("8. Delete Event As Manager - Success")
    void deleteEventAsManagerSuccess8() {
        reg("1", "1");
        String token = log("1", "1");
        reg("2", "2");
        String token2 = log("2", "2");
        companyService.CreateCompany("1", token);

        Set<Permission> permissions = new HashSet<>();
        permissions.add(Permission.MANAGE_INVENTORY);
        companyService.AppointAManager("2", "1", permissions, token);
        companyService.ApproveAppointmentForManager(token2, "1");

        eventService.createEvent(token, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea());

        var companyEvents = eventService.getCompanyEvents(token, "1");
        String actualEventId = companyEvents.getData().get(0).eventId();

        assertTrue(eventService.deleteEvent(actualEventId, "1", token2).isSuccess());
    }

    @Test
    @DisplayName("9. Delete Event As Owner - Success")
    void deleteEventAsOwnerSuccess9() {
        reg("1", "1");
        String token = log("1", "1");
        reg("2", "2");
        String token2 = log("2", "2");
        companyService.CreateCompany("1", token);

        companyService.AppointOwner("2", "1", token);
        companyService.ApproveAppointmentForOwner(token2, "1");

        eventService.createEvent(token, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea());

        var events = eventService.getCompanyEvents(token, "1").getData();
        String eventId = events.get(0).eventId();

        assertTrue(eventService.deleteEvent(eventId, "1", token2).isSuccess());
    }

    @Test
    @DisplayName("10. Delete Event Failed (Event Not Found)")
    void deleteEventFailedNoEventFound10() {
        reg("1", "1");
        String token = log("1", "1");
        companyService.CreateCompany("1", token);
        assertTrue(eventService.deleteEvent("1", "1", token).isError());
    }

    @Test
    @DisplayName("11. Delete Event Failed (Not Part of Company)")
    void deleteEventFailedNotAuthorizedNotPartOfTheCompany11() {
        reg("1", "1");
        String token = log("1", "1");
        reg("2", "2");
        String token2 = log("2", "2");
        companyService.CreateCompany("1", token);
        eventService.createEvent(token, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea());
        assertTrue(eventService.deleteEvent("1", "1", token2).isError());
    }

    @Test
    @DisplayName("12. Delete Event Failed (Pending Ownership Approval)")
    void deleteEventFailedNotAuthorizedNotAprroveTheOwnerShip12() {
        reg("1", "1");
        String token = log("1", "1");
        reg("2", "2");
        String token2 = log("2", "2");
        companyService.CreateCompany("1", token);
        companyService.AppointOwner("2", "1", token);
        eventService.createEvent(token, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea());
        assertTrue(eventService.deleteEvent("1", "1", token2).isError());
    }

    @Test
    @DisplayName("13. Delete Event Failed (Insufficient Permissions)")
    void deleteEventFailedNotAuthorizedNotHaveTheRightPermission13() {
        reg("1", "1");
        String token = log("1", "1");
        reg("2", "2");
        String token2 = log("2", "2");
        companyService.CreateCompany("1", token);
        Set<Permission> permissions = new HashSet<>();
        permissions.add(Permission.CHANGE_POLICIES);
        companyService.AppointAManager("2", "1", permissions, token);
        companyService.ApproveAppointmentForManager(token2, "1");
        eventService.createEvent(token, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea());
        assertTrue(eventService.deleteEvent("1", "1", token2).isError());
    }


    @Test
    @DisplayName("14. Update Event As Founder - Success")
    void updateEventAsFounderSuccess14() {
        reg("1", "1");
        String token = log("1", "1");
        companyService.CreateCompany("1", token);
        eventService.createEvent(token, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea());
        assertTrue(eventService.UpdateEvent(token, "1", "2", EventType.PLAY, 100, new Date(), "1", "1", getMapArea(), 0).isSuccess());
    }

    @Test
    @DisplayName("15. Update Event As Manager - Success")
    void updateEventAsManagerSuccess15() {
        reg("1", "1");
        String token = log("1", "1");
        reg("2", "2");
        String token2 = log("2", "2");
        companyService.CreateCompany("1", token);
        Set<Permission> permissions = new HashSet<>();
        permissions.add(Permission.MANAGE_INVENTORY);
        companyService.AppointAManager("2", "1", permissions, token);
        companyService.ApproveAppointmentForManager(token2, "1");
        eventService.createEvent(token, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea());
        assertTrue(eventService.UpdateEvent(token2, "1", "2", EventType.PLAY, 100, new Date(), "1", "1", getMapArea(), 0).isSuccess());
    }

    @Test
    @DisplayName("16. Update Event As Owner - Success")
    void updateEventAsOwnerSuccess16() {
        reg("1", "1");
        String token = log("1", "1");
        reg("2", "2");
        String token2 = log("2", "2");
        companyService.CreateCompany("1", token);
        companyService.AppointOwner("2", "1", token);
        companyService.ApproveAppointmentForOwner(token2, "1");
        eventService.createEvent(token, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea());
        assertTrue(eventService.UpdateEvent(token2, "1", "2", EventType.PLAY, 100, new Date(), "1", "1", getMapArea(), 0).isSuccess());
    }

    @Test
    @DisplayName("17. Update Event Failed (Event Not Found)")
    void updateEventFailedNoEventFound17() {
        reg("1", "1");
        String token = log("1", "1");
        companyService.CreateCompany("1", token);
        assertTrue(eventService.UpdateEvent(token, "1", "2", EventType.PLAY, 100, new Date(), "1", "1", getMapArea(), 0).isError());
    }

    @Test
    @DisplayName("18. Update Event Failed (Not Part of Company)")
    void updateEventFailedNotAuthorizedNotPartOfTheCompany18() {
        reg("1", "1");
        String token = log("1", "1");
        reg("2", "2");
        String token2 = log("2", "2");
        companyService.CreateCompany("1", token);
        eventService.createEvent(token, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea());
        assertTrue(eventService.UpdateEvent(token2, "1", "2", EventType.PLAY, 100, new Date(), "1", "1", getMapArea(), 0).isError());
    }

    @Test
    @DisplayName("19. Update Event Failed (Pending Ownership Approval)")
    void updateEventFailedNotAuthorizedNotAprroveTheOwnerShip19() {
        reg("1", "1");
        String token = log("1", "1");
        reg("2", "2");
        String token2 = log("2", "2");
        companyService.CreateCompany("1", token);
        companyService.AppointOwner("2", "1", token);
        eventService.createEvent(token, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea());
        assertTrue(eventService.UpdateEvent(token2, "1", "2", EventType.PLAY, 100, new Date(), "1", "1", getMapArea(), 0).isError());
    }

    @Test
    @DisplayName("20. Update Event Failed (Insufficient Permissions)")
    void updateEventFailedNotAuthorizedNotHaveTheRightPermission20() {
        reg("1", "1");
        String token = log("1", "1");
        reg("2", "2");
        String token2 = log("2", "2");
        companyService.CreateCompany("1", token);
        Set<Permission> permissions = new HashSet<>();
        permissions.add(Permission.CHANGE_POLICIES);
        companyService.AppointAManager("2", "1", permissions, token);
        companyService.ApproveAppointmentForManager(token2, "1");
        eventService.createEvent(token, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea());
        assertTrue(eventService.UpdateEvent(token2, "1", "2", EventType.PLAY, 100, new Date(), "1", "1", getMapArea(), 0).isError());
    }

    @Test
    void CreateEventInvalidToken() {
        assertTrue(eventService.createEvent("null", "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea()).isError());
    }

    @Test
    void deleteEventInvalidToken() {
        assertTrue(eventService.deleteEvent("null", "1", "").isError());
    }

    @Test
    void UpdateEventInvalidToken() {
        assertTrue(eventService.UpdateEvent("null", "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea(), 1).isError());
    }

    @Test
    @DisplayName("Create Event - Fail (User Is Suspended)")
    void createEventFailedUserSuspended() {
        reg("owner_user_create", "password123");
        String ownerToken = log("owner_user_create", "password123");
        companyService.CreateCompany("company_create", ownerToken);

        reg("admin", "admin");
        log("admin", "admin");
        String ownerId = userRepository.getUserByUsername("owner_user_create").getID();
        adminService.suspendUser(ownerId, "admin", 7);

        Response<String> response = eventService.createEvent(
                ownerToken, "Suspended Event", "Artist", EventType.PLAY,
                100.0, new Date(), "Location", "company_create", getMapArea()
        );

        assertFalse(response.isSuccess());
        assertTrue(response.isError());
        assertTrue(response.getMessage().contains("User is suspended"));
    }

    @Test
    @DisplayName("Update Event - Fail (User Is Suspended)")
    void updateEventFailedUserSuspended() {
        reg("owner_user_update", "password123");
        String ownerToken = log("owner_user_update", "password123");
        companyService.CreateCompany("company_update", ownerToken);
        eventService.createEvent(ownerToken, "Original Event", "Artist", EventType.PLAY, 100.0, new Date(), "Location", "company_update", getMapArea());

        reg("admin", "admin");
        log("admin", "admin");
        String ownerId = userRepository.getUserByUsername("owner_user_update").getID();
        adminService.suspendUser(ownerId, "admin", 7);

        Response<String> response = eventService.UpdateEvent(
                ownerToken, "Original Event", "New Artist", EventType.PLAY,
                120.0, new Date(), "New Location", "company_update", getMapArea(), 5.0
        );

        assertFalse(response.isSuccess());
        assertTrue(response.isError());
        assertEquals("User is suspended", response.getMessage());
    }

    @Test
    @DisplayName("Delete Event - Fail (User Is Suspended)")
    void deleteEventFailedUserSuspended() {
        reg("owner_user_delete", "password123");
        String ownerToken = log("owner_user_delete", "password123");
        companyService.CreateCompany("company_delete", ownerToken);
        eventService.createEvent(ownerToken, "Event to Delete", "Artist", EventType.PLAY, 100.0, new Date(), "Location", "company_delete", getMapArea());

        String eventId = eventService.getCompanyEvents(ownerToken, "company_delete").getData().get(0).eventId();

        reg("admin", "admin");
        log("admin", "admin");
        String ownerId = userRepository.getUserByUsername("owner_user_delete").getID();
        adminService.suspendUser(ownerId, "admin", 7);

        Response<String> response = eventService.deleteEvent(eventId, "company_delete", ownerToken);

        assertFalse(response.isSuccess());
        assertTrue(response.isError());
        assertTrue(response.getMessage().contains("User is suspended"));
    }
}
