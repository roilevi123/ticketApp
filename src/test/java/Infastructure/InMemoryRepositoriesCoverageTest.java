package Infastructure;

import com.ticketing.ticketapp.Domain.Company.Company;
import com.ticketing.ticketapp.Domain.Event.Event;
import com.ticketing.ticketapp.Domain.Event.EventType;
import com.ticketing.ticketapp.Domain.Event.MapArea;
import com.ticketing.ticketapp.Domain.Order.ActiveOrder;
import com.ticketing.ticketapp.Domain.OwnerManagerTree.Manager;
import com.ticketing.ticketapp.Domain.OwnerManagerTree.Owner;
import com.ticketing.ticketapp.Domain.OwnerManagerTree.Permission;
import com.ticketing.ticketapp.Domain.OwnerManagerTree.iTreeOfRoleRepository;
import com.ticketing.ticketapp.Domain.PurchasedOrderAggregate.PurchaseOrder;
import com.ticketing.ticketapp.Domain.Ticket.Ticket;
import com.ticketing.ticketapp.Domain.User.Suspension;
import com.ticketing.ticketapp.Domain.User.User;
import com.ticketing.ticketapp.Infastructure.*;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryRepositoriesCoverageTest {

    private static final String COMPANY = "company1";
    private static final String EVENT = "event1";
    private static final String USER = "user1";

    private static final MapArea[][] MAP = new MapArea[][]{
            {MapArea.SEAT, MapArea.STAND},
            {MapArea.SEAT, MapArea.SEAT}
    };

    @Test
    void adminRepository_addCheckDuplicateAndDeleteAll() {
        AdminRepositoryImpl repo = new AdminRepositoryImpl();

        assertFalse(repo.isAdmin("admin1"));

        repo.addAdmin("admin1");
        repo.addAdmin("admin1");

        assertTrue(repo.isAdmin("admin1"));

        repo.deleteAll();

        assertFalse(repo.isAdmin("admin1"));
    }

    @Test
    void companyRepository_fullFlowAndMissingBranches() {
        CompanyRepositoryImpl repo = new CompanyRepositoryImpl();

        assertEquals("", repo.getCompanyDescription(COMPANY));
        assertFalse(repo.isCompanyActive(COMPANY));

        assertThrows(RuntimeException.class, () -> repo.getCompanyFounder(COMPANY));

        repo.store(COMPANY, USER);

        Company company = repo.getCompany(COMPANY);

        assertNotNull(company);
        assertEquals(USER, repo.getCompanyFounder(COMPANY));
        assertTrue(repo.isCompanyActive(COMPANY));
        assertFalse(repo.getCompanyDescription(COMPANY).isBlank());
        assertEquals(1, repo.getActiveCompanies().size());

        company.freezeCompany(USER);
        repo.save(company);

        assertFalse(repo.isCompanyActive(COMPANY));
        assertTrue(repo.getActiveCompanies().isEmpty());

        repo.deleteCompany(COMPANY);

        assertNull(repo.getCompany(COMPANY));
    }

    @Test
    void companyRepository_saveMissingCompanyThrows() {
        CompanyRepositoryImpl repo = new CompanyRepositoryImpl();

        Company company = new Company(COMPANY, USER);

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> repo.save(company)
        );

        assertTrue(ex.getMessage().contains("Company not found"));
    }

    @Test
    void eventRepository_fullFlowSearchDeleteAndExceptions() {
        EventRepositoryImpl repo = new EventRepositoryImpl();

        Date date = new Date(System.currentTimeMillis() + 100_000);

        Event event = repo.store(
                EVENT,
                "artist1",
                EventType.PLAY,
                100.0,
                date,
                "Tel Aviv",
                COMPANY,
                MAP
        );

        assertNotNull(event);
        assertEquals(EVENT, event.getName());

        assertSame(event, repo.getEvent(EVENT, COMPANY));
        assertEquals(event, repo.getEventById(event.getId(), COMPANY));
        assertNull(repo.getEventById(event.getId(), "wrong_company"));

        assertEquals(1, repo.getEventsByCompany(COMPANY).size());
        assertArrayEquals(MAP, repo.getMapArea(COMPANY, EVENT));

        assertEquals(1, repo.searchEvents("artist", COMPANY, EventType.PLAY, 50.0, 150.0, null, null, "Tel Aviv", null).size());
        assertEquals(1, repo.searchEvents("", null, null, null, null, null, null, null, 0.0).size());
        assertTrue(repo.searchEvents("missing", COMPANY, null, null, null, null, null, null, null).isEmpty());

        Event copy = new Event(event);
        copy.setLocation("Haifa");

        Event updated = repo.save(copy);

        assertEquals("Haifa", updated.getLocation());
        assertEquals(1, updated.getVersion());

        assertThrows(RuntimeException.class, () ->
                repo.store(EVENT, "artist1", EventType.PLAY, 100.0, date, "Tel Aviv", COMPANY, MAP)
        );

        assertThrows(RuntimeException.class, () ->
                repo.getMapArea(COMPANY, "missing_event")
        );

        assertThrows(RuntimeException.class, () ->
                repo.save(new Event("missing", COMPANY, null, "missing", "loc", "artist", date, 1, 1, EventType.PLAY, MAP))
        );

        repo.deleteEvent(event.getId(), COMPANY);

        assertNull(repo.getEventById(event.getId(), COMPANY));

        assertThrows(RuntimeException.class, () ->
                repo.deleteEvent("missing_id", COMPANY)
        );
    }

    @Test
    void eventRepository_deleteCompanyEventAndDeleteAll() {
        EventRepositoryImpl repo = new EventRepositoryImpl();

        repo.store("e1", "artist", EventType.PLAY, 100, new Date(), "loc", COMPANY, MAP);
        repo.store("e2", "artist", EventType.PLAY, 100, new Date(), "loc", COMPANY, MAP);
        repo.store("e3", "artist", EventType.PLAY, 100, new Date(), "loc", "other_company", MAP);

        repo.deleteCompanyEvent(COMPANY);

        assertTrue(repo.getEventsByCompany(COMPANY).isEmpty());
        assertEquals(1, repo.getEventsByCompany("other_company").size());

        repo.deleteAllEvents();

        assertTrue(repo.getEventsByCompany("other_company").isEmpty());

        Event firstAfterReset = repo.store("reset", "artist", EventType.PLAY, 100, new Date(), "loc", COMPANY, MAP);
        assertEquals("1", firstAfterReset.getId());
    }

    @Test
    void orderRepository_storeDuplicateExpiredSaveDeleteAndQueries() {
        OrderRepositoryImpl repo = new OrderRepositoryImpl();

        String orderId = repo.store(
                COMPANY,
                EVENT,
                List.of("T1", "T2"),
                USER,
                new Date(System.currentTimeMillis() + 100_000)
        );

        assertEquals(orderId, repo.findById(orderId).getOrderId());
        assertNull(repo.findById(null));
        assertEquals(List.of("T1", "T2"), repo.getTicketsId(USER));
        assertNotNull(repo.getOrder(USER));
        assertEquals(1, repo.getAllActiveOrders().size());

        assertThrows(RuntimeException.class, () ->
                repo.store(COMPANY, EVENT, List.of("T3"), USER, new Date(System.currentTimeMillis() + 100_000))
        );

        ActiveOrder order = repo.findById(orderId);
        repo.save(order);

        ActiveOrder saved = repo.findById(orderId);
        assertEquals(2, saved.getVersion());

        assertThrows(RuntimeException.class, () -> repo.save(order));

        repo.delete(orderId);

        assertNull(repo.findById(orderId));
        assertTrue(repo.getTicketsId(USER).isEmpty());
        assertNull(repo.getOrder(USER));
    }

    @Test
    void orderRepository_existingExpiredOrderIsReplaced() {
        OrderRepositoryImpl repo = new OrderRepositoryImpl();

        repo.store(
                COMPANY,
                EVENT,
                List.of("old"),
                USER,
                new Date(System.currentTimeMillis() - 100_000)
        );

        String newId = repo.store(
                COMPANY,
                EVENT,
                List.of("new"),
                USER,
                new Date(System.currentTimeMillis() + 100_000)
        );

        assertNotNull(newId);
        assertEquals(List.of("new"), repo.getTicketsId(USER));
        assertEquals(1, repo.getAllActiveOrders().size());
    }

    @Test
    void orderRepository_saveMissingOrderThrowsAndUpdateDoesNothing() {
        OrderRepositoryImpl repo = new OrderRepositoryImpl();

        ActiveOrder missing = new ActiveOrder(
                COMPANY,
                EVENT,
                List.of("T1"),
                USER,
                "missing",
                new Date()
        );

        assertThrows(RuntimeException.class, () -> repo.save(missing));

        repo.update(missing);

        assertNull(repo.findById("missing"));
    }

    @Test
    void purchasedOrderRepository_storeQueryDeleteAndNullBranches() {
        PurchasedOrderRepositoryImpl repo = new PurchasedOrderRepositoryImpl();

        repo.StorePurchasedOrder(COMPANY, EVENT, List.of("T1"), USER, "order1");
        repo.StorePurchasedOrder(COMPANY, EVENT, List.of("T2"), "user2", "order2", List.of("EXT1"));

        assertEquals(2, repo.GetAllPurchasedOrders().size());

        PurchaseOrder order1 = repo.getByOrderId("order1");
        assertNotNull(order1);
        assertEquals(USER, order1.getBuyerID());

        PurchaseOrder order2 = repo.getByOrderId("order2");
        assertEquals(List.of("EXT1"), order2.getExternalTicketIds());

        assertEquals(1, repo.getPurchasedOrdersForUser(USER).size());
        assertEquals(2, repo.getPurchasedOrdersForCompany(COMPANY).size());
        assertTrue(repo.getPurchasedOrdersForUser("missing").isEmpty());
        assertTrue(repo.getPurchasedOrdersForCompany("missing").isEmpty());

        repo.deleteByOrderId("order1");

        assertNull(repo.getByOrderId("order1"));

        repo.deleteAll();

        assertTrue(repo.GetAllPurchasedOrders().isEmpty());
    }

    @Test
    void ticketRepository_fullFlowSaveMapQueriesAndExceptions() {
        TicketRepositoryImpl repo = new TicketRepositoryImpl();

        repo.storeTicket(0, 0, EVENT, COMPANY, 100);
        repo.storeTicket(0, 1, EVENT, COMPANY, 100);

        List<Ticket> tickets = repo.getTicketsForEvent(COMPANY, EVENT);

        assertEquals(2, tickets.size());

        Ticket ticket = tickets.get(0);

        assertNotNull(repo.getTicketById(ticket.getId()));
        assertNull(repo.getTicketById("missing"));

        assertEquals(2, repo.getAllTicketsByEventAndCompany(EVENT, COMPANY).size());
        assertNull(repo.getAllTicketsByEventAndCompany("missing", COMPANY));

        assertEquals(2, repo.getAvailableTicketsByEventAndCompany(COMPANY, EVENT).size());
        assertTrue(repo.getAvailableTicketsByEventAndCompany(COMPANY, "missing").isEmpty());

        assertEquals(2, repo.getAllTicketsByCompany(COMPANY).size());
        assertFalse(repo.getTicketsDescription(List.of(ticket.getId())).isBlank());
        assertEquals(1, repo.getTickets(List.of(ticket.getId())).size());

        ticket.purchase();
        repo.save(ticket);

        Ticket updated = repo.getTicketById(ticket.getId());
        assertTrue(updated.isPurchased());
        assertEquals(2, updated.getVersion());

        MapArea[][] takenMap = repo.getMapAreas(COMPANY, EVENT, MAP);
        assertEquals(MapArea.TAKEN, takenMap[0][0]);

        assertThrows(RuntimeException.class, () -> repo.save(ticket));

        Ticket missingEventTicket = new Ticket(0, 0, "missing", COMPANY, "missing_id", 100);
        assertThrows(RuntimeException.class, () -> repo.save(missingEventTicket));

        Ticket missingTicket = new Ticket(0, 0, EVENT, COMPANY, "missing_id", 100);
        assertThrows(RuntimeException.class, () -> repo.save(missingTicket));

        repo.makeMapToTicket(COMPANY, "map_event", MAP, new Date(), 50);
        assertEquals(4, repo.getTicketsForEvent(COMPANY, "map_event").size());

        repo.deleteAllTickets();

        assertTrue(repo.getTicketsForEvent(COMPANY, EVENT).isEmpty());
    }

    @Test
    void treeOfRoleRepository_fullFlowRolesPermissionsDeleteBranches() {
        TreeOfRoleRepositoryImpl repo = new TreeOfRoleRepositoryImpl();

        repo.storeOwner("founder", COMPANY, iTreeOfRoleRepository.FOUNDER_APPOINTER);

        assertTrue(repo.exitsOwner("founder", COMPANY));
        assertTrue(repo.isOwner("founder", COMPANY));
        assertEquals("FOUNDER", repo.getRoleInCompany("founder", COMPANY));

        assertThrows(RuntimeException.class, () ->
                repo.storeManager("founder", COMPANY, Set.of(Permission.MANAGE_INVENTORY), "someone")
        );

        repo.storeOwner("owner1", COMPANY, "founder");

        Owner owner = repo.getOwner("owner1", COMPANY);
        assertNotNull(owner);
        assertFalse(owner.isAccepted());

        owner.acceptAppointment();
        repo.save(owner);

        assertTrue(repo.exitsOwner("owner1", COMPANY));
        assertTrue(repo.isAppointerOwner("owner1", COMPANY, "founder"));
        assertFalse(repo.isAppointerOwner("owner1", COMPANY, "wrong"));
        assertEquals("OWNER", repo.getRoleInCompany("owner1", COMPANY));

        repo.storeManager(
                "manager1",
                COMPANY,
                Set.of(Permission.MANAGE_INVENTORY, Permission.GENERATE_SALES_REPORTS),
                "owner1"
        );

        Manager manager = repo.getManager("manager1", COMPANY);
        assertNotNull(manager);
        assertFalse(manager.isAccepted());

        assertFalse(repo.ManagerPermitedToCreateUpdateDelete("manager1", COMPANY));

        manager.acceptAppointment();
        repo.save(manager);

        assertTrue(repo.isManager("manager1", COMPANY));
        assertTrue(repo.isAppointerManager("manager1", COMPANY, "owner1"));
        assertFalse(repo.isAppointerManager("manager1", COMPANY, "wrong"));

        assertTrue(repo.ManagerPermitedToCreateUpdateDelete("manager1", COMPANY));
        assertTrue(repo.ManagerPermitToSeeTransactions("manager1", COMPANY));

        assertEquals(
                Set.of(Permission.MANAGE_INVENTORY, Permission.GENERATE_SALES_REPORTS),
                repo.getManagerPermissions("manager1", COMPANY)
        );

        assertEquals("MANAGER", repo.getRoleInCompany("manager1", COMPANY));
        assertEquals("MEMBER", repo.getRoleInCompany("regular", COMPANY));

        assertTrue(repo.getUserCompanies("owner1").contains(COMPANY));
        assertTrue(repo.getUserCompanies("manager1").contains(COMPANY));

        assertFalse(repo.ManagerPermitedToCreateUpdateDelete("missing", COMPANY));
        assertFalse(repo.ManagerPermitToSeeTransactions("missing", COMPANY));
        assertFalse(repo.exitsOwner("missing", COMPANY));
        assertFalse(repo.isOwner("missing", COMPANY));
        assertFalse(repo.isManager("missing", COMPANY));

        assertFalse(repo.getAllOwnersByCompany(COMPANY).isEmpty());
        assertFalse(repo.getAllManagersByCompany(COMPANY).isEmpty());

        repo.deleteManager("manager1", COMPANY);
        assertFalse(repo.isManager("manager1", COMPANY));

        repo.deleteOwner("owner1", COMPANY);
        assertFalse(repo.isOwner("owner1", COMPANY));
    }

    @Test
    void treeOfRoleRepository_saveMissingAndDeleteCompanyUserRoles() {
        TreeOfRoleRepositoryImpl repo = new TreeOfRoleRepositoryImpl();

        Owner missingOwner = new Owner("missingOwner", COMPANY, "founder");
        assertThrows(RuntimeException.class, () -> repo.save(missingOwner));

        Manager missingManager = new Manager("missingManager", COMPANY, Set.of(), "founder");
        assertThrows(RuntimeException.class, () -> repo.save(missingManager));

        repo.storeOwner("founder", COMPANY, iTreeOfRoleRepository.FOUNDER_APPOINTER);
        repo.storeOwner("owner1", COMPANY, "founder");
        repo.storeManager("manager1", COMPANY, Set.of(Permission.GENERATE_SALES_REPORTS), "founder");
        repo.storeManager("manager2", "other_company", Set.of(), "founder");

        repo.deleteUserRoles("manager1");
        assertFalse(repo.isManager("manager1", COMPANY));
        assertTrue(repo.isManager("manager2", "other_company"));

        repo.deleteCompanyMangersAndOwners(COMPANY);

        assertTrue(repo.getAllOwnersByCompany(COMPANY).isEmpty());
        assertTrue(repo.getAllManagersByCompany(COMPANY).isEmpty());
        assertTrue(repo.isManager("manager2", "other_company"));

        repo.deleteAllRoles();

        assertTrue(repo.getAllManagersByCompany("other_company").isEmpty());
    }

    @Test
    void treeOfRoleRepository_storeManagerConvertsNonFounderOwner() {
        TreeOfRoleRepositoryImpl repo = new TreeOfRoleRepositoryImpl();

        repo.storeOwner("founder", COMPANY, iTreeOfRoleRepository.FOUNDER_APPOINTER);
        repo.storeOwner("owner_to_convert", COMPANY, "founder");

        assertTrue(repo.isOwner("owner_to_convert", COMPANY));

        repo.storeManager("owner_to_convert", COMPANY, Set.of(), "founder");

        assertFalse(repo.isOwner("owner_to_convert", COMPANY));
        assertTrue(repo.isManager("owner_to_convert", COMPANY));
    }

    @Test
    void userRepository_storeSaveRenameSuspensionsDeleteAndExceptions() {
        UserRepositoryImpl repo = new UserRepositoryImpl();

        User user = repo.Store(USER, "pass", 20, "u@test.com");

        assertNotNull(user);
        assertTrue(repo.usernameExists(USER));
        assertEquals("pass", repo.getUserPassword(USER));
        assertEquals(user.getID(), repo.getUserByUsername(USER).getID());
        assertEquals(USER, repo.getUsernameByID(user.getID()));
        assertNotNull(repo.getUserByID(user.getID()));

        assertThrows(RuntimeException.class, () ->
                repo.Store(USER, "pass", 20, "u@test.com")
        );

        assertThrows(RuntimeException.class, () ->
                repo.getUserPassword("missing")
        );

        assertThrows(RuntimeException.class, () ->
                repo.getUsernameByID("missing")
        );

        User copy = repo.getUserByID(user.getID());
        copy.setName("new_name");
        repo.save(copy);

        assertFalse(repo.usernameExists(USER));
        assertTrue(repo.usernameExists("new_name"));
        assertEquals("new_name", repo.getUsernameByID(user.getID()));

        User another = repo.Store("taken_name", "pass", 20, "t@test.com");
        User renamed = repo.getUserByID(user.getID());
        renamed.setName("taken_name");

        assertThrows(RuntimeException.class, () -> repo.save(renamed));

        User missing = new User("missing", "pass", 20, "m@test.com");
        assertThrows(RuntimeException.class, () -> repo.save(missing));

        repo.deleteUser(another.getID());
        assertNull(repo.getUserByID(another.getID()));

        repo.deleteAll();

        assertNull(repo.getUserByUsername("new_name"));
    }


}