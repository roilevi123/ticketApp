package Infastructure;

import com.ticketing.ticketapp.Domain.Lottery.LotteryCode;
import com.ticketing.ticketapp.Domain.Lottery.LotteryRegistration;
import com.ticketing.ticketapp.Infastructure.LotteryCodeRepositoryImpl;
import com.ticketing.ticketapp.Infastructure.LotteryRepositoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LotteryRepositoryImplTest {

    private LotteryRepositoryImpl repo;
    private LotteryCodeRepositoryImpl codeRepo;

    @BeforeEach
    void setUp() {
        repo = new LotteryRepositoryImpl();
        codeRepo = new LotteryCodeRepositoryImpl();
    }

    // ── LotteryRepositoryImpl ────────────────────────────────────────────────

    @Test
    void configure_CreatesNewLottery() {
        repo.configure("Concert", "AcmeCorp", null, futureDate(2), 5);
        assertTrue(repo.exists("Concert", "AcmeCorp"));
    }

    @Test
    void configure_UpdatesExistingLottery_MaxWinnersAndDates() {
        repo.configure("Concert", "AcmeCorp", null, futureDate(2), 5);
        Date newEnd = futureDate(3);
        repo.configure("Concert", "AcmeCorp", null, newEnd, 10);

        LotteryRegistration lr = repo.find("Concert", "AcmeCorp");
        assertNotNull(lr);
        assertEquals(10, lr.getMaxWinners());
        assertEquals(newEnd, lr.getEndDate());
    }

    @Test
    void configure_UpdatePreservesRegistrations() {
        repo.configure("Concert", "AcmeCorp", pastDate(1), futureDate(2), 5);
        repo.register("Concert", "AcmeCorp", "user1");
        repo.configure("Concert", "AcmeCorp", pastDate(1), futureDate(3), 10);

        assertTrue(repo.isRegistered("Concert", "AcmeCorp", "user1"));
    }

    @Test
    void register_ReturnsTrue_ForNewUser() {
        repo.configure("Concert", "AcmeCorp", pastDate(1), futureDate(1), 5);
        assertTrue(repo.register("Concert", "AcmeCorp", "user1"));
    }

    @Test
    void register_ReturnsFalse_ForDuplicateUser() {
        repo.configure("Concert", "AcmeCorp", pastDate(1), futureDate(1), 5);
        repo.register("Concert", "AcmeCorp", "user1");
        assertFalse(repo.register("Concert", "AcmeCorp", "user1"));
    }

    @Test
    void register_Throws_WhenNoLotteryConfigured() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> repo.register("Unknown", "AcmeCorp", "user1"));
        assertTrue(ex.getMessage().contains("No lottery"));
    }

    @Test
    void register_Throws_WhenLotteryAlreadyDrawn() {
        repo.configure("Concert", "AcmeCorp", pastDate(1), futureDate(1), 5);
        repo.markDrawn("Concert", "AcmeCorp");
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> repo.register("Concert", "AcmeCorp", "user1"));
        assertTrue(ex.getMessage().contains("already been drawn"));
    }

    @Test
    void register_Throws_WhenLotteryNotOpenYet() {
        repo.configure("Concert", "AcmeCorp", futureDate(1), futureDate(2), 5);
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> repo.register("Concert", "AcmeCorp", "user1"));
        assertTrue(ex.getMessage().contains("not opened yet"));
    }

    @Test
    void register_Throws_WhenLotteryClosed() {
        repo.configure("Concert", "AcmeCorp", pastDate(2), pastDate(1), 5);
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> repo.register("Concert", "AcmeCorp", "user1"));
        assertTrue(ex.getMessage().contains("closed"));
    }

    @Test
    void isRegistered_ReturnsFalse_WhenNoLottery() {
        assertFalse(repo.isRegistered("Unknown", "AcmeCorp", "user1"));
    }

    @Test
    void isRegistered_ReturnsFalse_WhenUserNotRegistered() {
        repo.configure("Concert", "AcmeCorp", pastDate(1), futureDate(1), 5);
        assertFalse(repo.isRegistered("Concert", "AcmeCorp", "user1"));
    }

    @Test
    void isRegistered_ReturnsTrue_WhenUserRegistered() {
        repo.configure("Concert", "AcmeCorp", pastDate(1), futureDate(1), 5);
        repo.register("Concert", "AcmeCorp", "user1");
        assertTrue(repo.isRegistered("Concert", "AcmeCorp", "user1"));
    }

    @Test
    void find_ReturnsNull_WhenNoLottery() {
        assertNull(repo.find("Unknown", "AcmeCorp"));
    }

    @Test
    void find_ReturnsLottery_WhenExists() {
        repo.configure("Concert", "AcmeCorp", null, futureDate(2), 5);
        LotteryRegistration lr = repo.find("Concert", "AcmeCorp");
        assertNotNull(lr);
        assertEquals("Concert", lr.getEventName());
        assertEquals("AcmeCorp", lr.getCompanyName());
    }

    @Test
    void findClosedUndrawn_ReturnsOnlyClosedUndrawnLotteries() {
        repo.configure("Past", "AcmeCorp", pastDate(2), pastDate(1), 5);
        repo.configure("Future", "AcmeCorp", null, futureDate(1), 5);

        List<LotteryRegistration> result = repo.findClosedUndrawn();
        assertEquals(1, result.size());
        assertEquals("Past", result.get(0).getEventName());
    }

    @Test
    void findClosedUndrawn_ExcludesDrawnLotteries() {
        repo.configure("Past", "AcmeCorp", pastDate(2), pastDate(1), 5);
        repo.markDrawn("Past", "AcmeCorp");

        assertTrue(repo.findClosedUndrawn().isEmpty());
    }

    @Test
    void findClosedUndrawn_ReturnsEmpty_WhenAllOpen() {
        repo.configure("Future", "AcmeCorp", null, futureDate(1), 5);
        assertTrue(repo.findClosedUndrawn().isEmpty());
    }

    @Test
    void markDrawn_SetsDrawnFlag() {
        repo.configure("Concert", "AcmeCorp", null, futureDate(2), 5);
        assertFalse(repo.find("Concert", "AcmeCorp").isDrawn());
        repo.markDrawn("Concert", "AcmeCorp");
        assertTrue(repo.find("Concert", "AcmeCorp").isDrawn());
    }

    @Test
    void markDrawn_DoesNothingWhenLotteryNotFound() {
        assertDoesNotThrow(() -> repo.markDrawn("Unknown", "AcmeCorp"));
    }

    @Test
    void exists_ReturnsTrue_WhenConfigured() {
        repo.configure("Concert", "AcmeCorp", null, futureDate(2), 5);
        assertTrue(repo.exists("Concert", "AcmeCorp"));
    }

    @Test
    void exists_ReturnsFalse_WhenNotConfigured() {
        assertFalse(repo.exists("Unknown", "AcmeCorp"));
    }

    @Test
    void exists_IsScopedByBothEventAndCompany() {
        repo.configure("Concert", "AcmeCorp", null, futureDate(2), 5);
        assertFalse(repo.exists("Concert", "OtherCorp"));
        assertFalse(repo.exists("OtherEvent", "AcmeCorp"));
    }

    @Test
    void deleteAll_ClearsAllLotteries() {
        repo.configure("Concert1", "AcmeCorp", null, futureDate(1), 5);
        repo.configure("Concert2", "AcmeCorp", null, futureDate(2), 5);
        repo.deleteAll();
        assertFalse(repo.exists("Concert1", "AcmeCorp"));
        assertFalse(repo.exists("Concert2", "AcmeCorp"));
    }

    // ── LotteryCodeRepositoryImpl ────────────────────────────────────────────

    @Test
    void codeRepo_Generate_StoresCodeRetrievableByCode() {
        LotteryCode lc = codeRepo.generate("user1", "Concert", "AcmeCorp", futureDate(1));
        assertNotNull(lc);
        assertNotNull(lc.getCode());
        assertEquals(lc, codeRepo.findByCode(lc.getCode()));
    }

    @Test
    void codeRepo_Generate_CreatesUniqueCodesForSameUser() {
        LotteryCode lc1 = codeRepo.generate("user1", "Concert", "AcmeCorp", futureDate(1));
        LotteryCode lc2 = codeRepo.generate("user1", "Concert", "AcmeCorp", futureDate(1));
        assertNotEquals(lc1.getCode(), lc2.getCode());
    }

    @Test
    void codeRepo_FindByCode_ReturnsNull_WhenNotFound() {
        assertNull(codeRepo.findByCode("nonexistent-code"));
    }

    @Test
    void codeRepo_Validate_ReturnsFalse_WhenCodeNotFound() {
        assertFalse(codeRepo.validate("nonexistent", "user1", "Concert", "AcmeCorp"));
    }

    @Test
    void codeRepo_Validate_ReturnsTrue_ForValidCode() {
        LotteryCode lc = codeRepo.generate("user1", "Concert", "AcmeCorp", futureDate(1));
        assertTrue(codeRepo.validate(lc.getCode(), "user1", "Concert", "AcmeCorp"));
    }

    @Test
    void codeRepo_Validate_ReturnsFalse_ForWrongUser() {
        LotteryCode lc = codeRepo.generate("user1", "Concert", "AcmeCorp", futureDate(1));
        assertFalse(codeRepo.validate(lc.getCode(), "other", "Concert", "AcmeCorp"));
    }

    @Test
    void codeRepo_Validate_ReturnsFalse_AfterMarkUsed() {
        LotteryCode lc = codeRepo.generate("user1", "Concert", "AcmeCorp", futureDate(1));
        codeRepo.markUsed(lc.getCode());
        assertFalse(codeRepo.validate(lc.getCode(), "user1", "Concert", "AcmeCorp"));
    }

    @Test
    void codeRepo_MarkUsed_SetsUsedFlag() {
        LotteryCode lc = codeRepo.generate("user1", "Concert", "AcmeCorp", futureDate(1));
        assertFalse(lc.isUsed());
        codeRepo.markUsed(lc.getCode());
        assertTrue(lc.isUsed());
    }

    @Test
    void codeRepo_MarkUsed_DoesNothing_WhenCodeNotFound() {
        assertDoesNotThrow(() -> codeRepo.markUsed("nonexistent"));
    }

    @Test
    void codeRepo_FindByUser_ReturnsAllCodesForUser() {
        codeRepo.generate("user1", "Concert", "AcmeCorp", futureDate(1));
        codeRepo.generate("user1", "Festival", "AcmeCorp", futureDate(1));
        codeRepo.generate("user2", "Concert", "AcmeCorp", futureDate(1));

        List<LotteryCode> user1Codes = codeRepo.findByUser("user1");
        assertEquals(2, user1Codes.size());
        assertTrue(user1Codes.stream().allMatch(lc -> lc.getUserId().equals("user1")));
    }

    @Test
    void codeRepo_FindByUser_ReturnsEmpty_WhenNoCodesForUser() {
        assertTrue(codeRepo.findByUser("unknown").isEmpty());
    }

    @Test
    void codeRepo_DeleteAll_ClearsAllCodes() {
        LotteryCode lc = codeRepo.generate("user1", "Concert", "AcmeCorp", futureDate(1));
        codeRepo.deleteAll();
        assertNull(codeRepo.findByCode(lc.getCode()));
        assertTrue(codeRepo.findByUser("user1").isEmpty());
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

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
