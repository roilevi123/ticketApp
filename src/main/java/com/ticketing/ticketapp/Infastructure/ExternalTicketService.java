package com.ticketing.ticketapp.Infastructure;

import com.ticketing.ticketapp.Appliction.IExternalTicketService;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
public class ExternalTicketService implements IExternalTicketService {

    private String API_URL = "https://damp-lynna-wsep-1984852e.koyeb.app/";

    public void setApiUrl(String apiUrl) {
        this.API_URL = apiUrl;
    }

    @Override
    public String issueTicket(String customerId, String eventId, String zone, int row, int seat) {
        try {
            String seats = "[{\"row\": " + row + ", \"seat\": " + seat + "}]";
            String requestBody = "action_type=issue_ticket"
                    + "&customer_id=" + URLEncoder.encode(customerId, StandardCharsets.UTF_8)
                    + "&event_id=" + URLEncoder.encode(eventId, StandardCharsets.UTF_8)
                    + "&zone=" + URLEncoder.encode(zone, StandardCharsets.UTF_8)
                    + "&is_seating=true"
                    + "&seats=" + URLEncoder.encode(seats, StandardCharsets.UTF_8);

            System.out.println("[ExternalTicketService] issueTicket request: " + requestBody);

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(3))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .timeout(Duration.ofSeconds(5))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new ExternalServiceException("Ticket service returned HTTP " + response.statusCode() + " for issueTicket");
            }
            String body = response.body();
            if (body == null || body.isBlank()) {
                throw new ExternalServiceException("Ticket service returned empty response for issueTicket");
            }
            String trimmedBody = body.trim();
            System.out.println("[ExternalTicketService] issueTicket response: " + trimmedBody);
            if (trimmedBody.equals("-1")) {
                throw new ExternalServiceException("Ticket service returned error: -1");
            }
            return trimmedBody;

        } catch (ExternalServiceException e) {
            throw e;
        } catch (HttpTimeoutException e) {
            System.err.println("[ExternalTicketService] issueTicket Timed Out!");
            return "-1";
        } catch (Exception e) {
            throw new ExternalServiceException("Ticket service issueTicket error: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean cancelTicket(String ticketId) {
        try {
            String requestBody = "action_type=cancel_ticket"
                    + "&ticket_id=" + URLEncoder.encode(ticketId, StandardCharsets.UTF_8);

            System.out.println("[ExternalTicketService] cancelTicket request: " + requestBody);

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(3))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .timeout(Duration.ofSeconds(5))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new ExternalServiceException("Ticket service returned HTTP " + response.statusCode() + " for cancelTicket");
            }
            String body = response.body();
            if (body == null || body.isBlank()) {
                throw new ExternalServiceException("Ticket service returned empty response for cancelTicket");
            }
            System.out.println("[ExternalTicketService] cancelTicket response: " + body);
            int result = Integer.parseInt(body.trim());
            if (result == -1) {
                throw new ExternalServiceException("Ticket cancel service returned error: -1");
            }
            return result == 1;

        } catch (ExternalServiceException e) {
            throw e;
        } catch (HttpTimeoutException e) {
            System.err.println("[ExternalTicketService] cancelTicket Timed Out!");
            return false;
        } catch (Exception e) {
            throw new ExternalServiceException("Ticket service cancelTicket error: " + e.getMessage(), e);
        }
    }
}
