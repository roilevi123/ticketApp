package com.ticketing.ticketapp.Domain.PurchasePolicy;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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

    /** OR = satisfying any branch is enough, so the most permissive (maximum) seat cap applies. */
    @Override
    public Integer getMaxSeats() {
        return components.stream()
                .map(PurchaseComponent::getMaxSeats)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(null);
    }
}
