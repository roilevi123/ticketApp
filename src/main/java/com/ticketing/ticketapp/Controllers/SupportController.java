package com.ticketing.ticketapp.Controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/support")
public class SupportController {

    private static final Logger log = LoggerFactory.getLogger(SupportController.class);

    @PostMapping("/ticket")
    public ResponseEntity<?> submitTicket(
            @RequestAttribute("cleanToken") String token,
            @RequestBody SupportTicketDTO request) {

        log.info("[SECURITY] Secure Support Ticket submitted to {}: {}",
                request.getSupportType(), request.getSubject());

        return ResponseEntity.ok(Map.of("message", "Your support ticket has been submitted successfully."));
    }
}

class SupportTicketDTO {
    private String supportType;
    private String subject;
    private String message;

    public String getSupportType()          { return supportType; }
    public void   setSupportType(String v)  { supportType = v; }
    public String getSubject()              { return subject; }
    public void   setSubject(String v)      { subject = v; }
    public String getMessage()              { return message; }
    public void   setMessage(String v)      { message = v; }
}
