package Infastructure;

import com.sun.net.httpserver.HttpServer;
import com.ticketing.ticketapp.Domain.payment.CreditCardDetails;
import com.ticketing.ticketapp.Infastructure.ExternalPaymentService;
import com.ticketing.ticketapp.Infastructure.ExternalServiceException;
import com.ticketing.ticketapp.Infastructure.ExternalTicketService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that gateways throw ExternalServiceException when the external API
 * responds with a payload of "-1", rather than silently returning a sentinel value.
 */
public class ExternalServicesNegativeResponseTest {

    private HttpServer server;
    private String serverUrl;

    private ExternalPaymentService paymentService;
    private ExternalTicketService ticketService;

    @BeforeEach
    public void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", exchange -> {
            byte[] body = "-1".getBytes();
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        server.start();

        int port = server.getAddress().getPort();
        serverUrl = "http://localhost:" + port + "/";

        paymentService = new ExternalPaymentService();
        paymentService.setApiUrl(serverUrl);

        ticketService = new ExternalTicketService();
        ticketService.setApiUrl(serverUrl);
    }

    @AfterEach
    public void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

//    @Test
//    public void processPayment_WhenApiReturnsMinusOne_ThrowsExternalServiceException() {
//        CreditCardDetails card = new CreditCardDetails(
//                "1234567812345678", "12", "2026", "John Doe", "123", "123456789");
//
//        ExternalServiceException ex = assertThrows(ExternalServiceException.class,
//                () -> paymentService.processPayment(card, 100.0, "USD"));
//
//        assertTrue(ex.getMessage().contains("-1"));
//    }

    @Test
    public void refund_WhenApiReturnsMinusOne_ThrowsExternalServiceException() {
        ExternalServiceException ex = assertThrows(ExternalServiceException.class,
                () -> paymentService.refund(42));

        assertTrue(ex.getMessage().contains("-1"));
    }

    @Test
    public void issueTicket_WhenApiReturnsMinusOne_ThrowsExternalServiceException() {
        ExternalServiceException ex = assertThrows(ExternalServiceException.class,
                () -> ticketService.issueTicket("user1", "event1", "A", 1, 1));

        assertTrue(ex.getMessage().contains("-1"));
    }

    @Test
    public void cancelTicket_WhenApiReturnsMinusOne_ThrowsExternalServiceException() {
        ExternalServiceException ex = assertThrows(ExternalServiceException.class,
                () -> ticketService.cancelTicket("ticket-abc"));

        assertTrue(ex.getMessage().contains("-1"));
    }
}
