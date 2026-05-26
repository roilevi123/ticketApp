package Infastructure;

import com.ticketing.ticketapp.Domain.Event.MapArea;
import com.ticketing.ticketapp.Domain.Order.ActiveOrder;
import com.ticketing.ticketapp.Domain.Ticket.Ticket;
import com.ticketing.ticketapp.Infastructure.OrderRepositoryImpl;
import com.ticketing.ticketapp.Infastructure.TicketRepositoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OrderAndTicketRepositoryTest {

    // --- OrderRepositoryImpl uncovered methods ---

    private OrderRepositoryImpl orderRepo;
    private TicketRepositoryImpl ticketRepo;
    private static final String COMPANY = "Co";
    private static final String EVENT = "Show";
    private static final String USER = "user1";

    @BeforeEach
    void setUp() {
        orderRepo = new OrderRepositoryImpl();
        ticketRepo = new TicketRepositoryImpl();
    }

    @Test
    void store_ExpiredOrder_StoresNewOrderForSameUser() {
        Date past = new Date(System.currentTimeMillis() - 100_000);
        String oldId = orderRepo.store(COMPANY, EVENT, List.of("t1"), USER, past);

        Date future = new Date(System.currentTimeMillis() + 100_000);
        String newId = orderRepo.store(COMPANY, EVENT, List.of("t2"), USER, future);

        assertNotEquals(oldId, newId);
        assertNotNull(orderRepo.findById(newId));
    }

    @Test
    void save_UpdatesOrder() {
        Date future = new Date(System.currentTimeMillis() + 100_000);
        String id = orderRepo.store(COMPANY, EVENT, List.of("t1"), USER, future);
        ActiveOrder order = orderRepo.findById(id);

        orderRepo.save(order);
        assertNotNull(orderRepo.findById(id));
    }

    @Test
    void save_NotFound_Throws() {
        ActiveOrder phantom = new ActiveOrder(COMPANY, EVENT, List.of(), USER, "phantom-id", new Date());
        assertThrows(RuntimeException.class, () -> orderRepo.save(phantom));
    }

    @Test
    void save_VersionMismatch_Throws() {
        Date future = new Date(System.currentTimeMillis() + 100_000);
        String id = orderRepo.store(COMPANY, EVENT, List.of("t1"), USER, future);
        ActiveOrder order = orderRepo.findById(id);
        ActiveOrder staleOrder = new ActiveOrder(order);
        staleOrder.SetVersion(99);
        assertThrows(RuntimeException.class, () -> orderRepo.save(staleOrder));
    }

    @Test
    void getAllActiveOrders_ReturnsAll() {
        Date future = new Date(System.currentTimeMillis() + 100_000);
        orderRepo.store(COMPANY, EVENT, List.of("t1"), "u1", future);
        orderRepo.store(COMPANY, EVENT, List.of("t2"), "u2", future);
        assertEquals(2, orderRepo.getAllActiveOrders().size());
    }

    @Test
    void deleteAllActiveOrders_ClearsAll() {
        Date future = new Date(System.currentTimeMillis() + 100_000);
        orderRepo.store(COMPANY, EVENT, List.of("t1"), USER, future);
        orderRepo.deleteAllActiveOrders();
        assertTrue(orderRepo.getAllActiveOrders().isEmpty());
    }

    @Test
    void getTicketsId_ReturnsTicketsForUser() {
        Date future = new Date(System.currentTimeMillis() + 100_000);
        orderRepo.store(COMPANY, EVENT, List.of("t1", "t2"), USER, future);
        List<String> ids = orderRepo.getTicketsId(USER);
        assertEquals(2, ids.size());
    }

    @Test
    void getTicketsId_UnknownUser_ReturnsEmpty() {
        assertTrue(orderRepo.getTicketsId("ghost").isEmpty());
    }

    // --- TicketRepositoryImpl uncovered methods ---

    @Test
    void getTicketsDescription_ReturnsFormattedString() {
        ticketRepo.storeTicket(0, 0, EVENT, COMPANY, 50.0);
        Ticket t = ticketRepo.getTicketsForEvent(COMPANY, EVENT).get(0);
        String desc = ticketRepo.getTicketsDescription(List.of(t.getId()));
        assertNotNull(desc);
        assertFalse(desc.isBlank());
    }

    @Test
    void getTickets_ReturnsByIds() {
        ticketRepo.storeTicket(0, 0, EVENT, COMPANY, 50.0);
        ticketRepo.storeTicket(0, 1, EVENT, COMPANY, 60.0);
        List<Ticket> all = ticketRepo.getTicketsForEvent(COMPANY, EVENT);
        List<String> ids = all.stream().map(Ticket::getId).toList();
        List<Ticket> fetched = ticketRepo.getTickets(ids);
        assertEquals(2, fetched.size());
    }

    @Test
    void getMapAreas_MarksPurchasedTicketsAsTaken() {
        ticketRepo.storeTicket(0, 0, EVENT, COMPANY, 50.0);
        Ticket t = ticketRepo.getTicketsForEvent(COMPANY, EVENT).get(0);
        t.purchase();
        ticketRepo.save(t);

        MapArea[][] base = {{MapArea.SEAT}};
        MapArea[][] result = ticketRepo.getMapAreas(COMPANY, EVENT, base);
        assertEquals(MapArea.TAKEN, result[0][0]);
    }

    @Test
    void getMapAreas_UnpurchasedTickets_KeepsOriginalArea() {
        ticketRepo.storeTicket(0, 0, EVENT, COMPANY, 50.0);
        MapArea[][] base = {{MapArea.SEAT}};
        MapArea[][] result = ticketRepo.getMapAreas(COMPANY, EVENT, base);
        assertEquals(MapArea.SEAT, result[0][0]);
    }

    @Test
    void getAllTicketsByEventAndCompany_ReturnsTickets() {
        ticketRepo.storeTicket(0, 0, EVENT, COMPANY, 50.0);
        List<Ticket> tickets = ticketRepo.getAllTicketsByEventAndCompany(EVENT, COMPANY);
        assertNotNull(tickets);
        assertEquals(1, tickets.size());
    }

    @Test
    void getAllTicketsByEventAndCompany_NotFound_ReturnsNull() {
        assertNull(ticketRepo.getAllTicketsByEventAndCompany("NoEvent", COMPANY));
    }

    @Test
    void getAllTicketsByCompany_ReturnsAllCompanyTickets() {
        ticketRepo.storeTicket(0, 0, "Event1", COMPANY, 50.0);
        ticketRepo.storeTicket(0, 0, "Event2", COMPANY, 60.0);
        ticketRepo.storeTicket(0, 0, "Event3", "TotallyDifferent", 70.0);
        List<Ticket> tickets = ticketRepo.getAllTicketsByCompany(COMPANY);
        assertEquals(2, tickets.size());
    }

    @Test
    void makeMapToTicket_CreatesTicketsForSeatsAndStands() {
        MapArea[][] map = {
            {MapArea.SEAT, MapArea.STAND, MapArea.STAGE},
            {MapArea.ENTRANCE, MapArea.SEAT, MapArea.SEAT}
        };
        ticketRepo.makeMapToTicket(COMPANY, EVENT, map, new Date(), 30.0);
        List<Ticket> tickets = ticketRepo.getAllTicketsByEventAndCompany(EVENT, COMPANY);
        assertNotNull(tickets);
        assertEquals(4, tickets.size()); // 3 SEATs + 1 STAND
    }

    @Test
    void deleteAllTickets_ClearsAll() {
        ticketRepo.storeTicket(0, 0, EVENT, COMPANY, 50.0);
        ticketRepo.deleteAllTickets();
        assertTrue(ticketRepo.getTicketsForEvent(COMPANY, EVENT).isEmpty());
    }
}
