package Domain.Domains;

import Domain.QueueAggregates.QueueEntry;
import Domain.QueueAggregates.iQueueRepository;
import java.util.List;
import java.util.Optional;

public class QueueDomain {
    private iQueueRepository queueRepository;
    private final int MAX_ACTIVE_USERS = 100;
    private final long ACCESS_DURATION = 10 * 60 * 1000;

    public QueueDomain(iQueueRepository queueRepository) {
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