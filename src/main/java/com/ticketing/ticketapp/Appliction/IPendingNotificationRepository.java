package com.ticketing.ticketapp.Appliction;

import java.util.List;

public interface IPendingNotificationRepository {
    void save(String userId, String message);
    List<String> retrieveAndDelete(String userId);
}