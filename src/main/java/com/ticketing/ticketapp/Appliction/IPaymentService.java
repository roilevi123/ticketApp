package com.ticketing.ticketapp.Appliction;

import com.ticketing.ticketapp.Domain.payment.CreditCardDetails;

public interface IPaymentService {

    int processPayment(CreditCardDetails cardDetails, double amount, String currency);


    int refund(int transactionId);
}