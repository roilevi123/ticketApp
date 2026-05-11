package com.ticketing.ticketapp.Infastructure;

import com.ticketing.ticketapp.Domain.PurchasedOrderAggregate.PurchaseOrder;
import com.ticketing.ticketapp.Domain.PurchasedOrderAggregate.iPurchasedOrderRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class PurchasedOrderRepositoryImpl implements iPurchasedOrderRepository {
    private List<PurchaseOrder> purchasedOrders=new ArrayList<PurchaseOrder>();


    @Override
    public void StorePurchasedOrder(String company, String event, List<String> ticketsId, String buyerID, String orderId) {
        PurchaseOrder order=new PurchaseOrder(company, event, ticketsId, buyerID, orderId);
        purchasedOrders.add(order);

    }

//    @Override
//    public String GetPurchasedOrder(String orderId) {
//        PurchaseOrder order1=null;
//        for (PurchaseOrder order : purchasedOrders) {
//            if (order.getOrderId().equals(orderId)) {
//                order1=order;
//            }
//        }
//        return order1==null?"null":order1.getOrderId();
//    }

    @Override
    public List<PurchaseOrder> GetAllPurchasedOrders() {
        return purchasedOrders;
    }

    @Override
    public List<PurchaseOrder> getPurchasedOrdersForUser(String userID) {
        return purchasedOrders.stream()
                .filter(order -> order.getBuyerID() != null && order.getBuyerID().equals(userID))
                .collect(Collectors.toList());
    }

    @Override
    public List<PurchaseOrder> getPurchasedOrdersForCompany(String company) {
        return purchasedOrders.stream()
                .filter(order -> order.getCompany() != null && order.getCompany().equals(company))
                .collect(Collectors.toList());
    }

    @Override
    public void deleteAll() {
        purchasedOrders.clear();

    }

}
