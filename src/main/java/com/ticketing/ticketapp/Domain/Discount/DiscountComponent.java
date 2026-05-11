package com.ticketing.ticketapp.Domain.Discount;

public interface DiscountComponent {
    double calculateDiscount(double originalPrice, PurchaseContext context);
}
