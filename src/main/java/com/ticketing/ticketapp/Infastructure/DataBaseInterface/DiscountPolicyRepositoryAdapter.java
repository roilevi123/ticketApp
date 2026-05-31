package com.ticketing.ticketapp.Infastructure.DataBaseInterface;

import com.ticketing.ticketapp.Domain.Discount.DiscountPolicy;
import com.ticketing.ticketapp.Domain.Discount.DiscountTargetType;
import com.ticketing.ticketapp.Domain.Discount.JpaDiscountPolicyRepository;
import com.ticketing.ticketapp.Domain.Discount.iDiscountPolicyRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Primary
public class DiscountPolicyRepositoryAdapter implements iDiscountPolicyRepository {

    private final JpaDiscountPolicyRepository jpaRepository;

    public DiscountPolicyRepositoryAdapter(JpaDiscountPolicyRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void save(DiscountPolicy policy) {
        jpaRepository.saveAndFlush(policy);
    }

    @Override
    public DiscountPolicy findByEvent(String eventId) {
        return jpaRepository.findByTargetIdAndTargetType(eventId, DiscountTargetType.EVENT)
                .orElse(null);
    }

    @Override
    public DiscountPolicy findByCompany(String companyName) {
        return jpaRepository.findByTargetIdAndTargetType(companyName, DiscountTargetType.COMPANY)
                .orElse(null);
    }

    @Override
    public DiscountPolicy getPolicy(String policyId) {
        return jpaRepository.findById(policyId)
                .orElse(null);
    }

    @Override
    public void delete(String policyId) {
        jpaRepository.deleteById(policyId);
        jpaRepository.flush();
    }

    @Override
    public void deleteAll() {
        jpaRepository.deleteAll();
        jpaRepository.flush();
    }

    @Override
    public List<DiscountPolicy> findByEventAndCompany(String eventId, String companyName) {
        return jpaRepository.findByEventAndCompany(eventId, companyName);
    }
}