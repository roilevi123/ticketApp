package com.ticketing.ticketapp.Domain.Event;

import java.util.Date;

public record EventDTO(
        String eventId,
        String companyName,
        String queueId,
        String name,
        String description,
        double rating,
        EventType type,
        String location,
        String artistName,
        Date date,
        double price,
        int totalTickets,
        int availableTickets,
        MapArea[][] map
) {
    public static EventDTO fromEntity(Event event) {
        if (event == null) return null;

        MapArea[][] mapCopy = null;
        if (event.getMap() != null) {
            int rows = event.getMap().length;
            mapCopy = new MapArea[rows][];
            for (int i = 0; i < rows; i++) {
                if (event.getMap()[i] != null) {
                    mapCopy[i] = event.getMap()[i].clone();
                }
            }
        }

        return new EventDTO(
                event.getId(),
                event.getCompany(),
                event.getQueueId(),
                event.getName(),
                event.getDescription(),
                event.getRating(),
                event.getType(),
                event.getLocation(),
                event.getArtistName(),
                event.getDate(),
                event.getPrice(),
                event.getTotalTickets(),
                event.getAvailableTickets(),
                mapCopy
        );
    }
}
