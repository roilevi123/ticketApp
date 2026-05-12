package com.ticketing.ticketapp.Infastructure;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import com.ticketing.ticketapp.Domain.Event.MapArea;
import com.ticketing.ticketapp.Domain.Ticket.*;
import org.springframework.stereotype.Repository;

@Repository
public class TicketRepositoryImpl implements iTicketRepository {
    
    private ConcurrentHashMap<String, List<Ticket>> tickets = new ConcurrentHashMap<>();
    private AtomicLong idCounter = new AtomicLong(1);
    
    @Override
    public void storeTicket(int row, int col, String event, String company, double price) {
        tickets.computeIfAbsent(event + company, k -> new CopyOnWriteArrayList<>())
                .add(new Ticket(row, col, event, company, String.valueOf(idCounter.getAndIncrement()), price));
    }

    @Override
    public Ticket getTicketById(String id) {
        for (List<Ticket> ticketList : tickets.values()) {
            for (Ticket ticket : ticketList) {
                if (ticket.getId().equals(id)) {
                    return ticket;
                }
            }
        }
        return null;
    }
    @Override
    public String getTicketsDescription(List<String> ticketIds) {
        StringBuilder sb = new StringBuilder();
        Set<String> idSet = new HashSet<>(ticketIds);
        tickets.values().stream()
                .flatMap(List::stream)
                .filter(t -> idSet.contains(t.getId()))
                .forEach(t -> sb.append(t.toString()).append("\n"));

        return sb.toString().trim();
    }
    @Override
    public List<Ticket> getTickets(List<String> ticketIds) {
        Set<String> idSet = new HashSet<>(ticketIds);

        return tickets.values().stream()
                .flatMap(List::stream)
                .filter(t -> idSet.contains(t.getId()))
                .toList();
    }

    @Override
    public MapArea[][] getMapAreas(String company, String event,MapArea[][] mapAreas) {
        List<Ticket> ticketList = tickets.get(event + company);
        int rows = mapAreas.length;
        int cols = mapAreas[0].length;
        MapArea[][] newMapAreas = new MapArea[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                newMapAreas[i][j] = mapAreas[i][j];
            }
        }
        for (Ticket ticket : ticketList) {
            if(ticket.isPurchased()){
                newMapAreas[ticket.getRow()][ticket.getCol()]=MapArea.TAKEN;
            }
        }
        return newMapAreas;
    }



   @Override
    public synchronized void save(Ticket ticketToUpdate) {
        String listKey = ticketToUpdate.getEvent() + ticketToUpdate.getCompany();
        List<Ticket> eventTickets = tickets.get(listKey);

        if (eventTickets == null) {
            throw new RuntimeException("No tickets found for update");
        }

        Ticket currentInDb = eventTickets.stream()
                .filter(t -> t.getId().equals(ticketToUpdate.getId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Ticket not found for update"));

        if (currentInDb.getVersion() != ticketToUpdate.getVersion()) {
            throw new RuntimeException("Optimistic Lock Failure: Ticket was updated by another thread");
        }

        Ticket updatedTicket = new Ticket(ticketToUpdate);
        updatedTicket.setVersion(ticketToUpdate.getVersion() + 1);

        int index = eventTickets.indexOf(currentInDb);
        eventTickets.set(index, updatedTicket);
    }



    @Override
    public List<Ticket> getAllTicketsByEventAndCompany(String event, String company) {
        return tickets.getOrDefault(event + company, null);
    }

    @Override
    public List<Ticket> getAvailableTicketsByEventAndCompany(String company, String event) {
        List<Ticket> allTickets = tickets.get(event + company);
        if (allTickets == null) {
            return null;
        }
        List<Ticket> availableTickets = new ArrayList<>();
        for (Ticket ticket : allTickets) {
            if (!ticket.isPurchased()) {
                availableTickets.add(ticket);
            }
        }
        return availableTickets;
    }




    @Override
    public List<Ticket> getAllTicketsByCompany(String company) {
        List<Ticket> companyTickets = new ArrayList<>();
        for (Map.Entry<String, List<Ticket>> entry : tickets.entrySet()) {
            if (entry.getKey().endsWith(company)) {
                companyTickets.addAll(entry.getValue());
            }
        }
        return companyTickets;
    }








    @Override
    public void makeMapToTicket(String company, String event, MapArea[][] mapAreas, Date date, double price) {
        for (int i = 0; i < mapAreas.length; i++) {
            for (int j = 0; j < mapAreas[i].length; j++) {
                MapArea area = mapAreas[i][j];

                if (area == MapArea.SEAT) {
                    storeTicket(i, j, event, company, price);
                } else if (area == MapArea.STAND) {
                    for (int k = 0; k < 1; k++) {
                        storeTicket(i, j, event, company, price);
                    }
                }
            }
        }
    }
    @Override
    public List<Ticket> getTicketsForEvent(String company, String event) {
        List<Ticket> eventTickets = tickets.get(event + company);
        if (eventTickets == null) {
            return new ArrayList<>();
        }
        return eventTickets.stream().map(Ticket::new).collect(Collectors.toList());
    }

    @Override
    public void deleteAllTickets() {
        tickets.clear();
        idCounter.set(1);
    }
}
