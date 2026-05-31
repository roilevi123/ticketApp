package com.ticketing.ticketapp.Domain.PurchasePolicy;

import jakarta.persistence.*;

@Entity
@Table(name = "purchase_components")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "component_type", discriminatorType = DiscriminatorType.STRING)
public abstract class PurchaseComponent {

    @Id
    @Column(name = "id")
    private String id;

    public PurchaseComponent() {
        this.id = java.util.UUID.randomUUID().toString();
    }

    public abstract boolean isSatisfied(PurchaseValidationData data);

    public abstract String getDescription();

    public Integer getMaxSeats() {
        return null;
    }

    public String getId() {
        return id;
    }
}