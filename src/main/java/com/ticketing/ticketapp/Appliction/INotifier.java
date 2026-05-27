package com.ticketing.ticketapp.Appliction;

public interface INotifier {
    void notifyUser(String userId, String title, String message);
    void notifyUserWithSender(String userId, String senderId, String title, String message);
    void broadcast(String title, String message);
}