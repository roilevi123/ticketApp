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
import Infastructure.TokenService;
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
    private TokenService tokenService;
    private static final Logger logger = LoggerFactory.getLogger(AdminService.class);

    public AdminService(
             iTreeOfRoleRepository treeOfRoleRepository
            , iCompanyRepository companyRepository
            , iAdminRepository adminRepository
            , IUserRepository userRepository
            , iPurchasedOrderRepository purchasedOrderRepository
            , iTicketRepository ticketRepository
            , iEventRepository eventRepository
            , TokenService tokenService
    ) {
        this.treeOfRoleRepository = treeOfRoleRepository;
        this.companyRepository = companyRepository;
        this.adminRepository = adminRepository;
        this.userRepository = userRepository;
        this.purchasedOrderRepository = purchasedOrderRepository;
        this.ticketRepository = ticketRepository;
        this.eventRepository = eventRepository;
        this.tokenService = tokenService;
    }
    public String CloseCompany(String companyName,String adminID) {
        try {
            logger.info("Deleting company " + companyName);
            if(!adminRepository.isAdmin(adminID)) {
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
    public String removeUser(String UserID,String adminID) {
        try {
            logger.info("Deleting user " + UserID);
            if(!adminRepository.isAdmin(adminID)) {
                throw new Exception("Admin does not exist");
            }
            userRepository.deleteUser(UserID);
            treeOfRoleRepository.deleteUserRoles(UserID);
            logger.info("Deleted user " + UserID);
            return "success";
        }catch (Exception e) {
            logger.error(e.getMessage());
            return e.getMessage();
        }
    }
    public List<PurchaseOrderDTO> GetAllPurchasedOrders(String adminID) {
        try {
            logger.info("Getting all purchased orders");
            if(!adminRepository.isAdmin(adminID)) {
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
    public String banUser(String targetUserId, String adminId) {
        try {
            logger.info("Banning user " + targetUserId);
            if (!adminRepository.isAdmin(adminId)) {
                throw new Exception("Admin does not exist");
            }
            if (userRepository.getUserByID(targetUserId) == null) {
                throw new Exception("User not found");
            }
            tokenService.banUser(targetUserId);
            logger.info("Banned user " + targetUserId);
            return "success";
        } catch (Exception e) {
            logger.error(e.getMessage());
            return e.getMessage();
        }
    }

    public String unbanUser(String targetUserId, String adminId) {
        try {
            logger.info("Unbanning user " + targetUserId);
            if (!adminRepository.isAdmin(adminId)) {
                throw new Exception("Admin does not exist");
            }
            tokenService.unbanUser(targetUserId);
            logger.info("Unbanned user " + targetUserId);
            return "success";
        } catch (Exception e) {
            logger.error(e.getMessage());
            return e.getMessage();
        }
    }
}
