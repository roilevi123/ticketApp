package com.ticketing.ticketapp.Domain.PurchasedOrderAggregate;

import java.util.ArrayList;
import java.util.List;

public interface iPurchasedOrderRepository {
    void StorePurchasedOrder(String company, String event, List<String> ticketsId, String buyerID, String orderId);

    default void StorePurchasedOrder(String company, String event, List<String> ticketsId, String buyerID, String orderId, List<String> externalTicketIds) {
        StorePurchasedOrder(company, event, ticketsId, buyerID, orderId);
    }

    PurchaseOrder getByOrderId(String orderId);

    void deleteByOrderId(String orderId);

    List<PurchaseOrder> GetAllPurchasedOrders();
    List<PurchaseOrder> getPurchasedOrdersForUser(String userID);
    List<PurchaseOrder> getPurchasedOrdersForCompany(String company);
    void deleteAll();
}
