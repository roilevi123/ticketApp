package Appliction;

import Domain.QueueAggregates.QueueEntry;
import Domain.QueueAggregates.iQueueRepository;
import java.util.List;
import java.util.Optional;

public class QueueService {
    private iQueueRepository queueRepository;
    private final int MAX_ACTIVE_USERS = 100;
    private final long ACCESS_DURATION = 10 * 60 * 1000;

    public QueueService(iQueueRepository queueRepository) {
        this.queueRepository = queueRepository;
    }

    public String checkStatus(String eventId, String username) {
        return queueRepository.checkStatusAtomic(
                eventId,
                username,
                MAX_ACTIVE_USERS,
                ACCESS_DURATION
        );
    }
}