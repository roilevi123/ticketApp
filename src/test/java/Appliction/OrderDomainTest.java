package Appliction;

import Domain.Domains.OrderDomain;
import Domain.Ticket.Ticket;
import Domain.User.IUserRepository;
import Infastructure.OrderRepositoryImpl;
import Infastructure.TicketRepositoryImpl;
import Infastructure.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

class OrderDomainTest {

    private OrderDomain reserveTicketService;
    private TicketRepositoryImpl ticketRepository;
    private OrderRepositoryImpl orderRepository;

    @Mock
    private TokenService tokenService;
    @Mock
    private IUserRepository userRepository;

    private final String TOKEN = "valid_token";
    private final String USERNAME = "test_user";
    private final String COMPANY = "test_company";
    private final String EVENT = "test_event";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ticketRepository = spy(new TicketRepositoryImpl());
        orderRepository = spy(new OrderRepositoryImpl());
        reserveTicketService = new OrderDomain(orderRepository, tokenService, ticketRepository);
    }

    @Test
    void reserveTickets_Success_UpdatesRepositories() {
        Ticket t1 = new Ticket(0, 0, EVENT, COMPANY, "id1", 100);
        Ticket t2 = new Ticket(1, 1, EVENT, COMPANY, "id2", 100);


        ticketRepository.storeTicket(0, 0, EVENT,COMPANY,100);
        ticketRepository.storeTicket(1, 1, EVENT,COMPANY,100);

//        when(ticketRepository.getAvailableTicketsByEventAndCompany(COMPANY, EVENT))
//                .thenReturn(List.of(t1, t2));

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUsername(TOKEN)).thenReturn(USERNAME);

        when(orderRepository.store(any(), any(), any(), any(), any())).thenReturn("1");

        List<int[]> requests = new ArrayList<>();
        requests.add(new int[]{0, 0});
        requests.add(new int[]{1, 1});

        String orderId = reserveTicketService.reserveTickets(TOKEN, COMPANY, EVENT, requests);

        assertEquals("1", orderId);
        verify(orderRepository, times(1)).store(eq(COMPANY), eq(EVENT), anyList(), eq(USERNAME), any(Date.class));
    }

    @Test
    void reserveTickets_Failure_UserAlreadyHasActiveOrder() {
        Ticket t1 = new Ticket(0, 0, EVENT, COMPANY, "T1", 100.0);
        when(ticketRepository.getAvailableTicketsByEventAndCompany(COMPANY, EVENT)).thenReturn(List.of(t1));

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUsername(TOKEN)).thenReturn(USERNAME);

        when(orderRepository.store(any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("you have already order"));

        List<int[]> requests = List.of(new int[]{0, 0});

        String string = reserveTicketService.reserveTickets(TOKEN, COMPANY, EVENT, requests);

        assertNull(string);
    }

    @Test
    void reserveTickets_ReplaceExpiredOrder_Success() {

        Ticket t1 = new Ticket(0, 0, EVENT, COMPANY, "T_NEW", 100.0);
        ticketRepository.storeTicket(0, 0, EVENT,COMPANY,100);
        ticketRepository.storeTicket(1, 1, EVENT,COMPANY,100);
//        when(ticketRepository.getAvailableTicketsByEventAndCompany(COMPANY, EVENT)).thenReturn(List.of(t1));

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUsername(TOKEN)).thenReturn(USERNAME);

//        when(orderRepository.store(any(), any(), any(), any(), any())).thenReturn("1");
        when(orderRepository.getTicketsId(USERNAME)).thenReturn(List.of("T_NEW"));

        List<int[]> requests = List.of(new int[]{0, 0});
        Date pastDate = new Date(System.currentTimeMillis() - 100000);
        orderRepository.store(COMPANY, EVENT, List.of("T_OLD"), USERNAME, pastDate);

        String orderId = reserveTicketService.reserveTickets(TOKEN, COMPANY, EVENT, requests);

        assertEquals("2", orderId);
        assertEquals(1, orderRepository.getTicketsId(USERNAME).size());
    }
}