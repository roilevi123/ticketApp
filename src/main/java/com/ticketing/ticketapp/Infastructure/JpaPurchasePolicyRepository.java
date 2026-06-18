package com.ticketing.ticketapp.Infastructure;

import com.ticketing.ticketapp.Domain.PurchasePolicy.PurchasePolicy;
import com.ticketing.ticketapp.Domain.PurchasePolicy.PurchaseTargetType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JpaPurchasePolicyRepository extends JpaRepository<PurchasePolicy, String> {

    List<PurchasePolicy> findByTargetIdAndTargetType(String targetId, PurchaseTargetType targetType);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PurchasePolicy p WHERE p.policyId = :policyId")
    Optional<PurchasePolicy> findByIdForUpdate(@Param("policyId") String policyId);
}