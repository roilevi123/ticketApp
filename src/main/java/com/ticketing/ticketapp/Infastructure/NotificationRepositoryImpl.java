package com.ticketing.ticketapp.Infastructure;

import com.ticketing.ticketapp.Domain.Notification.INotificationRepository;
import com.ticketing.ticketapp.Domain.Notification.Notification;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class NotificationRepositoryImpl implements INotificationRepository {

    private final Map<String, List<Notification>> store = new ConcurrentHashMap<>();

    @Override
    public void save(String userId, String message) {
        Notification notification = new Notification(UUID.randomUUID().toString(), userId, message);
        store.computeIfAbsent(userId, k -> Collections.synchronizedList(new ArrayList<>())).add(notification);
    }

    @Override
    public List<Notification> getAll(String userId) {
        List<Notification> notifications = store.get(userId);
        return notifications != null ? new ArrayList<>(notifications) : new ArrayList<>();
    }

    @Override
    public List<Notification> getUnread(String userId) {
        return getAll(userId).stream()
                .filter(n -> !n.isRead())
                .collect(Collectors.toList());
    }

    @Override
    public void markAsRead(String userId, String notificationId) {
        List<Notification> notifications = store.get(userId);
        if (notifications == null) return;
        synchronized (notifications) {
            notifications.stream()
                    .filter(n -> n.getId().equals(notificationId))
                    .findFirst()
                    .ifPresent(n -> n.setRead(true));
        }
    }

    @Override
    public void deleteAll() {
        store.clear();
    }
}
