package Domain.OwnerManagerTree;

import java.util.HashSet;
import java.util.Set;

public class Manager {
    private String userName;
    private String companyName;
    private Set<Permission> permissions;
    private boolean isAccepted;
    private int version;
    private String appointer;
    public Manager(String userName, String companyName, Set<Permission> permissions,String appointer) {
        this.userName = userName;
        this.companyName = companyName;
        this.permissions = new HashSet<>(permissions);
        this.isAccepted = false;
        this.version = 0;
        this.appointer = appointer;
    }
    public Manager(Manager other) {
        this.userName = other.getUserName();
        this.companyName = other.getCompanyName();
        this.permissions = new HashSet<>(other.getPermissions());
        this.isAccepted = other.isAccepted();
        this.version = other.getVersion();
        this.appointer = other.getAppointer();
    }
    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }
    public String getAppointer(){
        return appointer;
    }
    public void acceptAppointment() {
        if (isAccepted) {
            throw new RuntimeException("Manager is already accepted");
        }
        this.isAccepted = true;
    }
    public void DisacceptAppointment() {
        if(!isAccepted) {
            throw  new RuntimeException("You are already accepted2");
        }
        this.isAccepted = false;
    }

    public boolean hasPermission(Permission permission) {
        return isAccepted && permissions.contains(permission);
    }

    public String getUserName() {
        return userName;
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