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
    public void testProcessPayment_ThrowsTimeout() {
        CreditCardDetails mockCard = new CreditCardDetails("1234567812345678", "12", "2026", "John Doe", "123", "123456789");
        assertThrows(ExternalServiceException.class, () -> paymentService.processPayment(mockCard, 100.0, "USD"));
    }

    @Test
    public void testIssueTicket_ThrowsTimeout_WhenExternalSystemFails() {
        String customerId = "cust_123";
        String eventId = "event_999";
        String zone = "A";
        int row = 5;
        int seat = 10;

        assertThrows(ExternalServiceException.class,
                () -> ticketService.issueTicket(customerId, eventId, zone, row, seat),
                "System should throw ExternalServiceException when ticket service times out"
        );
    }

    @Test
    public void testRefund_ThrowsTimeout_WhenExternalSystemFails() {
        int invalidTransactionId = 999;

        assertThrows(ExternalServiceException.class,
                () -> paymentService.refund(invalidTransactionId),
                "System should throw ExternalServiceException when refund service times out"
        );
    }

    @Test
    public void testRefund_ThrowsTimeout() {
        assertThrows(ExternalServiceException.class,
                () -> paymentService.refund(999),
                "Refund service should throw exception on timeout");
    }

    @Test
    public void testIssueTicket_ThrowsTimeout() {
        assertThrows(ExternalServiceException.class,
                () -> ticketService.issueTicket("cust_1", "event_1", "A", 1, 1),
                "System must throw ExternalServiceException on issueTicket timeout");
    }

    @Test
    public void testCancelTicket_ThrowsTimeout() {
        assertThrows(ExternalServiceException.class,
                () -> ticketService.cancelTicket("ticket_123"),
                "System must throw ExternalServiceException on cancelTicket timeout to ensure robustness");
    }

}
