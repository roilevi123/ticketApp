package com.ticketing.ticketapp.Infastructure;

import com.ticketing.ticketapp.Appliction.ISupplyService;
import org.springframework.stereotype.Component;

@Component
public class SupplyServiceMock implements ISupplyService {

    @Override
    public boolean supplyToEmail(String emailAddress, String content) {
        // Simulate sending email by printing to console
        System.out.println("Mock: Email successfully sent to " + emailAddress);
        return true;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}
