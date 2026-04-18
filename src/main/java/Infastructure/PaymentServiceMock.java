package Infastructure;

import Appliction.IPaymentService;

public class PaymentServiceMock implements IPaymentService {
    
    @Override
    public boolean processPayment(String creditCardDetails, double amount) {
        if (creditCardDetails == null || creditCardDetails.trim().isEmpty() || amount <= 0) {
            return false;
        }
        return true; 
    }

    @Override
    public boolean isAvailable() {
        // Simulate that the payment service is always available
        return true; 
    }
}