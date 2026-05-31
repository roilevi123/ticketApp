package com.ticketing.ticketapp.Domain.Discount;

import jakarta.persistence.*;

@Entity
@Table(name = "discount_components")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "discount_type", discriminatorType = DiscriminatorType.STRING)
public abstract class DiscountComponent {

    @Id
    @Column(name = "id")
    private String id;

    protected DiscountComponent() {}

    public DiscountComponent(String id) {
        this.id = id;
    }

    public abstract double calculateDiscount(double originalPrice, PurchaseContext context);

    public abstract String getDescription();

    public String getId() {
        return id;
    }
}