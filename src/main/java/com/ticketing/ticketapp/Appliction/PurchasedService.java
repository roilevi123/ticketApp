package com.ticketing.ticketapp.Appliction;


import com.ticketing.ticketapp.Domain.Discount.DiscountPolicy;
import com.ticketing.ticketapp.Domain.Discount.MaxDiscountComposite;
import com.ticketing.ticketapp.Domain.Discount.PurchaseContext;
import com.ticketing.ticketapp.Domain.Discount.iDiscountPolicyRepository;
import com.ticketing.ticketapp.Domain.Order.ActiveOrder;
import com.ticketing.ticketapp.Domain.Order.IActiveOrderRepository;
import com.ticketing.ticketapp.Domain.OwnerManagerTree.Owner;
import com.ticketing.ticketapp.Domain.OwnerManagerTree.iTreeOfRoleRepository;
import com.ticketing.ticketapp.Domain.PurchasedOrderAggregate.PurchaseOrder;
import com.ticketing.ticketapp.Domain.PurchasedOrderAggregate.PurchaseOrderException;
import com.ticketing.ticketapp.Domain.User.IUserRepository;
import com.ticketing.ticketapp.Domain.User.User;
import com.ticketing.ticketapp.Domain.PurchasedOrderAggregate.PurchaseOrderDTO;
import com.ticketing.ticketapp.Domain.PurchasedOrderAggregate.iPurchasedOrderRepository;
import com.ticketing.ticketapp.Domain.PurchasedOrderAggregate.SalesReportDTO;
import com.ticketing.ticketapp.Domain.Ticket.Ticket;
import com.ticketing.ticketapp.Domain.Ticket.TicketDTO;
import com.ticketing.ticketapp.Domain.Ticket.iTicketRepository;
import com.ticketing.ticketapp.Domain.payment.CreditCardDetails;
import com.ticketing.ticketapp.Infastructure.ExternalServiceException;
import com.ticketing.ticketapp.Infastructure.TokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import com.ticketing.ticketapp.Domain.OwnerManagerTree.Manager;

@Service
public class PurchasedService {
    private IActiveOrderRepository repository;
    private ISupplyService supplyService;
    private IPaymentService paymentService;
    private IBarcodeGenerator barcodeGenerator;
    private IExternalTicketService externalTicketService;
    private iTicketRepository ticketRepository;
    private iPurchasedOrderRepository purchasedOrderRepository;
    private iTreeOfRoleRepository treeOfRoleRepository;
    private IUserRepository userRepository;
    private INotifier notifier;
    private static final Logger logger = LoggerFactory.getLogger(PurchasedService.class);
    private TokenService tokenService;
    private iDiscountPolicyRepository discountRepo;

    public PurchasedService(
            IActiveOrderRepository repository,
            iTicketRepository ticketRepository,
            iPurchasedOrderRepository purchasedOrderRepository,
            ISupplyService supplyService,
            IPaymentService paymentService,
            IBarcodeGenerator barcodeGenerator,
            TokenService tokenService,
            iTreeOfRoleRepository treeOfRoleRepository,
            iDiscountPolicyRepository discountRepo,
            IUserRepository userRepository,
            INotifier notifier,
            IExternalTicketService externalTicketService
    ) {
        this.repository = repository;
        this.supplyService = supplyService;
        this.paymentService = paymentService;
        this.barcodeGenerator = barcodeGenerator;
        this.externalTicketService = externalTicketService;
        this.ticketRepository = ticketRepository;
        this.purchasedOrderRepository = purchasedOrderRepository;
        this.tokenService = tokenService;
        this.treeOfRoleRepository = treeOfRoleRepository;
        this.discountRepo = discountRepo;
        this.userRepository = userRepository;
        this.notifier = notifier;
    }

    public boolean isAuthorized(String company, String userID) {
        boolean o = treeOfRoleRepository.exitsOwner(userID, company);
        boolean m = treeOfRoleRepository.ManagerPermitToSeeTransactions(userID, company);
        return m || (o);
    }

    @Transactional
    public Response<String> PurchaseTicket(String email, String orderId, String token, String userCoupon, CreditCardDetails paymentDetails) {
        try {
            logger.info("User of token {} is attempting to purchase tickets of the order {}", token, orderId);
            ActiveOrder order = repository.findById(orderId);
            if (tokenService.validateToken(token)) {
                String userID = tokenService.extractUserId(token);
                String username = tokenService.extractUsername(token);
                if (username != null && userRepository.isUserSuspendedNow(username) ||
                        (userID != null && userRepository.isUserSuspendedNow(userID))) {
                    throw new PurchaseOrderException("User is suspended");
                }
                order = repository.getOrder(userID);
            }
            if (order == null || order.getExpirationTime().before(new Date())) {
                throw new PurchaseOrderException("Order expired or not found");
            }

            PurchaseContext context = new PurchaseContext(
                    order.getTicketIds().size(),
                    userCoupon,
                    new Date()
            );
            List<Ticket> purchasedTickets = order.getTicketIds().stream()
                    .map(id -> ticketRepository.getTicketById(id))
                    .filter(java.util.Objects::nonNull)
                    .map(original -> {
                        Ticket copy = new Ticket(original);
                        copy.purchase();
                        return copy;
                    })
                    .toList();

            final String finalEventId = order.getEventId();
            final String finalCompanyId = order.getCompanyId();
            final PurchaseContext finalContext = context;

            double totalPriceAfterDiscounts = purchasedTickets.stream()
                    .mapToDouble(t -> getPriceAfterDiscounts(
                            finalEventId,
                            finalCompanyId,
                            t.getPrice(),
                            finalContext))
                    .sum();

            int transactionID = paymentService.processPayment(paymentDetails, totalPriceAfterDiscounts, "USD");

            List<String> externalTicketCodes = new ArrayList<>();
            try {
                String customerId = order.getUserId() != null ? order.getUserId() : "guest";
                for (Ticket t : purchasedTickets) {
                    String ticketCode = externalTicketService.issueTicket(
                            customerId, t.getEvent(), t.getEvent(), t.getRow(), t.getCol());
                    externalTicketCodes.add(ticketCode);
                }

                for (Ticket t : purchasedTickets) {
                    ticketRepository.save(t);
                }

                for (int i = 0; i < purchasedTickets.size(); i++) {
                    supplyService.supplyToEmail(email, externalTicketCodes.get(i));
                }

                purchasedOrderRepository.StorePurchasedOrder(
                        order.getCompanyId(), order.getEventId(), order.getTicketIds(),
                        order.getUserId(), order.getOrderId(), externalTicketCodes);

                repository.delete(order.getOrderId());

            } catch (PurchaseOrderException e) {
                throw e;
            } catch (Exception e) {
                for (String code : externalTicketCodes) {
                    try {
                        externalTicketService.cancelTicket(code);
                    } catch (ExternalServiceException ex) {
                        logger.error("Failed to cancel external ticket {} during rollback: {}", code, ex.getMessage());
                    }
                }
                try {
                    paymentService.refund(transactionID);
                } catch (ExternalServiceException ex) {
                    logger.error("Failed to refund transaction {} during rollback: {}", transactionID, ex.getMessage());
                }
                throw new PurchaseOrderException("Failed during save or supply: " + e.getMessage(), e);
            }

            if (order.getUserId() != null) {
                notifier.notifyUser(order.getUserId(), "Purchase Successful",
                        "Your purchase for event '" + order.getEventId() + "' is confirmed. " + purchasedTickets.size() + " ticket(s) sent to " + email + ".");
            }

            boolean soldOut = ticketRepository.getAvailableTicketsByEventAndCompany(order.getCompanyId(), order.getEventId()).isEmpty();
            if (soldOut) {
                String soldOutMsg = "Your event '" + order.getEventId() + "' is now sold out!";
                treeOfRoleRepository.getAllOwnersByCompany(order.getCompanyId()).forEach(o -> {
                    User u = userRepository.getUserByUsername(o.getUserID());
                    if (u != null) notifier.notifyUser(u.getID(), "Event Sold Out", soldOutMsg);
                });
                treeOfRoleRepository.getAllManagersByCompany(order.getCompanyId()).forEach(m -> {
                    User u = userRepository.getUserByUsername(m.getUserID());
                    if (u != null) notifier.notifyUser(u.getID(), "Event Sold Out", soldOutMsg);
                });
            }

            logger.info("User of token {} purchased tickets of order {} successfully", token, orderId);
            return Response.success("success");

        } catch (PurchaseOrderException e) {
            logger.error("Transaction aborted: " + e.getMessage());
            throw e;
        } catch (ExternalServiceException e) {
            logger.error("External service error during purchase: {}", e.getMessage());
            throw new PurchaseOrderException("External service error: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error(e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    @Transactional
    public Response<String> cancelOrder(String orderId, String token) {
        try {
            logger.info("User of token {} is attempting to cancel the order: {}", token, orderId);
            if (!tokenService.validateToken(token)) {
                return Response.error("Invalid token");
            }
            String userId = tokenService.extractUserId(token);

            PurchaseOrder order = purchasedOrderRepository.getByOrderId(orderId);
            if (order == null) {
                return Response.error("Order not found");
            }
            if (!userId.equals(order.getBuyerID())) {
                return Response.error("Not authorized to cancel this order");
            }

            for (String ticketCode : order.getExternalTicketIds()) {
                try {
                    externalTicketService.cancelTicket(ticketCode);
                } catch (Exception e) {
                    logger.warn("Failed to cancel external ticket {} during order cancellation: {}", ticketCode, e.getMessage());
                }
            }

            for (String ticketId : order.getTicketsId()) {
                Ticket t = ticketRepository.getTicketById(ticketId);
                if (t != null) {
                    t.cancelPurchase();
                    ticketRepository.save(t);
                }
            }

            purchasedOrderRepository.deleteByOrderId(orderId);

            logger.info("Order {} cancelled by user {}", orderId, userId);
            return Response.success("Order cancelled successfully");

        } catch (Exception e) {
            logger.error(e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Response<List<PurchaseOrderDTO>> getCompanyTransaction(String company, String token) {
        try {
            logger.info("User of token {} is attempting to get company transactions for company {}", token, company);
            if (!tokenService.validateToken(token)) {
                throw new PurchaseOrderException("Invalid token");
            }
            String username = tokenService.extractUserId(token);
            if (username == null || !isAuthorized(company, username)) {
                throw new PurchaseOrderException("User not authorized");
            }
            List<PurchaseOrder> purchaseOrders = purchasedOrderRepository.getPurchasedOrdersForCompany(company);
            StringBuilder orders = new StringBuilder();
            List<PurchaseOrderDTO> orderDTOS = new ArrayList<>();

            for (PurchaseOrder purchasedOrder : purchaseOrders) {
                orders.append(purchasedOrder.toString() + "\n");
                List<String> ticketsId = purchasedOrder.getTicketsId();
                String tickets = ticketRepository.getTicketsDescription(ticketsId);
                List<Ticket> ticketList = ticketRepository.getTickets(ticketsId);
                List<TicketDTO> ticketDTOS = new ArrayList<>();
                for (Ticket ticket : ticketList) {
                    ticketDTOS.add(TicketDTO.fromEntity(ticket));
                }
                orderDTOS.add(PurchaseOrderDTO.create(purchasedOrder, ticketDTOS));
                orders.append(tickets + "\n");
            }
            logger.info("User {} got company transactions for company {} successfully", username, company);
            return Response.success(orderDTOS);
        } catch (Exception e) {
            logger.error(e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Response<List<PurchaseOrderDTO>> getUserTransaction(String token) {
        try {
            logger.info("User of token {} is attempting to get their transaction", token);
            if (!tokenService.validateToken(token)) {
                throw new PurchaseOrderException("Invalid token");
            }
            String userID = tokenService.extractUserId(token);

            List<PurchaseOrder> purchaseOrders = purchasedOrderRepository.getPurchasedOrdersForUser(userID);
            StringBuilder orders = new StringBuilder();
            List<PurchaseOrderDTO> orderDTOS = new ArrayList<>();

            for (PurchaseOrder purchasedOrder : purchaseOrders) {
                orders.append(purchasedOrder.toString() + "\n");
                List<String> ticketsId = purchasedOrder.getTicketsId();
                String tickets = ticketRepository.getTicketsDescription(ticketsId);
                List<Ticket> ticketList = ticketRepository.getTickets(ticketsId);
                List<TicketDTO> ticketDTOS = new ArrayList<>();
                for (Ticket ticket : ticketList) {
                    ticketDTOS.add(TicketDTO.fromEntity(ticket));
                }
                orderDTOS.add(PurchaseOrderDTO.create(purchasedOrder, ticketDTOS));
                orders.append(tickets + "\n");
            }
            logger.info("User {} got their transaction successfully", userID);
            return Response.success(orderDTOS);
        } catch (Exception e) {
            logger.error(e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    public double getPriceAfterDiscounts(String eventId, String companyName, double originalPrice, PurchaseContext context) {
        DiscountPolicy eventPolicy = discountRepo.findByEvent(eventId);
        DiscountPolicy companyPolicy = discountRepo.findByCompany(companyName);
        String policyId = UUID.randomUUID().toString();

        MaxDiscountComposite combinedRoot = new MaxDiscountComposite(policyId);

        if (eventPolicy != null) combinedRoot.add(eventPolicy.getRoot());
        if (companyPolicy != null) combinedRoot.add(companyPolicy.getRoot());

        double discountAmount = combinedRoot.calculateDiscount(originalPrice, context);
        return originalPrice - discountAmount;
    }

    @Transactional(readOnly = true)
    public Response<SalesReportDTO> getSubTreeSalesReport(String token, String companyName) {
        try {
            logger.info("User of token {} is attempting to get sub tree sales report for the company {}",token, companyName);
            if (!tokenService.validateToken(token)) {
                throw new PurchaseOrderException("Invalid token");
            }

            String currentUsername = tokenService.extractUserId(token);
            boolean isOwner = treeOfRoleRepository.exitsOwner(currentUsername, companyName);
            boolean isAuthorizedManager = treeOfRoleRepository.ManagerPermitToSeeTransactions(currentUsername, companyName);

            if (!isOwner && !isAuthorizedManager) {
                throw new PurchaseOrderException("Unauthorized: User does not have permission to generate sales reports");
            }

            List<Owner> allOwners = treeOfRoleRepository.getAllOwnersByCompany(companyName);
            List<Manager> allManagers = treeOfRoleRepository.getAllManagersByCompany(companyName);
            Set<String> subTreeUserIds = new HashSet<>();
            populateSubTreeIds(currentUsername, allOwners, allManagers, subTreeUserIds);
            List<PurchaseOrder> allCompanyOrders = purchasedOrderRepository.getPurchasedOrdersForCompany(companyName);
            double totalRevenue = 0.0;
            int totalTicketsSold = 0;
            List<PurchaseOrderDTO> matchingOrderDTOs = new ArrayList<>();

            for (PurchaseOrder order : allCompanyOrders) {
                if (subTreeUserIds.contains(order.getBuyerID()) || order.getBuyerID().equals(currentUsername)) {
                    List<String> ticketsId = order.getTicketsId();
                    List<Ticket> ticketList = ticketRepository.getTickets(ticketsId);
                    List<TicketDTO> ticketDTOs = new ArrayList<>();
                    double orderTotal = 0.0;

                    for (Ticket ticket : ticketList) {
                        ticketDTOs.add(TicketDTO.fromEntity(ticket));
                        orderTotal += ticket.getPrice();
                    }

                    totalRevenue += orderTotal;
                    totalTicketsSold += ticketsId.size();
                    matchingOrderDTOs.add(PurchaseOrderDTO.create(order, ticketDTOs));
                }
            }

            SalesReportDTO report = new SalesReportDTO();
            report.setTotalRevenue(totalRevenue);
            report.setTotalTicketsSold(totalTicketsSold);
            report.setOrders(matchingOrderDTOs);

            logger.info("Successfully generated sales report for sub-tree of {}. Total Revenue: {}, Tickets Sold: {}",
                    currentUsername, totalRevenue, totalTicketsSold);

            return Response.success(report);

        } catch (Exception e) {
            logger.error("Failed to generate sub-tree sales report: {}", e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    private void populateSubTreeIds(String currentUserId, List<Owner> allOwners, List<Manager> allManagers, Set<String> subTreeUserIds) {
        List<String> directDownlineOwners = allOwners.stream()
                .filter(o -> currentUserId.equals(o.getAppointerID()) && o.isAccepted())
                .map(Owner::getUserID)
                .toList();

        List<String> directDownlineManagers = allManagers.stream()
                .filter(m -> currentUserId.equals(m.getAppointerID()))
                .map(Manager::getUserID)
                .toList();

        for (String ownerId : directDownlineOwners) {
            if (subTreeUserIds.add(ownerId)) {
                populateSubTreeIds(ownerId, allOwners, allManagers, subTreeUserIds);
            }
        }

        for (String managerId : directDownlineManagers) {
            if (subTreeUserIds.add(managerId)) {
                populateSubTreeIds(managerId, allOwners, allManagers, subTreeUserIds);
            }
        }
    }
}
