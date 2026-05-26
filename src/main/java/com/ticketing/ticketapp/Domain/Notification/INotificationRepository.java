package com.ticketing.ticketapp.Domain.Notification;

import java.util.List;

public interface INotificationRepository {
    void save(String userId, String message);
    List<Notification> getAll(String userId);
    List<Notification> getUnread(String userId);
    void markAsRead(String userId, String notificationId);
    void deleteAll();
}
