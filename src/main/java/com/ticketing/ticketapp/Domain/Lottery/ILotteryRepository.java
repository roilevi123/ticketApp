package com.ticketing.ticketapp.Domain.Lottery;

import java.util.Date;
import java.util.List;

/**
 * Repository for managing lottery registrations keyed by (eventName, companyName).
 */
public interface ILotteryRepository {

    void configure(String eventName, String companyName, Date startDate, Date endDate, int maxWinners);

    /**
     * Registers a user for the lottery.
     * @return {@code true} if the user was newly added; {@code false} if already registered.
     * @throws RuntimeException when there is no lottery, it is closed, or already drawn.
     */
    boolean register(String eventName, String companyName, String userId);

    boolean isRegistered(String eventName, String companyName, String userId);

    /** Retrieves the lottery registration, or {@code null} if none configured. */
    LotteryRegistration find(String eventName, String companyName);

    /** Returns all lotteries whose end-date has passed but have not been drawn yet. */
    List<LotteryRegistration> findClosedUndrawn();

    /** Marks a lottery as drawn so the scheduler won't process it again. */
    void markDrawn(String eventName, String companyName);

    boolean exists(String eventName, String companyName);

    /** Clears all data (for tests). */
    void deleteAll();
}
