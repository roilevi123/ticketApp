package com.ticketing.ticketapp.Domain.OwnerManagerTree;

import java.util.HashSet;
import java.util.Set;

public class Manager {
    private String userID;
    private String companyName;
    private Set<Permission> permissions;
    private boolean isAccepted;
    private int version;
    private String appointerID;
    public Manager(String userID, String companyName, Set<Permission> permissions,String appointerID) {
        this.userID = userID;
        this.companyName = companyName;
        this.permissions = new HashSet<>(permissions);
        this.isAccepted = false;
        this.version = 0;
        this.appointerID = appointerID;
    }
    public Manager(Manager other) {
        this.userID = other.getUserID();
        this.companyName = other.getCompanyName();
        this.permissions = new HashSet<>(other.getPermissions());
        this.isAccepted = other.isAccepted();
        this.version = other.getVersion();
        this.appointerID = other.getAppointerID();
    }
    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }
    public String getAppointerID(){
        return appointerID;
    }
    public void acceptAppointment() {
        if (isAccepted) {
            throw new RuntimeException("Manager is already accepted");
        }
        this.isAccepted = true;
    }



    public String getUserID() {
        return userID;
    }

    public String getCompanyName() {
        return companyName;
    }



    public Set<Permission> getPermissions() {
        return permissions;
    }

    public boolean isAccepted() {
        return isAccepted;
    }

    public void setPermissions(Set<Permission> permissions) {
        this.permissions = permissions;
    }
}
