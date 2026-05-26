package com.ticketing.ticketapp.Domain.Order;

import com.ticketing.ticketapp.Domain.Ticket.TicketDTO;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ActiveOrderTest {

    private ActiveOrder buildOrder() {
        Date expiry = new Date(System.currentTimeMillis() + 60000);
        return new ActiveOrder("CompanyA", "EventX", List.of("T1", "T2"), "user1", "order-1", expiry);
    }

    @Test
    void constructor_SetsAllFields() {
        Date expiry = new Date(System.currentTimeMillis() + 60000);
        ActiveOrder order = new ActiveOrder("CompanyA", "EventX", List.of("T1"), "user1", "order-1", expiry);

        assertEquals("order-1", order.getOrderId());
        assertEquals("user1", order.getUserId());
        assertEquals("EventX", order.getEventId());
        assertEquals("CompanyA", order.getCompanyId());
        assertEquals(List.of("T1"), order.getTicketIds());
        assertEquals(expiry, order.getExpirationTime());
        assertEquals(1, order.getVersion());
    }

    @Test
    void copyConstructor_CopiesAllFields() {
        ActiveOrder original = buildOrder();
        ActiveOrder copy = new ActiveOrder(original);

        assertEquals(original.getOrderId(), copy.getOrderId());
        assertEquals(original.getUserId(), copy.getUserId());
        assertEquals(original.getEventId(), copy.getEventId());
        assertEquals(original.getCompanyId(), copy.getCompanyId());
        assertEquals(original.getTicketIds(), copy.getTicketIds());
        assertEquals(original.getExpirationTime(), copy.getExpirationTime());
        assertEquals(original.getVersion(), copy.getVersion());
    }

    @Test
    void setVersion_UpdatesVersion() {
        ActiveOrder order = buildOrder();
        order.SetVersion(5);
        assertEquals(5, order.getVersion());
    }

    @Test
    void copyConstructor_IsIndependentOfOriginal() {
        ActiveOrder original = buildOrder();
        ActiveOrder copy = new ActiveOrder(original);

        copy.SetVersion(99);

        assertNotEquals(original.getVersion(), copy.getVersion());
    }
}

class ActiveOrderDTOTest {

    @Test
    void create_MapsAllFieldsFromActiveOrder() {
        Date expiry = new Date(System.currentTimeMillis() + 60000);
        ActiveOrder order = new ActiveOrder("CompanyA", "EventX", List.of("T1"), "user1", "order-1", expiry);
        List<TicketDTO> tickets = List.of(new TicketDTO("T1", 0, 0, "EventX", "CompanyA", 50.0, new Date(), false));

        ActiveOrderDTO dto = ActiveOrderDTO.create(order, tickets);

        assertEquals("order-1", dto.orderId());
        assertEquals("user1", dto.userId());
        assertEquals("EventX", dto.eventId());
        assertEquals("CompanyA", dto.companyId());
        assertEquals(expiry, dto.expirationTime());
        assertEquals(1, dto.tickets().size());
        assertEquals("T1", dto.tickets().get(0).id());
    }
}
