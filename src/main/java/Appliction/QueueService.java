package Application;

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
        List<QueueEntry> queue = queueRepository.getQueue(eventId);
        if (queue == null) return "ERROR";

        synchronized (queue) {
            long now = System.currentTimeMillis();

            queue.removeIf(e -> e.getGrantedAccessTime() != null &&
                    (now - e.getGrantedAccessTime() > ACCESS_DURATION));

            Optional<QueueEntry> existingEntry = queue.stream()
                    .filter(e -> e.getUsername().equals(username))
                    .findFirst();

            QueueEntry userEntry;
            if (existingEntry.isEmpty()) {
                queueRepository.addToQueue(eventId, username);
                userEntry = queue.stream()
                        .filter(e -> e.getUsername().equals(username))
                        .findFirst()
                        .orElse(null);
            } else {
                userEntry = existingEntry.get();
            }

            if (userEntry == null) return "ERROR";

            if (userEntry.getGrantedAccessTime() != null) {
                return "AUTHORIZED";
            }

            long activeCount = queue.stream()
                    .filter(e -> e.getGrantedAccessTime() != null)
                    .count();

            long waitingBefore = queue.stream()
                    .takeWhile(e -> !e.getUsername().equals(username))
                    .filter(e -> e.getGrantedAccessTime() == null)
                    .count();

            if (activeCount < MAX_ACTIVE_USERS && waitingBefore == 0) {
                userEntry.setGrantedAccessTime(now);
                return "AUTHORIZED";
            }

            return "WAITING_POSITION_" + (waitingBefore + 1);
        }
    }
}