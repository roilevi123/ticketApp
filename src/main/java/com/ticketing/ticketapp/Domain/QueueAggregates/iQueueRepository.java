package com.ticketing.ticketapp.Domain.QueueAggregates;

import java.util.List;

public interface iQueueRepository {
    List<QueueEntry> getQueue(String eventId);
    void removeFromQueue(String eventId, String userID);
    void initQueue(String eventId);
    public void deleteAll();
    String checkStatusAtomic(String eventId, String userID, int maxActiveUsers, long accessDuration);
}
