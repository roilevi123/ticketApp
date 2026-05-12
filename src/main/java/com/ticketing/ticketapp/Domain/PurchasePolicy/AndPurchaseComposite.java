package com.ticketing.ticketapp.Domain.PurchasePolicy;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AndPurchaseComposite implements PurchaseComponent {
    private final List<PurchaseComponent> components = new ArrayList<>();

    public void add(PurchaseComponent component) {
        components.add(component);
    }

    @Override
    public boolean isSatisfied(PurchaseValidationData data) {
        return components.stream().allMatch(c -> c.isSatisfied(data));
    }
    @Override
    public String getDescription() {
        String childrenDesc = components.stream()
                .map(PurchaseComponent::getDescription)
                .collect(Collectors.joining(" AND "));
        return "(" + childrenDesc + ")";
    }

    public List<PurchaseComponent> getComponents() {
        return components;
    }
}
