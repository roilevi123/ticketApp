package com.ticketing.ticketapp.Infastructure;

import com.ticketing.ticketapp.Appliction.INotifier;
import org.springframework.stereotype.Service;

@Service
public class NotifierImpl implements INotifier {

    private final Broadcaster broadcaster;

    public NotifierImpl(Broadcaster broadcaster) {
        this.broadcaster = broadcaster;
    }

    @Override
    public void notifyUser(String userId, String title, String message) {
        String formattedMessage = String.format("{\"title\": \"%s\", \"message\": \"%s\"}", title, message);
        broadcaster.broadcast(userId, formattedMessage);
    }
    @Override
    public void broadcast(String title, String message) {
        String formattedMessage = String.format("{\"title\": \"%s\", \"message\": \"%s\"}", title, message);
        broadcaster.broadcastToAll(formattedMessage);
    }
}