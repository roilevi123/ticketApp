package com.ticketing.ticketapp.Domain.PurchasePolicy;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Entity
public class OrPurchaseComposite extends PurchaseComponent {

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinTable(
            name = "or_components",
            joinColumns = @JoinColumn(name = "composite_id"),
            inverseJoinColumns = @JoinColumn(name = "component_id")
    )
    private List<PurchaseComponent> components = new ArrayList<>();

    public OrPurchaseComposite() {} // חובה עבור JPA

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

    @Override
    public Integer getMaxSeats() {
        return components.stream()
                .map(PurchaseComponent::getMaxSeats)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(null);
    }
}