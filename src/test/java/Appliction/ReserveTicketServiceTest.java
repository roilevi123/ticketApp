package Appliction;


import Domain.Event.MapArea;
import Domain.Order.IActiveOrderRepository;
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
import static org.mockito.Mockito.*;

class ReserveTicketServiceTest {

    private OrderService reserveTicketService;
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
        reserveTicketService = new OrderService(orderRepository, tokenService, ticketRepository);
    }

    @Test
    void reserveTickets_Success_UpdatesRepositories() {
        MapArea[][] map = new MapArea[2][2];
        map[0][0] = MapArea.SEAT;
        map[1][1] = MapArea.STAND;
        map[0][1] = MapArea.STAND;
        map[1][0] = MapArea.STAND;

        ticketRepository.makeMapToTicket(COMPANY, EVENT, map, new Date(),100);

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUsername(TOKEN)).thenReturn(USERNAME);

        List<int[]> requests = new ArrayList<>();
        requests.add(new int[]{0, 0, 1});
        requests.add(new int[]{1, 1, 2});
        doReturn("1").when(orderRepository).store(any(), any(), any(), any(), any());

        String orderId=reserveTicketService.reserveTickets(TOKEN, COMPANY, EVENT, requests);
        assertEquals(orderId,"1");
        List<Ticket> eventTickets = ticketRepository.getTicketsForEvent(COMPANY, EVENT);
        long reservedCount = eventTickets.stream().filter(t -> t.getDate() != null).count();

        assertEquals(2, reservedCount);
        verify(orderRepository, times(1)).store(eq(COMPANY), eq(EVENT), anyList(), eq(USERNAME), any(Date.class));
    }

    @Test
    void reserveTickets_Failure_UserAlreadyHasActiveOrder() {
        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUsername(TOKEN)).thenReturn(USERNAME);

        // הזמנה שטרם פגה (בעתיד)
        Date futureDate = new Date(System.currentTimeMillis() + 100000);
        orderRepository.store(COMPANY, EVENT, List.of("T1"), USERNAME, futureDate);

        List<int[]> requests = List.of(new int[]{0, 0, 1});

        String string=reserveTicketService.reserveTickets(TOKEN, COMPANY, EVENT, requests);

        assertNull(string);
    }

    @Test
    void reserveTickets_ReplaceExpiredOrder_Success() {
        MapArea[][] map = new MapArea[1][1];
        map[0][0] = MapArea.SEAT;
        ticketRepository.makeMapToTicket(COMPANY, EVENT, map, new Date(),100);

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUsername(TOKEN)).thenReturn(USERNAME);
        Date pastDate = new Date(System.currentTimeMillis() - 100000);
        orderRepository.store(COMPANY, EVENT, List.of("T_OLD"), USERNAME, pastDate);

        List<int[]> requests = List.of(new int[]{0, 0, 1});
        when(orderRepository.store(any(),any(),any(),any(),any())).thenReturn("1");

        String orderId=reserveTicketService.reserveTickets(TOKEN, COMPANY, EVENT, requests);

        assertEquals("1", orderId);

        assertEquals(1, orderRepository.getTicketsId(USERNAME).size());
    }

    @Test
    void getActiveOrderTickets_Success() {
        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUsername(TOKEN)).thenReturn(USERNAME);

        ticketRepository.storeTicket(0, 0, EVENT,COMPANY,100);
        Ticket t = ticketRepository.getTicketsForEvent(COMPANY, EVENT).get(0);
        Date expiration = new Date(System.currentTimeMillis() + 600000);

        orderRepository.store(COMPANY, EVENT, List.of(t.getId()), USERNAME, expiration);

        String description = reserveTicketService.getActiveOrderTickets(TOKEN);

        assertNotNull(description);
        assertTrue(description.contains(COMPANY));
    }

}