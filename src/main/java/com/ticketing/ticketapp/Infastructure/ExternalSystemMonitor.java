package com.ticketing.ticketapp.Infastructure;

import jakarta.annotation.PostConstruct;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ExternalSystemMonitor {

    private final ExternalSystemClient client;

    private volatile boolean available = false;

    public ExternalSystemMonitor(ExternalSystemClient client) {
        this.client = client;
    }

    @PostConstruct
    public void init() {
        refreshHandshake();
    }

    @Scheduled(fixedDelay = 5000)
    public void refreshHandshake() {

        boolean oldStatus = available;

        available = client.handshake();

        if (oldStatus != available) {
            System.out.println(
                    "External system status changed: " + available
            );
        }
    }

    public boolean isAvailable() {
        return available;
    }
}