package Appliction;

import Domain.Domains.AdminDomain;
import Domain.PurchasedOrderAggregate.PurchaseOrder;

import java.util.List;

public class AdminService {
    AdminDomain adminDomain;
    public  AdminService(AdminDomain adminDomain) {
        this.adminDomain = adminDomain;
    }
    public String CloseCompany(String companyName,String adminName) {
        return adminDomain.CloseCompany(companyName,adminName);

    }
    public String removeUser(String username,String adminName) {
        return adminDomain.removeUser(username,adminName);
    }
    public String GetAllPurchasedOrders(String adminName) {
        return adminDomain.GetAllPurchasedOrders(adminName);
    }
}
