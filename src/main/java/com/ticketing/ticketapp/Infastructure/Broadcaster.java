package com.ticketing.ticketapp.Infastructure;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.ticketing.ticketapp.Appliction.IPendingNotificationRepository;

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
    private final IPendingNotificationRepository pendingRepo;

    public Broadcaster(IPendingNotificationRepository pendingRepo) {
        this.pendingRepo = pendingRepo;
    }

    public void register(String userId, Consumer<String> connectionListener) {
        activeConnections.put(userId, connectionListener);
        logger.info("User {} registered to Broadcaster.", userId);
        List<String> pendingMessages = pendingRepo.retrieveAndDelete(userId);
        if (!pendingMessages.isEmpty()) {
            logger.info("Sending {} pending messages to user {}", pendingMessages.size(), userId);
            for (String msg : pendingMessages) {
                broadcast(userId, msg); 
            }
        }
    }
    public void unregister(String userId) {
        activeConnections.remove(userId);
        logger.info("User {} unregistered from Broadcaster.", userId);
    }

    // broadcast a message to a specific user
    public void broadcast(String userId, String message) {
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
        }
        else {
            logger.info("User {} is offline. Saving message to pending repository.", userId);
            pendingRepo.save(userId, message);
        }
    }
    // broadcast a message to all connected users
    public void broadcastToAll(String message) {
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
