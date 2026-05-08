package Infastructure;

import Domain.QueueAggregates.QueueEntry;
import Domain.QueueAggregates.iQueueRepository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public class QueueRepositoryImpl implements iQueueRepository {

    private final Map<String, LinkedList<QueueEntry>> eventQueues = new ConcurrentHashMap<>();

    @Override
    public String checkStatusAtomic(String eventId, String userID, int maxActiveUsers, long accessDuration) {
        AtomicReference<String> result = new AtomicReference<>("ERROR");
        long now = System.currentTimeMillis();

        eventQueues.compute(eventId, (id, queue) -> {
            if (queue == null) {
                result.set("ERROR");
                return null;
            }

            queue.removeIf(e ->
                    e.getGrantedAccessTime() != null &&
                            now - e.getGrantedAccessTime() > accessDuration
            );

            QueueEntry userEntry = queue.stream()
                    .filter(e -> e.getUserID().equals(userID))
                    .findFirst()
                    .orElse(null);

            if (userEntry == null) {
                userEntry = new QueueEntry(userID);
                queue.add(userEntry);
            }

            if (userEntry.getGrantedAccessTime() != null) {
                result.set("AUTHORIZED");
                return queue;
            }

            long activeCount = queue.stream()
                    .filter(e -> e.getGrantedAccessTime() != null)
                    .count();

            long waitingBefore = queue.stream()
                    .takeWhile(e -> !e.getUserID().equals(userID))
                    .filter(e -> e.getGrantedAccessTime() == null)
                    .count();

            if (activeCount < maxActiveUsers && waitingBefore == 0) {
                userEntry.setGrantedAccessTime(now);
                result.set("AUTHORIZED");
            } else {
                result.set("WAITING_POSITION_" + (waitingBefore + 1));
            }

            return queue;
        });

        return result.get();
    }

    @Override
    public void addToQueue(String eventId, String userID) {
        eventQueues.computeIfAbsent(eventId, k -> new LinkedList<>());
        eventQueues.computeIfPresent(eventId, (id, queue) -> {
            boolean alreadyIn = queue.stream()
                    .anyMatch(e -> e.getUserID().equals(userID));

            if (!alreadyIn) {
                queue.add(new QueueEntry(userID));
            }

            return queue;
        });
    }

    @Override
    public List<QueueEntry> getQueue(String eventId) {
        LinkedList<QueueEntry> queue = eventQueues.get(eventId);
        if (queue == null) {
            throw new RuntimeException("Event '" + eventId + "' not found");
        }
        return new ArrayList<>(queue);
    }

    @Override
    public void removeFromQueue(String eventId, String userID) {
        eventQueues.computeIfPresent(eventId, (id, queue) -> {
            queue.removeIf(e -> e.getUserID().equals(userID));
            return queue;
        });
    }

    @Override
    public void initQueue(String eventId) {
        eventQueues.putIfAbsent(eventId, new LinkedList<>());
    }

    @Override
    public void deleteQueue(String eventId) {
        eventQueues.remove(eventId);
    }

    @Override
    public void deleteAll() {
        eventQueues.clear();
    }
}