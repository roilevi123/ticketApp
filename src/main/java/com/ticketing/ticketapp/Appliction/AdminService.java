package com.ticketing.ticketapp.Appliction;

import com.ticketing.ticketapp.Domain.AdminAggregate.iAdminRepository;
import com.ticketing.ticketapp.Domain.Company.iCompanyRepository;
import com.ticketing.ticketapp.Domain.Event.iEventRepository;
import com.ticketing.ticketapp.Domain.OwnerManagerTree.iTreeOfRoleRepository;

import com.ticketing.ticketapp.Domain.PurchasedOrderAggregate.PurchaseOrder;
import com.ticketing.ticketapp.Domain.PurchasedOrderAggregate.PurchaseOrderDTO;
import com.ticketing.ticketapp.Domain.PurchasedOrderAggregate.iPurchasedOrderRepository;

import com.ticketing.ticketapp.Domain.Ticket.Ticket;
import com.ticketing.ticketapp.Domain.Ticket.TicketDTO;
import com.ticketing.ticketapp.Domain.Ticket.iTicketRepository;
import com.ticketing.ticketapp.Domain.User.IUserRepository;
import com.ticketing.ticketapp.Infastructure.TokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
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
