package com.ticketing.ticketapp.Controllers;

import com.ticketing.ticketapp.Appliction.IPaymentService;
import com.ticketing.ticketapp.Appliction.ISupplyService;
import com.ticketing.ticketapp.Appliction.Response;
import com.ticketing.ticketapp.Appliction.SystemService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/system")
public class SystemController {

    private final SystemService systemService;
    private final IPaymentService paymentService;
    private final ISupplyService supplyService;

    public SystemController(SystemService systemService, IPaymentService paymentService,
                            ISupplyService supplyService) {
        this.systemService = systemService;
        this.paymentService = paymentService;
        this.supplyService = supplyService;
    }

    @PostMapping("/init")
    public ResponseEntity<?> initSystem(
            @RequestAttribute("cleanToken") String token,
            @RequestBody InitRequest request) {

        Response<String> response = systemService.initSystem(
                token,
                request.getAdminUsername(),
                request.getAdminPassword(),
                request.getAdminAge(),
                request.getAdminEmail()
        );

        if (response.isSuccess()) {
            return ResponseEntity.ok(Map.of("message", response.getData()));
        }
        return ResponseEntity.badRequest().body(Map.of("error", response.getMessage()));
    }

    @PostMapping("/open")
    public ResponseEntity<?> openSystem(
            @RequestAttribute("cleanToken") String token) {

        Response<String> response = systemService.openSystem(token);

        if (response.isSuccess()) {
            return ResponseEntity.ok(Map.of("message", response.getData()));
        }
        return ResponseEntity.badRequest().body(Map.of("error", response.getMessage()));
    }

    @PostMapping("/external/payment")
    public ResponseEntity<?> processPayment(
            @RequestAttribute("cleanToken") String token,
            @RequestBody PaymentRequest request) {

        boolean result = paymentService.processPayment(request.getCreditCardDetails(), request.getAmount());
        if (result) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "transactionId", "TXN-" + UUID.randomUUID()
            ));
        }
        return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "Payment declined"
        ));
    }

    @PostMapping("/external/supply")
    public ResponseEntity<?> supplyTicket(
            @RequestAttribute("cleanToken") String token,
            @RequestBody SupplyRequest request) {

        boolean result = supplyService.supplyToEmail(request.getEmailAddress(), request.getContent());
        if (result) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Ticket delivered to " + request.getEmailAddress()
            ));
        }
        return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "Supply failed"
        ));
    }

    public static class InitRequest {
        private String adminUsername;
        private String adminPassword;
        private int adminAge;
        private String adminEmail;

        public String getAdminUsername() { return adminUsername; }
        public void setAdminUsername(String adminUsername) { this.adminUsername = adminUsername; }

        public String getAdminPassword() { return adminPassword; }
        public void setAdminPassword(String adminPassword) { this.adminPassword = adminPassword; }

        public int getAdminAge() { return adminAge; }
        public void setAdminAge(int adminAge) { this.adminAge = adminAge; }

        public String getAdminEmail() { return adminEmail; }
        public void setAdminEmail(String adminEmail) { this.adminEmail = adminEmail; }
    }

    public static class PaymentRequest {
        private String creditCardDetails;
        private double amount;

        public String getCreditCardDetails() { return creditCardDetails; }
        public void setCreditCardDetails(String creditCardDetails) { this.creditCardDetails = creditCardDetails; }

        public double getAmount() { return amount; }
        public void setAmount(double amount) { this.amount = amount; }
    }

    public static class SupplyRequest {
        private String emailAddress;
        private String content;

        public String getEmailAddress() { return emailAddress; }
        public void setEmailAddress(String emailAddress) { this.emailAddress = emailAddress; }

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }
}
