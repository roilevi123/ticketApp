package com.ticketing.ticketapp.Infastructure;

import com.ticketing.ticketapp.Domain.PurchasePolicy.PurchasePolicy;
import com.ticketing.ticketapp.Domain.PurchasePolicy.PurchaseTargetType;
import com.ticketing.ticketapp.Domain.PurchasePolicy.iPurchasePolicyRepository;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class PurchasePolicyRepositoryAdapter implements iPurchasePolicyRepository {

    private final JpaPurchasePolicyRepository jpaRepository;

    public PurchasePolicyRepositoryAdapter(JpaPurchasePolicyRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void save(PurchasePolicy policy) {
        jpaRepository.saveAndFlush(policy);
    }

    @Override
    public PurchasePolicy getPolicy(String policyId) {
        return jpaRepository.findById(policyId).orElse(null);
    }

    @Override
    public PurchasePolicy findByEvent(String eventId) {
        return jpaRepository.findByTargetIdAndTargetType(eventId, PurchaseTargetType.EVENT);
    }

    @Override
    public PurchasePolicy findByCompany(String companyName) {
        return jpaRepository.findByTargetIdAndTargetType(companyName, PurchaseTargetType.COMPANY);
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
    public List<PurchasePolicy> findByEventAndCompany(String eventId, String companyName) {
        List<PurchasePolicy> policies = new ArrayList<>();
        PurchasePolicy eventPolicy = findByEvent(eventId);
        if (eventPolicy != null) {
            policies.add(eventPolicy);
        }
        PurchasePolicy companyPolicy = findByCompany(companyName);
        if (companyPolicy != null) {
            policies.add(companyPolicy);
        }
        return policies;
    }
}