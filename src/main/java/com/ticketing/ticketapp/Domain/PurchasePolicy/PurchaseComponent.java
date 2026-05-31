package com.ticketing.ticketapp.Domain.PurchasePolicy;

import jakarta.persistence.*;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "purchase_components")
public abstract class PurchaseComponent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    public abstract boolean isSatisfied(PurchaseValidationData data);

    public abstract String getDescription();

    public Integer getMaxSeats() {
        return null;
    }

    public String getId() {
        return id;
    }
}