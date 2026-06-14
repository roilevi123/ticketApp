package com.ticketing.ticketapp.Domain.User;

public class UserSuspendedException extends RuntimeException {
    public UserSuspendedException(String userId) {
        super("User is suspended");
    }
}
