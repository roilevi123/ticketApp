package com.ticketing.ticketapp.Infastructure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
public class ExternalSystemClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${external.system.url}")
    private String externalUrl;

    public boolean handshake() {
        try {
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("action_type", "handshake");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<MultiValueMap<String, String>> request =
                    new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    externalUrl,
                    request,
                    String.class
            );

            System.out.println("Handshake raw response: " + response.getBody());

            return "OK".equals(response.getBody());

        } catch (Exception e) {
            System.out.println("Handshake failed: " + e.getMessage());
            return false;
        }
    }
}