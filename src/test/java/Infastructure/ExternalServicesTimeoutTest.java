package Infastructure;

import com.ticketing.ticketapp.Infastructure.ExternalPaymentService;
import com.ticketing.ticketapp.Infastructure.ExternalServiceException;
import com.ticketing.ticketapp.Infastructure.ExternalTicketService;
import com.ticketing.ticketapp.Domain.payment.CreditCardDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
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
    public void testProcessPayment_ThrowsTimeout_AndThrowsExternalServiceException() {
        CreditCardDetails mockCard = new CreditCardDetails("1234567812345678", "12", "2026", "John Doe", "123", "123456789");

        long startTime = System.currentTimeMillis();
        assertThrows(ExternalServiceException.class,
                () -> paymentService.processPayment(mockCard, 100.0, "USD"),
                "A timeout should throw ExternalServiceException");
        long endTime = System.currentTimeMillis();

        assertTrue((endTime - startTime) < 6000, "The request took too long! Timeout limit was bypassed.");
    }

    @Test
    public void testCancelTicket_ThrowsTimeout_AndThrowsExternalServiceException() {
        long startTime = System.currentTimeMillis();
        assertThrows(ExternalServiceException.class,
                () -> ticketService.cancelTicket("mock_ticket_id"),
                "A timeout should throw ExternalServiceException");
        long endTime = System.currentTimeMillis();

        assertTrue((endTime - startTime) < 6000, "The request took too long! Timeout limit was bypassed.");
    }
}
