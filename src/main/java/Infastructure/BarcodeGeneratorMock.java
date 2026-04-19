package Infastructure;

import java.util.UUID;

import Appliction.IBarcodeGenerator;

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