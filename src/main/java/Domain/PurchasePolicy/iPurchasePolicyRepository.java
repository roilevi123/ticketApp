package Domain.PurchasePolicy;


import java.util.List;

public interface iPurchasePolicyRepository {
    void save(PurchasePolicy policy);
    PurchasePolicy getPolicy(String policyId);
    PurchasePolicy findByEvent(String eventId);
    PurchasePolicy findByCompany(String companyName);
    void delete(String policyId);
    void deleteAll();
}