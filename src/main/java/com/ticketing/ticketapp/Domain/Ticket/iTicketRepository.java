package com.ticketing.ticketapp.Domain.Ticket;

import java.util.List;
import java.util.Date;
import com.ticketing.ticketapp.Domain.Event.MapArea;

public interface iTicketRepository {
    public void storeTicket(int row, int col, String event, String company, double price);
    public Ticket getTicketById(String id);
    public void save(Ticket ticketToUpdate);
    public List<Ticket> getAllTicketsByEventAndCompany(String event, String company);
    public List<Ticket> getAvailableTicketsByEventAndCompany(String event, String company);
    public List<Ticket> getAllTicketsByCompany(String company);

    public void makeMapToTicket(String company, String event, MapArea[][] mapAreas,Date date, double price);
    public void deleteAllTickets();
    public String getTicketsDescription(List<String> ticketIds);
    public List<Ticket> getTicketsForEvent(String company, String event);
    public List<Ticket> getTickets(List<String> ticketIds);
    public MapArea[][] getMapAreas(String company, String event,MapArea[][] mapAreas);
}
