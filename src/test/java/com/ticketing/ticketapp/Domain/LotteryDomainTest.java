package com.ticketing.ticketapp.Domain;

import com.ticketing.ticketapp.Domain.Lottery.LotteryCode;
import com.ticketing.ticketapp.Domain.Lottery.LotteryRegistration;
import org.junit.jupiter.api.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LotteryDomainTest {

    // ── LotteryCode ──────────────────────────────────────────────────────────

    @Test
    void lotteryCode_Constructor_SetsFieldsAndNotUsed() {
        Date expiry = futureDate(1);
        LotteryCode lc = new LotteryCode("user1", "Concert", "AcmeCorp", expiry);

        assertNotNull(lc.getCode());
        assertFalse(lc.getCode().isEmpty());
        assertEquals("user1", lc.getUserId());
        assertEquals("Concert", lc.getEventName());
        assertEquals("AcmeCorp", lc.getCompanyName());
        assertEquals(expiry, lc.getExpiryDate());
        assertFalse(lc.isUsed());
    }

    @Test
    void lotteryCode_TwoCodes_HaveUniqueUUIDs() {
        LotteryCode lc1 = new LotteryCode("user1", "Concert", "AcmeCorp", futureDate(1));
        LotteryCode lc2 = new LotteryCode("user1", "Concert", "AcmeCorp", futureDate(1));
        assertNotEquals(lc1.getCode(), lc2.getCode());
    }

    @Test
    void lotteryCode_IsValid_ReturnsTrue_WhenNotUsedNotExpiredMatchingContext() {
        LotteryCode lc = new LotteryCode("user1", "Concert", "AcmeCorp", futureDate(1));
        assertTrue(lc.isValid("user1", "Concert", "AcmeCorp"));
    }

    @Test
    void lotteryCode_IsValid_ReturnsFalse_WhenUsed() {
        LotteryCode lc = new LotteryCode("user1", "Concert", "AcmeCorp", futureDate(1));
        lc.setUsed(true);
        assertFalse(lc.isValid("user1", "Concert", "AcmeCorp"));
    }

    @Test
    void lotteryCode_IsValid_ReturnsFalse_WhenExpired() {
        LotteryCode lc = new LotteryCode("user1", "Concert", "AcmeCorp", pastDate(1));
        assertFalse(lc.isValid("user1", "Concert", "AcmeCorp"));
    }

    @Test
    void lotteryCode_IsValid_ReturnsFalse_WhenWrongUserId() {
        LotteryCode lc = new LotteryCode("user1", "Concert", "AcmeCorp", futureDate(1));
        assertFalse(lc.isValid("other", "Concert", "AcmeCorp"));
    }

    @Test
    void lotteryCode_IsValid_ReturnsFalse_WhenWrongEventName() {
        LotteryCode lc = new LotteryCode("user1", "Concert", "AcmeCorp", futureDate(1));
        assertFalse(lc.isValid("user1", "OtherEvent", "AcmeCorp"));
    }

    @Test
    void lotteryCode_IsValid_ReturnsFalse_WhenWrongCompanyName() {
        LotteryCode lc = new LotteryCode("user1", "Concert", "AcmeCorp", futureDate(1));
        assertFalse(lc.isValid("user1", "Concert", "OtherCorp"));
    }

    @Test
    void lotteryCode_IsValid_ReturnsTrue_WhenNullExpiry() {
        LotteryCode lc = new LotteryCode("user1", "Concert", "AcmeCorp", null);
        assertTrue(lc.isValid("user1", "Concert", "AcmeCorp"));
    }

    @Test
    void lotteryCode_SetUsed_MarksAsUsed() {
        LotteryCode lc = new LotteryCode("user1", "Concert", "AcmeCorp", futureDate(1));
        assertFalse(lc.isUsed());
        lc.setUsed(true);
        assertTrue(lc.isUsed());
        lc.setUsed(false);
        assertFalse(lc.isUsed());
    }

    // ── LotteryRegistration ──────────────────────────────────────────────────

    @Test
    void lotteryRegistration_Constructor_SetsFieldsCorrectly() {
        Date start = futureDate(1);
        Date end = futureDate(2);
        LotteryRegistration lr = new LotteryRegistration("Concert", "AcmeCorp", start, end, 10);

        assertEquals("Concert", lr.getEventName());
        assertEquals("AcmeCorp", lr.getCompanyName());
        assertEquals(start, lr.getStartDate());
        assertEquals(end, lr.getEndDate());
        assertEquals(10, lr.getMaxWinners());
        assertFalse(lr.isDrawn());
        assertTrue(lr.getRegisteredUserIds().isEmpty());
    }

    @Test
    void lotteryRegistration_TwoArgConstructor_HasNullStartDate() {
        Date end = futureDate(2);
        LotteryRegistration lr = new LotteryRegistration("Concert", "AcmeCorp", end, 5);
        assertNull(lr.getStartDate());
        assertEquals(end, lr.getEndDate());
        assertEquals(5, lr.getMaxWinners());
    }

    @Test
    void lotteryRegistration_AddUser_ReturnsTrue_ForNewUser() {
        LotteryRegistration lr = openLottery();
        assertTrue(lr.addUser("user1"));
        assertTrue(lr.isRegistered("user1"));
    }

    @Test
    void lotteryRegistration_AddUser_ReturnsFalse_ForDuplicateUser() {
        LotteryRegistration lr = openLottery();
        lr.addUser("user1");
        assertFalse(lr.addUser("user1"));
    }

    @Test
    void lotteryRegistration_AddUser_AcceptsMultipleDifferentUsers() {
        LotteryRegistration lr = openLottery();
        assertTrue(lr.addUser("user1"));
        assertTrue(lr.addUser("user2"));
        assertEquals(2, lr.getRegisteredUserIds().size());
    }

    @Test
    void lotteryRegistration_IsRegistered_ReturnsFalse_BeforeAdding() {
        LotteryRegistration lr = openLottery();
        assertFalse(lr.isRegistered("user1"));
    }

    @Test
    void lotteryRegistration_IsRegistered_ReturnsTrue_AfterAdding() {
        LotteryRegistration lr = openLottery();
        lr.addUser("user1");
        assertTrue(lr.isRegistered("user1"));
    }

    @Test
    void lotteryRegistration_IsClosed_ReturnsFalse_WhenEndDateInFuture() {
        LotteryRegistration lr = new LotteryRegistration("Concert", "AcmeCorp", futureDate(2), 5);
        assertFalse(lr.isClosed());
    }

    @Test
    void lotteryRegistration_IsClosed_ReturnsTrue_WhenEndDateInPast() {
        LotteryRegistration lr = new LotteryRegistration("Concert", "AcmeCorp", pastDate(1), 5);
        assertTrue(lr.isClosed());
    }

    @Test
    void lotteryRegistration_IsClosed_ReturnsFalse_WhenNullEndDate() {
        LotteryRegistration lr = new LotteryRegistration("Concert", "AcmeCorp", null, null, 5);
        assertFalse(lr.isClosed());
    }

    @Test
    void lotteryRegistration_IsOpen_ReturnsTrue_WhenBetweenStartAndEnd() {
        LotteryRegistration lr = new LotteryRegistration("Concert", "AcmeCorp", pastDate(1), futureDate(1), 5);
        assertTrue(lr.isOpen());
    }

    @Test
    void lotteryRegistration_IsOpen_ReturnsFalse_WhenBeforeStartDate() {
        LotteryRegistration lr = new LotteryRegistration("Concert", "AcmeCorp", futureDate(1), futureDate(2), 5);
        assertFalse(lr.isOpen());
    }

    @Test
    void lotteryRegistration_IsOpen_ReturnsFalse_WhenAfterEndDate() {
        LotteryRegistration lr = new LotteryRegistration("Concert", "AcmeCorp", pastDate(2), pastDate(1), 5);
        assertFalse(lr.isOpen());
    }

    @Test
    void lotteryRegistration_IsOpen_ReturnsTrue_WhenNullStartAndFutureEnd() {
        LotteryRegistration lr = new LotteryRegistration("Concert", "AcmeCorp", futureDate(2), 5);
        assertTrue(lr.isOpen());
    }

    @Test
    void lotteryRegistration_GetRegisteredUserIds_ReturnsImmutableSnapshot() {
        LotteryRegistration lr = openLottery();
        lr.addUser("user1");
        lr.addUser("user2");

        List<String> snapshot = lr.getRegisteredUserIds();
        assertEquals(2, snapshot.size());
        snapshot.add("user3");
        assertEquals(2, lr.getRegisteredUserIds().size());
    }

    @Test
    void lotteryRegistration_SetDrawn_UpdatesState() {
        LotteryRegistration lr = openLottery();
        assertFalse(lr.isDrawn());
        lr.setDrawn(true);
        assertTrue(lr.isDrawn());
    }

    @Test
    void lotteryRegistration_Setters_UpdateCorrectly() {
        LotteryRegistration lr = openLottery();
        Date newStart = futureDate(1);
        Date newEnd = futureDate(3);

        lr.setStartDate(newStart);
        lr.setEndDate(newEnd);
        lr.setMaxWinners(20);

        assertEquals(newStart, lr.getStartDate());
        assertEquals(newEnd, lr.getEndDate());
        assertEquals(20, lr.getMaxWinners());
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private LotteryRegistration openLottery() {
        return new LotteryRegistration("Concert", "AcmeCorp", pastDate(1), futureDate(1), 10);
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
