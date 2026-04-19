package Infastructure;

import Domain.QueueAggregates.QueueEntry;
import Domain.QueueAggregates.iQueueRepository;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class QueueRepositoryImpl implements iQueueRepository {
    private Map<String, LinkedList<QueueEntry>> eventQueues = new java.util.concurrent.ConcurrentHashMap<>();
    @Override
    public void addToQueue(String eventId, String username) {
        eventQueues.computeIfAbsent(eventId, k -> new LinkedList<>());

        LinkedList<QueueEntry> queue = eventQueues.get(eventId);

        synchronized (queue) {
            boolean alreadyIn = queue.stream()
                    .anyMatch(e -> e.getUsername().equals(username));
            if (!alreadyIn) {
                queue.add(new QueueEntry(username));
            }
        }
    }

    @Override
    public List<QueueEntry> getQueue(String eventId) {
        if (!eventQueues.containsKey(eventId)) {
            throw new RuntimeException("Event '"+eventId+"' not found");
        }
        return eventQueues.computeIfAbsent(eventId, k -> new LinkedList<>());
    }
    @Override
    public void removeFromQueue(String eventId, String username) {
        if (eventQueues.containsKey(eventId)) {
            eventQueues.get(eventId).removeIf(e -> e.getUsername().equals(username));
        }
    }

    @Override
    public void initQueue(String eventId) {
        if (!eventQueues.containsKey(eventId)) {
            eventQueues.put(eventId, new LinkedList<>());
        }
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