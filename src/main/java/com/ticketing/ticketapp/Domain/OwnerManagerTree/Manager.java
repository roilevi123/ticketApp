package com.ticketing.ticketapp.Domain.OwnerManagerTree;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "managers")
@IdClass(RoleKey.class)
public class Manager {

    @Id
    @Column(name = "user_id")
    private String userID;

    @Id
    @Column(name = "company_name")
    private String companyName;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "manager_permissions",
        joinColumns = {
            @JoinColumn(name = "user_id",      referencedColumnName = "user_id"),
            @JoinColumn(name = "company_name", referencedColumnName = "company_name")
        }
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "permission")
    private Set<Permission> permissions;

    @Column(name = "is_accepted")
    private boolean isAccepted;

    @Version
    @Column(name = "version")
    private int version;

    @Column(name = "appointer_id")
    private String appointerID;

    protected Manager() {}

    public Manager(String userID, String companyName, Set<Permission> permissions, String appointerID) {
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

    public String getAppointerID() {
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
