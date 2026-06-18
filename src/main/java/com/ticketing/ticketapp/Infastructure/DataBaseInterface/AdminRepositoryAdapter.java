package com.ticketing.ticketapp.Infastructure.DataBaseInterface;

import com.ticketing.ticketapp.Domain.AdminAggregate.iAdminRepository;
import org.springframework.stereotype.Repository;
import java.util.HashSet;
import java.util.Set;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
@Repository
@ConditionalOnProperty(name = "repository.type", havingValue = "DB")
public class AdminRepositoryAdapter implements iAdminRepository {

    // בבדיקות או ב-In-Memory, ניתן להשתמש בסט לשמירת האדמינים
    private final Set<String> adminIds = new HashSet<>();

    @Override
    public boolean isAdmin(String adminID) {
        return adminIds.contains(adminID);
    }

    @Override
    public void addAdmin(String adminID) {
        adminIds.add(adminID);
    }

    @Override
    public void deleteAll() {
        adminIds.clear();
    }
}