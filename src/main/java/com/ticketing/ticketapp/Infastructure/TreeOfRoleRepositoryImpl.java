package com.ticketing.ticketapp.Infastructure;

import com.ticketing.ticketapp.Domain.OwnerManagerTree.Manager;
import com.ticketing.ticketapp.Domain.OwnerManagerTree.Owner;
import com.ticketing.ticketapp.Domain.OwnerManagerTree.Permission;
import com.ticketing.ticketapp.Domain.OwnerManagerTree.iTreeOfRoleRepository;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class TreeOfRoleRepositoryImpl implements iTreeOfRoleRepository {
    private Map<String, Owner> owners=new ConcurrentHashMap<String,Owner>();
    private Map<String, Manager> managers=new ConcurrentHashMap<String,Manager>();

    @Override
    public void storeOwner(String ownerID, String company, String appointerID) {
        managers.remove(ownerID + company);
        Owner o = new Owner(ownerID, company, appointerID);
        if (FOUNDER_APPOINTER.equals(appointerID)) {
            o.acceptAppointment(); // founder auto-accepts their own company creation
        }
        owners.put(ownerID + company, o);
    }

    @Override
    public boolean exitsOwner(String ownerID, String company) {
        if(owners.containsKey(ownerID+company)){
            return owners.get(ownerID+company).isAccepted();
        }
        return false;
    }
    @Override
    public void storeManager(String managerID, String company, Set<Permission> permissions, String appointerID) {
        // Cannot demote a founder to manager
        Owner existingOwner = owners.get(managerID + company);
        if (existingOwner != null && FOUNDER_APPOINTER.equals(existingOwner.getAppointerID())) {
            throw new RuntimeException("Cannot assign a manager role to the company founder");
        }
        owners.remove(managerID + company);
        Manager m = new Manager(managerID, company, permissions, appointerID);
        managers.put(managerID + company, m);
    }
    @Override
    public void deleteCompanyMangersAndOwners(String company) {
        List<String> names=new ArrayList<>();
        for(Manager m:managers.values()) {
            if(m.getCompanyName().equals(company)) {
                names.add(m.getUserID());
            }
        }
        for(Owner o:owners.values()) {
            if(o.getCompanyName().equals(company)) {
                names.add(o.getUserID());
            }
        }
        for(String n:names) {
            if(managers.containsKey(n+company)) {
                managers.remove(n+company);
            }
            if(owners.containsKey(n+company)) {
                owners.remove(n+company);
            }
        }
    }

    @Override
    public Manager getManager(String managerID,String company) {
        return managers.get(managerID+company);
    }

    @Override
    public void save(Owner ownerToUpdate) {
        String key = ownerToUpdate.getUserID() + ownerToUpdate.getCompanyName();

        Owner currentInDb = owners.get(key);

        if (currentInDb == null) {
            throw new RuntimeException("Owner not found for update: " + key);
        }

        Owner updatedOwner = new Owner(ownerToUpdate);
        updatedOwner.setVersion(ownerToUpdate.getVersion() + 1);

        boolean success = owners.replace(key, currentInDb, updatedOwner);

        if (!success) {
            throw new RuntimeException("Optimistic Lock Failure: Owner '" + key +
                    "' was updated by another thread.");
        }
    }

    @Override
    public void save(Manager managerToUpdate) {
        String key = managerToUpdate.getUserID() + managerToUpdate.getCompanyName();
        Manager currentInDb = managers.get(key);

        if (currentInDb == null) {
            throw new RuntimeException("Manager not found for update: " + key);
        }

        Manager updatedManager = new Manager(managerToUpdate);
        updatedManager.setVersion(managerToUpdate.getVersion() + 1);

        boolean success = managers.replace(key, currentInDb, updatedManager);

        if (!success) {
            throw new RuntimeException("Optimistic Lock Failure: Manager '" + key +
                    "' was updated by another thread.");
        }
    }

    @Override
    public void deleteManager(String managerID, String company) {
        managers.remove(managerID+company);
    }

    @Override
    public void deleteOwner(String ownerID, String company) {
        owners.remove(ownerID+company);
    }

    @Override
    public boolean isManager(String managerID, String company) {
        if(managers.containsKey(managerID+company)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean isOwner(String ownerID, String company) {
        if(owners.containsKey(ownerID+company)) {
            return true;
        }
        return false;
    }


    public boolean ManagerPermitedToCreateUpdateDelete(String managerID,String company){
        if (managers.containsKey(managerID + company)) {
            Manager m=managers.get(managerID + company);
            return m.getPermissions().contains(Permission.MANAGE_INVENTORY) && managers.get(managerID + company).isAccepted();
        }
        return false;
    }


    @Override
    public Owner getOwner(String ownerID,String company) {
        return owners.get(ownerID + company);
    }
    @Override
    public boolean isAppointerManager(String managerID, String company, String appointerID) {
        String key = managerID + company;
        if(managers.containsKey(key)) {
            Manager m=managers.get(key);
            return m.isAccepted()&& appointerID.equals(m.getAppointerID());
        }
        return false;
    }

    @Override
    public boolean isAppointerOwner(String ownerID, String company, String appointer) {
        String key = ownerID + company;
        if (owners.containsKey(key)) {
            Owner o=owners.get(key);
            return o.isAccepted()&& appointer.equals(o.getAppointerID());
        }
        return false;
    }
    @Override
    public Set<Permission> getManagerPermissions(String managerID, String company) {
        return managers.get(managerID + company).getPermissions();
    }
    @Override
    public List<Owner> getAllOwnersByCompany(String company) {
        return owners.values().stream()
                .filter(o -> o.getCompanyName().equals(company))
                .collect(Collectors.toList());
    }

    @Override
    public List<Manager> getAllManagersByCompany(String company) {
        return managers.values().stream()
                .filter(m -> m.getCompanyName().equals(company))
                .collect(Collectors.toList());
    }

    @Override
    public void deleteAllRoles() {
        managers.clear();
        owners.clear();
    }
    @Override
    public boolean ManagerPermitToSeeTransactions(String manager, String company) {
        if (managers.containsKey(manager+company)) {
            Manager m=managers.get(manager+company);
            return m.getPermissions().contains(Permission.GENERATE_SALES_REPORTS);
        }
        return false;
    }
    @Override
    public void deleteUserRoles(String userID) {
        List<String> companyNames = new ArrayList<>();
        for (Manager m : managers.values()) {
            if (m.getUserID().equals(userID)) {
                companyNames.add(m.getCompanyName());
            }
        }
        for (Owner o : owners.values()) {
            if (o.getUserID().equals(userID)) {
                companyNames.add(o.getCompanyName());
            }
        }
        for (String companyName : companyNames) {
            managers.remove(userID + companyName);
            owners.remove(userID + companyName);
        }
    }

    @Override
    public List<String> getUserCompanies(String userId) {
        Set<String> companySet = new HashSet<>();
        for (Owner o : owners.values()) {
            if (o.getUserID().equals(userId) && o.isAccepted()) {
                companySet.add(o.getCompanyName());
            }
        }
        for (Manager m : managers.values()) {
            if (m.getUserID().equals(userId) && m.isAccepted()) {
                companySet.add(m.getCompanyName());
            }
        }
        return new ArrayList<>(companySet);
    }

    @Override
    public String getRoleInCompany(String userId, String companyName) {
        String key = userId + companyName;
        Owner o = owners.get(key);
        if (o != null && o.isAccepted()) {
            return FOUNDER_APPOINTER.equals(o.getAppointerID()) ? "FOUNDER" : "OWNER";
        }
        Manager m = managers.get(key);
        if (m != null && m.isAccepted()) return "MANAGER";
        return "MEMBER";
    }

    @Override
    public String getUserHighestRole(String userId) {
        for (Owner o : owners.values()) {
            if (o.getUserID().equals(userId) && o.isAccepted()) return "OWNER";
        }
        for (Manager m : managers.values()) {
            if (m.getUserID().equals(userId) && m.isAccepted()) return "MANAGER";
        }
        return "MEMBER";
    }

}
