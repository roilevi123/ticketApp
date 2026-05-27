package com.ticketing.ticketapp.Infastructure;

import com.ticketing.ticketapp.Appliction.IPendingNotificationRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class PendingNotificationRepositoryImpl implements IPendingNotificationRepository {
    private final Map<String, List<String>> store = new ConcurrentHashMap<>();

    @Override
    public void save(String userId, String message) {
        // if the user doesn't have a list yet, create a new synchronized list for them, then add the message to their list
        store.computeIfAbsent(userId, k -> Collections.synchronizedList(new ArrayList<>())).add(message);
    }

    @Override
    public List<String> retrieveAndDelete(String userId) {
        List<String> pendingMessages = store.remove(userId);
        // if there are no pending messages, return an empty list
        return pendingMessages != null ? pendingMessages : new ArrayList<>();
    }
}