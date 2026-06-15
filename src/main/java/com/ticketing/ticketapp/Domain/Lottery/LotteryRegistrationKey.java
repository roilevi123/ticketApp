package com.ticketing.ticketapp.Domain.Lottery;

import java.io.Serializable;
import java.util.Objects;

public class LotteryRegistrationKey implements Serializable {
    private String eventName;
    private String companyName;

    public LotteryRegistrationKey() {}

    public LotteryRegistrationKey(String eventName, String companyName) {
        this.eventName = eventName;
        this.companyName = companyName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LotteryRegistrationKey)) return false;
        LotteryRegistrationKey that = (LotteryRegistrationKey) o;
        return Objects.equals(eventName, that.eventName) && Objects.equals(companyName, that.companyName);
    }

    @Override
    public int hashCode() { return Objects.hash(eventName, companyName); }
}
