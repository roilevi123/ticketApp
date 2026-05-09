package Infastructure;

import Domain.Discount.DiscountPolicy;
import Domain.Discount.DiscountTargetType;
import Domain.Discount.iDiscountPolicyRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InMemoryDiscountPolicyRepository implements iDiscountPolicyRepository {

    private final Map<String, DiscountPolicy> allPolicies = new ConcurrentHashMap<>();

    @Override
    public void save(DiscountPolicy policy) {
        allPolicies.put(policy.getPolicyId(), policy);
    }

    @Override
    public DiscountPolicy getPolicy(String policyId) {
        return allPolicies.get(policyId);
    }

    @Override
    public DiscountPolicy findByEvent(String eventId) {
        return allPolicies.values().stream()
                .filter(p -> p.getTargetType() == DiscountTargetType.EVENT && p.getTargetId().equals(eventId))
                .findFirst()
                .orElse(null);
    }

    @Override
    public DiscountPolicy findByCompany(String companyName) {
        return allPolicies.values().stream()
                .filter(p -> p.getTargetType() == DiscountTargetType.COMPANY && p.getTargetId().equals(companyName))
                .findFirst()
                .orElse(null);
    }

    @Override
    public void delete(String policyId) {
        allPolicies.remove(policyId);
    }

    @Override
    public void deleteAll() {
        allPolicies.clear();
    }
}