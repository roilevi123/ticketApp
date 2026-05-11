package com.ticketing.ticketapp.Domain.OwnerManagerTree;


public class Owner {
    private String userID;
    private String CompanyName;
    private boolean isAccepted;
    private int version;
    private String appointerID;
    public Owner(String userID, String companyName,String appointerID) {
        this.userID = userID;
        this.CompanyName = companyName;

        this.isAccepted = false;
        this.version = 0;
        this.appointerID = appointerID  ;
    }
    public Owner(Owner owner) {
        this.userID = owner.getUserID();
        this.CompanyName = owner.getCompanyName();

        this.isAccepted = owner.isAccepted();
        this.version = owner.version;
        this.appointerID = owner.getAppointerID();
    }
    public String getCompanyName() {
        return CompanyName;
    }

    public int getVersion() {
        return version;
    }
    public String getAppointerID()
    {
        return appointerID;
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
            throw  new RuntimeException("You are already accepted");
        }
        this.isAccepted = true;
    }
    public void rejectAppointment() {
        if(!isAccepted) {
            throw  new RuntimeException("You are already rejected");
        }
        this.isAccepted = false;
    }

    public boolean isAccepted() {
        return isAccepted;
    }



    public String getUserID() {
        return userID;
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
