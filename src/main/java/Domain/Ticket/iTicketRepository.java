package Domain.Ticket;

import java.util.List;
import java.util.Date;
import Domain.Event.MapArea;

public interface iTicketRepository {
    public void storeTicket(int row, int col, String event, String company, double price);
    public Ticket getTicketById(String id);
    public boolean ticketExists(String id);
    public void save(Ticket ticketToUpdate);
    public void deleteTicket(String id);
    public List<Ticket> getAllTicketsByEventAndCompany(String event, String company);
    public List<Ticket> getAvailableTicketsByEventAndCompany(String event, String company);
    public List<Ticket> getPurchasedTicketsByEventAndCompany(String event, String company);
    public List<Ticket> getAllTicketsByCompany(String company);
    public List<Ticket> getAllTicketsByEvent(String event);
    public List<Ticket> getTicketsByDateRange(Date from, Date to, String company);
    public Ticket getTicketBySeat(String event, String company, int row, int col);
    public List<Ticket> getTicketsByPriceRange(String event, String company, double minPrice, double maxPrice);
    public void makeMapToTicket(String company, String event, MapArea[][] mapAreas,Date date, double price);
    public void deleteAllTickets();
    public String getTicketsDescription(List<String> ticketIds);
    public List<Ticket> getTicketsForEvent(String company, String event);

}
