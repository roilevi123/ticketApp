package com.ticketing.ticketapp.Domain.PurchasedOrderAggregate;

import java.util.List;

public interface iPurchasedOrderRepository {
    public void StorePurchasedOrder(String company, String event, List<String> ticketsId, String buyerID, String orderId);
//    public String GetPurchasedOrder(String orderId);
    public List<PurchaseOrder> GetAllPurchasedOrders();
    public List<PurchaseOrder> getPurchasedOrdersForUser(String userID);
    public List<PurchaseOrder> getPurchasedOrdersForCompany(String company);
    public void deleteAll();

}
