package Appliction;

public interface IPaymentService {

    boolean processPayment(String creditCardDetails, double amount);
    boolean refund(String creditCardDetails, double amount);
    boolean isAvailable();
}
