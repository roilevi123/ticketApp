package com.ticketing.ticketapp.Infastructure;

import com.ticketing.ticketapp.Domain.PurchasedOrderAggregate.PurchaseOrder;
import com.ticketing.ticketapp.Domain.PurchasedOrderAggregate.iPurchasedOrderRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "repository.type", havingValue = "MEMORY")
public class PurchasedOrderRepositoryImpl implements iPurchasedOrderRepository {

    private final List<PurchaseOrder> purchasedOrders = new ArrayList<>();

    @Override
    public void StorePurchasedOrder(String company, String event, List<String> ticketsId, String buyerID, String orderId) {
        purchasedOrders.add(new PurchaseOrder(company, event, ticketsId, buyerID, orderId));
    }

    @Override
    public void StorePurchasedOrder(String company, String event, List<String> ticketsId, String buyerID, String orderId, List<String> externalTicketIds) {
        PurchaseOrder order = new PurchaseOrder(company, event, ticketsId, buyerID, orderId);
        order.setExternalTicketIds(externalTicketIds);
        purchasedOrders.add(order);
    }

    @Override
    public PurchaseOrder getByOrderId(String orderId) {
        return purchasedOrders.stream()
                .filter(o -> o.getOrderId().equals(orderId))
                .findFirst()
                .orElse(null);
    }

    @Override
    public void deleteByOrderId(String orderId) {
        purchasedOrders.removeIf(o -> o.getOrderId().equals(orderId));
    }

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
