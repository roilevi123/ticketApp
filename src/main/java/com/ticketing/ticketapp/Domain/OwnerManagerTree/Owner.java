package com.ticketing.ticketapp.Domain.OwnerManagerTree;

import jakarta.persistence.*;

@Entity
@Table(name = "owners")
@IdClass(RoleKey.class)
public class Owner {

    @Id
    @Column(name = "user_id")
    private String userID;

    @Id
    @Column(name = "company_name")
    private String companyName;

    @Column(name = "is_accepted")
    private boolean isAccepted;

    @Version
    @Column(name = "version")
    private int version;

    @Column(name = "appointer_id")
    private String appointerID;

    protected Owner() {}

    public Owner(String userID, String companyName, String appointerID) {
        this.userID = userID;
        this.companyName = companyName;
        this.isAccepted = false;
        this.version = 0;
        this.appointerID = appointerID;
    }

    public Owner(Owner owner) {
        this.userID = owner.getUserID();
        this.companyName = owner.getCompanyName();
        this.isAccepted = owner.isAccepted();
        this.version = owner.version;
        this.appointerID = owner.getAppointerID();
    }

    public String getCompanyName() {
        return companyName;
    }

    public int getVersion() {
        return version;
    }

    public String getAppointerID() {
        return appointerID;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public void acceptAppointment() {
        if (isAccepted) {
            throw new RuntimeException("You are already accepted");
        }
        this.isAccepted = true;
    }

    public boolean isAccepted() {
        return isAccepted;
    }

    public String getUserID() {
        return userID;
    }
}
