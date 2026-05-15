package com.ticketing.ticketapp.Domain.Company;



public class Company {

    private String companyName;
    private String founderID;
    private boolean active=true;
    private int version;
    private double rating;
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
            throw new RuntimeException("this is not the founder so we don't freeze the company");


        }
        if(!active) {
            throw new RuntimeException("already frozen the company");
        }
        active=false;
    }
    public void unfreezeCompany(String userID) {
        if(!founderID.equals(userID)) {
            throw new RuntimeException("this is not the founder so we don't unfreeze the company");
        }
        if(active) {
            throw new RuntimeException("already unfreeze the company");
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
