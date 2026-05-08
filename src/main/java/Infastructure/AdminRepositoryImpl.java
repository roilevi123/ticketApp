package Infastructure;

import Domain.AdminAggregate.iAdminRepository;

import java.util.ArrayList;
import java.util.List;

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
