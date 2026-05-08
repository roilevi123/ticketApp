package Infastructure;

import Domain.OwnerManagerTree.Manager;
import Domain.OwnerManagerTree.Owner;
import Domain.OwnerManagerTree.Permission;
import Domain.OwnerManagerTree.iTreeOfRoleRepository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class TreeOfRoleRepositoryImpl implements iTreeOfRoleRepository {
    private Map<String, Owner> owners=new ConcurrentHashMap<String,Owner>();
    private Map<String, Manager> managers=new ConcurrentHashMap<String,Manager>();

    @Override
    public void storeOwner(String ownerID, String company, String appointerID) {
        Owner o=new Owner(ownerID,company,appointerID);
        if(appointerID.equals(FOUNDER_APPOINTER))
        {
            o.acceptAppointment();
        }
        owners.put(ownerID+company,o);
    }

    @Override
    public boolean exitsOwner(String ownerID, String company) {
        if(owners.containsKey(ownerID+company)){
            return owners.get(ownerID+company).isAccepted();
        }
        return false;
    }
    @Override
    public void storeManager(String managerID, String company, Set<Permission> permissions,String appointerID) {
        Manager m=new Manager(managerID,company,permissions,appointerID);
        managers.put(managerID+company,m);

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
            Owner m = owners.get(ownerID + company);
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
        List<String> names=new ArrayList<>();
        for(Manager m:managers.values()) {
            if(m.getUserID().equals(userID)) {
                names.add(m.getCompanyName());
            }
        }
        for(Owner o:owners.values()) {
            if(o.getUserID().equals(userID)) {
                names.add(o.getCompanyName());
            }
        }
        for(String n:names) {
            if(managers.containsKey(n + userID)) {
                managers.remove(n + userID);
            }
            if(owners.containsKey(n + userID)) {
                owners.remove(n + userID);
            }
        }
    }

}
