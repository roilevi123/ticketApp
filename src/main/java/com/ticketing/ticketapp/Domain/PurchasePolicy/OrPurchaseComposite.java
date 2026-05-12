package com.ticketing.ticketapp.Domain.PurchasePolicy;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class OrPurchaseComposite implements PurchaseComponent {
    private final List<PurchaseComponent> components = new ArrayList<>();

    public void add(PurchaseComponent component) {
        components.add(component);
    }

    @Override
    public boolean isSatisfied(PurchaseValidationData data) {
        return components.stream().anyMatch(c -> c.isSatisfied(data));
    }
    @Override
    public String getDescription() {
        String childrenDesc = components.stream()
                .map(PurchaseComponent::getDescription)
                .collect(Collectors.joining(" OR "));
        return "(" + childrenDesc + ")";
    }

    public List<PurchaseComponent> getComponents() {
        return components;
    }
}
