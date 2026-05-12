package com.ticketing.ticketapp.Appliction;

import com.ticketing.ticketapp.Domain.Order.ActiveOrder;
import com.ticketing.ticketapp.Domain.Order.IActiveOrderRepository;
import com.ticketing.ticketapp.Domain.PurchasePolicy.PurchasePolicy;
import com.ticketing.ticketapp.Domain.PurchasePolicy.PurchaseValidationData;
import com.ticketing.ticketapp.Domain.PurchasePolicy.iPurchasePolicyRepository;
import com.ticketing.ticketapp.Domain.Ticket.Ticket;
import com.ticketing.ticketapp.Domain.Ticket.TicketDTO;
import com.ticketing.ticketapp.Domain.Ticket.iTicketRepository;
import com.ticketing.ticketapp.Domain.User.IUserRepository;
import com.ticketing.ticketapp.Domain.User.User;
import com.ticketing.ticketapp.Infastructure.TokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class OrderService {
    
    private IActiveOrderRepository activeOrderRepository;
    private iTicketRepository ticketRepository;
    private TokenService tokenService;
    private IUserRepository userRepository;
    private iPurchasePolicyRepository purchasePolicyRepo;
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

public OrderService(IActiveOrderRepository activeOrderRepository, TokenService tokenService, iTicketRepository ticketRepository,IUserRepository userRepository,iPurchasePolicyRepository purchasePolicyRepo) {
        this.activeOrderRepository = activeOrderRepository;
        this.tokenService = tokenService;
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.purchasePolicyRepo = purchasePolicyRepo;

    }

    public String reserveTickets(String token, String company, String event, List<int[]> requests) {
        List<String> reservedTicketIds = new ArrayList<>();

        try {
            logger.info("Starting bulk ticket reservation for event: " + event);
            String userID = null;
            if (tokenService.validateToken(token)) {
                userID = tokenService.extractUserId(token);
            }
            int totalRequested = requests.stream().mapToInt(r -> (r.length > 2) ? r[2] : 1).sum();
            validatePurchasePolicies(event, company, userID, totalRequested);

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
    private void validatePurchasePolicies(String eventId, String companyName, String userId, int totalRequested) throws Exception {
        User user = userRepository.getUserByID(userId);
        int age = (user != null) ? user.getAge() : 10000;

        PurchaseValidationData data = new PurchaseValidationData(age, totalRequested);

        PurchasePolicy eventPolicy = purchasePolicyRepo.findByEvent(eventId);
        if (eventPolicy != null && !eventPolicy.validate(data)) {
            throw new Exception("Doesn't stand in Event Purchase Policy");
        }

        PurchasePolicy companyPolicy = purchasePolicyRepo.findByCompany(companyName);
        if (companyPolicy != null && !companyPolicy.validate(data)) {
            throw new Exception("Doesn't stand in Company Purchase Policy");
        }
    }


}
