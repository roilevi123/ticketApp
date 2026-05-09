package Appliction;


import Domain.Discount.DiscountPolicy;
import Domain.Discount.MaxDiscountComposite;
import Domain.Discount.PurchaseContext;
import Domain.Discount.iDiscountPolicyRepository;
import Domain.Order.ActiveOrder;
import Domain.Order.IActiveOrderRepository;
import Domain.OwnerManagerTree.Manager;
import Domain.OwnerManagerTree.Owner;
import Domain.OwnerManagerTree.iTreeOfRoleRepository;
import Domain.PurchasedOrderAggregate.PurchaseOrder;
import Domain.PurchasedOrderAggregate.PurchaseOrderDTO;
import Domain.PurchasedOrderAggregate.iPurchasedOrderRepository;

import Domain.Ticket.Ticket;
import Domain.Ticket.TicketDTO;
import Domain.Ticket.iTicketRepository;
import Infastructure.TokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
    private INotifer notifier;


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
            INotifer notifier
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
        this.notifier = notifier;
    }
    public boolean isAuthorized(String company,String userID) {
        boolean o=treeOfRoleRepository.exitsOwner(userID,company);
        boolean m=treeOfRoleRepository.ManagerPermitToSeeTransactions(userID,company);
        return m || (o );
    }

    public String PurchaseTicket(String email, String orderId,String token,String userCoupon) {
        try {
            ActiveOrder order = repository.findById(orderId);
            if(tokenService.validateToken(token)){
                String userID=tokenService.extractUserId(token);
                order=repository.getOrder(userID);
            }
            if (order == null  || order.getExpirationTime().before(new Date())) {


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
                    String barcode = barcodeGenerator.generateBarcode(t.getEvent(),t.getId());
                    supplyService.supplyToEmail(email, barcode);
                }

                purchasedOrderRepository.StorePurchasedOrder(order.getCompanyId(), order.getEventId(), order.getTicketIds(), order.getUserId(), order.getOrderId());
//                purchasedTickets.forEach(ticket -> {ticketRepository.save(ticket);});

                repository.delete(order.getOrderId());

            } catch (Exception e) {
                paymentService.refund(email, totalPriceAfterDiscounts);
                throw e;
            }
            boolean isSoldout=ticketRepository.isSoldOut(order.getEventId(),order.getCompanyId());
            String message = "successfully sold out for event: "+order.getEventId()+ "for company: "+order.getCompanyId();
            List<Owner> owners = treeOfRoleRepository.getAllOwnersByCompany(order.getCompanyId());
            for (Owner owner : owners) {
                boolean isSend=notifier.notifyUser(owner.getUserID(), message);
                if(!isSend){
                    notifier.saveMessage(owner.getUserID(), message);
                }
            }
            List<Manager> managers = treeOfRoleRepository.getAllManagersByCompany(order.getCompanyId());
            for (Manager manager : managers) {
                boolean isSend= notifier.notifyUser(manager.getUserID(), message);
                if(!isSend){
                    notifier.saveMessage(manager.getUserID(), message);
                }
            }
            boolean isSend=notifier.notifyUser(order.getCompanyId(), message);
            if(!isSend){
                notifier.saveMessage(order.getCompanyId(), message);
            }
            return "success";
        } catch (Exception e) {
            logger.error(e.getMessage());
            return e.getMessage();
        }
    }
    public List<PurchaseOrderDTO> getCompanyTransaction(String company,String token) {
        try {
            logger.info("getCompanyTransaction");
            if(!tokenService.validateToken(token)) {
                throw new Exception("Invalid token");
            }
            String username = tokenService.extractUsername(token);
            if (username == null || !isAuthorized(company, username)) {
                throw new Exception("User not authorized");
            }
            List<PurchaseOrder> purchaseOrders=purchasedOrderRepository.getPurchasedOrdersForCompany(company);
            StringBuilder orders = new StringBuilder();
            List<PurchaseOrderDTO> orderDTOS=new ArrayList<>();

            for(PurchaseOrder purchasedOrder : purchaseOrders) {
                orders.append(purchasedOrder.toString()+"\n");
                List<String> ticketsId = purchasedOrder.getTicketsId();
                String tickets=ticketRepository.getTicketsDescription(ticketsId);
                List<Ticket> ticketList=ticketRepository.getTickets(ticketsId);
                List<TicketDTO> ticketDTOS=new ArrayList<>();
                for(Ticket ticket : ticketList) {
                    ticketDTOS.add(TicketDTO.fromEntity(ticket));
                }
                orderDTOS.add(PurchaseOrderDTO.create(purchasedOrder,ticketDTOS));
                orders.append(tickets+"\n");

            }
            return orderDTOS;
        }
        catch (Exception e) {
            logger.error(e.getMessage());
            return null;
        }
    }
    public List<PurchaseOrderDTO> getUserTransaction(String token) {
        try {
            logger.info("getUserTransaction");
            if(!tokenService.validateToken(token)) {
                throw new Exception("Invalid token");
            }
            String userID=tokenService.extractUserId(token);

            List<PurchaseOrder> purchaseOrders=purchasedOrderRepository.getPurchasedOrdersForUser(userID);
            StringBuilder orders = new StringBuilder();
            List<PurchaseOrderDTO> orderDTOS=new ArrayList<>();

            for(PurchaseOrder purchasedOrder : purchaseOrders) {
                orders.append(purchasedOrder.toString()+"\n");
                List<String> ticketsId = purchasedOrder.getTicketsId();
                String tickets=ticketRepository.getTicketsDescription(ticketsId);
                List<Ticket> ticketList=ticketRepository.getTickets(ticketsId);
                List<TicketDTO> ticketDTOS=new ArrayList<>();
                for(Ticket ticket : ticketList) {
                    ticketDTOS.add(TicketDTO.fromEntity(ticket));
                }
                orderDTOS.add(PurchaseOrderDTO.create(purchasedOrder,ticketDTOS));
                orders.append(tickets+"\n");

            }
            return orderDTOS;
        }
        catch (Exception e) {
            logger.error(e.getMessage());
            return null;
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