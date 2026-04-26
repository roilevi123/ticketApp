package Domain.Domains;

import Domain.AdminAggregate.iAdminRepository;
import Domain.Company.iCompanyRepository;
import Domain.Event.iEventRepository;
import Domain.OwnerManagerTree.iTreeOfRoleRepository;

import Domain.PurchasedOrderAggregate.PurchaseOrder;
import Domain.PurchasedOrderAggregate.iPurchasedOrderRepository;

import Domain.Ticket.iTicketRepository;
import Domain.User.IUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class AdminDomain {
    private iCompanyRepository companyRepository;
    private iTreeOfRoleRepository treeOfRoleRepository;
    private iAdminRepository adminRepository;
    private IUserRepository userRepository;
    private iPurchasedOrderRepository purchasedOrderRepository;
    private iTicketRepository ticketRepository;
    private iEventRepository eventRepository;
    private static final Logger logger = LoggerFactory.getLogger(AdminDomain.class);
    public AdminDomain(
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
            return "error";
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
            return "error";
        }
    }
    public String GetAllPurchasedOrders(String adminName) {
        try {
            logger.info("Getting all purchased orders");
            if(!adminRepository.isAdmin(adminName)) {
                throw new Exception("Admin does not exist");
            }
            List<PurchaseOrder> purchasedOrders = purchasedOrderRepository.GetAllPurchasedOrders();
            StringBuilder orders = new StringBuilder();
            for(PurchaseOrder purchasedOrder : purchasedOrders) {
                orders.append(purchasedOrder.toString()+"\n");
                List<String> ticketsId = purchasedOrder.getTicketsId();
                String tickets=ticketRepository.getTicketsDescription(ticketsId);
                orders.append(tickets+"\n");
            }

            return orders.toString();
        }catch (Exception e) {
            logger.error(e.getMessage());
            return null;
        }
    }




}
