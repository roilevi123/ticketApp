package Infastructure;

import Domain.PurchasePolicy.PurchasePolicy;
import Domain.PurchasePolicy.PurchaseTargetType;
import Domain.PurchasePolicy.iPurchasePolicyRepository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryPurchasePolicyRepository implements iPurchasePolicyRepository {

    private final Map<String, PurchasePolicy> allPolicies = new ConcurrentHashMap<>();

    private final Map<String, PurchasePolicy> eventPolicies = new ConcurrentHashMap<>();
    private final Map<String, PurchasePolicy> companyPolicies = new ConcurrentHashMap<>();

    @Override
    public void save(PurchasePolicy policy) {
        allPolicies.put(policy.getPolicyId(), policy);

        if (policy.getTargetType() == PurchaseTargetType.EVENT) {
            eventPolicies.put(policy.getTargetId(), policy);
        } else {
            companyPolicies.put(policy.getTargetId(), policy);
        }
    }

    @Override
    public PurchasePolicy getPolicy(String policyId) {
        return allPolicies.get(policyId);
    }

    @Override
    public PurchasePolicy findByEvent(String eventId) {
        return eventPolicies.get(eventId);
    }

    @Override
    public PurchasePolicy findByCompany(String companyName) {
        return companyPolicies.get(companyName);
    }

    @Override
    public void delete(String policyId) {
        PurchasePolicy policy = allPolicies.remove(policyId);
        if (policy != null) {
            if (policy.getTargetType() == PurchaseTargetType.EVENT) {
                eventPolicies.remove(policy.getTargetId());
            } else {
                companyPolicies.remove(policy.getTargetId());
            }
        }
    }

    @Override
    public void deleteAll() {
        allPolicies.clear();
        eventPolicies.clear();
        companyPolicies.clear();
    }
}