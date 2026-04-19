package Domain.QueueAggregates;

import java.util.List;

public interface iQueueRepository {
    void addToQueue(String eventId, String username);
    List<QueueEntry> getQueue(String eventId);
    void removeFromQueue(String eventId, String username);
    void initQueue(String eventId);
    void deleteQueue(String eventId);
    public void deleteAll();

}