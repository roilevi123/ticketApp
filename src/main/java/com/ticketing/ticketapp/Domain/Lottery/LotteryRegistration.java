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
    private Date startDate;
    private Date endDate;
    private int maxWinners;
    private final List<String> registeredUserIds;
    private boolean drawn;

    public LotteryRegistration(String eventName, String companyName, Date startDate, Date endDate, int maxWinners) {
        this.eventName = eventName;
        this.companyName = companyName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.maxWinners = maxWinners;
        this.registeredUserIds = new CopyOnWriteArrayList<>();
        this.drawn = false;
    }

    public LotteryRegistration(String eventName, String companyName, Date endDate, int maxWinners) {
        this(eventName, companyName, null, endDate, maxWinners);
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

    public boolean isClosed() {
        return endDate != null && endDate.before(new Date());
    }

    public boolean isOpen() {
        Date now = new Date();
        if (startDate != null && now.before(startDate)) return false;
        return !isClosed();
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public String getEventName()          { return eventName; }
    public String getCompanyName()        { return companyName; }
    public Date   getStartDate()          { return startDate; }
    public Date   getEndDate()            { return endDate; }
    public int    getMaxWinners()         { return maxWinners; }
    public boolean isDrawn()              { return drawn; }

    /** Returns a snapshot copy so callers cannot mutate the list. */
    public List<String> getRegisteredUserIds() { return new ArrayList<>(registeredUserIds); }

    // ── Setters ──────────────────────────────────────────────────────────────

    public void setDrawn(boolean drawn)         { this.drawn = drawn; }
    public void setStartDate(Date startDate)    { this.startDate = startDate; }
    public void setEndDate(Date endDate)        { this.endDate = endDate; }
    public void setMaxWinners(int maxWinners)   { this.maxWinners = maxWinners; }
}
