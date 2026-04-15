package Domain.Company;

public interface iCompanyRepository {
    public void store(String company, String founder);
    public String getCompanyFounder(String company);

}
