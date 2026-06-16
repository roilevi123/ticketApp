package com.ticketing.ticketapp.Domain.Company;

import jakarta.persistence.*;

@Entity
@Table(name = "companies")
public class Company {

    @Id
    @Column(name = "company_name")
    private String companyName;

    @Column(name = "founder_id", nullable = false)
    private String founderID;

    @Column(name = "active", nullable = false)
    private boolean active=true;

    @Version
    @Column(name = "version")
    private int version;

    @Column(name = "rating")
    private double rating;

    protected Company() {}

    public Company(String companyName, String founderID) {
        this.companyName = companyName;

        this.founderID = founderID;
        this.version = 0;
        this.rating = 0;
    }
    public Company(Company other) {
        this.companyName = other.getCompanyName();
        this.founderID = other.getFounderID();

        this.active=other.getActive();
        this.version=other.getVersion();
        this.rating=other.getRating();
    }

    public void freezeCompany(String userID) {
        if(!founderID.equals(userID)) {
            throw new CompanyDomainException("this is not the founder so we don't freeze the company");


        }
        if(!active) {
            throw new CompanyDomainException("already frozen the company");
        }
        active=false;
    }
    public void unfreezeCompany(String userID) {
        if(!founderID.equals(userID)) {
            throw new CompanyDomainException("this is not the founder so we don't unfreeze the company");
        }
        if(active) {
            throw new CompanyDomainException("already unfreeze the company");
        }
        active=true;
    }

    public String getCompanyName() {
        return companyName;
    }
    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }
    public String getFounderID() {
        return founderID;
    }
    public boolean getActive() {
        return active;
    }
    public void setActive(boolean active) {
        this.active = active;
    }
    public int getVersion() {
        return version;
    }
    public void setVersion(int version) {
        this.version = version;
    }
    public double getRating() {
        return rating;
    }
    public void setRating(double rating) {
        this.rating = rating;
    }

    @Override
    public String toString() {
        return "Company Summary:" +
                "\nName: " + companyName +
                "\nFounder/Owner ID: " + founderID +
                "\nStatus: " + (active ? "Active" : "Frozen") +
                "\nRating: " + rating;
    }

}
