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
    public void storeOwner(String owner, String company, String appointer) {
        Owner o=new Owner(owner,company,appointer);
        if(appointer.equals("Administrator"))
        {
            o.acceptAppointment();
        }
        owners.put(owner+company,o);
    }

    @Override
    public boolean exitsOwner(String owner, String company) {
        if(owners.containsKey(owner+company)){
            return owners.get(owner+company).isAccepted();
        }
        return false;
    }
    @Override
    public void storeManager(String manager, String company, Set<Permission> permissions,String appointer) {
        Manager m=new Manager(manager,company,permissions,appointer);
        managers.put(manager+company,m);

    }

    @Override
    public Manager getManager(String manager,String company) {
        return managers.get(manager+company);
    }

    @Override
    public void save(Owner ownerToUpdate) {
        String key = ownerToUpdate.getUserName() + ownerToUpdate.getCompanyName();

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
        String key = managerToUpdate.getUserName() + managerToUpdate.getCompanyName();
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
    public void deleteManager(String manager, String company) {
        managers.remove(manager+company);
    }

    @Override
    public void deleteOwner(String owner, String company) {
        owners.remove(owner+company);
    }

    @Override
    public boolean isManager(String manager, String company) {
        if(managers.containsKey(manager+company)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean isOwner(String owner, String company) {
        if(owners.containsKey(owner+company)) {
            Owner m = owners.get(owner + company);
            return true;
        }
        return false;
    }

    @Override
    public Owner getOwner(String owner,String company) {
        return owners.get(owner+company);
    }
    @Override
    public boolean isAppointerManager(String manager, String company, String appointer) {
        if(managers.containsKey(manager+company)) {
            Manager m=managers.get(manager+company);
            return m.isAccepted()&& appointer.equals(m.getAppointer());
        }
        return false;
    }

    @Override
    public boolean isAppointerOwner(String owner, String company, String appointer) {
        if (owners.containsKey(owner+company)) {
            Owner o=owners.get(owner+company);
            return o.isAccepted()&& appointer.equals(o.getAppointer());
        }
        return false;
    }
    @Override
    public Set<Permission> getManagerPermissions(String manager, String company) {
        return managers.get(manager+company).getPermissions();
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

}
