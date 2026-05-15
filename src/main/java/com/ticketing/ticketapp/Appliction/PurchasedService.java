package com.ticketing.ticketapp.Appliction;


import com.ticketing.ticketapp.Domain.Discount.DiscountPolicy;
import com.ticketing.ticketapp.Domain.Discount.MaxDiscountComposite;
import com.ticketing.ticketapp.Domain.Discount.PurchaseContext;
import com.ticketing.ticketapp.Domain.Discount.iDiscountPolicyRepository;
import com.ticketing.ticketapp.Domain.Order.ActiveOrder;
import com.ticketing.ticketapp.Domain.Order.IActiveOrderRepository;
import com.ticketing.ticketapp.Domain.OwnerManagerTree.iTreeOfRoleRepository;
import com.ticketing.ticketapp.Domain.PurchasedOrderAggregate.PurchaseOrder;
import com.ticketing.ticketapp.Domain.PurchasedOrderAggregate.PurchaseOrderDTO;
import com.ticketing.ticketapp.Domain.PurchasedOrderAggregate.iPurchasedOrderRepository;

import com.ticketing.ticketapp.Domain.Ticket.Ticket;
import com.ticketing.ticketapp.Domain.Ticket.TicketDTO;
import com.ticketing.ticketapp.Domain.Ticket.iTicketRepository;
import com.ticketing.ticketapp.Infastructure.TokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class PurchasedService {
    private IActiveOrderRepository repository;
    private ISupplyService supplyService;
    private IPaymentService paymentService;
    private IBarcodeGenerator barcodeGenerator;
    private iTicketRepository ticketRepository;
    private iPurchasedOrderRepository purchasedOrderRepository;
    private iTreeOfRoleRepository treeOfRoleRepository;
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
            iDiscountPolicyRepository discountRepo
    ) {
        this.repository = repository;
        this.supplyService = supplyService;
        this.paymentService = paymentService;
        this.barcodeGenerator = barcodeGenerator;
        this.ticketRepository = ticketRepository;
        this.purchasedOrderRepository = purchasedOrderRepository;
        this.tokenService = tokenService;
        this.treeOfRoleRepository = treeOfRoleRepository;
        this.discountRepo = discountRepo;
    }

    public boolean isAuthorized(String company, String userID) {
        boolean o = treeOfRoleRepository.exitsOwner(userID, company);
        boolean m = treeOfRoleRepository.ManagerPermitToSeeTransactions(userID, company);
        return m || (o);
    }

    public Response<String> PurchaseTicket(String email, String orderId, String token, String userCoupon) {
        try {
            ActiveOrder order = repository.findById(orderId);
            if (tokenService.validateToken(token)) {
                String userID = tokenService.extractUserId(token);
                order = repository.getOrder(userID);
            }
            if (order == null || order.getExpirationTime().before(new Date())) {
                throw new Exception("Order expired or not found");
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

            if (!paymentService.processPayment(email, totalPriceAfterDiscounts)) {
                throw new Exception("Payment failed");
            }

            try {
                for (Ticket t : purchasedTickets) {
                    ticketRepository.save(t);
                }

                for (Ticket t : purchasedTickets) {
                    String barcode = barcodeGenerator.generateBarcode(t.getEvent(), t.getId());
                    supplyService.supplyToEmail(email, barcode);
                }

                purchasedOrderRepository.StorePurchasedOrder(order.getCompanyId(), order.getEventId(), order.getTicketIds(), order.getUserId(), order.getOrderId());

                repository.delete(order.getOrderId());

            } catch (Exception e) {
                paymentService.refund(email, totalPriceAfterDiscounts);
                throw e;
            }
            return Response.success("success");
        } catch (Exception e) {
            logger.error(e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    public Response<List<PurchaseOrderDTO>> getCompanyTransaction(String company, String token) {
        try {
            logger.info("getCompanyTransaction");
            if (!tokenService.validateToken(token)) {
                throw new Exception("Invalid token");
            }
            String username = tokenService.extractUsername(token);
            if (username == null || !isAuthorized(company, username)) {
                throw new Exception("User not authorized");
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
            return Response.success(orderDTOS);
        } catch (Exception e) {
            logger.error(e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    public Response<List<PurchaseOrderDTO>> getUserTransaction(String token) {
        try {
            logger.info("getUserTransaction");
            if (!tokenService.validateToken(token)) {
                throw new Exception("Invalid token");
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
            return Response.success(orderDTOS);
        } catch (Exception e) {
            logger.error(e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    public double getPriceAfterDiscounts(String eventId, String companyName, double originalPrice, PurchaseContext context) {
        DiscountPolicy eventPolicy = discountRepo.findByEvent(eventId);
        DiscountPolicy companyPolicy = discountRepo.findByCompany(companyName);

        MaxDiscountComposite combinedRoot = new MaxDiscountComposite();

        if (eventPolicy != null) combinedRoot.add(eventPolicy.getRoot());
        if (companyPolicy != null) combinedRoot.add(companyPolicy.getRoot());

        double discountAmount = combinedRoot.calculateDiscount(originalPrice, context);
        return originalPrice - discountAmount;
    }
}
