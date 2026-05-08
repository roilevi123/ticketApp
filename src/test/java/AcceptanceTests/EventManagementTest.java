package AcceptanceTests;

import Appliction.*;
import Domain.Company.iCompanyRepository;
import Domain.Event.EventType;
import Domain.Event.MapArea;
import Domain.Event.iEventRepository;
import Domain.Order.IActiveOrderRepository;
import Domain.OwnerManagerTree.Permission;
import Domain.OwnerManagerTree.iTreeOfRoleRepository;
import Domain.PurchasedOrderAggregate.iPurchasedOrderRepository;
import Domain.QueueAggregates.iQueueRepository;
import Domain.Ticket.iTicketRepository;
import Domain.User.IUserRepository;
import Infastructure.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Complete Event Management Acceptance Tests")
public class EventManagementTest {

    private UserService userService;
    private CompanyService companyService;
    private EventService eventService;

    @BeforeEach
    void setUp() {
        // Infrastructure & Repositories
        IUserRepository userRepository = new UserRepositoryImpl();
        iCompanyRepository companyRepository = new CompanyRepositoryImpl();
        iEventRepository eventRepository = new EventRepositoryImpl();
        iQueueRepository queueRepository = new QueueRepositoryImpl();
        iTreeOfRoleRepository treeOfRoleRepository = new TreeOfRoleRepositoryImpl();
        IActiveOrderRepository activeOrderRepository = new OrderRepositoryImpl();
        iTicketRepository ticketRepository = new TicketRepositoryImpl();
        iPurchasedOrderRepository purchasedOrderRepository = new PurchasedOrderRepositoryImpl();
        TokenService tokenService = new TokenService();
        IPasswordEncoder passwordEncoder = new PasswordEncoderImpl();

        // Application Services
        this.userService = new UserService(passwordEncoder, userRepository, tokenService);
        this.companyService = new CompanyService(companyRepository, userRepository, treeOfRoleRepository, tokenService);
        this.eventService = new EventService(companyRepository, eventRepository, tokenService, treeOfRoleRepository, ticketRepository, queueRepository);

        // Data Cleanup
        activeOrderRepository.deleteAllActiveOrders();
        eventRepository.deleteAllEvents();
        treeOfRoleRepository.deleteAllRoles();
        companyRepository.deleteAllCompany();
        purchasedOrderRepository.deleteAll();
        queueRepository.deleteAll();
        ticketRepository.deleteAllTickets();
        userRepository.deleteAll();
        tokenService.clearAllData();
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
        userService.register("1", "1");
        String token = userService.login("1", "1");
        companyService.CreateCompany("1", token);
        Response<String> result = eventService.createEvent(token, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea());
        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("2. Create Event As Manager - Success")
    void createEventAsManagerSuccess2() {
        userService.register("1", "1");
        String token = userService.login("1", "1");
        userService.register("2", "2");
        String token2 = userService.login("2", "2");
        companyService.CreateCompany("1", token);
        Set<Permission> permissions = new HashSet<>();
        permissions.add(Permission.MANAGE_INVENTORY);
        companyService.AppointAManager("2", "1", permissions, token);
        companyService.ApproveAppointmentForManager(token2, "1");
        Response<String> result = eventService.createEvent(token2, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea());
        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("3. Create Event As Owner - Success")
    void createEventAsOwnerSuccess3() {
        userService.register("1", "1");
        String token = userService.login("1", "1");
        userService.register("2", "2");
        String token2 = userService.login("2", "2");
        companyService.CreateCompany("1", token);
        companyService.AppointOwner("2", "1", token);
        companyService.ApproveAppointmentForOwner(token2, "1");
        Response<String> result = eventService.createEvent(token2, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea());
        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("4. Create Event - Fail (Not Part of Company)")
    void createEventFailedNotAuthorizedNotPartOfTheCompany4() {
        userService.register("1", "1");
        String token = userService.login("1", "1");
        userService.register("2", "2");
        String token2 = userService.login("2", "2");
        companyService.CreateCompany("1", token);
        Response<String> result = eventService.createEvent(token2, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea());
        assertFalse(result.isSuccess());
    }

    @Test
    @DisplayName("5. Create Event - Fail (Pending Ownership Approval)")
    void createEventFailedNotAuthorizedNotAprroveTheOwnerShip5() {
        userService.register("1", "1");
        String token = userService.login("1", "1");
        userService.register("2", "2");
        String token2 = userService.login("2", "2");
        companyService.CreateCompany("1", token);
        companyService.AppointOwner("2", "1", token);
        Response<String> result = eventService.createEvent(token2, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea());
        assertFalse(result.isSuccess());
    }

    @Test
    @DisplayName("6. Create Event - Fail (No MANAGE_INVENTORY Permission)")
    void createEventFailedNotAuthorizedNotHaveTheRightPermission6() {
        userService.register("1", "1");
        String token = userService.login("1", "1");
        userService.register("2", "2");
        String token2 = userService.login("2", "2");
        companyService.CreateCompany("1", token);
        Set<Permission> permissions = new HashSet<>();
        permissions.add(Permission.CHANGE_POLICIES);
        companyService.AppointAManager("2", "1", permissions, token);
        companyService.ApproveAppointmentForManager(token2, "1");
        Response<String> result = eventService.createEvent(token2, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea());
        assertFalse(result.isSuccess());
    }

    // --- Delete Event Tests ---

    @Test
    @DisplayName("7. Delete Event As Founder - Success")
    void deleteEventAsFounderSuccess7() {
        userService.register("1", "1");
        String token = userService.login("1", "1");
        companyService.CreateCompany("1", token);
        eventService.createEvent(token, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea());
        Response<Void> result = eventService.deleteEvent("1", "1", token);
        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("8. Delete Event As Manager - Success")
    void deleteEventAsManagerSuccess8() {
        userService.register("1", "1");
        String token = userService.login("1", "1");
        userService.register("2", "2");
        String token2 = userService.login("2", "2");
        companyService.CreateCompany("1", token);
        Set<Permission> permissions = new HashSet<>();
        permissions.add(Permission.MANAGE_INVENTORY);
        companyService.AppointAManager("2", "1", permissions, token);
        companyService.ApproveAppointmentForManager(token2, "1");
        eventService.createEvent(token, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea());
        Response<Void> result = eventService.deleteEvent("1", "1", token2);
        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("9. Delete Event As Owner - Success")
    void deleteEventAsOwnerSuccess9() {
        userService.register("1", "1");
        String token = userService.login("1", "1");
        userService.register("2", "2");
        String token2 = userService.login("2", "2");
        companyService.CreateCompany("1", token);
        companyService.AppointOwner("2", "1", token);
        companyService.ApproveAppointmentForOwner(token2, "1");
        eventService.createEvent(token, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea());
        Response<Void> result = eventService.deleteEvent("1", "1", token2);
        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("10. Delete Event Failed (Event Not Found)")
    void deleteEventFailedNoEventFound10() {
        userService.register("1", "1");
        String token = userService.login("1", "1");
        companyService.CreateCompany("1", token);
        Response<Void> result = eventService.deleteEvent("1", "1", token);
        assertFalse(result.isSuccess());
    }

    @Test
    @DisplayName("11. Delete Event Failed (Not Part of Company)")
    void deleteEventFailedNotAuthorizedNotPartOfTheCompany11() {
        userService.register("1", "1");
        String token = userService.login("1", "1");
        userService.register("2", "2");
        String token2 = userService.login("2", "2");
        companyService.CreateCompany("1", token);
        eventService.createEvent(token, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea());
        Response<Void> result = eventService.deleteEvent("1", "1", token2);
        assertFalse(result.isSuccess());
    }

    @Test
    @DisplayName("12. Delete Event Failed (Pending Ownership Approval)")
    void deleteEventFailedNotAuthorizedNotAprroveTheOwnerShip12() {
        userService.register("1", "1");
        String token = userService.login("1", "1");
        userService.register("2", "2");
        String token2 = userService.login("2", "2");
        companyService.CreateCompany("1", token);
        companyService.AppointOwner("2", "1", token);
        eventService.createEvent(token, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea());
        Response<Void> result = eventService.deleteEvent("1", "1", token2);
        assertFalse(result.isSuccess());
    }

    @Test
    @DisplayName("13. Delete Event Failed (Insufficient Permissions)")
    void deleteEventFailedNotAuthorizedNotHaveTheRightPermission13() {
        userService.register("1", "1");
        String token = userService.login("1", "1");
        userService.register("2", "2");
        String token2 = userService.login("2", "2");
        companyService.CreateCompany("1", token);
        Set<Permission> permissions = new HashSet<>();
        permissions.add(Permission.CHANGE_POLICIES);
        companyService.AppointAManager("2", "1", permissions, token);
        companyService.ApproveAppointmentForManager(token2, "1");
        eventService.createEvent(token, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea());
        Response<Void> result = eventService.deleteEvent("1", "1", token2);
        assertFalse(result.isSuccess());
    }

    // --- Update Event Tests ---

    @Test
    @DisplayName("14. Update Event As Founder - Success")
    void updateEventAsFounderSuccess14() {
        userService.register("1", "1");
        String token = userService.login("1", "1");
        companyService.CreateCompany("1", token);
        eventService.createEvent(token, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea());
        String result = eventService.UpdateEvent(token, "1", "2", EventType.PLAY, 100, new Date(), "1", "1", getMapArea(), 0);
        assertEquals("success", result);
    }

    @Test
    @DisplayName("15. Update Event As Manager - Success")
    void updateEventAsManagerSuccess15() {
        userService.register("1", "1");
        String token = userService.login("1", "1");
        userService.register("2", "2");
        String token2 = userService.login("2", "2");
        companyService.CreateCompany("1", token);
        Set<Permission> permissions = new HashSet<>();
        permissions.add(Permission.MANAGE_INVENTORY);
        companyService.AppointAManager("2", "1", permissions, token);
        companyService.ApproveAppointmentForManager(token2, "1");
        eventService.createEvent(token, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea());
        String result = eventService.UpdateEvent(token2, "1", "2", EventType.PLAY, 100, new Date(), "1", "1", getMapArea(), 0);
        assertEquals("success", result);
    }

    @Test
    @DisplayName("16. Update Event As Owner - Success")
    void updateEventAsOwnerSuccess16() {
        userService.register("1", "1");
        String token = userService.login("1", "1");
        userService.register("2", "2");
        String token2 = userService.login("2", "2");
        companyService.CreateCompany("1", token);
        companyService.AppointOwner("2", "1", token);
        companyService.ApproveAppointmentForOwner(token2, "1");
        eventService.createEvent(token, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea());
        String result = eventService.UpdateEvent(token2, "1", "2", EventType.PLAY, 100, new Date(), "1", "1", getMapArea(), 0);
        assertEquals("success", result);
    }

    @Test
    @DisplayName("17. Update Event Failed (Event Not Found)")
    void updateEventFailedNoEventFound17() {
        userService.register("1", "1");
        String token = userService.login("1", "1");
        companyService.CreateCompany("1", token);
        String result = eventService.UpdateEvent(token, "1", "2", EventType.PLAY, 100, new Date(), "1", "1", getMapArea(), 0);
        assertEquals("failed", result);
    }

    @Test
    @DisplayName("18. Update Event Failed (Not Part of Company)")
    void updateEventFailedNotAuthorizedNotPartOfTheCompany18() {
        userService.register("1", "1");
        String token = userService.login("1", "1");
        userService.register("2", "2");
        String token2 = userService.login("2", "2");
        companyService.CreateCompany("1", token);
        eventService.createEvent(token, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea());
        String result = eventService.UpdateEvent(token2, "1", "2", EventType.PLAY, 100, new Date(), "1", "1", getMapArea(), 0);
        assertEquals("failed", result);
    }

    @Test
    @DisplayName("19. Update Event Failed (Pending Ownership Approval)")
    void updateEventFailedNotAuthorizedNotAprroveTheOwnerShip19() {
        userService.register("1", "1");
        String token = userService.login("1", "1");
        userService.register("2", "2");
        String token2 = userService.login("2", "2");
        companyService.CreateCompany("1", token);
        companyService.AppointOwner("2", "1", token);
        eventService.createEvent(token, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea());
        String result = eventService.UpdateEvent(token2, "1", "2", EventType.PLAY, 100, new Date(), "1", "1", getMapArea(), 0);
        assertEquals("failed", result);
    }

    @Test
    @DisplayName("20. Update Event Failed (Insufficient Permissions)")
    void updateEventFailedNotAuthorizedNotHaveTheRightPermission20() {
        userService.register("1", "1");
        String token = userService.login("1", "1");
        userService.register("2", "2");
        String token2 = userService.login("2", "2");
        companyService.CreateCompany("1", token);
        Set<Permission> permissions = new HashSet<>();
        permissions.add(Permission.CHANGE_POLICIES);
        companyService.AppointAManager("2", "1", permissions, token);
        companyService.ApproveAppointmentForManager(token2, "1");
        eventService.createEvent(token, "1", "1", EventType.PLAY, 100, new Date(), "1", "1", getMapArea());
        String result = eventService.UpdateEvent(token2, "1", "2", EventType.PLAY, 100, new Date(), "1", "1", getMapArea(), 0);
        assertEquals("failed", result);
    }
}