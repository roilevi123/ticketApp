package Appliction;

import com.ticketing.ticketapp.Appliction.*;

import com.ticketing.ticketapp.Domain.PurchasePolicy.iPurchasePolicyRepository;
import com.ticketing.ticketapp.Domain.Ticket.Ticket;
import com.ticketing.ticketapp.Domain.User.IUserRepository;
import com.ticketing.ticketapp.Infastructure.*;
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

class OrderServiceTest {

    private OrderService reserveTicketService;
    private TicketRepositoryImpl ticketRepository;
    private OrderRepositoryImpl orderRepository;
    private InMemoryPurchasePolicyRepository purchasePolicyRepository;

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
        userRepository = spy(new UserRepositoryImpl());
        purchasePolicyRepository = spy(new InMemoryPurchasePolicyRepository());

        INotifier notifierMock = mock(INotifier.class);
        reserveTicketService = new OrderService(orderRepository, tokenService, ticketRepository, userRepository, purchasePolicyRepository, notifierMock);
    }

    @Test
    void reserveTickets_Success_UpdatesRepositories() {
        ticketRepository.storeTicket(0, 0, EVENT, COMPANY, 100);
        ticketRepository.storeTicket(1, 1, EVENT, COMPANY, 100);

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);
        when(orderRepository.store(any(), any(), any(), any(), any())).thenReturn("1");

        List<int[]> requests = new ArrayList<>();
        requests.add(new int[]{0, 0});
        requests.add(new int[]{1, 1});

        Response<String> response = reserveTicketService.reserveTickets(TOKEN, COMPANY, EVENT, requests);

        assertTrue(response.isSuccess());
        assertEquals("1", response.getData());
        verify(orderRepository, times(1)).store(eq(COMPANY), eq(EVENT), anyList(), eq(USERNAME), any(Date.class));
    }

    public boolean isNumeric(String str) {
        if (str == null) return false;
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Test
    void reserveTickets_Failure_UserAlreadyHasActiveOrder() {
        Ticket t1 = new Ticket(0, 0, EVENT, COMPANY, "T1", 100.0);
        when(ticketRepository.getAvailableTicketsByEventAndCompany(COMPANY, EVENT)).thenReturn(List.of(t1));

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);

        when(orderRepository.store(any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("you have already order"));

        List<int[]> requests = List.of(new int[]{0, 0});

        Response<String> response = reserveTicketService.reserveTickets(TOKEN, COMPANY, EVENT, requests);

        assertFalse(response.isSuccess());
    }

    @Test
    void reserveTickets_ReplaceExpiredOrder_Success() {
        ticketRepository.storeTicket(0, 0, EVENT, COMPANY, 100);
        ticketRepository.storeTicket(1, 1, EVENT, COMPANY, 100);

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);
        when(orderRepository.getTicketsId(USERNAME)).thenReturn(List.of("T_NEW"));

        List<int[]> requests = List.of(new int[]{0, 0});
        Date pastDate = new Date(System.currentTimeMillis() - 100000);
        orderRepository.store(COMPANY, EVENT, List.of("T_OLD"), USERNAME, pastDate);

        Response<String> response = reserveTicketService.reserveTickets(TOKEN, COMPANY, EVENT, requests);

        assertTrue(response.isSuccess());
        assertTrue(isNumeric(response.getData()));
        assertNotNull(orderRepository.findById(response.getData()));
    }
}
