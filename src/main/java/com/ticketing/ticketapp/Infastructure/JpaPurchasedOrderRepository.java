package com.ticketing.ticketapp.Infastructure;

import com.ticketing.ticketapp.Domain.PurchasedOrderAggregate.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JpaPurchasedOrderRepository extends JpaRepository<PurchaseOrder, String> {
    List<PurchaseOrder> findByBuyerID(String buyerID);
    List<PurchaseOrder> findByCompany(String company);
}
