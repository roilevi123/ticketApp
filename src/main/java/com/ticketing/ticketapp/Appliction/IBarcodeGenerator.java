package com.ticketing.ticketapp.Appliction;

public interface IBarcodeGenerator {

    String generateBarcode(String eventID, String ticketID);
    boolean isAvailable();
}
