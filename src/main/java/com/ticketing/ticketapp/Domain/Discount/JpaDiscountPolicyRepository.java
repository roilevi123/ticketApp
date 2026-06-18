package com.ticketing.ticketapp.Domain.Discount;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JpaDiscountPolicyRepository extends JpaRepository<DiscountPolicy, String> {

    Optional<DiscountPolicy> findByTargetIdAndTargetType(
            String targetId,
            DiscountTargetType targetType
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT dp FROM DiscountPolicy dp WHERE dp.policyId = :policyId")
    Optional<DiscountPolicy> findByIdForUpdate(@Param("policyId") String policyId);

    @Query("SELECT dp FROM DiscountPolicy dp WHERE " +
            "(dp.targetId = :eventId AND dp.targetType = 'EVENT') OR " +
            "(dp.targetId = :companyName AND dp.targetType = 'COMPANY')")
    List<DiscountPolicy> findByEventAndCompany(
            @Param("eventId") String eventId,
            @Param("companyName") String companyName
    );
}