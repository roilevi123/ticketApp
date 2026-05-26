package com.ticketing.ticketapp.Domain.Notification;

public class Notification {
    private final String id;
    private final String userId;
    private final String message;
    private boolean read;

    public Notification(String id, String userId, String message) {
        this.id = id;
        this.userId = userId;
        this.message = message;
        this.read = false;
    }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getMessage() { return message; }
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
}
