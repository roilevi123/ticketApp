package com.ticketing.ticketapp.Infastructure;

import com.ticketing.ticketapp.Domain.PurchasePolicy.PurchasePolicy;
import com.ticketing.ticketapp.Domain.PurchasePolicy.PurchaseTargetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaPurchasePolicyRepository extends JpaRepository<PurchasePolicy, String> {
    PurchasePolicy findByTargetIdAndTargetType(String targetId, PurchaseTargetType targetType);
}