package com.ticketing.ticketapp.Infastructure.DataBaseInterface;

import com.ticketing.ticketapp.Domain.PurchasedOrderAggregate.PurchaseOrder;
import com.ticketing.ticketapp.Domain.PurchasedOrderAggregate.iPurchasedOrderRepository;
import com.ticketing.ticketapp.Infastructure.JpaPurchasedOrderRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
@Repository
@ConditionalOnProperty(name = "repository.type", havingValue = "DB")
public class PurchasedOrderRepositoryAdapter implements iPurchasedOrderRepository {

    private final JpaPurchasedOrderRepository jpaRepository;

    public PurchasedOrderRepositoryAdapter(JpaPurchasedOrderRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void StorePurchasedOrder(String company, String event, List<String> ticketsId, String buyerID, String orderId) {
        PurchaseOrder order = new PurchaseOrder(company, event, ticketsId, buyerID, orderId);
        jpaRepository.saveAndFlush(order);
    }

    @Override
    public void StorePurchasedOrder(String company, String event, List<String> ticketsId, String buyerID, String orderId, List<String> externalTicketIds) {
        PurchaseOrder order = new PurchaseOrder(company, event, ticketsId, buyerID, orderId);
        order.setExternalTicketIds(externalTicketIds);
        jpaRepository.saveAndFlush(order);
    }

    @Override
    public PurchaseOrder getByOrderId(String orderId) {
        return jpaRepository.findById(orderId).orElse(null);
    }

    @Override
    public void deleteByOrderId(String orderId) {
        jpaRepository.deleteById(orderId);
        jpaRepository.flush();
    }

    @Override
    public List<PurchaseOrder> GetAllPurchasedOrders() {
        return jpaRepository.findAll();
    }

    @Override
    public List<PurchaseOrder> getPurchasedOrdersForUser(String userID) {
        return jpaRepository.findByBuyerID(userID);
    }

    @Override
    public List<PurchaseOrder> getPurchasedOrdersForCompany(String company) {
        List<PurchaseOrder> a =jpaRepository.findByCompany(company);
        return a;
    }

    @Override
    public void deleteAll() {
        jpaRepository.deleteAll();
        jpaRepository.flush();
    }
}
