package com.ticketing.ticketapp.Domain.Lottery;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Represents the lottery configuration and registration list for a high-demand event.
 */
@Entity
@Table(name = "lottery_registrations")
@IdClass(LotteryRegistrationKey.class)
public class LotteryRegistration {

    @Id
    @Column(name = "event_name")
    private String eventName;

    @Id
    @Column(name = "company_name")
    private String companyName;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "start_date")
    private Date startDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "end_date")
    private Date endDate;

    @Column(name = "max_winners")
    private int maxWinners;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "lottery_registered_users",
        joinColumns = {
            @JoinColumn(name = "event_name", referencedColumnName = "event_name"),
            @JoinColumn(name = "company_name", referencedColumnName = "company_name")
        }
    )
    @Column(name = "user_id")
    private List<String> registeredUserIds;

    @Column(name = "drawn")
    private boolean drawn;

    protected LotteryRegistration() {}

    public LotteryRegistration(String eventName, String companyName, Date startDate, Date endDate, int maxWinners) {
        this.eventName = eventName;
        this.companyName = companyName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.maxWinners = maxWinners;
        this.registeredUserIds = new ArrayList<>();
        this.drawn = false;
    }

    public LotteryRegistration(String eventName, String companyName, Date endDate, int maxWinners) {
        this(eventName, companyName, null, endDate, maxWinners);
    }

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

    public String getEventName() { return eventName; }
    public String getCompanyName() { return companyName; }
    public Date getStartDate() { return startDate; }
    public Date getEndDate() { return endDate; }
    public int getMaxWinners() { return maxWinners; }
    public boolean isDrawn() { return drawn; }

    /** Returns a snapshot copy so callers cannot mutate the backing list. */
    public List<String> getRegisteredUserIds() { return new ArrayList<>(registeredUserIds); }

    public void setDrawn(boolean drawn) { this.drawn = drawn; }
    public void setStartDate(Date startDate) { this.startDate = startDate; }
    public void setEndDate(Date endDate) { this.endDate = endDate; }
    public void setMaxWinners(int maxWinners) { this.maxWinners = maxWinners; }
}
