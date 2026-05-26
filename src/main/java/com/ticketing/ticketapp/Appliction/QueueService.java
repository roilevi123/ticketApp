package com.ticketing.ticketapp.Appliction;

import com.ticketing.ticketapp.Domain.QueueAggregates.iQueueRepository;
import com.ticketing.ticketapp.Infastructure.TokenService;
import org.springframework.stereotype.Service;

@Service
public class QueueService {
    private iQueueRepository queueRepository;
    private TokenService tokenService;
    private INotifier notifier;
    private final int MAX_ACTIVE_USERS = 100;
    private final long ACCESS_DURATION = 10 * 60 * 1000;

    public QueueService(iQueueRepository queueRepository, TokenService tokenService, INotifier notifier) {
        this.queueRepository = queueRepository;
        this.tokenService = tokenService;
        this.notifier = notifier;
    }

    public Response<String> checkStatus(String token, String eventId) {
        try {
            if (!tokenService.validateToken(token)) {
                return Response.error("Invalid token");
            }
            String userID = tokenService.extractUserId(token);

            boolean hadAccess = false;
            try {
                hadAccess = queueRepository.getQueue(eventId).stream()
                        .anyMatch(e -> e.getUserID().equals(userID) && e.getGrantedAccessTime() != null);
            } catch (Exception ignored) {}

            String status = queueRepository.checkStatusAtomic(eventId, userID, MAX_ACTIVE_USERS, ACCESS_DURATION);

            if ("AUTHORIZED".equals(status) && !hadAccess) {
                notifier.notifyUser(userID, "You Won the Lottery!",
                        "You've been granted access to purchase tickets for event '" + eventId + "'. Act fast before your time expires!");
            }

            return Response.success(status);
        } catch (Exception e) {
            return Response.error(e.getMessage());
        }
    }
}
