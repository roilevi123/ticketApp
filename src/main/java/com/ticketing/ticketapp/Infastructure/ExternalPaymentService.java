package com.ticketing.ticketapp.Infastructure;

import com.ticketing.ticketapp.Appliction.IPaymentService;
import com.ticketing.ticketapp.Domain.payment.CreditCardDetails;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;

public class ExternalPaymentService implements IPaymentService {

    private String API_URL = "https://damp-lynna-wsep-1984852e.koyeb.app/";

    public void setApiUrl(String apiUrl) {
        this.API_URL = apiUrl;
    }

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    @Override
    public int processPayment(CreditCardDetails cardDetails, double amount, String currency) {
        try {
            Map<String, String> formData = Map.of(
                    "action_type", "pay",
                    "amount", String.valueOf((int) amount),
                    "currency", currency,
                    "card_number", cardDetails.cardNumber(),
                    "month", cardDetails.month(),
                    "year", cardDetails.year(),
                    "holder", cardDetails.holder(),
                    "cvv", cardDetails.cvv(),
                    "id", cardDetails.id());

            String requestBody = formData.entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining("&"));

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .timeout(Duration.ofSeconds(5))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            return Integer.parseInt(response.body().trim());

        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    @Override
    public int refund(int transactionId) {
        try {
            String requestBody = "action_type=refund&transaction_id=" + transactionId;

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .timeout(Duration.ofSeconds(5))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return Integer.parseInt(response.body().trim());

        } catch (HttpTimeoutException e) {
            System.err.println("[ExternalPaymentService] Timed Out! (Limit reached)");
            return -1;
        } catch (Exception e) {
            System.err.println("[ExternalPaymentService] Network error or timeout: " + e.getMessage());
            return -1;
        }
    }
}