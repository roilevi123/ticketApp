package com.ticketing.ticketapp.Domain.Lottery;

import java.util.Date;
import java.util.List;

/**
 * Repository for managing single-use lottery purchase codes issued to winners.
 */
public interface ILotteryCodeRepository {

    /** Generates and persists a new lottery code for the given winner. */
    LotteryCode generate(String userId, String eventName, String companyName, Date expiryDate);

    /** Retrieves a code by its UUID string, or {@code null} if not found. */
    LotteryCode findByCode(String code);

    /**
     * Validates that the code exists, is unused, has not expired, and belongs to
     * the specified user/event/company triple.
     */
    boolean validate(String code, String userId, String eventName, String companyName);

    /** Marks a code as consumed so it cannot be used again. */
    void markUsed(String code);

    /** Returns all (used and unused) codes issued to a particular user. */
    List<LotteryCode> findByUser(String userId);

    /** Clears all data (for tests). */
    void deleteAll();
}
