package com.ticketing.ticketapp.Infastructure;

import com.ticketing.ticketapp.Appliction.IPaymentService;
import com.ticketing.ticketapp.Domain.payment.CreditCardDetails;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;
@Component
public class ExternalPaymentService implements IPaymentService {

    private String API_URL = "https://damp-lynna-wsep-1984852e.koyeb.app/";

    public void setApiUrl(String apiUrl) {
        this.API_URL = apiUrl;
    }

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
                    "id", cardDetails.id()
            );

            String body = sendPost(formData, "Payment service");

            int result = parseIntBody(body, "Payment service");

            if (result == -1) {
                throw new ExternalServiceException(   "Payment was declined by the external payment service");
            }

            return result;

        } catch (ExternalServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ExternalServiceException("Payment service error: " + e.getMessage(), e);
        }
    }

    @Override
    public int refund(int transactionId) {
        try {
            Map<String, String> formData = Map.of(
                    "action_type", "refund",
                    "transaction_id", String.valueOf(transactionId)
            );

            String body = sendPost(formData, "Payment refund service");

            int result = parseIntBody(body, "Payment refund service");

            if (result == -1) {
                throw new ExternalServiceException("Payment refund service returned failure: -1");
            }

            return result;

        } catch (ExternalServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ExternalServiceException("Payment refund service error: " + e.getMessage(), e);
        }
    }

    private String sendPost(Map<String, String> formData, String serviceName) {
        try {
            String requestBody = toFormData(formData);

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(3))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .timeout(Duration.ofSeconds(5))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new ExternalServiceException(
                        serviceName + " returned HTTP " + response.statusCode()
                );
            }

            String body = response.body();

            if (body == null || body.isBlank()) {
                throw new ExternalServiceException(serviceName + " returned empty response");
            }

            return body.trim();

        } catch (HttpTimeoutException e) {
            throw new ExternalServiceException(serviceName + " timeout", e);
        } catch (IOException e) {
            throw new ExternalServiceException(serviceName + " communication failure", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExternalServiceException(serviceName + " interrupted", e);
        }
    }

    private int parseIntBody(String body, String serviceName) {
        try {
            return Integer.parseInt(body.trim());
        } catch (NumberFormatException e) {
            throw new ExternalServiceException(
                    serviceName + " returned invalid numeric response: " + body,
                    e
            );
        }
    }

    private String toFormData(Map<String, String> formData) {
        return formData.entrySet().stream()
                .map(e -> encode(e.getKey()) + "=" + encode(e.getValue()))
                .collect(Collectors.joining("&"));
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}