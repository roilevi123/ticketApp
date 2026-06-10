package com.ticketing.ticketapp.Infastructure;

import com.ticketing.ticketapp.Appliction.IExternalTicketService;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

@Component
public class ExternalTicketService implements IExternalTicketService {

    private static final String API_URL = "https://damp-lynna-wsep-1984852e.koyeb.app/";

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
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("[ExternalTicketService] issueTicket response: " + response.body());
            return response.body().trim();

        } catch (Exception e) {
            System.out.println("[ExternalTicketService] issueTicket exception: " + e.getMessage());
            e.printStackTrace();
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
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("[ExternalTicketService] cancelTicket response: " + response.body());
            return Integer.parseInt(response.body().trim()) == 1;

        } catch (Exception e) {
            System.out.println("[ExternalTicketService] cancelTicket exception: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
