package Appliction;

import com.ticketing.ticketapp.Appliction.INotifier;
import com.ticketing.ticketapp.Appliction.LotteryDrawScheduler;
import com.ticketing.ticketapp.Appliction.LotteryService;
import com.ticketing.ticketapp.Appliction.ReservationExpirationScheduler;
import com.ticketing.ticketapp.Domain.Lottery.ILotteryRepository;
import com.ticketing.ticketapp.Domain.Lottery.LotteryRegistration;
import com.ticketing.ticketapp.Domain.Order.ActiveOrder;
import com.ticketing.ticketapp.Domain.Order.IActiveOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.mockito.Mockito.*;

class SchedulerTest {

    // ── LotteryDrawScheduler ─────────────────────────────────────────────────

    private ILotteryRepository lotteryRepo;
    private LotteryService lotteryService;
    private LotteryDrawScheduler lotteryDrawScheduler;

    // ── ReservationExpirationScheduler ───────────────────────────────────────

    private IActiveOrderRepository orderRepo;
    private INotifier notifier;
    private ReservationExpirationScheduler expirationScheduler;

    @BeforeEach
    void setUp() {
        lotteryRepo = mock(ILotteryRepository.class);
        lotteryService = mock(LotteryService.class);
        lotteryDrawScheduler = new LotteryDrawScheduler(lotteryRepo, lotteryService);

        orderRepo = mock(IActiveOrderRepository.class);
        notifier = mock(INotifier.class);
        expirationScheduler = new ReservationExpirationScheduler(orderRepo, notifier);
    }

    // ── LotteryDrawScheduler tests ───────────────────────────────────────────

    @Test
    void drawExpiredLotteries_NoClosedLotteries_DoesNothing() {
        when(lotteryRepo.findClosedUndrawn()).thenReturn(Collections.emptyList());

        lotteryDrawScheduler.drawExpiredLotteries();

        verify(lotteryService, never()).performDraw(any());
    }

    @Test
    void drawExpiredLotteries_OneClosedLottery_CallsPerformDraw() {
        LotteryRegistration lr = new LotteryRegistration("Concert", "AcmeCorp", pastDate(1), 5);
        when(lotteryRepo.findClosedUndrawn()).thenReturn(List.of(lr));

        lotteryDrawScheduler.drawExpiredLotteries();

        verify(lotteryService).performDraw(lr);
    }

    @Test
    void drawExpiredLotteries_MultipleClosedLotteries_CallsPerformDrawForEach() {
        LotteryRegistration lr1 = new LotteryRegistration("Concert1", "AcmeCorp", pastDate(1), 5);
        LotteryRegistration lr2 = new LotteryRegistration("Concert2", "AcmeCorp", pastDate(1), 3);
        when(lotteryRepo.findClosedUndrawn()).thenReturn(List.of(lr1, lr2));

        lotteryDrawScheduler.drawExpiredLotteries();

        verify(lotteryService).performDraw(lr1);
        verify(lotteryService).performDraw(lr2);
    }

    @Test
    void drawExpiredLotteries_PerformDrawThrows_ContinuesWithNext() {
        LotteryRegistration lr1 = new LotteryRegistration("FailEvent", "AcmeCorp", pastDate(1), 5);
        LotteryRegistration lr2 = new LotteryRegistration("OkEvent", "AcmeCorp", pastDate(1), 3);
        when(lotteryRepo.findClosedUndrawn()).thenReturn(List.of(lr1, lr2));
        doThrow(new RuntimeException("Draw failed")).when(lotteryService).performDraw(lr1);

        lotteryDrawScheduler.drawExpiredLotteries();

        verify(lotteryService).performDraw(lr1);
        verify(lotteryService).performDraw(lr2);
    }

    // ── ReservationExpirationScheduler tests ─────────────────────────────────

    @Test
    void warnExpiringReservations_NoOrders_DoesNothing() {
        when(orderRepo.getAllActiveOrders()).thenReturn(Collections.emptyList());

        expirationScheduler.warnExpiringReservations();

        verify(notifier, never()).notifyUser(any(), any(), any());
    }

    @Test
    void warnExpiringReservations_OrderWithNullUserId_Skipped() {
        ActiveOrder order = new ActiveOrder("Corp", "Event", List.of("t1"),
                null, "order1", futureDate(60_000));
        when(orderRepo.getAllActiveOrders()).thenReturn(List.of(order));

        expirationScheduler.warnExpiringReservations();

        verify(notifier, never()).notifyUser(any(), any(), any());
    }

    @Test
    void warnExpiringReservations_OrderWithNullExpiry_Skipped() {
        ActiveOrder order = new ActiveOrder("Corp", "Event", List.of("t1"),
                "user1", "order1", null);
        when(orderRepo.getAllActiveOrders()).thenReturn(List.of(order));

        expirationScheduler.warnExpiringReservations();

        verify(notifier, never()).notifyUser(any(), any(), any());
    }

    @Test
    void warnExpiringReservations_OrderExpiringWithin2Minutes_SendsWarning() {
        // Expires in 90 seconds (within 2-minute threshold)
        ActiveOrder order = new ActiveOrder("Corp", "Event", List.of("t1"),
                "user1", "order1", futureDate(90_000));
        when(orderRepo.getAllActiveOrders()).thenReturn(List.of(order));

        expirationScheduler.warnExpiringReservations();

        verify(notifier).notifyUser(eq("user1"), eq("Reservation Expiring Soon"), anyString());
    }

    @Test
    void warnExpiringReservations_OrderAlreadyExpired_NoWarning() {
        // Expired 30 seconds ago
        ActiveOrder order = new ActiveOrder("Corp", "Event", List.of("t1"),
                "user1", "order1", pastDate(30_000));
        when(orderRepo.getAllActiveOrders()).thenReturn(List.of(order));

        expirationScheduler.warnExpiringReservations();

        verify(notifier, never()).notifyUser(any(), any(), any());
    }

    @Test
    void warnExpiringReservations_OrderExpiringFarInFuture_NoWarning() {
        // Expires in 10 minutes — outside 2-minute window
        ActiveOrder order = new ActiveOrder("Corp", "Event", List.of("t1"),
                "user1", "order1", futureDate(600_000));
        when(orderRepo.getAllActiveOrders()).thenReturn(List.of(order));

        expirationScheduler.warnExpiringReservations();

        verify(notifier, never()).notifyUser(any(), any(), any());
    }

    @Test
    void warnExpiringReservations_MixedOrders_OnlyWarnsForExpiringSoon() {
        ActiveOrder soonExpiring = new ActiveOrder("Corp", "Event1", List.of("t1"),
                "user1", "order1", futureDate(90_000));
        ActiveOrder farExpiring = new ActiveOrder("Corp", "Event2", List.of("t2"),
                "user2", "order2", futureDate(600_000));
        ActiveOrder expired = new ActiveOrder("Corp", "Event3", List.of("t3"),
                "user3", "order3", pastDate(30_000));
        when(orderRepo.getAllActiveOrders()).thenReturn(List.of(soonExpiring, farExpiring, expired));

        expirationScheduler.warnExpiringReservations();

        verify(notifier, times(1)).notifyUser(eq("user1"), any(), any());
        verify(notifier, never()).notifyUser(eq("user2"), any(), any());
        verify(notifier, never()).notifyUser(eq("user3"), any(), any());
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /** Returns a Date that is {@code millisFromNow} milliseconds in the future. */
    private Date futureDate(long millisFromNow) {
        return new Date(System.currentTimeMillis() + millisFromNow);
    }

    /** Returns a Date that is {@code millisAgo} milliseconds in the past. */
    private Date pastDate(long millisAgo) {
        return new Date(System.currentTimeMillis() - millisAgo);
    }
}
