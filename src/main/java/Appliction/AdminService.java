package Appliction;

import Domain.AdminAggregate.iAdminRepository;
import Domain.Company.iCompanyRepository;
import Domain.Event.iEventRepository;
import Domain.OwnerManagerTree.iTreeOfRoleRepository;

import Domain.PurchasedOrderAggregate.PurchaseOrder;
import Domain.PurchasedOrderAggregate.PurchaseOrderDTO;
import Domain.PurchasedOrderAggregate.iPurchasedOrderRepository;

import Domain.Ticket.Ticket;
import Domain.Ticket.TicketDTO;
import Domain.Ticket.iTicketRepository;
import Domain.User.IUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class AdminService {
    private iCompanyRepository companyRepository;
    private iTreeOfRoleRepository treeOfRoleRepository;
    private iAdminRepository adminRepository;
    private IUserRepository userRepository;
    private iPurchasedOrderRepository purchasedOrderRepository;
    private iTicketRepository ticketRepository;
    private iEventRepository eventRepository;
    private static final Logger logger = LoggerFactory.getLogger(AdminService.class);
    public AdminService(
             iTreeOfRoleRepository treeOfRoleRepository
            , iCompanyRepository companyRepository
            , iAdminRepository adminRepository
            , IUserRepository userRepository
            , iPurchasedOrderRepository purchasedOrderRepository
            , iTicketRepository ticketRepository,
             iEventRepository eventRepository
    ) {
        this.treeOfRoleRepository = treeOfRoleRepository;
        this.companyRepository = companyRepository;
        this.adminRepository = adminRepository;
        this.userRepository = userRepository;
        this.purchasedOrderRepository = purchasedOrderRepository;
        this.ticketRepository = ticketRepository;
        this.eventRepository = eventRepository;
    }
    public String CloseCompany(String companyName,String adminName) {
        try {
            logger.info("Deleting company " + companyName);
            if(!adminRepository.isAdmin(adminName)) {
                throw new Exception("Admin does not exist");
            }
            companyRepository.deleteCompany(companyName);
            eventRepository.deleteCompanyEvent(companyName);
            treeOfRoleRepository.deleteCompanyMangersAndOwners(companyName);
            logger.info("Deleted company " + companyName);
            return "success";
        }catch (Exception e) {
            logger.error(e.getMessage());
            return e.getMessage();
        }

    }
    public String removeUser(String username,String adminName) {
        try {
            logger.info("Deleting user " + username);
            if(!adminRepository.isAdmin(adminName)) {
                throw new Exception("Admin does not exist");
            }
            userRepository.deleteUser(username);
            treeOfRoleRepository.deleteUserRoles(username);
            logger.info("Deleted user " + username);
            return "success";
        }catch (Exception e) {
            logger.error(e.getMessage());
            return e.getMessage();
        }
    }
    public List<PurchaseOrderDTO> GetAllPurchasedOrders(String adminName) {
        try {
            logger.info("Getting all purchased orders");
            if(!adminRepository.isAdmin(adminName)) {
                throw new Exception("Admin does not exist");
            }

            List<PurchaseOrder> purchasedOrders = purchasedOrderRepository.GetAllPurchasedOrders();
            StringBuilder orders = new StringBuilder();
            List<PurchaseOrderDTO> orderDTOS=new ArrayList<>();

            for(PurchaseOrder purchasedOrder : purchasedOrders) {
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
        }catch (Exception e) {
            logger.error(e.getMessage());
            return null;
        }

    }




}
