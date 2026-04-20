package Appliction;


import Domain.Order.ActiveOrder;
import Domain.Order.IActiveOrderRepository;
import Domain.OwnerManagerTree.iTreeOfRoleRepository;
import Domain.PurchasedOrderAggregate.PurchaseOrder;
import Domain.PurchasedOrderAggregate.iPurchasedOrderRepository;

import Domain.Ticket.Ticket;
import Domain.Ticket.iTicketRepository;
import Infastructure.TokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public PurchasedService(
            IActiveOrderRepository repository,
            iTicketRepository ticketRepository,
            iPurchasedOrderRepository purchasedOrderRepository,
            ISupplyService supplyService,
            IPaymentService paymentService,
            IBarcodeGenerator barcodeGenerator,
            TokenService tokenService,
            iTreeOfRoleRepository treeOfRoleRepository
            ) {

        this.repository = repository;
        this.supplyService = supplyService;
        this.paymentService = paymentService;
        this.barcodeGenerator = barcodeGenerator;
        this.ticketRepository = ticketRepository;
        this.purchasedOrderRepository = purchasedOrderRepository;
        this.tokenService = tokenService;
        this.treeOfRoleRepository = treeOfRoleRepository;
    }
    public boolean isAuthorized(String company,String username) {
        boolean o=treeOfRoleRepository.exitsOwner(username,company);
        boolean m=treeOfRoleRepository.ManagerPermitToSeeTransactions(username,company);
        return m || (o );
    }

    public String  PurchaseTicket(String email, String orderId,String token) {
        try {
            ActiveOrder order = repository.findById(orderId);
            if(tokenService.validateToken(token)){
                String username=tokenService.extractUsername(token);
                order=repository.getOrder(username);
            }
            if (order == null  || order.getExpirationTime().before(new Date())) {


                        throw new Exception("Order expired or not found");

            }

            List<Ticket> purchasedTickets = order.getTicketIds().stream()
                    .map(id -> ticketRepository.getTicketById(id))
                    .filter(java.util.Objects::nonNull)
                    .map(original -> {
                        Ticket copy = new Ticket(original);
                        copy.purchase();
                        return copy;
                    })
                    .toList();

            double totalPrice = purchasedTickets.stream()
                    .mapToDouble(Ticket::getPrice)
                    .sum();

            if (!paymentService.processPayment(email, totalPrice)) {
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
                paymentService.refund(email, totalPrice);
                throw e;
            }
            return "success";
        } catch (Exception e) {
            logger.error(e.getMessage());
            return "error";
        }
    }
    public String getCompanyTransaction(String company,String token) {
        try {
            logger.info("getCompanyTransaction");
            if(!tokenService.validateToken(token)) {
                throw new Exception("Invalid token");
            }
            String user=tokenService.extractUsername(token);

            if(!isAuthorized(company,user)) {
                throw new Exception("User not authorized");
            }
            List<PurchaseOrder> purchaseOrders=purchasedOrderRepository.getPurchasedOrdersForCompany(company);
            StringBuilder stringBuilder=new StringBuilder();
            for (PurchaseOrder purchaseOrder : purchaseOrders) {
                stringBuilder.append(purchaseOrder.toString()+"\n");
                List<String> ticketsId=purchaseOrder.getTicketsId();
                String tickets=ticketRepository.getTicketsDescription(ticketsId);
                stringBuilder.append(tickets+"\n");
            }
            return stringBuilder.toString();
        }
        catch (Exception e) {
            logger.error(e.getMessage());
            return e.getMessage();
        }
    }
    public String getUserTransaction(String token) {
        try {
            logger.info("getCompanyTransaction");
            if(!tokenService.validateToken(token)) {
                throw new Exception("Invalid token");
            }
            String user=tokenService.extractUsername(token);

            List<PurchaseOrder> purchaseOrders=purchasedOrderRepository.getPurchasedOrdersForUser(user);
            StringBuilder stringBuilder=new StringBuilder();
            for (PurchaseOrder purchaseOrder : purchaseOrders) {
                stringBuilder.append(purchaseOrder.toString()+"\n");
                List<String> ticketsId=purchaseOrder.getTicketsId();
                String tickets=ticketRepository.getTicketsDescription(ticketsId);
                stringBuilder.append(tickets+"\n");
            }
            return stringBuilder.toString();
        }
        catch (Exception e) {
            logger.error(e.getMessage());
            return e.getMessage();
        }
    }
}