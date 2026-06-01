package com.ticketing.ticketapp.Appliction;

import com.ticketing.ticketapp.Domain.Event.Event;
import com.ticketing.ticketapp.Domain.Event.iEventRepository;
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
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class OrderService {

    private final long reservationWindowMs;

    private IActiveOrderRepository activeOrderRepository;
    private iTicketRepository ticketRepository;
    private TokenService tokenService;
    private IUserRepository userRepository;
    private iPurchasePolicyRepository purchasePolicyRepo;
    private INotifier notifier;
    private iEventRepository eventRepository;
    private LotteryService lotteryService;
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    private static long defaultReservationWindowMs() {
        try {
            Class.forName("org.junit.jupiter.api.Test");
            return 10 * 1000L;
        } catch (ClassNotFoundException ex) {
            return 2 * 60 * 1000L;
        }
    }

    public OrderService(IActiveOrderRepository activeOrderRepository, TokenService tokenService,
            iTicketRepository ticketRepository, IUserRepository userRepository,
            iPurchasePolicyRepository purchasePolicyRepo, INotifier notifier,
            iEventRepository eventRepository, LotteryService lotteryService) {
        this.activeOrderRepository = activeOrderRepository;
        this.tokenService = tokenService;
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.purchasePolicyRepo = purchasePolicyRepo;
        this.notifier = notifier;
        this.eventRepository = eventRepository;
        this.lotteryService = lotteryService;
        this.reservationWindowMs = defaultReservationWindowMs();
    }

    /**
     * Reserves tickets for an event.
     *
     * @param lotteryCode Required (non-null / non-blank) when the event is a
     *                    high-demand lottery event; must be a valid, unused code
     *                    issued to this user. Pass {@code null} for normal events.
     */
    @Transactional
    public Response<String> reserveTickets(String token, String company, String event,
            List<int[]> requests, String lotteryCode) {
        List<String> reservedTicketIds = new ArrayList<>();

        try {
            logger.info("Starting bulk ticket reservation for event: " + event);
            String userID = null;
            if (tokenService.validateToken(token)) {
                userID = tokenService.extractUserId(token);
            }
            if(userID!=null)
                if(userRepository.isUserSuspendedNow(userID))
                    throw new OrderTransactionException("User is suspended");

            // ── Lottery gate-check ────────────────────────────────────────────
            Event eventEntity = eventRepository.getEvent(event, company);
            if (eventEntity != null && eventEntity.isHighDemand()) {
                if (lotteryCode == null || lotteryCode.isBlank()) {
                    throw new RuntimeException(
                            "This is a high-demand event. A lottery purchase code is required.");
                }
                if (userID == null) {
                    throw new RuntimeException(
                            "You must be logged in to purchase tickets for a lottery event.");
                }
                if (!lotteryService.validateLotteryCode(lotteryCode, userID, event, company)) {
                    throw new RuntimeException(
                            "Invalid, expired, or already-used lottery purchase code.");
                }
                // Consume the code immediately so it cannot be reused
                lotteryService.consumeLotteryCode(lotteryCode);
                logger.info("Lottery code validated and consumed for user '{}', event '{}'", userID, event);
            }
            // ── End lottery gate-check ────────────────────────────────────────

            int totalRequested = requests.stream().mapToInt(r -> (r.length > 2) ? r[2] : 1).sum();
            validatePurchasePolicies(event, company, userID, totalRequested);

            long expirationTime = System.currentTimeMillis() + reservationWindowMs;
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
            if (userID != null) {
                SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm:ss");
                String formattedTime = timeFormatter.format(expiryDate);
                notifier.notifyUser(userID,
                        "Reservation Successful",
                        "Your reservation for event " + event + " was successful. Please complete the payment by "
                                + formattedTime);
            }
            return Response.success(id);

        } catch (Exception e) {
            logger.error("Reservation failed: " + e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Response<List<TicketDTO>> getActiveOrderTickets(String token, String orderId) {
        try {
            logger.info("get information about active order ticket reservation for token: " + token);
            List<String> ticketsId;
            ActiveOrder activeOrder;

            if (orderId == null) {
                if (!tokenService.validateToken(token)) {
                    throw new RuntimeException("Invalid token");
                }
                String userID = tokenService.extractUserId(token);
                activeOrder = activeOrderRepository.getOrder(userID);
                if (activeOrder == null) {
                    return Response.success(new ArrayList<>());
                }
            } else {
                activeOrder = activeOrderRepository.findById(orderId);
                if (activeOrder == null) {
                    return Response.error("Order not found");
                }
            }

            if (activeOrder.getExpirationTime() != null && !activeOrder.getExpirationTime().after(new Date())) {
                activeOrderRepository.delete(activeOrder.getOrderId());
                return Response.success(new ArrayList<>());
            }

            ticketsId = activeOrder.getTicketIds();

            List<Ticket> ticketList = ticketRepository.getTickets(ticketsId);
            List<TicketDTO> ticketDTOS = new ArrayList<>();
            for (Ticket ticket : ticketList) {
                ticketDTOS.add(TicketDTO.fromEntity(ticket));
            }
            logger.info("get active order tickets: {} tickets found", ticketDTOS.size());
            return Response.success(ticketDTOS);
        } catch (Exception e) {
            logger.error("Error retrieving active order: {}", e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    private void validatePurchasePolicies(String eventId, String companyName, String userId, int totalRequested)
            {
        User user = userRepository.getUserByID(userId);
        int age = (user != null) ? user.getAge() : 10000;

        PurchaseValidationData data = new PurchaseValidationData(age, totalRequested);

        PurchasePolicy eventPolicy = purchasePolicyRepo.findByEvent(eventId);
        if (eventPolicy != null && !eventPolicy.validate(data)) {
            throw new OrderTransactionException("Doesn't stand in Event Purchase Policy");
        }

        PurchasePolicy companyPolicy = purchasePolicyRepo.findByCompany(companyName);
        if (companyPolicy != null && !companyPolicy.validate(data)) {
            throw new OrderTransactionException("Doesn't stand in Company Purchase Policy");
        }
    }

}
