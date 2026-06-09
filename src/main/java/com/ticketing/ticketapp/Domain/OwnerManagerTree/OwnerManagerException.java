package com.ticketing.ticketapp.Domain.OwnerManagerTree;

public class OwnerManagerException extends RuntimeException {
    public OwnerManagerException(String message) {
        super(message);
    }

    public OwnerManagerException(String message, Throwable cause) {
        super(message, cause);
    }
}
