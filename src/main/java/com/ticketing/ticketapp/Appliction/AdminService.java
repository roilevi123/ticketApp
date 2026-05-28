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

import com.ticketing.ticketapp.Domain.Lottery.ILotteryRepository;
import com.ticketing.ticketapp.Domain.Order.IActiveOrderRepository;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private IActiveOrderRepository activeOrderRepository;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private ILotteryRepository lotteryRepository;
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
            , IActiveOrderRepository activeOrderRepository
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
        this.activeOrderRepository = activeOrderRepository;
    }
    public Response<String> CloseCompany(String companyName,String adminID) {
        try {
            logger.info("Deleting company " + companyName);
            if(!adminRepository.isAdmin(adminID)) {
                throw new Exception("Admin does not exist");
            }
            if (companyRepository.getCompany(companyName) == null) {
                return Response.error("Company '" + companyName + "' not found");
            }
            List<Owner> owners = treeOfRoleRepository.getAllOwnersByCompany(companyName);
            List<Manager> managers = treeOfRoleRepository.getAllManagersByCompany(companyName);
            purchasedOrderRepository.getPurchasedOrdersForCompany(companyName).forEach(order ->
                notifier.notifyUser(order.getBuyerID(), "Event Cancelled",
                    "The event '" + order.getEvent() + "' by " + companyName
                        + " has been cancelled by an administrator. Your tickets are no longer valid."));
            if (lotteryRepository != null) {
                lotteryRepository.deleteAllForCompany(companyName);
            }
            companyRepository.deleteCompany(companyName);
            eventRepository.deleteCompanyEvent(companyName);
            treeOfRoleRepository.deleteCompanyMangersAndOwners(companyName);
            logger.info("Deleted company " + companyName);
            String title = "Company Closed";
            String message = "Company '" + companyName + "' has been permanently closed by an administrator.";
            owners.forEach(o -> notifier.notifyUser(o.getUserID(), title, message));
            managers.forEach(m -> notifier.notifyUser(m.getUserID(), title, message));
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

    public Response<String> removeUser(String UserID,String adminID) {
        try {
            logger.info("Deleting user " + UserID);
            if(!adminRepository.isAdmin(adminID)) {
                throw new Exception("Admin does not exist");
            }
            notifier.notifyUser(UserID, "FORCE_LOGOUT", "Your account has been removed by an administrator.");
            tokenService.banUser(UserID);
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
        try {
            logger.info("Admin {} is suspending user {} for {} days", adminID, targetUserID, durationInDays);

            if(!adminRepository.isAdmin(adminID))
                throw new Exception("Admin does not exist");

            User user = userRepository.getUserByID(targetUserID);
            if(user==null)
                throw new Exception("User not found");

            LocalDateTime startTime = LocalDateTime.now();
            Suspension suspension;
            LocalDateTime endTime;

            if(durationInDays==0){
                suspension=new Suspension(targetUserID, startTime);
                userRepository.addCurrentSuspension(targetUserID,suspension);
                logger.info("User {} suspended permanently successfully", targetUserID);
                notifier.notifyUser(targetUserID, "Account Suspended", "Your account has been suspended by an adminstrator for good");

            }
            else {
                endTime = startTime.plusDays(durationInDays);
                suspension = new Suspension(targetUserID, startTime, endTime);
                userRepository.addCurrentSuspension(targetUserID,suspension);
                logger.info("User {} suspended successfully until {}", targetUserID, endTime);
                notifier.notifyUser(targetUserID, "Account Suspended", "Your account has been suspended by an adminstrator until "+endTime.toString());
            }

            return Response.success("success");

        }catch (Exception e){
            logger.info("Failed to suspend user: {}", e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    public Response<String> cancelSuspension(String targetUserId, String adminId){
        try{
            logger.info("Admin {} is canceling the suspension of the user {}", targetUserId, adminId);

            if(!adminRepository.isAdmin(adminId))
                throw new Exception("Admin does not exist");

            User user = userRepository.getUserByID(targetUserId);
            if(user==null)
                throw new Exception("User does not exist");

            userRepository.cancelSuspension(targetUserId);
            logger.info("User {} is not suspended anymore", targetUserId);
            notifier.notifyUser(targetUserId, "Account is no longer suspended", "The suspension of your account was canceled by an adminstrator");

            return Response.success("success");
        }catch(Exception e){
            logger.info("Failed to cancel suspension of user: {}", e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    public Response<List<Suspension>> getAllSuspensions(String adminId){
        try{
            logger.info("Admin {} is viewing suspension history", adminId);

            if(!adminRepository.isAdmin(adminId))
                throw new Exception("Admin does not exist");

            List<Suspension> suspensions = userRepository.getAllSuspensions();
            return Response.success(suspensions);
        }catch(Exception e){
            logger.info("Failed to get all suspensions: {}", e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    public Response<List<PurchaseOrderDTO>> getPurchaseHistory(String adminId, String buyerId, String company, String eventId) {
        try {
            if (!adminRepository.isAdmin(adminId))
                throw new Exception("Admin does not exist");

            List<PurchaseOrder> orders;
            if (company != null && !company.isBlank()) {
                orders = purchasedOrderRepository.getPurchasedOrdersForCompany(company);
            } else if (buyerId != null && !buyerId.isBlank()) {
                orders = purchasedOrderRepository.getPurchasedOrdersForUser(buyerId);
            } else {
                orders = purchasedOrderRepository.GetAllPurchasedOrders();
            }

            if (eventId != null && !eventId.isBlank()) {
                orders = orders.stream().filter(o -> eventId.equals(o.getEvent())).collect(Collectors.toList());
            }

            List<PurchaseOrderDTO> dtos = new ArrayList<>();
            for (PurchaseOrder order : orders) {
                List<Ticket> ticketList = ticketRepository.getTickets(order.getTicketsId());
                List<TicketDTO> ticketDTOs = ticketList.stream().map(TicketDTO::fromEntity).collect(Collectors.toList());
                dtos.add(PurchaseOrderDTO.create(order, ticketDTOs));
            }
            return Response.success(dtos);
        } catch (Exception e) {
            logger.error(e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    public Response<Map<String, Long>> getSystemAnalytics(String adminId) {
        try {
            if (!adminRepository.isAdmin(adminId))
                throw new Exception("Admin does not exist");

            long totalPurchases = purchasedOrderRepository.GetAllPurchasedOrders().size();
            long activeOrders = activeOrderRepository.getAllActiveOrders().size();

            Map<String, Long> analytics = new HashMap<>();
            analytics.put("totalPurchases", totalPurchases);
            analytics.put("activeOrders", activeOrders);
            return Response.success(analytics);
        } catch (Exception e) {
            logger.error(e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    public boolean isAdmin(String userId) {
        return adminRepository.isAdmin(userId);
    }

    public Response<String> broadcastMessage(String adminId, String title, String message) {
        try {
            if (!adminRepository.isAdmin(adminId)) {
                throw new Exception("Not authorized");
            }
            notifier.broadcast(title, message);
            return Response.success("Broadcast sent");
        } catch (Exception e) {
            logger.error(e.getMessage());
            return Response.error(e.getMessage());
        }
    }

}
