package com.ticketing.ticketapp.Domain.Lottery;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Represents the lottery configuration and registration list for a high-demand event.
 */
public class LotteryRegistration {
    private final String eventName;
    private final String companyName;
    private Date endDate;
    private int maxWinners;
    private final List<String> registeredUserIds;
    private boolean drawn;

    public LotteryRegistration(String eventName, String companyName, Date endDate, int maxWinners) {
        this.eventName = eventName;
        this.companyName = companyName;
        this.endDate = endDate;
        this.maxWinners = maxWinners;
        this.registeredUserIds = new CopyOnWriteArrayList<>();
        this.drawn = false;
    }

    /**
     * Adds a user to the lottery. Returns false if already registered.
     */
    public boolean addUser(String userId) {
        if (registeredUserIds.contains(userId)) return false;
        registeredUserIds.add(userId);
        return true;
    }

    public boolean isRegistered(String userId) {
        return registeredUserIds.contains(userId);
    }

    /**
     * Returns true when the lottery registration window has closed.
     */
    public boolean isClosed() {
        return endDate != null && endDate.before(new Date());
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public String getEventName()          { return eventName; }
    public String getCompanyName()        { return companyName; }
    public Date   getEndDate()            { return endDate; }
    public int    getMaxWinners()         { return maxWinners; }
    public boolean isDrawn()              { return drawn; }

    /** Returns a snapshot copy so callers cannot mutate the list. */
    public List<String> getRegisteredUserIds() { return new ArrayList<>(registeredUserIds); }

    // ── Setters ──────────────────────────────────────────────────────────────

    public void setDrawn(boolean drawn)         { this.drawn = drawn; }
    public void setEndDate(Date endDate)        { this.endDate = endDate; }
    public void setMaxWinners(int maxWinners)   { this.maxWinners = maxWinners; }
}
