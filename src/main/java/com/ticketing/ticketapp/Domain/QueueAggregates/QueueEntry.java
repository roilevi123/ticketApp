package com.ticketing.ticketapp.Domain.QueueAggregates;

public class QueueEntry {
    private String userID;
    private long entryTime;
    private Long grantedAccessTime;
    private boolean isExpired;

    public QueueEntry(String userID) {
        this.userID = userID;
        this.entryTime = System.currentTimeMillis();
        this.grantedAccessTime = null;
        this.isExpired = false;
    }

    // Getters
    public String getUserID() { return userID; }
    public Long getGrantedAccessTime() { return grantedAccessTime; }
    public void setGrantedAccessTime(Long time) { this.grantedAccessTime = time; }


}
