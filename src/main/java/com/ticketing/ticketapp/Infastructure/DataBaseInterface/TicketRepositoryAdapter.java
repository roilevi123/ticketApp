package com.ticketing.ticketapp.Infastructure.DataBaseInterface;

import com.ticketing.ticketapp.Domain.Event.MapArea;
import com.ticketing.ticketapp.Domain.Ticket.Ticket;
import com.ticketing.ticketapp.Domain.Ticket.iTicketRepository;
import com.ticketing.ticketapp.Infastructure.JpaTicketRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
@Repository
@ConditionalOnProperty(name = "repository.type", havingValue = "DB")
public class TicketRepositoryAdapter implements iTicketRepository {

    private final JpaTicketRepository jpaTicketRepository;

    public TicketRepositoryAdapter(JpaTicketRepository jpaTicketRepository) {
        this.jpaTicketRepository = jpaTicketRepository;
    }

    @Override
    @Transactional
    public void storeTicket(int row, int col, String event, String company, double price) {
        String id = UUID.randomUUID().toString();
        jpaTicketRepository.save(new Ticket(row, col, event, company, id, price));
    }

    @Override
    public Ticket getTicketById(String id) {
        return jpaTicketRepository.findById(id).orElse(null);
    }

    @Override
    @Transactional
    public void save(Ticket ticketToUpdate) {
        // @Version on Ticket.version handles optimistic locking automatically
        jpaTicketRepository.save(ticketToUpdate);
    }

    @Override
    public List<Ticket> getAllTicketsByEventAndCompany(String event, String company) {
        return jpaTicketRepository.findByEventAndCompany(event, company);
    }

    @Override
    public List<Ticket> getAvailableTicketsByEventAndCompany(String event, String company) {
        // Callers pass (companyName, eventName) — the interface parameter names are reversed
        // relative to actual call-site convention (matching the legacy in-memory impl behaviour).
        return jpaTicketRepository.findAvailableByEventAndCompany(company, event);
    }

    @Override
    public List<Ticket> getAllTicketsByCompany(String company) {
        return jpaTicketRepository.findByCompany(company);
    }

    @Override
    @Transactional
    public void makeMapToTicket(String company, String event, MapArea[][] mapAreas, Date date, double price) {
        List<Ticket> toSave = new ArrayList<>();
        for (int i = 0; i < mapAreas.length; i++) {
            for (int j = 0; j < mapAreas[i].length; j++) {
                MapArea area = mapAreas[i][j];
                if (area == MapArea.SEAT || area == MapArea.STAND) {
                    Ticket ticket = new Ticket(i, j, event, company, UUID.randomUUID().toString(), price);
                    ticket.setDate(date);
                    toSave.add(ticket);
                }
            }
        }
        jpaTicketRepository.saveAll(toSave);
    }

    @Override
    @Transactional
    public void deleteAllTickets() {
        jpaTicketRepository.deleteAll();
    }

    @Override
    public String getTicketsDescription(List<String> ticketIds) {
        List<Ticket> found = jpaTicketRepository.findAllById(ticketIds);
        StringBuilder sb = new StringBuilder();
        for (Ticket t : found) {
            sb.append(t.toString()).append("\n");
        }
        return sb.toString().trim();
    }

    @Override
    public List<Ticket> getTicketsForEvent(String company, String event) {
        return jpaTicketRepository.findByEventAndCompany(event, company);
    }

    @Override
    public List<Ticket> getTickets(List<String> ticketIds) {
        return jpaTicketRepository.findAllById(ticketIds);
    }

    @Override
    public MapArea[][] getMapAreas(String company, String event, MapArea[][] mapAreas) {
        List<Ticket> ticketList = jpaTicketRepository.findByEventAndCompany(event, company);
        int rows = mapAreas.length;
        int cols = mapAreas[0].length;
        MapArea[][] newMapAreas = new MapArea[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                newMapAreas[i][j] = mapAreas[i][j];
            }
        }
        for (Ticket ticket : ticketList) {
            if (ticket.isPurchased()) {
                newMapAreas[ticket.getRow()][ticket.getCol()] = MapArea.TAKEN;
            }
        }
        return newMapAreas;
    }
}
