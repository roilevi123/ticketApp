package com.ticketing.ticketapp.Controllers;

import com.ticketing.ticketapp.Domain.OwnerManagerTree.OwnerManagerException;
import com.ticketing.ticketapp.Domain.PurchasedOrderAggregate.PurchaseOrderException; 
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OwnerManagerException.class)
    public ResponseEntity<String> handleOwnerManagerException(OwnerManagerException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    @ExceptionHandler(PurchaseOrderException.class)
    public ResponseEntity<String> handlePurchaseOrderException(PurchaseOrderException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    
}