package com.ticketing.ticketapp.Infastructure;

import com.ticketing.ticketapp.Domain.Notification.INotificationRepository;
import com.ticketing.ticketapp.Domain.Notification.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
@Component
public class Broadcaster {
    private static final Logger logger = LoggerFactory.getLogger(Broadcaster.class);
    private final Map<String, Consumer<String>> activeConnections = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final INotificationRepository notificationRepository;

    public Broadcaster(INotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public void register(String userId, Consumer<String> connectionListener) {
        activeConnections.put(userId, connectionListener);
        logger.info("User {} registered to Broadcaster.", userId);

        executorService.submit(() -> {
            try {
                List<Notification> unread = notificationRepository.getUnread(userId);

                if (!unread.isEmpty()) {
                    logger.info("Delivering {} unread notifications to user {}", unread.size(), userId);

                    for (Notification n : unread) {
                        try {
                            connectionListener.accept(n.getMessage());
                        } catch (Exception e) {
                            logger.error("Failed to deliver notification to user {}", userId, e);
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("Failed to load unread notifications for user {}", userId, e);
            }
        });
    }

    public void unregister(String userId) {
        activeConnections.remove(userId);
        logger.info("User {} unregistered from Broadcaster.", userId);
    }

    public void broadcast(String userId, String message) {
        notificationRepository.save(userId, message);

        Consumer<String> connection = activeConnections.get(userId);
        if (connection != null) {
            executorService.submit(() -> {
                try {
                    connection.accept(message);
                    logger.info("Message sent to user {}: {}", userId, message);
                } catch (Exception e) {
                    logger.error("Failed to send message to user {}", userId, e);
                }
            });
        } else {
            logger.info("User {} is offline. Message saved to notification history.", userId);
        }
    }

    public void broadcastToAll(String message) {
        notificationRepository.save("BROADCAST", message);
        activeConnections.forEach((userId, connection) -> {
            executorService.submit(() -> {
                try {
                    connection.accept(message);
                } catch (Exception e) {
                    logger.error("Failed to send message to user {}", userId, e);
                }
            });
        });
    }
}
