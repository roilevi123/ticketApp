package com.ticketing.ticketapp.Infastructure.DataBaseInterface;

import com.ticketing.ticketapp.Domain.Notification.INotificationRepository;
import com.ticketing.ticketapp.Domain.Notification.Notification;
import com.ticketing.ticketapp.Infastructure.JpaNotificationRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
@ConditionalOnProperty(name = "repository.type", havingValue = "DB", matchIfMissing = true)
public class NotificationRepositoryAdapter implements INotificationRepository {

    private final JpaNotificationRepository jpaRepository;

    public NotificationRepositoryAdapter(JpaNotificationRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public void save(String userId, String message) {
        Notification notification =
                new Notification(UUID.randomUUID().toString(), userId, message);

        jpaRepository.saveAndFlush(notification);
    }

    @Override
    public List<Notification> getAll(String userId) {
        return jpaRepository.findByUserId(userId);
    }

    @Override
    public List<Notification> getUnread(String userId) {
        return jpaRepository.findByUserIdAndReadFalse(userId);
    }

    @Override
    @Transactional
    public void markAsRead(String userId, String notificationId) {
        setReadStatus(userId, notificationId, true);
    }

    @Override
    @Transactional
    public void markAsUnread(String userId, String notificationId) {
        setReadStatus(userId, notificationId, false);
    }

    @Override
    @Transactional
    public void markAllAsRead(String userId) {
        List<Notification> notifications = jpaRepository.findByUserId(userId);

        for (Notification notification : notifications) {
            notification.setRead(true);
        }

        jpaRepository.saveAllAndFlush(notifications);
    }

    private void setReadStatus(String userId, String notificationId, boolean read) {
        Notification notification = jpaRepository.findById(notificationId)
                .filter(n -> n.getUserId().equals(userId))
                .orElse(null);

        if (notification == null) {
            return;
        }

        notification.setRead(read);
        jpaRepository.saveAndFlush(notification);
    }

    @Override
    @Transactional
    public void deleteAll() {
        jpaRepository.deleteAll();
        jpaRepository.flush();
    }
}