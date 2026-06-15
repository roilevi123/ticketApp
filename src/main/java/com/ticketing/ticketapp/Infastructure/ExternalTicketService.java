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

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

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

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .timeout(Duration.ofSeconds(5))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("[ExternalTicketService] issueTicket response: " + response.body());
            return response.body().trim();

        } catch (HttpTimeoutException e) {
            System.err.println("[ExternalTicketService] issueTicket Timed Out!");
            return "-1";
        } catch (Exception e) {
            System.err.println("[ExternalTicketService] issueTicket error: " + e.getMessage());
            return "-1";
        }
    }

    @Override
    public boolean cancelTicket(String ticketId) {
        try {
            String requestBody = "action_type=cancel_ticket"
                    + "&ticket_id=" + URLEncoder.encode(ticketId, StandardCharsets.UTF_8);

            System.out.println("[ExternalTicketService] cancelTicket request: " + requestBody);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .timeout(Duration.ofSeconds(5))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("[ExternalTicketService] cancelTicket response: " + response.body());
            return Integer.parseInt(response.body().trim()) == 1;

        } catch (HttpTimeoutException e) {
            System.err.println("[ExternalTicketService] cancelTicket Timed Out!");
            return false;
        } catch (Exception e) {
            System.err.println("[ExternalTicketService] cancelTicket error: " + e.getMessage());
            return false;
        }
    }
}
