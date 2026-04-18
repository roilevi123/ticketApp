package Infastructure;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import Domain.Event.MapArea;
import Domain.Ticket.*;

public class TicketRepositoryImpl implements iTicketRepository {
    
    private Map<String, List<Ticket>> tickets = new ConcurrentHashMap<>();
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
    public boolean ticketExists(String id) {
        for (List<Ticket> ticketList : tickets.values()) {
            for (Ticket ticket : ticketList) {
                if (ticket.getId().equals(id)) {
                    return true;
                }
            }
        }
        return false;
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
    public void deleteTicket(String id) {
        for (List<Ticket> ticketList : tickets.values()) {
            ticketList.removeIf(ticket -> ticket.getId().equals(id));
        }
    }

    @Override
    public List<Ticket> getAllTicketsByEventAndCompany(String event, String company) {
        return tickets.getOrDefault(event + company, null);
    }

    @Override
    public List<Ticket> getAvailableTicketsByEventAndCompany(String event, String company) {
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
    public List<Ticket> getPurchasedTicketsByEventAndCompany(String event, String company) {
        List<Ticket> allTickets = tickets.get(event + company);
        if (allTickets == null) {
            return null;
        }
        List<Ticket> purchasedTickets = new ArrayList<>();
        for (Ticket ticket : allTickets) {
            if (ticket.isPurchased()) {
                purchasedTickets.add(ticket);
            }
        }
        return purchasedTickets;
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
    public List<Ticket> getAllTicketsByEvent(String event) {
        List<Ticket> eventTickets = new ArrayList<>();
        for (Map.Entry<String, List<Ticket>> entry : tickets.entrySet()) {
            if (entry.getKey().startsWith(event)) {
                eventTickets.addAll(entry.getValue());
            }
        }
        return eventTickets;
    }

    @Override
    public List<Ticket> getTicketsByDateRange(Date from, Date to, String company) {
        List<Ticket> companyTickets = getAllTicketsByCompany(company);
        if (companyTickets == null) {
            return null;
        }
        List<Ticket> ticketsInRange = new ArrayList<>();
        for (Ticket ticket : companyTickets) {
            if (ticket.getDate().after(from) && ticket.getDate().before(to)) {
                ticketsInRange.add(ticket);
            }
        }
        return ticketsInRange;

    }

    @Override
    public Ticket getTicketBySeat(String event, String company, int row, int col) {
        List<Ticket> eventTickets = tickets.get(event + company);
        if (eventTickets == null) {
            return null;
        }
        for (Ticket ticket : eventTickets) {
            if (ticket.getRow() == row && ticket.getCol() == col) {
                return ticket;
            }
        }
        return null;
    }

    @Override
    public List<Ticket> getTicketsByPriceRange(String event, String company, double minPrice, double maxPrice) {
        List<Ticket> eventTickets = tickets.get(event + company);
        if (eventTickets == null) {
            return null;
        }
        List<Ticket> ticketsInPriceRange = new ArrayList<>();
        for (Ticket ticket : eventTickets) {
            if (ticket.getPrice() >= minPrice && ticket.getPrice() <= maxPrice) {
                ticketsInPriceRange.add(ticket);
            }
        }
        return ticketsInPriceRange;
    }

    @Override
    public void makeMapToTicket(String company, String event, MapArea[][] mapAreas, Date date, double price) {
        for (int i = 0; i < mapAreas.length; i++) {
            for (int j = 0; j < mapAreas[i].length; j++) {
                MapArea area = mapAreas[i][j];

                if (area == MapArea.SEAT) {
                    storeTicket(i, j, event, company, price);
                } else if (area == MapArea.STAND) {
                    for (int k = 0; k < 3; k++) {
                        storeTicket(i, j, event, company, price);
                    }
                }
            }
        }
    }

    @Override
    public void deleteAllTickets() {
        tickets.clear();
        idCounter.set(1);
    }
}
