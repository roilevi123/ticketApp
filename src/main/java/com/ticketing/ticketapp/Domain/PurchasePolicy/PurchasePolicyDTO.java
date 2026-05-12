package com.ticketing.ticketapp.Domain.PurchasePolicy;

public record PurchasePolicyDTO(
        String id,
        String targetId,
        String type,
        String description
) {}
