package com.ticketing.ticketapp.Domain.User;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

public class SuspensionKey implements Serializable {

    private String userID;
    private LocalDateTime startTime;

    public SuspensionKey() {}

    public SuspensionKey(String userID, LocalDateTime startTime) {
        this.userID = userID;
        this.startTime = startTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SuspensionKey)) return false;
        SuspensionKey that = (SuspensionKey) o;
        return Objects.equals(userID, that.userID) && Objects.equals(startTime, that.startTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userID, startTime);
    }
}
