package Appliction;

import Domain.AdminAggregate.iAdminRepository;
import Domain.Company.iCompanyRepository;
import Domain.OwnerManagerTree.iTreeOfRoleRepository;

import Domain.PurchasedOrderAggregate.PurchaseOrder;
import Domain.PurchasedOrderAggregate.iPurchasedOrderRepository;

import Domain.Ticket.iTicketRepository;
import Domain.User.IUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class AdminService {
    private iCompanyRepository companyRepository;
    private iTreeOfRoleRepository treeOfRoleRepository;
    private iAdminRepository adminRepository;
    private IUserRepository userRepository;
    private iPurchasedOrderRepository purchasedOrderRepository;
    private iTicketRepository ticketRepository;
    private static final Logger logger = LoggerFactory.getLogger(AdminService.class);
    public AdminService(
             iTreeOfRoleRepository treeOfRoleRepository
            , iCompanyRepository companyRepository
            , iAdminRepository adminRepository
            , IUserRepository userRepository
            , iPurchasedOrderRepository purchasedOrderRepository
            , iTicketRepository ticketRepository
    ) {
        this.treeOfRoleRepository = treeOfRoleRepository;
        this.companyRepository = companyRepository;
        this.adminRepository = adminRepository;
        this.userRepository = userRepository;
        this.purchasedOrderRepository = purchasedOrderRepository;
        this.ticketRepository = ticketRepository;
    }
    public String CloseCompany(String companyName,String adminName) {
        try {
            logger.info("Deleting company " + companyName);
            if(!adminRepository.isAdmin(adminName)) {
                throw new Exception("Admin does not exist");
            }
            companyRepository.deleteCompany(companyName);
//            eventRepository.deleteCompanyEvents(companyName);
            treeOfRoleRepository.deleteCompanyMangersAndOwners(companyName);
            logger.info("Deleted company " + companyName);
            return "success";
        }catch (Exception e) {
            logger.error(e.getMessage());
            return "error";
        }

    }




}
