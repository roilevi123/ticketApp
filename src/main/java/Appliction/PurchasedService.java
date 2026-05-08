package Appliction;


import Domain.Order.ActiveOrder;
import Domain.Order.IActiveOrderRepository;
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
            return e.getMessage();
        }
    }
    public List<PurchaseOrderDTO> getCompanyTransaction(String company,String token) {
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
            logger.info("getCompanyTransaction");
            if(!tokenService.validateToken(token)) {
                throw new Exception("Invalid token");
            }
            String user=tokenService.extractUsername(token);

            List<PurchaseOrder> purchaseOrders=purchasedOrderRepository.getPurchasedOrdersForUser(user);
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
}