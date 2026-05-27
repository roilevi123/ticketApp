package Appliction;

import com.ticketing.ticketapp.Appliction.INotifier;
import com.ticketing.ticketapp.Appliction.LotteryService;
import com.ticketing.ticketapp.Appliction.Response;
import com.ticketing.ticketapp.Domain.Event.Event;
import com.ticketing.ticketapp.Domain.Event.EventType;
import com.ticketing.ticketapp.Domain.Event.MapArea;
import com.ticketing.ticketapp.Domain.Event.iEventRepository;
import com.ticketing.ticketapp.Domain.Lottery.ILotteryCodeRepository;
import com.ticketing.ticketapp.Domain.Lottery.ILotteryRepository;
import com.ticketing.ticketapp.Domain.Lottery.LotteryCode;
import com.ticketing.ticketapp.Domain.Lottery.LotteryRegistration;
import com.ticketing.ticketapp.Infastructure.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LotteryServiceTest {

    private ILotteryRepository lotteryRepo;
    private ILotteryCodeRepository codeRepo;
    private iEventRepository eventRepo;
    private TokenService tokenService;
    private INotifier notifier;
    private LotteryService lotteryService;

    private static final String TOKEN = "valid-token";
    private static final String COMPANY = "AcmeCorp";
    private static final String EVENT = "Concert";

    @BeforeEach
    void setUp() {
        lotteryRepo = mock(ILotteryRepository.class);
        codeRepo = mock(ILotteryCodeRepository.class);
        eventRepo = mock(iEventRepository.class);
        tokenService = mock(TokenService.class);
        notifier = mock(INotifier.class);
        lotteryService = new LotteryService(lotteryRepo, codeRepo, eventRepo, tokenService, notifier);

        when(tokenService.validateToken(TOKEN)).thenReturn(true);
        when(tokenService.extractUserId(TOKEN)).thenReturn("user1");
    }

    // ── configureLottery ─────────────────────────────────────────────────────

    @Test
    void configureLottery_InvalidToken_ReturnsError() {
        when(tokenService.validateToken("bad")).thenReturn(false);
        Response<String> r = lotteryService.configureLottery("bad", COMPANY, EVENT, null, futureDate(1), 10);
        assertTrue(r.isError());
        verify(lotteryRepo, never()).configure(any(), any(), any(), any(), anyInt());
    }

    @Test
    void configureLottery_EventNotFound_ReturnsError() {
        when(eventRepo.getEvent(EVENT, COMPANY)).thenReturn(null);
        Response<String> r = lotteryService.configureLottery(TOKEN, COMPANY, EVENT, null, futureDate(1), 10);
        assertTrue(r.isError());
        assertTrue(r.getMessage().contains("Event not found"));
    }

    @Test
    void configureLottery_ZeroMaxWinners_ReturnsError() {
        when(eventRepo.getEvent(EVENT, COMPANY)).thenReturn(makeEvent());
        Response<String> r = lotteryService.configureLottery(TOKEN, COMPANY, EVENT, null, futureDate(1), 0);
        assertTrue(r.isError());
        assertTrue(r.getMessage().contains("maxWinners"));
    }

    @Test
    void configureLottery_NegativeMaxWinners_ReturnsError() {
        when(eventRepo.getEvent(EVENT, COMPANY)).thenReturn(makeEvent());
        Response<String> r = lotteryService.configureLottery(TOKEN, COMPANY, EVENT, null, futureDate(1), -1);
        assertTrue(r.isError());
    }

    @Test
    void configureLottery_NullEndDate_ReturnsError() {
        when(eventRepo.getEvent(EVENT, COMPANY)).thenReturn(makeEvent());
        Response<String> r = lotteryService.configureLottery(TOKEN, COMPANY, EVENT, null, null, 10);
        assertTrue(r.isError());
        assertTrue(r.getMessage().contains("end date"));
    }

    @Test
    void configureLottery_PastEndDate_ReturnsError() {
        when(eventRepo.getEvent(EVENT, COMPANY)).thenReturn(makeEvent());
        Response<String> r = lotteryService.configureLottery(TOKEN, COMPANY, EVENT, null, pastDate(1), 10);
        assertTrue(r.isError());
        assertTrue(r.getMessage().contains("end date"));
    }

    @Test
    void configureLottery_Success_ConfiguresAndMarksEventHighDemand() {
        Event event = makeEvent();
        when(eventRepo.getEvent(EVENT, COMPANY)).thenReturn(event);
        Date end = futureDate(2);

        Response<String> r = lotteryService.configureLottery(TOKEN, COMPANY, EVENT, null, end, 10);

        assertTrue(r.isSuccess());
        assertTrue(event.isHighDemand());
        assertEquals(end, event.getLotteryEndDate());
        assertEquals(10, event.getLotteryMaxWinners());
        verify(lotteryRepo).configure(EVENT, COMPANY, null, end, 10);
        verify(eventRepo).save(event);
    }

    // ── registerForLottery ───────────────────────────────────────────────────

    @Test
    void registerForLottery_InvalidToken_ReturnsError() {
        when(tokenService.validateToken("bad")).thenReturn(false);
        Response<String> r = lotteryService.registerForLottery("bad", COMPANY, EVENT);
        assertTrue(r.isError());
        verify(lotteryRepo, never()).register(any(), any(), any());
    }

    @Test
    void registerForLottery_NullUserId_ReturnsError() {
        when(tokenService.extractUserId(TOKEN)).thenReturn(null);
        Response<String> r = lotteryService.registerForLottery(TOKEN, COMPANY, EVENT);
        assertTrue(r.isError());
        assertTrue(r.getMessage().contains("user"));
    }

    @Test
    void registerForLottery_NoLotteryConfigured_ReturnsError() {
        when(lotteryRepo.exists(EVENT, COMPANY)).thenReturn(false);
        Response<String> r = lotteryService.registerForLottery(TOKEN, COMPANY, EVENT);
        assertTrue(r.isError());
        assertTrue(r.getMessage().contains("No lottery"));
    }

    @Test
    void registerForLottery_AlreadyRegistered_ReturnsError() {
        when(lotteryRepo.exists(EVENT, COMPANY)).thenReturn(true);
        when(lotteryRepo.register(EVENT, COMPANY, "user1")).thenReturn(false);
        Response<String> r = lotteryService.registerForLottery(TOKEN, COMPANY, EVENT);
        assertTrue(r.isError());
        assertTrue(r.getMessage().contains("already registered"));
    }

    @Test
    void registerForLottery_Success_ReturnsSuccessMessage() {
        when(lotteryRepo.exists(EVENT, COMPANY)).thenReturn(true);
        when(lotteryRepo.register(EVENT, COMPANY, "user1")).thenReturn(true);
        Response<String> r = lotteryService.registerForLottery(TOKEN, COMPANY, EVENT);
        assertTrue(r.isSuccess());
        verify(lotteryRepo).register(EVENT, COMPANY, "user1");
    }

    @Test
    void registerForLottery_RepositoryThrows_ReturnsError() {
        when(lotteryRepo.exists(EVENT, COMPANY)).thenReturn(true);
        when(lotteryRepo.register(any(), any(), any())).thenThrow(new RuntimeException("Lottery closed"));
        Response<String> r = lotteryService.registerForLottery(TOKEN, COMPANY, EVENT);
        assertTrue(r.isError());
    }

    // ── getLotteryStatus ─────────────────────────────────────────────────────

    @Test
    void getLotteryStatus_NoLottery_ReturnsHasLotteryFalse() {
        when(lotteryRepo.find(EVENT, COMPANY)).thenReturn(null);
        Response<Map<String, Object>> r = lotteryService.getLotteryStatus(null, COMPANY, EVENT);
        assertTrue(r.isSuccess());
        assertFalse((Boolean) r.getData().get("hasLottery"));
    }

    @Test
    void getLotteryStatus_WithLottery_NullToken_ReturnsBasicInfoNotRegistered() {
        LotteryRegistration lr = makeLotteryRegistration();
        when(lotteryRepo.find(EVENT, COMPANY)).thenReturn(lr);

        Response<Map<String, Object>> r = lotteryService.getLotteryStatus(null, COMPANY, EVENT);

        assertTrue(r.isSuccess());
        assertTrue((Boolean) r.getData().get("hasLottery"));
        assertFalse((Boolean) r.getData().get("registered"));
        assertFalse((Boolean) r.getData().get("hasWon"));
        assertEquals(lr.getMaxWinners(), r.getData().get("maxWinners"));
    }

    @Test
    void getLotteryStatus_WithValidToken_UserRegistered_ReturnsRegisteredTrue() {
        LotteryRegistration lr = makeLotteryRegistration();
        lr.addUser("user1");
        when(lotteryRepo.find(EVENT, COMPANY)).thenReturn(lr);
        when(codeRepo.findByUser("user1")).thenReturn(Collections.emptyList());

        Response<Map<String, Object>> r = lotteryService.getLotteryStatus(TOKEN, COMPANY, EVENT);

        assertTrue(r.isSuccess());
        assertTrue((Boolean) r.getData().get("registered"));
        assertFalse((Boolean) r.getData().get("hasWon"));
    }

    @Test
    void getLotteryStatus_WithValidToken_UserHasActiveWinningCode_ReturnsHasWonTrue() {
        LotteryRegistration lr = makeLotteryRegistration();
        when(lotteryRepo.find(EVENT, COMPANY)).thenReturn(lr);

        LotteryCode winCode = new LotteryCode("user1", EVENT, COMPANY, futureDate(1));
        when(codeRepo.findByUser("user1")).thenReturn(List.of(winCode));

        Response<Map<String, Object>> r = lotteryService.getLotteryStatus(TOKEN, COMPANY, EVENT);

        assertTrue(r.isSuccess());
        assertTrue((Boolean) r.getData().get("hasWon"));
        assertEquals(winCode.getCode(), r.getData().get("winningCode"));
        assertNotNull(r.getData().get("codeExpiry"));
    }

    @Test
    void getLotteryStatus_WithValidToken_UserCodeExpired_ReturnsHasWonFalse() {
        LotteryRegistration lr = makeLotteryRegistration();
        when(lotteryRepo.find(EVENT, COMPANY)).thenReturn(lr);

        LotteryCode expiredCode = new LotteryCode("user1", EVENT, COMPANY, pastDate(1));
        when(codeRepo.findByUser("user1")).thenReturn(List.of(expiredCode));

        Response<Map<String, Object>> r = lotteryService.getLotteryStatus(TOKEN, COMPANY, EVENT);

        assertTrue(r.isSuccess());
        assertFalse((Boolean) r.getData().get("hasWon"));
    }

    @Test
    void getLotteryStatus_WithValidToken_UserCodeUsed_ReturnsHasWonFalse() {
        LotteryRegistration lr = makeLotteryRegistration();
        when(lotteryRepo.find(EVENT, COMPANY)).thenReturn(lr);

        LotteryCode usedCode = new LotteryCode("user1", EVENT, COMPANY, futureDate(1));
        usedCode.setUsed(true);
        when(codeRepo.findByUser("user1")).thenReturn(List.of(usedCode));

        Response<Map<String, Object>> r = lotteryService.getLotteryStatus(TOKEN, COMPANY, EVENT);

        assertTrue(r.isSuccess());
        assertFalse((Boolean) r.getData().get("hasWon"));
    }

    @Test
    void getLotteryStatus_CodeForDifferentEvent_ReturnsHasWonFalse() {
        LotteryRegistration lr = makeLotteryRegistration();
        when(lotteryRepo.find(EVENT, COMPANY)).thenReturn(lr);

        LotteryCode otherEventCode = new LotteryCode("user1", "OtherEvent", COMPANY, futureDate(1));
        when(codeRepo.findByUser("user1")).thenReturn(List.of(otherEventCode));

        Response<Map<String, Object>> r = lotteryService.getLotteryStatus(TOKEN, COMPANY, EVENT);

        assertTrue(r.isSuccess());
        assertFalse((Boolean) r.getData().get("hasWon"));
    }

    @Test
    void getLotteryStatus_InvalidToken_ReturnsBasicInfoNotRegistered() {
        LotteryRegistration lr = makeLotteryRegistration();
        when(lotteryRepo.find(EVENT, COMPANY)).thenReturn(lr);
        when(tokenService.validateToken("bad")).thenReturn(false);

        Response<Map<String, Object>> r = lotteryService.getLotteryStatus("bad", COMPANY, EVENT);

        assertTrue(r.isSuccess());
        assertFalse((Boolean) r.getData().get("registered"));
        assertFalse((Boolean) r.getData().get("hasWon"));
    }

    // ── validateLotteryCode / consumeLotteryCode ─────────────────────────────

    @Test
    void validateLotteryCode_DelegatesToRepository_ReturnsTrue() {
        when(codeRepo.validate("code1", "user1", EVENT, COMPANY)).thenReturn(true);
        assertTrue(lotteryService.validateLotteryCode("code1", "user1", EVENT, COMPANY));
    }

    @Test
    void validateLotteryCode_DelegatesToRepository_ReturnsFalse() {
        when(codeRepo.validate("code1", "user1", EVENT, COMPANY)).thenReturn(false);
        assertFalse(lotteryService.validateLotteryCode("code1", "user1", EVENT, COMPANY));
    }

    @Test
    void consumeLotteryCode_CallsMarkUsedOnRepository() {
        lotteryService.consumeLotteryCode("code1");
        verify(codeRepo).markUsed("code1");
    }

    // ── performDraw ──────────────────────────────────────────────────────────

    @Test
    void performDraw_EmptyParticipants_NoWinnersOrNotifications() {
        LotteryRegistration lr = makeLotteryRegistration();

        lotteryService.performDraw(lr);

        verify(codeRepo, never()).generate(any(), any(), any(), any());
        verify(notifier, never()).notifyUser(any(), any(), any());
        verify(lotteryRepo).markDrawn(EVENT, COMPANY);
    }

    @Test
    void performDraw_AllParticipantsWin_WhenLessThanMaxWinners() {
        LotteryRegistration lr = makeLotteryRegistration();
        lr.addUser("user1");
        lr.addUser("user2");

        LotteryCode mockCode = new LotteryCode("user1", EVENT, COMPANY, futureDate(2));
        when(codeRepo.generate(any(), any(), any(), any())).thenReturn(mockCode);

        lotteryService.performDraw(lr);

        verify(codeRepo, times(2)).generate(any(), eq(EVENT), eq(COMPANY), any());
        verify(notifier, times(2)).notifyUser(any(), anyString(), anyString());
        verify(lotteryRepo).markDrawn(EVENT, COMPANY);
    }

    @Test
    void performDraw_OnlyMaxWinnersSelected_WhenMoreParticipantsThanMax() {
        LotteryRegistration lr = new LotteryRegistration(EVENT, COMPANY, futureDate(2), 2);
        for (int i = 0; i < 5; i++) {
            lr.addUser("user" + i);
        }

        LotteryCode mockCode = new LotteryCode("user0", EVENT, COMPANY, futureDate(2));
        when(codeRepo.generate(any(), any(), any(), any())).thenReturn(mockCode);

        lotteryService.performDraw(lr);

        verify(codeRepo, times(2)).generate(any(), any(), any(), any());
        verify(notifier, times(2)).notifyUser(any(), any(), any());
        verify(lotteryRepo).markDrawn(EVENT, COMPANY);
    }

    @Test
    void performDraw_NotifiesWinnerWithCodeInMessage() {
        LotteryRegistration lr = makeLotteryRegistration();
        lr.addUser("user1");

        LotteryCode winCode = new LotteryCode("user1", EVENT, COMPANY, futureDate(2));
        when(codeRepo.generate(any(), any(), any(), any())).thenReturn(winCode);

        lotteryService.performDraw(lr);

        verify(notifier).notifyUser(eq("user1"), contains("Won"), contains(winCode.getCode()));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Event makeEvent() {
        return new Event("e1", COMPANY, "q1", EVENT, "Venue", "Artist",
                futureDate(30), 50.0, 100, EventType.LIVE_PERFORMANCE, new MapArea[0][0]);
    }

    private LotteryRegistration makeLotteryRegistration() {
        return new LotteryRegistration(EVENT, COMPANY, futureDate(2), 10);
    }

    private Date futureDate(int daysFromNow) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, daysFromNow);
        return cal.getTime();
    }

    private Date pastDate(int daysAgo) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo);
        return cal.getTime();
    }
}
