package Domain.PurchasePolicy;

import java.util.ArrayList;
import java.util.List;

public class OrPurchaseComposite implements PurchaseComponent {
    private final List<PurchaseComponent> components = new ArrayList<>();

    public void add(PurchaseComponent component) {
        components.add(component);
    }

    @Override
    public boolean isSatisfied(PurchaseValidationData data) {
        return components.stream().anyMatch(c -> c.isSatisfied(data));
    }
}