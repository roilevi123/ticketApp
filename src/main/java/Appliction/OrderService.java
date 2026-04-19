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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class OrderService {
    
    private IActiveOrderRepository activeOrderRepository;
    private iTicketRepository ticketRepository;
    private TokenService tokenService;
    private IPaymentService paymentService;
    private IBarcodeGenerator barcodeGenerator;
    private ISupplyService supplyService;
    private IPurchasedOrderRepository purchasedOrderRepository;

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

public OrderService(IActiveOrderRepository activeOrderRepository, TokenService tokenService, iTicketRepository ticketRepository, 
                        IPaymentService paymentService, IBarcodeGenerator barcodeGenerator, 
                        ISupplyService supplyService, IPurchasedOrderRepository purchasedOrderRepository) {
        this.activeOrderRepository = activeOrderRepository;
        this.tokenService = tokenService;
        this.ticketRepository = ticketRepository;
        this.paymentService = paymentService;
        this.barcodeGenerator = barcodeGenerator;
        this.supplyService = supplyService;
        this.purchasedOrderRepository = purchasedOrderRepository;
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

            for (int[] request : requests) {
                int v = request[0];
                int h = request[1];

                int ticketsSecuredForSpot = 0;
                int maxRetries = 5;


                List<Ticket> availableAtSpot = ticketRepository.getAvailableTicketsByEventAndCompany(company, event).stream()
                        .filter(t -> t.getCol() == v &&
                                t.getRow() == h &&
                                (t.getDate() == null || t.getDate().before(new Date())) &&
                                !t.isPurchased() &&
                                !reservedTicketIds.contains(t.getId()))
                        .toList();

                if (availableAtSpot.isEmpty()) {
                    throw new RuntimeException("Not enough tickets available at spot: " + v + "," + h);
                }

                for (Ticket ticketInDb : availableAtSpot) {

                    try {
                        Ticket updatedTicket = new Ticket(ticketInDb);
                        updatedTicket.setDate(expiryDate);

                        ticketRepository.save(updatedTicket);

                        reservedTicketIds.add(updatedTicket.getId());
                        ticketsSecuredForSpot++;

                    } catch (RuntimeException e) {

                        break;
                    }
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
    public String purchaseTickets(String token, String orderId, String creditCard, String email) {
        try {
            logger.info("Starting purchase process for order: " + orderId);
            if (!tokenService.validateToken(token)) {
                throw new RuntimeException("Invalid token");
            }
            String username = tokenService.extractUsername(token);

            // fetch the active order
            ActiveOrder activeOrder = activeOrderRepository.findById(orderId);
            if (activeOrder == null) {
                throw new RuntimeException("Order not found or expired");
            }

            // calculate total price
            double totalPrice = 0.0;
            for (String ticketId : activeOrder.getTicketIds()) {
                Ticket t = ticketRepository.getTicketById(ticketId);
                if (t != null) totalPrice += t.getPrice();
            }
            

            boolean paymentSuccess = paymentService.processPayment(creditCard, totalPrice);
            if (!paymentSuccess) {
                throw new RuntimeException("Payment failed");
            }

            // update tickets as purchased and generate barcodes
            java.util.Map<String, String> barcodes = new java.util.HashMap<>();
            for (String ticketId : activeOrder.getTicketIds()) {
                Ticket t = ticketRepository.getTicketById(ticketId);
                if (t != null) {
                    Ticket updated = new Ticket(t);
                    updated.purchase(); 
                    updated.setDate(null); 
                    ticketRepository.save(updated);

                    String barcode = barcodeGenerator.generateBarcode(activeOrder.getEventId(), ticketId);
                    barcodes.put(ticketId, barcode);
                }
            }

            // save purchased order record
            PurchasedOrder purchasedOrder = new PurchasedOrder(
                    activeOrder.getOrderId(), username, activeOrder.getEventId(),
                    activeOrder.getCompanyId(), barcodes, totalPrice
            );
            purchasedOrderRepository.save(purchasedOrder);

            supplyService.supplyToEmail(email, "Your barcodes: " + barcodes.values().toString());

            activeOrderRepository.delete(orderId);

            logger.info("Purchase successful for order: " + orderId);
            return "success";

        } catch (Exception e) {
            logger.error("Purchase failed: " + e.getMessage());
            return "failed: " + e.getMessage();
        }
    }

}