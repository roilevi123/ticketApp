package Appliction;

import com.ticketing.ticketapp.Appliction.*;
import com.ticketing.ticketapp.Domain.Event.Event;
import com.ticketing.ticketapp.Domain.Event.EventType;
import com.ticketing.ticketapp.Domain.Event.iEventRepository;
import com.ticketing.ticketapp.Domain.PurchasePolicy.AgeLimitCondition;
import com.ticketing.ticketapp.Domain.PurchasePolicy.PurchasePolicy;
import com.ticketing.ticketapp.Domain.PurchasePolicy.PurchaseTargetType;
import com.ticketing.ticketapp.Domain.PurchasePolicy.QuantityLimitCondition;
import com.ticketing.ticketapp.Domain.User.User;
import com.ticketing.ticketapp.Infastructure.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OrderServiceExtendedTest {

    private OrderService orderService;
    private TicketRepositoryImpl ticketRepository;
    private OrderRepositoryImpl orderRepository;
    private InMemoryPurchasePolicyRepository policyRepository;
    private UserRepositoryImpl userRepository;

    @Mock private TokenService tokenService;
    @Mock private INotifier notifier;
    @Mock private iEventRepository eventRepository;
    @Mock private LotteryService lotteryService;

    private final String TOKEN = "token";
    private final String USERNAME = "user1";
    private final String COMPANY = "corp";
    private final String EVENT = "eventA";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ticketRepository = spy(new TicketRepositoryImpl());
        orderRepository = spy(new OrderRepositoryImpl());
        userRepository = spy(new UserRepositoryImpl());
        policyRepository = spy(new InMemoryPurchasePolicyRepository());

        when(eventRepository.getEvent(any(), any())).thenReturn(null);
        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn(USERNAME);

        orderService = new OrderService(orderRepository, tokenService, ticketRepository,
                userRepository, policyRepository, notifier, eventRepository, lotteryService);
    }

    // ── Lottery gate-check ────────────────────────────────────────────────────

    private Event highDemandEvent() {
        Event e = new Event("evId", COMPANY, null, EVENT, "loc", "artist", new Date(),
                100.0, 10, EventType.FESTIVAL, null);
        e.setHighDemand(true);
        return e;
    }

    @Test
    void reserveTickets_HighDemand_NullCode_ReturnsError() {
        when(eventRepository.getEvent(EVENT, COMPANY)).thenReturn(highDemandEvent());
        ticketRepository.storeTicket(0, 0, EVENT, COMPANY, 100);

        var result = orderService.reserveTickets(TOKEN, COMPANY, EVENT, List.of(new int[]{0, 0}), null);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("lottery purchase code is required"));
    }

    @Test
    void reserveTickets_HighDemand_BlankCode_ReturnsError() {
        when(eventRepository.getEvent(EVENT, COMPANY)).thenReturn(highDemandEvent());
        ticketRepository.storeTicket(0, 0, EVENT, COMPANY, 100);

        var result = orderService.reserveTickets(TOKEN, COMPANY, EVENT, List.of(new int[]{0, 0}), "   ");

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("lottery purchase code is required"));
    }

    @Test
    void reserveTickets_HighDemand_GuestUser_ReturnsError() {
        when(eventRepository.getEvent(EVENT, COMPANY)).thenReturn(highDemandEvent());
        when(tokenService.validateToken(TOKEN)).thenReturn(false);
        ticketRepository.storeTicket(0, 0, EVENT, COMPANY, 100);

        var result = orderService.reserveTickets(TOKEN, COMPANY, EVENT, List.of(new int[]{0, 0}), "some-code");

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("logged in"));
    }

    @Test
    void reserveTickets_HighDemand_InvalidCode_ReturnsError() {
        when(eventRepository.getEvent(EVENT, COMPANY)).thenReturn(highDemandEvent());
        when(lotteryService.validateLotteryCode("bad-code", USERNAME, EVENT, COMPANY)).thenReturn(false);
        ticketRepository.storeTicket(0, 0, EVENT, COMPANY, 100);

        var result = orderService.reserveTickets(TOKEN, COMPANY, EVENT, List.of(new int[]{0, 0}), "bad-code");

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Invalid"));
    }

    @Test
    void reserveTickets_HighDemand_ValidCode_ConsumesCodeAndSucceeds() {
        when(eventRepository.getEvent(EVENT, COMPANY)).thenReturn(highDemandEvent());
        when(lotteryService.validateLotteryCode("good-code", USERNAME, EVENT, COMPANY)).thenReturn(true);
        doNothing().when(lotteryService).consumeLotteryCode("good-code");
        ticketRepository.storeTicket(0, 0, EVENT, COMPANY, 100);

        var result = orderService.reserveTickets(TOKEN, COMPANY, EVENT, List.of(new int[]{0, 0}), "good-code");

        assertTrue(result.isSuccess());
        verify(lotteryService).consumeLotteryCode("good-code");
    }

    // ── getActiveOrderTickets missing branches ────────────────────────────────

    @Test
    void getActiveOrderTickets_NullOrderId_NoActiveOrder_ReturnsEmptyList() {
        var result = orderService.getActiveOrderTickets(TOKEN, null);

        assertTrue(result.isSuccess());
        assertTrue(result.getData().isEmpty());
    }

    @Test
    void getActiveOrderTickets_NullOrderId_ExpiredOrder_DeletesAndReturnsEmpty() {
        ticketRepository.storeTicket(0, 0, EVENT, COMPANY, 100);
        String ticketId = ticketRepository.getTicketsForEvent(COMPANY, EVENT).get(0).getId();
        Date pastDate = new Date(System.currentTimeMillis() - 100_000);
        orderRepository.store(COMPANY, EVENT, List.of(ticketId), USERNAME, pastDate);

        var result = orderService.getActiveOrderTickets(TOKEN, null);

        assertTrue(result.isSuccess());
        assertTrue(result.getData().isEmpty());
        assertNull(orderRepository.getOrder(USERNAME));
    }

    @Test
    void getActiveOrderTickets_WithOrderId_ExpiredOrder_DeletesAndReturnsEmpty() {
        ticketRepository.storeTicket(0, 0, EVENT, COMPANY, 100);
        String ticketId = ticketRepository.getTicketsForEvent(COMPANY, EVENT).get(0).getId();
        Date pastDate = new Date(System.currentTimeMillis() - 100_000);
        String orderId = orderRepository.store(COMPANY, EVENT, List.of(ticketId), USERNAME, pastDate);

        var result = orderService.getActiveOrderTickets(TOKEN, orderId);

        assertTrue(result.isSuccess());
        assertTrue(result.getData().isEmpty());
        assertNull(orderRepository.findById(orderId));
    }

    // ── validatePurchasePolicies branches ────────────────────────────────────

    @Test
    void reserveTickets_EventPolicyFails_ReturnsError() {
        policyRepository.save(new PurchasePolicy("p1", EVENT, PurchaseTargetType.EVENT,
                new QuantityLimitCondition(0, 0)));
        ticketRepository.storeTicket(0, 0, EVENT, COMPANY, 100);

        var result = orderService.reserveTickets(TOKEN, COMPANY, EVENT, List.of(new int[]{0, 0}), null);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Event Purchase Policy"));
    }

    @Test
    void reserveTickets_CompanyPolicyFails_ReturnsError() {
        policyRepository.save(new PurchasePolicy("p2", COMPANY, PurchaseTargetType.COMPANY,
                new QuantityLimitCondition(0, 0)));
        ticketRepository.storeTicket(0, 0, EVENT, COMPANY, 100);

        var result = orderService.reserveTickets(TOKEN, COMPANY, EVENT, List.of(new int[]{0, 0}), null);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Company Purchase Policy"));
    }

    @Test
    void reserveTickets_BothPoliciesPass_Succeeds() {
        policyRepository.save(new PurchasePolicy("p1", EVENT, PurchaseTargetType.EVENT,
                new QuantityLimitCondition(1, 10)));
        policyRepository.save(new PurchasePolicy("p2", COMPANY, PurchaseTargetType.COMPANY,
                new QuantityLimitCondition(1, 10)));
        ticketRepository.storeTicket(0, 0, EVENT, COMPANY, 100);

        var result = orderService.reserveTickets(TOKEN, COMPANY, EVENT, List.of(new int[]{0, 0}), null);

        assertTrue(result.isSuccess());
    }

    @Test
    void reserveTickets_NullUser_DefaultsAge10000_AgePolicyPasses() {
        // getUserByID returns null → age = 10000 → satisfies minAge 9999
        policyRepository.save(new PurchasePolicy("p1", EVENT, PurchaseTargetType.EVENT,
                new AgeLimitCondition(9999)));
        ticketRepository.storeTicket(0, 0, EVENT, COMPANY, 100);

        var result = orderService.reserveTickets(TOKEN, COMPANY, EVENT, List.of(new int[]{0, 0}), null);

        assertTrue(result.isSuccess());
    }

    @Test
    void reserveTickets_RealUser_AgeBelowLimit_PolicyFails() {
        User stored = userRepository.Store("realuser", "pass", 16, "u@test.com");
        when(tokenService.extractUserId(TOKEN)).thenReturn(stored.getID());

        policyRepository.save(new PurchasePolicy("p1", EVENT, PurchaseTargetType.EVENT,
                new AgeLimitCondition(18)));
        ticketRepository.storeTicket(0, 0, EVENT, COMPANY, 100);

        var result = orderService.reserveTickets(TOKEN, COMPANY, EVENT, List.of(new int[]{0, 0}), null);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Event Purchase Policy"));
    }
}
