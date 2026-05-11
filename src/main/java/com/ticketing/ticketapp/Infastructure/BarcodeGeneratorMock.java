package com.ticketing.ticketapp.Infastructure;

import java.util.UUID;

import com.ticketing.ticketapp.Appliction.IBarcodeGenerator;
import org.springframework.stereotype.Component;

@Component
public class BarcodeGeneratorMock implements IBarcodeGenerator {

    @Override
    public String generateBarcode(String eventId, String ticketId) {
        // random string to represent a barcode
        String randomStr = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "BARCODE-" + eventId + "-" + ticketId + "-" + randomStr;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}
