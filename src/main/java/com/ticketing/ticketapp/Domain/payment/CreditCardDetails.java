package com.ticketing.ticketapp.Domain.payment;

public record CreditCardDetails(
        String cardNumber,
        String month,
        String year,
        String holder,
        String cvv,
        String id
) {}