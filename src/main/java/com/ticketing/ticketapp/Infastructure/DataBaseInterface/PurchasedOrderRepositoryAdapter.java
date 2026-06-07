package com.ticketing.ticketapp.Infastructure.DataBaseInterface;

import com.ticketing.ticketapp.Domain.PurchasedOrderAggregate.PurchaseOrder;
import com.ticketing.ticketapp.Domain.PurchasedOrderAggregate.iPurchasedOrderRepository;
import com.ticketing.ticketapp.Infastructure.JpaPurchasedOrderRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Primary
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
    public List<PurchaseOrder> GetAllPurchasedOrders() {
        return jpaRepository.findAll();
    }

    @Override
    public List<PurchaseOrder> getPurchasedOrdersForUser(String userID) {
        return jpaRepository.findByBuyerID(userID);
    }

    @Override
    public List<PurchaseOrder> getPurchasedOrdersForCompany(String company) {
        return jpaRepository.findByCompany(company);
    }

    @Override
    public void deleteAll() {
        jpaRepository.deleteAll();
        jpaRepository.flush();
    }
}
