package com.ticketing.ticketapp.Domain.AdminAggregate;

public interface iAdminRepository {
    public boolean isAdmin(String adminID);
    public void deleteAll();
}
