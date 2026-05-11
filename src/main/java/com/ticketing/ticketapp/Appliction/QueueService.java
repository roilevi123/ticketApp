package com.ticketing.ticketapp.Appliction;

import com.ticketing.ticketapp.Domain.QueueAggregates.iQueueRepository;
import com.ticketing.ticketapp.Infastructure.TokenService;
import org.springframework.stereotype.Service;

@Service
public class QueueService {
    private iQueueRepository queueRepository;
    private TokenService tokenService;
    private final int MAX_ACTIVE_USERS = 100;
    private final long ACCESS_DURATION = 10 * 60 * 1000;

    public QueueService(iQueueRepository queueRepository, TokenService tokenService) {
        this.queueRepository = queueRepository;
        this.tokenService = tokenService;
    }

    public String checkStatus(String token, String eventId) {
        if (!tokenService.validateToken(token)) {
            throw new RuntimeException("Invalid token");
        }
        String userID = tokenService.extractUserId(token);
        return queueRepository.checkStatusAtomic(
                eventId,
                userID,
                MAX_ACTIVE_USERS,
                ACCESS_DURATION
        );
    }
}
