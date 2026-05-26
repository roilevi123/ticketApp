package com.ticketing.ticketapp.Controllers;

import com.ticketing.ticketapp.Appliction.OrderService;
import com.ticketing.ticketapp.Appliction.Response;
import com.ticketing.ticketapp.Domain.Ticket.TicketDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderController orderController;

    private static final String TOKEN = "test-token";

    @Test
    void reserveTickets_Success_Returns200() {
        ReserveRequestDTO dto = new ReserveRequestDTO();
        dto.setCompany("CompanyA");
        dto.setEvent("EventX");
        dto.setRequests(List.of(new int[]{1, 2}));

        when(orderService.reserveTickets(TOKEN, "CompanyA", "EventX", dto.getRequests()))
                .thenReturn(Response.success("order-123"));

        ResponseEntity<?> response = orderController.reserveTickets(TOKEN, dto);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("order-123", response.getBody());
    }

    @Test
    void reserveTickets_ServiceFailure_Returns400() {
        ReserveRequestDTO dto = new ReserveRequestDTO();
        dto.setCompany("CompanyA");
        dto.setEvent("EventX");
        dto.setRequests(List.of());

        when(orderService.reserveTickets(TOKEN, "CompanyA", "EventX", List.of()))
                .thenReturn(Response.error("No tickets available"));

        ResponseEntity<?> response = orderController.reserveTickets(TOKEN, dto);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("No tickets available", response.getBody());
    }

    @Test
    void getActiveOrderTickets_Success_Returns200() {
        TicketDTO ticket = mock(TicketDTO.class);
        List<TicketDTO> tickets = List.of(ticket);
        when(orderService.getActiveOrderTickets(TOKEN, "order-123"))
                .thenReturn(Response.success(tickets));

        ResponseEntity<?> response = orderController.getActiveOrderTickets(TOKEN, "order-123");

        assertEquals(200, response.getStatusCode().value());
        assertSame(tickets, response.getBody());
    }

    @Test
    void getActiveOrderTickets_Failure_Returns400() {
        when(orderService.getActiveOrderTickets(TOKEN, "bad-id"))
                .thenReturn(Response.error("Order not found"));

        ResponseEntity<?> response = orderController.getActiveOrderTickets(TOKEN, "bad-id");

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Order not found", response.getBody());
    }
}
