package com.ticketing.ticketapp.Appliction;

import com.ticketing.ticketapp.Domain.AdminAggregate.iAdminRepository;
import com.ticketing.ticketapp.Domain.QueueAggregates.QueueEntry;
import com.ticketing.ticketapp.Domain.QueueAggregates.iQueueRepository;
import com.ticketing.ticketapp.Infastructure.TokenService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class QueueService {
    private iQueueRepository queueRepository;
    private TokenService tokenService;
    private INotifier notifier;
    private iAdminRepository adminRepository;
    private final int MAX_ACTIVE_USERS = 100;
    private final long ACCESS_DURATION = 10 * 60 * 1000;
    private final Map<String, Integer> eventFlowRates = new ConcurrentHashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(PurchasePolicyService.class);


    public QueueService(iQueueRepository queueRepository, TokenService tokenService, INotifier notifier, iAdminRepository adminRepository) {
        this.queueRepository = queueRepository;
        this.tokenService = tokenService;
        this.notifier = notifier;
        this.adminRepository = adminRepository;
    }

    public Response<String> checkStatus(String token, String eventId) {
        try {
            logger.info("User of token {} is attempting to check status for the event: {}", token, eventId);
            if (!tokenService.validateToken(token)) {
                return Response.error("Invalid token");
            }
            String userID = tokenService.extractUserId(token);

            boolean hadAccess = false;
            try {
                hadAccess = queueRepository.getQueue(eventId).stream()
                        .anyMatch(e -> e.getUserID().equals(userID) && e.getGrantedAccessTime() != null);
            } catch (Exception ignored) {}

            int maxUsers = eventFlowRates.getOrDefault(eventId, MAX_ACTIVE_USERS);
            String status = queueRepository.checkStatusAtomic(eventId, userID, maxUsers, ACCESS_DURATION);

            if ("AUTHORIZED".equals(status) && !hadAccess) {
                notifier.notifyUser(userID, "You Won the Lottery!",
                        "You've been granted access to purchase tickets for event '" + eventId + "'. Act fast before your time expires!");
            }

            logger.info("User {} checked status for event {} successfully", userID, eventId);
            return Response.success(status);
        }

        catch (DataAccessException e) {
            return Response.error("Database unavailable");
        }
        catch (Exception e) {
            return Response.error(e.getMessage());
        }
    }

    public Response<List<QueueEntry>> getQueueForAdmin(String adminId, String eventId) {
        try {
            logger.info("Admin {} is attempting to get queue for the event: {}", adminId, eventId);
            if (!adminRepository.isAdmin(adminId)) {
                return Response.error("Admin does not exist");
            }
            List<QueueEntry> queue = queueRepository.getQueue(eventId);
            logger.info("Admin {} got queue for event {} successfully", adminId, eventId);
            return Response.success(queue);
        }
        catch (DataAccessException e) {
            return Response.error("Database unavailable");
        }
        catch (Exception e) {
            return Response.error(e.getMessage());
        }
    }

    public Response<String> clearQueueForAdmin(String adminId, String eventId) {
        try {
            logger.info("Admin {} is attempting to clear queue for the event: {}", adminId, eventId);
            if (!adminRepository.isAdmin(adminId)) {
                return Response.error("Admin does not exist");
            }
            queueRepository.clearQueue(eventId);
            logger.info("Admin {} cleared queue for event {} successfully", adminId, eventId);
            return Response.success("Queue cleared for event: " + eventId);
        }
        catch (DataAccessException e) {
            return Response.error("Database unavailable");
        }
        catch (Exception e) {
            return Response.error(e.getMessage());
        }
    }

    public Response<String> setFlowRate(String adminId, String eventId, int maxActiveUsers) {
        try {
            logger.info("Admin {} is attempting to set flow rate (max active users: {}) the event: {}", adminId,maxActiveUsers, eventId);
            if (!adminRepository.isAdmin(adminId)) {
                return Response.error("Admin does not exist");
            }
            if (maxActiveUsers < 1) {
                return Response.error("maxActiveUsers must be at least 1");
            }
            eventFlowRates.put(eventId, maxActiveUsers);
            logger.info("Admin {} set flow rate (max active users: {}) for event {} successfully", adminId,maxActiveUsers, eventId);
            return Response.success("Flow rate set to " + maxActiveUsers + " for event: " + eventId);
        }
        catch (DataAccessException e) {
            return Response.error("Database unavailable");
        }
        catch (Exception e) {
            return Response.error(e.getMessage());
        }
    }
}
