package Infastructure;

import com.ticketing.ticketapp.Domain.AdminAggregate.iAdminRepository;
import com.ticketing.ticketapp.Domain.Company.Company;
import com.ticketing.ticketapp.Domain.Company.CompanyDomainException;
import com.ticketing.ticketapp.Domain.Company.iCompanyRepository;
import com.ticketing.ticketapp.Domain.Event.*;
import com.ticketing.ticketapp.Domain.Lottery.*;
import com.ticketing.ticketapp.Domain.Notification.INotificationRepository;
import com.ticketing.ticketapp.Domain.Notification.Notification;
import com.ticketing.ticketapp.Domain.Order.ActiveOrder;
import com.ticketing.ticketapp.Domain.Order.IActiveOrderRepository;
import com.ticketing.ticketapp.Domain.OwnerManagerTree.*;
import com.ticketing.ticketapp.Domain.PurchasedOrderAggregate.PurchaseOrder;
import com.ticketing.ticketapp.Domain.PurchasedOrderAggregate.iPurchasedOrderRepository;
import com.ticketing.ticketapp.Domain.Ticket.Ticket;
import com.ticketing.ticketapp.Domain.Ticket.iTicketRepository;
import com.ticketing.ticketapp.Domain.User.IUserRepository;
import com.ticketing.ticketapp.Domain.User.User;
import com.ticketing.ticketapp.TicketappApplication;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = TicketappApplication.class)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "repository.type=DB"
})
@Transactional
class DatabaseAdaptersCoverageTest {

    @Autowired private iAdminRepository adminRepository;
    @Autowired private iCompanyRepository companyRepository;
    @Autowired private iEventRepository eventRepository;
    @Autowired private IActiveOrderRepository activeOrderRepository;
    @Autowired private iTicketRepository ticketRepository;
    @Autowired private IUserRepository userRepository;
    @Autowired private iPurchasedOrderRepository purchasedOrderRepository;
    @Autowired private iTreeOfRoleRepository treeOfRoleRepository;
    @Autowired private ILotteryRepository lotteryRepository;
    @Autowired private ILotteryCodeRepository lotteryCodeRepository;
    @Autowired private INotificationRepository notificationRepository;

    private static final String COMPANY = "db_company";
    private static final String EVENT = "db_event";
    private static final String USER = "db_user";

    private static final MapArea[][] MAP = new MapArea[][]{
            {MapArea.SEAT, MapArea.STAND},
            {MapArea.SEAT, MapArea.SEAT}
    };

    @BeforeEach
    void cleanInsideTransaction() {
        purchasedOrderRepository.deleteAll();
        activeOrderRepository.deleteAllActiveOrders();
        ticketRepository.deleteAllTickets();
        eventRepository.deleteAllEvents();
        lotteryCodeRepository.deleteAll();
        lotteryRepository.deleteAll();
        notificationRepository.deleteAll();
        treeOfRoleRepository.deleteAllRoles();
        companyRepository.deleteAllCompany();
        userRepository.deleteAll();
        adminRepository.deleteAll();
    }

    @Test
    void adminRepository_addIsAdminAndDeleteAll() {
        assertFalse(adminRepository.isAdmin("admin1"));

        adminRepository.addAdmin("admin1");

        assertTrue(adminRepository.isAdmin("admin1"));

        adminRepository.deleteAll();

        assertFalse(adminRepository.isAdmin("admin1"));
    }

    @Test
    void companyRepository_storeGetSaveActiveDescriptionAndDelete() {
        companyRepository.store(COMPANY, USER);

        Company company = companyRepository.getCompany(COMPANY);

        assertNotNull(company);
        assertEquals(USER, company.getFounderID());
        assertTrue(companyRepository.isCompanyActive(COMPANY));
        assertEquals(USER, companyRepository.getCompanyFounder(COMPANY));
        assertFalse(companyRepository.getCompanyDescription(COMPANY).isBlank());

        company.freezeCompany(USER);
        companyRepository.save(company);

        assertFalse(companyRepository.isCompanyActive(COMPANY));
        assertTrue(companyRepository.getActiveCompanies().isEmpty());

        companyRepository.deleteCompany(COMPANY);

        assertNull(companyRepository.getCompany(COMPANY));
        assertFalse(companyRepository.isCompanyActive(COMPANY));
        assertEquals("", companyRepository.getCompanyDescription(COMPANY));
    }

    @Test
    void companyRepository_duplicateAndMissingFounderBranches() {
        companyRepository.store(COMPANY, USER);

        assertThrows(
                CompanyDomainException.class,
                () -> companyRepository.store(COMPANY, USER)
        );

        assertThrows(
                CompanyDomainException.class,
                () -> companyRepository.getCompanyFounder("missing_company")
        );
    }

    @Test
    void eventRepository_storeGetSearchMapSaveDeleteCompanyEvents() {
        Date date = new Date(System.currentTimeMillis() + 100_000);

        Event event = eventRepository.store(
                EVENT,
                "artist",
                EventType.PLAY,
                100.0,
                date,
                "Tel Aviv",
                COMPANY,
                MAP
        );

        assertNotNull(event);
        assertNotNull(event.getId());

        assertNotNull(eventRepository.getEvent(EVENT, COMPANY));
        assertNotNull(eventRepository.getEventById(event.getId(), COMPANY));
        assertNull(eventRepository.getEventById(event.getId(), "wrong_company"));

        assertEquals(1, eventRepository.getEventsByCompany(COMPANY).size());
        assertEquals(1, eventRepository.searchEvents("art", COMPANY, EventType.PLAY, 50.0, 200.0, null, null, "Tel Aviv", null).size());
        assertEquals(1, eventRepository.searchEvents(null, null, null, null, null, null, null, null, 0.0).size());

        assertArrayEquals(MAP, eventRepository.getMapArea(COMPANY, EVENT));

        event.setLocation("Haifa");
        eventRepository.save(event);

        assertEquals("Haifa", eventRepository.getEvent(EVENT, COMPANY).getLocation());

        assertThrows(EventDomainException.class, () ->
                eventRepository.store(EVENT, "artist", EventType.PLAY, 100.0, date, "Tel Aviv", COMPANY, MAP)
        );

        eventRepository.deleteCompanyEvent(COMPANY);

        assertTrue(eventRepository.getEventsByCompany(COMPANY).isEmpty());
    }

    @Test
    void eventRepository_deleteEventAndMissingBranches() {
        Date date = new Date(System.currentTimeMillis() + 100_000);

        Event event = eventRepository.store(
                EVENT,
                "artist",
                EventType.PLAY,
                100.0,
                date,
                "Tel Aviv",
                COMPANY,
                MAP
        );

        eventRepository.deleteEvent(event.getId(), COMPANY);

        assertNull(eventRepository.getEventById(event.getId(), COMPANY));

        assertThrows(EventDomainException.class, () ->
                eventRepository.deleteEvent("missing_id", COMPANY)
        );

        assertThrows(EventDomainException.class, () ->
                eventRepository.getMapArea(COMPANY, "missing_event")
        );
    }

    @Test
    void activeOrderRepository_storeSaveUpdateFindGetTicketsDelete() {
        Date expiry = new Date(System.currentTimeMillis() + 100_000);

        String orderId = activeOrderRepository.store(
                COMPANY,
                EVENT,
                List.of("T1", "T2"),
                USER,
                expiry
        );

        assertNotNull(orderId);
        assertNotNull(activeOrderRepository.findById(orderId));
        assertNotNull(activeOrderRepository.getOrder(USER));
        assertEquals(List.of("T1", "T2"), activeOrderRepository.getTicketsId(USER));
        assertEquals(1, activeOrderRepository.getAllActiveOrders().size());

        ActiveOrder order = activeOrderRepository.findById(orderId);
        activeOrderRepository.update(order);
        activeOrderRepository.save(order);

        activeOrderRepository.delete(orderId);

        assertNull(activeOrderRepository.findById(orderId));
        assertTrue(activeOrderRepository.getTicketsId(USER).isEmpty());
    }

    @Test
    void activeOrderRepository_invalidStoreSaveUpdateBranches() {
        assertThrows(RuntimeException.class, () ->
                activeOrderRepository.store(null, EVENT, List.of("T1"), USER, new Date())
        );

        assertThrows(RuntimeException.class, () ->
                activeOrderRepository.store(COMPANY, EVENT, List.of(), USER, new Date())
        );

        assertThrows(RuntimeException.class, () ->
                activeOrderRepository.save(null)
        );

        assertThrows(RuntimeException.class, () ->
                activeOrderRepository.update(null)
        );
    }

    @Test
    void ticketRepository_storeMapQueriesDescriptionTakenMapAndDeleteAll() {
        ticketRepository.storeTicket(0, 0, EVENT, COMPANY, 100.0);

        List<Ticket> tickets = ticketRepository.getTicketsForEvent(COMPANY, EVENT);
        assertEquals(1, tickets.size());

        Ticket ticket = tickets.get(0);
        assertNotNull(ticketRepository.getTicketById(ticket.getId()));

        assertEquals(1, ticketRepository.getAllTicketsByEventAndCompany(EVENT, COMPANY).size());
        assertEquals(1, ticketRepository.getAllTicketsByCompany(COMPANY).size());

        String description = ticketRepository.getTicketsDescription(List.of(ticket.getId()));
        assertFalse(description.isBlank());

        ticket.purchase();
        ticketRepository.save(ticket);

        MapArea[][] updatedMap = ticketRepository.getMapAreas(COMPANY, EVENT, MAP);
        assertEquals(MapArea.TAKEN, updatedMap[0][0]);

        ticketRepository.makeMapToTicket(COMPANY, "map_event", MAP, new Date(), 50.0);
        assertEquals(4, ticketRepository.getTicketsForEvent(COMPANY, "map_event").size());

        assertFalse(ticketRepository.getTickets(List.of(ticket.getId())).isEmpty());

        ticketRepository.deleteAllTickets();

        assertTrue(ticketRepository.getTicketsForEvent(COMPANY, EVENT).isEmpty());
    }

    @Test
    void userRepository_storeFindPasswordSaveDeleteAndDuplicateBranches() {
        User user = userRepository.Store(USER, "pass", 20, "u@test.com");

        assertNotNull(user);
        assertTrue(userRepository.usernameExists(USER));
        assertEquals("pass", userRepository.getUserPassword(USER));
        assertEquals(user.getID(), userRepository.getUserByUsername(USER).getID());
        assertEquals(USER, userRepository.getUsernameByID(user.getID()));
        assertNotNull(userRepository.getUserByID(user.getID()));

        user.setEmail("new@test.com");
        userRepository.save(user);

        assertEquals("new@test.com", userRepository.getUserByID(user.getID()).getEmail());

        assertThrows(RuntimeException.class, () ->
                userRepository.Store(USER, "pass", 20, "u@test.com")
        );

        assertThrows(RuntimeException.class, () ->
                userRepository.getUserPassword("missing_user")
        );

        assertThrows(RuntimeException.class, () ->
                userRepository.getUsernameByID("missing_id")
        );

        userRepository.deleteUser(user.getID());

        assertNull(userRepository.getUserByID(user.getID()));
    }

    @Test
    void purchasedOrderRepository_storeQueryExternalIdsAndDelete() {
        purchasedOrderRepository.StorePurchasedOrder(
                COMPANY,
                EVENT,
                List.of("T1", "T2"),
                USER,
                "order1"
        );

        purchasedOrderRepository.StorePurchasedOrder(
                COMPANY,
                EVENT,
                List.of("T3"),
                "user2",
                "order2",
                List.of("EXT1")
        );

        assertNotNull(purchasedOrderRepository.getByOrderId("order1"));
        assertEquals(2, purchasedOrderRepository.GetAllPurchasedOrders().size());
        assertEquals(1, purchasedOrderRepository.getPurchasedOrdersForUser(USER).size());
        assertEquals(2, purchasedOrderRepository.getPurchasedOrdersForCompany(COMPANY).size());

        PurchaseOrder order2 = purchasedOrderRepository.getByOrderId("order2");
        assertEquals(List.of("EXT1"), order2.getExternalTicketIds());

        purchasedOrderRepository.deleteByOrderId("order1");

        assertNull(purchasedOrderRepository.getByOrderId("order1"));
    }

    @Test
    void treeOfRoleRepository_ownerManagerPermissionsCompaniesRolesAndDelete() {
        treeOfRoleRepository.storeOwner("founder", COMPANY, iTreeOfRoleRepository.FOUNDER_APPOINTER);

        assertTrue(treeOfRoleRepository.exitsOwner("founder", COMPANY));
        assertTrue(treeOfRoleRepository.isOwner("founder", COMPANY));
        assertEquals("FOUNDER", treeOfRoleRepository.getRoleInCompany("founder", COMPANY));

        treeOfRoleRepository.storeOwner("owner1", COMPANY, "founder");
        Owner owner = treeOfRoleRepository.getOwner("owner1", COMPANY);
        assertNotNull(owner);
        assertFalse(owner.isAccepted());

        owner.acceptAppointment();
        treeOfRoleRepository.save(owner);

        assertTrue(treeOfRoleRepository.exitsOwner("owner1", COMPANY));
        assertTrue(treeOfRoleRepository.isAppointerOwner("owner1", COMPANY, "founder"));
        assertEquals("OWNER", treeOfRoleRepository.getRoleInCompany("owner1", COMPANY));

        treeOfRoleRepository.storeManager(
                "manager1",
                COMPANY,
                Set.of(Permission.MANAGE_INVENTORY, Permission.GENERATE_SALES_REPORTS),
                "owner1"
        );

        Manager manager = treeOfRoleRepository.getManager("manager1", COMPANY);
        assertNotNull(manager);
        assertFalse(manager.isAccepted());

        manager.acceptAppointment();
        treeOfRoleRepository.save(manager);

        assertTrue(treeOfRoleRepository.isManager("manager1", COMPANY));
        assertTrue(treeOfRoleRepository.isAppointerManager("manager1", COMPANY, "owner1"));
        assertTrue(treeOfRoleRepository.ManagerPermitedToCreateUpdateDelete("manager1", COMPANY));
        assertTrue(treeOfRoleRepository.ManagerPermitToSeeTransactions("manager1", COMPANY));
        assertEquals(Set.of(Permission.MANAGE_INVENTORY, Permission.GENERATE_SALES_REPORTS),
                treeOfRoleRepository.getManagerPermissions("manager1", COMPANY));

        assertTrue(treeOfRoleRepository.getAllOwnersByCompany(COMPANY).size() >= 2);
        assertEquals(1, treeOfRoleRepository.getAllManagersByCompany(COMPANY).size());

        List<String> companies = treeOfRoleRepository.getUserCompanies("manager1");
        assertTrue(companies.contains(COMPANY));
        assertEquals("MANAGER", treeOfRoleRepository.getRoleInCompany("manager1", COMPANY));
        assertEquals("MEMBER", treeOfRoleRepository.getRoleInCompany("regular", COMPANY));

        treeOfRoleRepository.deleteManager("manager1", COMPANY);
        assertFalse(treeOfRoleRepository.isManager("manager1", COMPANY));

        treeOfRoleRepository.deleteOwner("owner1", COMPANY);
        assertFalse(treeOfRoleRepository.isOwner("owner1", COMPANY));
    }

    @Test
    void treeOfRoleRepository_storeManagerDeletesNonFounderOwnerButRejectsFounder() {
        treeOfRoleRepository.storeOwner("founder", COMPANY, iTreeOfRoleRepository.FOUNDER_APPOINTER);

        assertThrows(OwnerManagerException.class, () ->
                treeOfRoleRepository.storeManager("founder", COMPANY, Set.of(), "someone")
        );

        treeOfRoleRepository.storeOwner("owner_to_convert", COMPANY, "founder");
        assertTrue(treeOfRoleRepository.isOwner("owner_to_convert", COMPANY));

        treeOfRoleRepository.storeManager("owner_to_convert", COMPANY, Set.of(), "founder");

        assertFalse(treeOfRoleRepository.isOwner("owner_to_convert", COMPANY));
        assertTrue(treeOfRoleRepository.isManager("owner_to_convert", COMPANY));
    }

    @Test
    void treeOfRoleRepository_deleteCompanyAndUserRoles() {
        treeOfRoleRepository.storeOwner("founder", COMPANY, iTreeOfRoleRepository.FOUNDER_APPOINTER);
        treeOfRoleRepository.storeManager("manager1", COMPANY, Set.of(), "founder");

        treeOfRoleRepository.deleteUserRoles("manager1");
        assertFalse(treeOfRoleRepository.isManager("manager1", COMPANY));

        treeOfRoleRepository.deleteCompanyMangersAndOwners(COMPANY);

        assertTrue(treeOfRoleRepository.getAllOwnersByCompany(COMPANY).isEmpty());
        assertTrue(treeOfRoleRepository.getAllManagersByCompany(COMPANY).isEmpty());
    }

    @Test
    void lotteryCodeRepository_generateFindValidateMarkUsedFindByUserDelete() {
        LotteryCode code = lotteryCodeRepository.generate(
                USER,
                EVENT,
                COMPANY,
                new Date(System.currentTimeMillis() + 60_000)
        );

        assertNotNull(code);
        assertNotNull(code.getCode());
        assertNotNull(lotteryCodeRepository.findByCode(code.getCode()));
        assertTrue(lotteryCodeRepository.validate(code.getCode(), USER, EVENT, COMPANY));
        assertFalse(lotteryCodeRepository.validate("missing", USER, EVENT, COMPANY));
        assertFalse(lotteryCodeRepository.validate(code.getCode(), "wrong", EVENT, COMPANY));

        assertEquals(1, lotteryCodeRepository.findByUser(USER).size());

        lotteryCodeRepository.markUsed(code.getCode());
        assertFalse(lotteryCodeRepository.validate(code.getCode(), USER, EVENT, COMPANY));

        lotteryCodeRepository.markUsed("missing");

        lotteryCodeRepository.deleteAll();
        assertTrue(lotteryCodeRepository.findByUser(USER).isEmpty());
    }

    @Test
    void lotteryRepository_configureRegisterFindDrawClosedBranches() {
        Date start = new Date(System.currentTimeMillis() - 60_000);
        Date end = new Date(System.currentTimeMillis() + 60_000);

        lotteryRepository.configure(EVENT, COMPANY, start, end, 2);

        assertTrue(lotteryRepository.exists(EVENT, COMPANY));
        assertNotNull(lotteryRepository.find(EVENT, COMPANY));

        assertTrue(lotteryRepository.register(EVENT, COMPANY, USER));
        assertFalse(lotteryRepository.register(EVENT, COMPANY, USER));

        assertTrue(lotteryRepository.isRegistered(EVENT, COMPANY, USER));
        assertFalse(lotteryRepository.isRegistered(EVENT, COMPANY, "other"));
        assertFalse(lotteryRepository.isRegistered("missing", COMPANY, USER));

        lotteryRepository.configure(EVENT, COMPANY, start, end, 5);

        lotteryRepository.markDrawn(EVENT, COMPANY);
        assertTrue(lotteryRepository.find(EVENT, COMPANY).isDrawn());

        assertThrows(LotteryDomainException.class, () ->
                lotteryRepository.register(EVENT, COMPANY, "new_user")
        );

        lotteryRepository.markDrawn("missing", COMPANY);

        lotteryRepository.deleteAll();

        assertFalse(lotteryRepository.exists(EVENT, COMPANY));
        assertNull(lotteryRepository.find(EVENT, COMPANY));
    }

    @Test
    void lotteryRepository_notConfiguredNotOpenedClosedUndrawnBranches() {
        assertThrows(LotteryDomainException.class, () ->
                lotteryRepository.register("missing", COMPANY, USER)
        );

        lotteryRepository.configure(
                "future_event",
                COMPANY,
                new Date(System.currentTimeMillis() + 60_000),
                new Date(System.currentTimeMillis() + 120_000),
                2
        );

        assertThrows(LotteryDomainException.class, () ->
                lotteryRepository.register("future_event", COMPANY, USER)
        );

        lotteryRepository.configure(
                "closed_event",
                COMPANY,
                new Date(System.currentTimeMillis() - 120_000),
                new Date(System.currentTimeMillis() - 60_000),
                2
        );

        assertTrue(
                lotteryRepository.findClosedUndrawn()
                        .stream()
                        .anyMatch(lr -> lr.getEventName().equals("closed_event"))
        );

        assertThrows(LotteryDomainException.class, () ->
                lotteryRepository.register("closed_event", COMPANY, USER)
        );
    }

    @Test
    void notificationRepository_saveReadUnreadMarkAllMissingAndDelete() {
        notificationRepository.save(USER, "message1");
        notificationRepository.save(USER, "message2");
        notificationRepository.save("other", "message3");

        List<Notification> all = notificationRepository.getAll(USER);
        assertEquals(2, all.size());

        assertEquals(2, notificationRepository.getUnread(USER).size());

        String notificationId = all.get(0).getId();

        notificationRepository.markAsRead(USER, notificationId);
        assertEquals(1, notificationRepository.getUnread(USER).size());

        notificationRepository.markAsUnread(USER, notificationId);
        assertEquals(2, notificationRepository.getUnread(USER).size());

        notificationRepository.markAllAsRead(USER);
        assertTrue(notificationRepository.getUnread(USER).isEmpty());

        notificationRepository.markAsRead("wrong_user", notificationId);
        notificationRepository.markAsUnread("wrong_user", notificationId);
        notificationRepository.markAsRead(USER, "missing_id");
        notificationRepository.markAsUnread(USER, "missing_id");

        notificationRepository.deleteAll();

        assertTrue(notificationRepository.getAll(USER).isEmpty());
    }
}