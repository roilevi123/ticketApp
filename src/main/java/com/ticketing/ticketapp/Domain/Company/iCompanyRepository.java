package com.ticketing.ticketapp.Domain.Company;

import java.util.List;

public interface iCompanyRepository {
    public void store(String company, String founderID);
    public String getCompanyFounder(String company);
    public Company getCompany(String company);
    public void save(Company companyToUpdate);
    public void deleteAllCompany();
    public void deleteCompany(String company);
    public boolean isCompanyActive(String company);
    public String getCompanyDescription(String company);
    public List<Company> getActiveCompanies();
}
