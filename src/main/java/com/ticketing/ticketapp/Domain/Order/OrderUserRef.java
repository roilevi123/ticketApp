package com.ticketing.ticketapp.Domain.Order;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "order_users")
public class OrderUserRef {
    @Id
    @Column(name = "user_id")
    private String userId;

    protected OrderUserRef() {
    }

    public OrderUserRef(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }
}
