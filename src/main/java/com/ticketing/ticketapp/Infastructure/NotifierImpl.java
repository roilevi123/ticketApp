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
        broadcaster.broadcast(userId, buildJson(title, message));
    }

    @Override
    public void notifyUserWithSender(String userId, String senderId, String title, String message) {
        broadcaster.broadcast(userId, buildJsonWithSender(title, message, senderId));
    }

    @Override
    public void broadcast(String title, String message) {
        broadcaster.broadcastToAll(buildJson(title, message));
    }

    private String buildJson(String title, String message) {
        return "{\"title\":" + jsonString(title) + ",\"message\":" + jsonString(message) + "}";
    }

    private String buildJsonWithSender(String title, String message, String senderId) {
        return "{\"title\":" + jsonString(title) + ",\"message\":" + jsonString(message) + ",\"senderId\":" + jsonString(senderId) + "}";
    }

    private String jsonString(String value) {
        if (value == null) return "\"\"";
        return "\"" + value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                + "\"";
    }
}