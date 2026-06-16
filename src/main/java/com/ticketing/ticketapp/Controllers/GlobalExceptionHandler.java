package com.ticketing.ticketapp.Controllers;

import com.ticketing.ticketapp.Domain.OwnerManagerTree.OwnerManagerException;
import com.ticketing.ticketapp.Domain.PurchasedOrderAggregate.PurchaseOrderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(OwnerManagerException.class)
    public ResponseEntity<String> handleOwnerManagerException(OwnerManagerException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    @ExceptionHandler(PurchaseOrderException.class)
    public ResponseEntity<String> handlePurchaseOrderException(PurchaseOrderException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<String> handleDataAccessException(DataAccessException ex) {
        if (ex instanceof CannotGetJdbcConnectionException) {
            logger.error("Database unreachable: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("Database is temporarily unavailable. Please try again later.");
        }
        logger.error("Database error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("A database error occurred. Please try again later.");
    }
}