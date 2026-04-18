package Infastructure;

import Appliction.IPaymentService;

public class PaymentServiceMock implements IPaymentService {
    
    @Override
    public boolean processPayment(String creditCardDetails, double amount) {
        // check if the credit card details are valid and amount is positive
        if (creditCardDetails == null || creditCardDetails.trim().isEmpty() || amount <= 0) {
            return false;
        }
        return true; 
    }

    @Override
    public boolean refund(String creditCardDetails, double amount) {
        return true; // for simplicity, we assume all refunds are successful
    }

    @Override
    public boolean isAvailable() {
        // Simulate that the payment service is always available
        return true; 
    }
}