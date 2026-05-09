package Domain.PurchasePolicy;

public interface PurchaseComponent {
    boolean isSatisfied(PurchaseValidationData data);
}