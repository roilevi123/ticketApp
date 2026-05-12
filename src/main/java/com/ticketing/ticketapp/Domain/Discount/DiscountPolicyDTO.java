package com.ticketing.ticketapp.Domain.Discount;

public record DiscountPolicyDTO(
        String id,
        String targetId,
        String type,
        String description,
        double percentage
) {
    @Override
    public String toString() {
        return String.format("[%s] %s: %s",
                id.substring(0, Math.min(id.length(), 8)),
                type,
                description);
    }
}