package Appliction;

import Domain.Domains.PurchasedDomain;
import Domain.Order.ActiveOrder;
import Domain.PurchasedOrderAggregate.PurchaseOrder;
import Domain.Ticket.Ticket;

import java.util.Date;
import java.util.List;

public class PurchasedSevice {
    private PurchasedDomain domain;
    public PurchasedSevice(PurchasedDomain domain) {
        this.domain = domain;
    }
    public String  PurchaseTicket(String email, String orderId,String token) {
        return domain.PurchaseTicket(email, orderId, token);
    }
    public String getCompanyTransaction(String company,String token) {
        return domain.getCompanyTransaction(company,token);
    }
    public String getUserTransaction(String token) {
        return domain.getUserTransaction(token);
    }
}
