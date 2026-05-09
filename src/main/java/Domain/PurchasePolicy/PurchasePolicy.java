package Domain.PurchasePolicy;

public class PurchasePolicy {
    private final String policyId;
    private final String targetId;
    private final PurchaseTargetType targetType;
    private final PurchaseComponent rootCondition;

    public PurchasePolicy(String policyId, String targetId, PurchaseTargetType targetType, PurchaseComponent rootCondition) {
        this.policyId = policyId;
        this.targetId = targetId;
        this.targetType = targetType;
        this.rootCondition = rootCondition;
    }

    public boolean validate(PurchaseValidationData data) {
        return rootCondition.isSatisfied(data);
    }

    public String getPolicyId() { return policyId; }
    public String getTargetId() { return targetId; }
    public PurchaseComponent getRoot() { return rootCondition; }
    public  PurchaseTargetType getTargetType() { return targetType; }
}