package com.ticketing.ticketapp.Domain.Discount;

import java.util.List;

public interface iDiscountPolicyRepository {
    void save(DiscountPolicy policy);

    DiscountPolicy findByEvent(String eventId);

    DiscountPolicy findByCompany(String companyName);
    public DiscountPolicy getPolicy(String policyId);
    void delete(String policyId);
    public void deleteAll();
    List<DiscountPolicy> findByEventAndCompany(String eventId, String companyName);
}
