package Domain.Company;



public class Company {

    private String companyName;
    private String founder;
    private boolean active=true;
    private int version;
    private double rating;
    public Company(String companyName, String founder) {
        this.companyName = companyName;

        this.founder = founder;
        this.version = 0;
        this.rating = 0;
    }
    public Company(Company other) {
        this.companyName = other.getCompanyName();
        this.founder = other.getFounder();

        this.active=other.getActive();
        this.version=other.getVersion();
    }

    public void freezeCompany(String username) {
        if(!founder.equals(username)) {
            throw new RuntimeException("this is not the founder so we don't freeze the company");


        }
        if(!active) {
            throw new RuntimeException("already frozen the company");
        }
        active=false;
    }
    public void unfreezeCompany(String username) {
        if(!founder.equals( username)) {
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
    public String getFounder() {
        return founder;
    }
    public void setFounder(String founder) {
        this.founder = founder;
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
            "\nFounder/Owner: " + founder +
            "\nStatus: " + (active ? "Active" : "Frozen") +
            "\nRating: " + rating;
}

}
