package com.ticketing.ticketapp.Domain.Discount;

import java.util.ArrayList;
import java.util.List;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MaxDiscountComposite implements DiscountComponent {
    private final List<DiscountComponent> children = new ArrayList<>();

    public void add(DiscountComponent child) { children.add(child); }

    @Override
    public double calculateDiscount(double price, PurchaseContext context) {
        return children.stream()
                .mapToDouble(c -> c.calculateDiscount(price, context))
                .max()
                .orElse(0);
    }
    @Override
    public String getDescription() {
        String childrenDesc = children.stream()
                .map(DiscountComponent::getDescription)
                .collect(Collectors.joining(", "));
        return "Best of: [" + childrenDesc + "]";
    }
}
