package com.ticketing.ticketapp.Infastructure;

import com.ticketing.ticketapp.Domain.Notification.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JpaNotificationRepository extends JpaRepository<Notification, String> {

    List<Notification> findByUserId(String userId);

    List<Notification> findByUserIdAndReadFalse(String userId);
}