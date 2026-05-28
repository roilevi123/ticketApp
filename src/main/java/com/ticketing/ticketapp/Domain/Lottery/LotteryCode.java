package com.ticketing.ticketapp.Domain.Lottery;

import java.util.Date;
import java.util.UUID;

/**
 * A single-use, time-limited purchase code issued to a lottery winner.
 * The holder of this code may reserve tickets for the associated event.
 */
public class LotteryCode {
    private final String code;
    private final String userId;
    private final String eventName;
    private final String companyName;
    private final Date expiryDate;
    private boolean used;

    public LotteryCode(String userId, String eventName, String companyName, Date expiryDate) {
        this.code = UUID.randomUUID().toString();
        this.userId = userId;
        this.eventName = eventName;
        this.companyName = companyName;
        this.expiryDate = expiryDate;
        this.used = false;
    }

    /**
     * Returns true iff the code is valid for the given context (not used, not expired,
     * belongs to the right user/event/company).
     */
    public boolean isValid(String userId, String eventName, String companyName) {
        if (used) return false;
        if (expiryDate != null && expiryDate.before(new Date())) return false;
        return this.userId.equals(userId)
                && this.eventName.equals(eventName)
                && this.companyName.equals(companyName);
    }


    public String  getCode() { return code; }
    public String  getUserId() { return userId; }
    public String  getEventName() { return eventName; }
    public String  getCompanyName() { return companyName; }
    public Date    getExpiryDate() { return expiryDate; }
    public boolean isUsed() { return used; }


    public void setUsed(boolean used) { this.used = used; }
}
