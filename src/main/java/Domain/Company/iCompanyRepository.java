package Domain.Company;

public interface iCompanyRepository {
    public void store(String company, String founder);
    public String getCompanyFounder(String company);
    public Company getCompany(String company);
    public void save(Company companyToUpdate);
    public void deleteAllCompany();
    public void deleteCompany(String company);
    public boolean isCompanyActive(String company);
    public String getCompanyDescription(String company);
}
