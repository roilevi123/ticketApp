package com.ticketing.ticketapp.Domain.Discount;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@DiscriminatorValue("SUM_COMPOSITE")
public class SumDiscountComposite extends DiscountComponent {

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "parent_id")
    private List<DiscountComponent> children = new ArrayList<>();

    protected SumDiscountComposite() {
        super();
    }

    public SumDiscountComposite(String id) {
        super(id);
    }

    public void add(DiscountComponent child) {
        children.add(child);
    }

    @Override
    public double calculateDiscount(double price, PurchaseContext context) {
        return children.stream()
                .mapToDouble(c -> c.calculateDiscount(price, context))
                .sum();
    }

    @Override
    public String getDescription() {
        String childrenDesc = children.stream()
                .map(DiscountComponent::getDescription)
                .collect(Collectors.joining(" + "));
        return "Combined: (" + childrenDesc + ")";
    }

    public List<DiscountComponent> getChildren() {
        return children;
    }
}