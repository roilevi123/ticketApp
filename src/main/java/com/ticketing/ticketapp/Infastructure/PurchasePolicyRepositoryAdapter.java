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
        List<PurchasePolicy> policies = jpaRepository.findByTargetIdAndTargetType(eventId, PurchaseTargetType.EVENT);
        return (policies != null && !policies.isEmpty()) ? policies.get(0) : null;
    }

    @Override
    public PurchasePolicy findByCompany(String companyName) {
        List<PurchasePolicy> policies = jpaRepository.findByTargetIdAndTargetType(companyName, PurchaseTargetType.COMPANY);
        return (policies != null && !policies.isEmpty()) ? policies.get(0) : null;
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
        List<PurchasePolicy> policiesEvents = jpaRepository.findByTargetIdAndTargetType(eventId, PurchaseTargetType.EVENT);
        List<PurchasePolicy> policiesCompanies = jpaRepository.findByTargetIdAndTargetType(companyName, PurchaseTargetType.COMPANY);
        policiesCompanies.addAll(policiesEvents);
        return policiesCompanies;
    }
}