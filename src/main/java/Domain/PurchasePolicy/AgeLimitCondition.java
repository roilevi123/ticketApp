package Domain.PurchasePolicy;

public class AgeLimitCondition implements PurchaseComponent {
    private final int minAge;

    public AgeLimitCondition(int minAge) {
        this.minAge = minAge;
    }

    @Override
    public boolean isSatisfied(PurchaseValidationData data) {
        return data.getUserAge() >= minAge;
    }
}