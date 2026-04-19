package Infastructure;

import Domain.AdminAggregate.iAdminRepository;

import java.util.ArrayList;
import java.util.List;

public class AdminRepositoryImpl implements iAdminRepository {
    private final List<String> defaultAdmins = List.of("admin");
    private List<String> admins = new ArrayList<>(defaultAdmins);
    @Override
    public boolean isAdmin(String adminName) {
        return admins.contains(adminName);
    }

    @Override
    public void deleteAll() {
//        admins.clear();
//        admins.addAll(defaultAdmins);
    }


}
