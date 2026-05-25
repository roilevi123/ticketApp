package com.ticketing.ticketapp.Appliction;

import com.ticketing.ticketapp.Domain.AdminAggregate.iAdminRepository;
import com.ticketing.ticketapp.Domain.Company.iCompanyRepository;
import com.ticketing.ticketapp.Domain.Event.iEventRepository;
import com.ticketing.ticketapp.Domain.OwnerManagerTree.Manager;
import com.ticketing.ticketapp.Domain.OwnerManagerTree.Owner;
import com.ticketing.ticketapp.Domain.OwnerManagerTree.iTreeOfRoleRepository;

import com.ticketing.ticketapp.Domain.PurchasedOrderAggregate.PurchaseOrder;
import com.ticketing.ticketapp.Domain.PurchasedOrderAggregate.PurchaseOrderDTO;
import com.ticketing.ticketapp.Domain.PurchasedOrderAggregate.iPurchasedOrderRepository;

import com.ticketing.ticketapp.Domain.Ticket.Ticket;
import com.ticketing.ticketapp.Domain.Ticket.TicketDTO;
import com.ticketing.ticketapp.Domain.Ticket.iTicketRepository;
import com.ticketing.ticketapp.Domain.User.IUserRepository;
import com.ticketing.ticketapp.Domain.User.User;
import com.ticketing.ticketapp.Infastructure.TokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import java.time.LocalDateTime;
import com.ticketing.ticketapp.Domain.User.Suspension;

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
    private INotifier notifier;
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
            , INotifier notifier
    ) {
        this.treeOfRoleRepository = treeOfRoleRepository;
        this.companyRepository = companyRepository;
        this.adminRepository = adminRepository;
        this.userRepository = userRepository;
        this.purchasedOrderRepository = purchasedOrderRepository;
        this.ticketRepository = ticketRepository;
        this.eventRepository = eventRepository;
        this.tokenService = tokenService;
        this.notifier = notifier;
    }
    public Response<String> CloseCompany(String companyName,String adminID) {
        try {
            logger.info("Deleting company " + companyName);
            if(!adminRepository.isAdmin(adminID)) {
                throw new Exception("Admin does not exist");
            }
            List<Owner> owners = treeOfRoleRepository.getAllOwnersByCompany(companyName);
            List<Manager> managers = treeOfRoleRepository.getAllManagersByCompany(companyName);
            companyRepository.deleteCompany(companyName);
            eventRepository.deleteCompanyEvent(companyName);
            treeOfRoleRepository.deleteCompanyMangersAndOwners(companyName);
            logger.info("Deleted company " + companyName);
            String title = "Company Closed";
            String message = "Company '" + companyName + "' has been permanently closed by an administrator.";
            owners.forEach(o -> notifyMember(o.getUserID(), title, message));
            managers.forEach(m -> notifyMember(m.getUserID(), title, message));
            return Response.success("success");
        }catch (Exception e) {
            logger.error(e.getMessage());
            return Response.error(e.getMessage());
        }

    }

    public Response<String> sendMessageToUser(String adminId, String targetUserId, String message) {
        try {
            if (!adminRepository.isAdmin(adminId)) {
                throw new Exception("Admin does not exist");
            }
            notifier.notifyUser(targetUserId, "Message from Admin", message);
            return Response.success("success");
        } catch (Exception e) {
            logger.error(e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    private void notifyMember(String username, String title, String message) {
        try {
            User u = userRepository.getUserByUsername(username);
            if (u != null) notifier.notifyUser(u.getID(), title, message);
        } catch (Exception e) {
            logger.warn("Failed to notify user {}: {}", username, e.getMessage());
        }
    }
    public Response<String> removeUser(String UserID,String adminID) {
        try {
            logger.info("Deleting user " + UserID);
            if(!adminRepository.isAdmin(adminID)) {
                throw new Exception("Admin does not exist");
            }
            userRepository.deleteUser(UserID);
            treeOfRoleRepository.deleteUserRoles(UserID);
            logger.info("Deleted user " + UserID);
            return Response.success("success");
        }catch (Exception e) {
            logger.error(e.getMessage());
            return Response.error(e.getMessage());
        }
    }
    public Response<List<PurchaseOrderDTO>> GetAllPurchasedOrders(String adminID) {
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

            return Response.success(orderDTOS);
        }catch (Exception e) {
            logger.error(e.getMessage());
            return Response.error(e.getMessage());
        }

    }
    public Response<String> banUser(String targetUserId, String adminId) {
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
            return Response.success("success");
        } catch (Exception e) {
            logger.error(e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    public Response<String> unbanUser(String targetUserId, String adminId) {
        try {
            logger.info("Unbanning user " + targetUserId);
            if (!adminRepository.isAdmin(adminId)) {
                throw new Exception("Admin does not exist");
            }
            tokenService.unbanUser(targetUserId);
            logger.info("Unbanned user " + targetUserId);
            return Response.success("success");
        } catch (Exception e) {
            logger.error(e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    public Response<String> suspendUser(String targetUserID, String adminID, int durationInDays){
        return Response.error("not implemented yet");
    }
}
