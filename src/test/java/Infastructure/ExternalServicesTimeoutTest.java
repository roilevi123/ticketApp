package Infastructure;

import com.ticketing.ticketapp.Infastructure.ExternalPaymentService;
import com.ticketing.ticketapp.Infastructure.ExternalTicketService;
import com.ticketing.ticketapp.Domain.payment.CreditCardDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ExternalServicesTimeoutTest {

    private ExternalPaymentService paymentService;
    private ExternalTicketService ticketService;

    private static final String SLOW_API_URL = "https://httpstat.us/200?sleep=10000";

    
    @BeforeEach
    public void setup() {
        paymentService = new ExternalPaymentService();
        paymentService.setApiUrl(SLOW_API_URL);

        ticketService = new ExternalTicketService();
        ticketService.setApiUrl(SLOW_API_URL);
    }

    @Test
    public void testProcessPayment_ThrowsTimeout_AndReturnsMinusOne() {
        CreditCardDetails mockCard = new CreditCardDetails("1234567812345678", "12", "2026", "John Doe", "123", "123456789");
        
        long startTime = System.currentTimeMillis();
        int result = paymentService.processPayment(mockCard, 100.0, "USD");
        long endTime = System.currentTimeMillis();

        // timeout was set to 5 seconds, so we expect the total time to be less than 6 seconds 
        assertTrue((endTime - startTime) < 6000, "The request took too long! Timeout limit was bypassed.");
        assertEquals(-1, result, "A timeout exception should have been caught, returning -1.");
    }

    @Test
    public void testCancelTicket_ThrowsTimeout_AndReturnsFalse() {
        long startTime = System.currentTimeMillis();
        boolean result = ticketService.cancelTicket("mock_ticket_id");
        long endTime = System.currentTimeMillis();

        assertTrue((endTime - startTime) < 6000, "The request took too long! Timeout limit was bypassed.");
        assertFalse(result, "A timeout exception should have been caught, returning false.");
    }
}