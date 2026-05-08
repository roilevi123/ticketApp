package Appliction;

import Domain.Order.ActiveOrder;
import Domain.Order.IActiveOrderRepository;
import Domain.Ticket.Ticket;
import Domain.Ticket.TicketDTO;
import Domain.Ticket.iTicketRepository;
import Infastructure.TokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
            String userID = null;
            if (tokenService.validateToken(token)) {
                userID = tokenService.extractUserId(token);
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



            String id = activeOrderRepository.store(company, event, reservedTicketIds, userID, expiryDate);
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
            return e.getMessage();
        }
    }
    public List<TicketDTO> getActiveOrderTickets(String token, String orderId) {
        try {
            logger.info("get information about active order ticket reservation for token: " + token);
            List<String> ticketsId;

            if (orderId == null) {
                // No order ID: recover by user identity from token (e.g. after re-login)
                if (!tokenService.validateToken(token)) {
                    throw new RuntimeException("Invalid token");
                }
                String userID = tokenService.extractUserId(token);
                ticketsId = activeOrderRepository.getTicketsId(userID);
            } else {
                // Order ID provided: look up that specific order only
                ActiveOrder activeOrder = activeOrderRepository.findById(orderId);
                if (activeOrder == null) {
                    return null;
                }
                ticketsId = activeOrder.getTicketIds();
            }

            List<Ticket> ticketList = ticketRepository.getTickets(ticketsId);
            List<TicketDTO> ticketDTOS = new ArrayList<>();
            for (Ticket ticket : ticketList) {
                ticketDTOS.add(TicketDTO.fromEntity(ticket));
            }
            logger.info("get active order tickets: {} tickets found", ticketDTOS.size());
            return ticketDTOS;
        } catch (Exception e) {
            logger.error("Error retrieving active order: {}", e.getMessage());
            return null;
        }
    }


}