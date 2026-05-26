package com.ticketing.ticketapp.Controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SupportControllerTest {

    @InjectMocks
    private SupportController supportController;

    private static final String TOKEN = "test-token";

    @Test
    void submitTicket_AlwaysReturns200WithMessage() {
        SupportTicketDTO dto = new SupportTicketDTO();
        dto.setSupportType("TECHNICAL");
        dto.setSubject("Login issue");
        dto.setMessage("Cannot login to the platform");

        ResponseEntity<?> response = supportController.submitTicket(TOKEN, dto);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Your support ticket has been submitted successfully.",
                ((Map<?, ?>) response.getBody()).get("message"));
    }

    @Test
    void submitTicket_DifferentSupportTypes_AlwaysReturns200() {
        for (String type : new String[]{"BILLING", "GENERAL", "TECHNICAL"}) {
            SupportTicketDTO dto = new SupportTicketDTO();
            dto.setSupportType(type);
            dto.setSubject("Subject");
            dto.setMessage("Message");

            ResponseEntity<?> response = supportController.submitTicket(TOKEN, dto);

            assertEquals(200, response.getStatusCode().value(), "Expected 200 for type: " + type);
        }
    }
}
