package Domain.OwnerManagerTree;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Owner {
    private String userName;
    private String CompanyName;
    private boolean isAccepted;
    private int version;
    private String appointer;
    public Owner(String userName, String companyName,String appointer) {
        this.userName = userName;
        this.CompanyName = companyName;

        this.isAccepted = false;
        this.version = 0;
        this.appointer = appointer;
    }
    public Owner(Owner owner) {
        this.userName = owner.getUserName();
        this.CompanyName = owner.getCompanyName();

        this.isAccepted = owner.isAccepted();
        this.version = owner.version;
        this.appointer = owner.appointer;
    }
    public String getCompanyName() {
        return CompanyName;
    }

    public int getVersion() {
        return version;
    }
    public String getAppointer()
    {
        return appointer;
    }


    public void setVersion(int version) {
        this.version = version;
    }

//    public void AddOwner(String ownerUsername) {
//        Owner newOwner = new Owner(ownerUsername, CompanyName);
//        owners.add(newOwner);
//    }
//    public void AddOwner(Owner owner) {
//        owners.add(owner);
//    }

    public void acceptAppointment() {
        if(isAccepted) {
            throw  new RuntimeException("You are already accepted2");
        }
        this.isAccepted = true;
    }
    public void DisacceptAppointment() {
        if(!isAccepted) {
            throw  new RuntimeException("You are already accepted2");
        }
        this.isAccepted = false;
    }

    public boolean isAccepted() {
        return isAccepted;
    }



    public String getUserName() {
        return userName;
    }
//
//    public List<Owner> getOwners() {
//        return owners;
//    }
//
//    public List<Manager> getManagers() {
//        return managers;
//    }
//    public boolean isManager(String managerUsername) {
//        for (Manager m : managers) {
//            if(m.getUserName().equals(managerUsername)) {
//                return true;
//            }
//        }
//        return false;
//    }

//    public void AddManager(String manager, Set<Permission> permissions) {
//        Manager manager1 = new Manager(manager, CompanyName, permissions);
//        managers.add(manager1);
//    }
//    public void AddManager(Manager manager) {
//        managers.add(manager);
//    }

//    public List<String> RemoveOwners() {
//        List<String> ownersNames = new ArrayList<>();
//        for (Owner owner : owners) {
//            ownersNames.add(owner.userName);
//        }
//        List<String> ownersChildernNames = new ArrayList<>();
//        for (Owner owner : owners) {
//            List<String> childern = owner.RemoveOwners();
//            ownersChildernNames.addAll(childern);
//        }
//        List<String> allChildernNames = new ArrayList<>();
//        allChildernNames.addAll(ownersChildernNames);
//        allChildernNames.addAll(ownersNames);
//        return allChildernNames;
//    }
//
//    public List<String> RemoveManagers() {
//        List<String> managersNames = new ArrayList<>();
//        for (Manager manager : managers) {
//            managersNames.add(manager.getUserName());
//        }
//        return managersNames;
//    }
}