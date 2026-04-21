package Appliction;

import Domain.Event.Event;
import Domain.Event.iEventRepository;
import Domain.Order.ActiveOrder;
import Domain.Order.IActiveOrderRepository;
import Domain.Order.IPurchasedOrderRepository;
import Domain.Order.PurchasedOrder;
import Domain.Ticket.Ticket;
import Domain.Ticket.iTicketRepository;
import Infastructure.TokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class OrderService {
    
    private IActiveOrderRepository activeOrderRepository;
    private iTicketRepository ticketRepository;
    private TokenService tokenService;


    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

public OrderService(IActiveOrderRepository activeOrderRepository, TokenService tokenService, iTicketRepository ticketRepository) {
        this.activeOrderRepository = activeOrderRepository;
        this.tokenService = tokenService;
        this.ticketRepository = ticketRepository;

    }

    public String reserveTickets(String token, String company, String event, List<int[]> requests) {
        List<String> reservedTicketIds = new ArrayList<>();

        try {
            logger.info("Starting bulk ticket reservation for event: " + event);
            String username = null;
            if (tokenService.validateToken(token)) {
                username = tokenService.extractUsername(token);
            }

            long expirationTime = System.currentTimeMillis() + (10 * 10 * 100);
            Date expiryDate = new Date(expirationTime);
            List<Ticket> availableAtSpot1 = ticketRepository.getAvailableTicketsByEventAndCompany(company, event);

            for (int[] request : requests) {
                int v = request[0];
                int h = request[1];
                List<Ticket> availableAtSpot = availableAtSpot1.stream()
                        .filter(t -> t.getCol() == v &&
                                t.getRow() == h &&
                                (t.getDate() == null || t.getDate().before(new Date())) &&
                                !t.isPurchased() &&
                                !reservedTicketIds.contains(t.getId()))
                        .toList();
                int numRequested = (request.length > 2) ? request[2] : 1;
                int securedForThisSpot = 0;



                for (Ticket ticketInDb : availableAtSpot) {
                    if (securedForThisSpot >= numRequested) {
                        break;
                    }

                    try {
                        Ticket updatedTicket = new Ticket(ticketInDb);
                        updatedTicket.setDate(expiryDate);
                        ticketRepository.save(updatedTicket);
                        reservedTicketIds.add(updatedTicket.getId());
                        securedForThisSpot++;
                    } catch (RuntimeException e) {
                        logger.warn("Concurrency conflict for ticket: " + ticketInDb.getId());
                    }
                }

                if (securedForThisSpot < numRequested) {
                    throw new RuntimeException("Not enough tickets available at spot: " + v + "," + h);
                }
            }



            String id = activeOrderRepository.store(company, event, reservedTicketIds, username, expiryDate);
            logger.info("Successfully reserved " + reservedTicketIds.size() + " tickets");
            return id;

        } catch (Exception e) {
            logger.error("Reservation failed: " + e.getMessage());

            for (String id : reservedTicketIds) {
                try {
                    Ticket t = ticketRepository.getTicketById(id);
                    if (t != null) {
                        Ticket reverted = new Ticket(t);
                        reverted.setDate(null);
                        ticketRepository.save(reverted);
                    }
                } catch (Exception rollbackEx) {
                    logger.error("Failed to rollback ticket " + id);
                }
            }
            return null;
        }
    }
    public String getActiveOrderTickets(String token,String orderId) {
        try {
            logger.info("get information about active order ticket reservation for token: " + token);
            ActiveOrder activeOrder=activeOrderRepository.findById(orderId);
            if (!tokenService.validateToken(token) && activeOrder == null) {
                throw new RuntimeException("Invalid token3");
            }
            List<String> ticketsId=new ArrayList<>();
            if(activeOrder!=null){
                ticketsId=activeOrder.getTicketIds();
            }

            String username="";
            if(tokenService.validateToken(token)){
                username=tokenService.extractUsername(token);
                ticketsId = activeOrderRepository.getTicketsId(username);

            }
            String tickets=ticketRepository.getTicketsDescription(ticketsId);

            logger.info("get active order tickets: " + tickets);
            return tickets;
        } catch (Exception e) {
            logger.error("Error retrieving active order: {}", e.getMessage());
            return null;
        }
    }


}