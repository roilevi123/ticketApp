package com.ticketing.ticketapp.Infastructure;

import com.ticketing.ticketapp.Domain.AdminAggregate.iAdminRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class AdminRepositoryImpl implements iAdminRepository {
    private List<String> adminsIDs = new ArrayList<>();
    @Override
    public boolean isAdmin(String adminID) {
        return adminsIDs.contains(adminID);
    }

    @Override
    public void addAdmin(String adminID) {
        if (!adminsIDs.contains(adminID)) {
            adminsIDs.add(adminID);
        }
    }

    @Override
    public void deleteAll() {
        adminsIDs.clear();
    }


}
