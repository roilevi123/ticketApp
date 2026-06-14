package com.ticketing.ticketapp.Domain.User;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "suspensions")
@IdClass(SuspensionKey.class)
public class Suspension {

    @Id
    @Column(name = "user_id")
    private String userID;

    @Id
    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "is_permanent", nullable = false)
    private boolean isPermanent;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    protected Suspension() {}

    public Suspension(String userID, LocalDateTime startTime, LocalDateTime endTime) {
        this.userID = userID;
        this.startTime = startTime;
        this.endTime = endTime;
        this.isPermanent = false;
        this.isActive = true;
    }

    public Suspension(String userID, LocalDateTime startTime) {
        this.userID = userID;
        this.startTime = startTime;
        this.endTime = null;
        this.isPermanent = true;
        this.isActive = true;
    }

    public String getUserID()              { return userID; }
    public LocalDateTime getStartTime()    { return startTime; }
    public LocalDateTime getEndTime()      { return endTime; }
    public boolean isPermanent()           { return isPermanent; }
    public boolean isActive()              { return isActive; }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
        this.isPermanent = false;
    }

    public void setActive(boolean active)  { this.isActive = active; }
}
