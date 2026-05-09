package Domain.PurchasePolicy;

public class QuantityLimitCondition implements PurchaseComponent {
    private final int minQuantity;
    private final int maxQuantity;

    public QuantityLimitCondition(int min, int max) {
        this.minQuantity = min;
        this.maxQuantity = max;
    }

    @Override
    public boolean isSatisfied(PurchaseValidationData data) {
        return data.getQuantity() >= minQuantity && data.getQuantity() <= maxQuantity;
    }
}