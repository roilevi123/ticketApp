package com.ticketing.ticketapp.Infastructure;

import com.ticketing.ticketapp.Appliction.IPaymentService;
import com.ticketing.ticketapp.Domain.payment.CreditCardDetails;
import org.springframework.stereotype.Component;

@Component
public class PaymentServiceMock implements IPaymentService {
    
//    @Override
//    public boolean processPayment(String creditCardDetails, double amount) {
//        // check if the credit card details are valid and amount is positive
//
//        return true;
//    }
//
//    @Override
//    public boolean refund(String creditCardDetails, double amount) {
//        return true; // for simplicity, we assume all refunds are successful
//    }


    @Override
    public int processPayment(CreditCardDetails cardDetails, double amount, String currency) {
        return 0;
    }

    @Override
    public int refund(int transactionId) {
        return 0;
    }
}
