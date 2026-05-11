package com.ticketing.ticketapp.Infastructure;



import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.ticketing.ticketapp.Domain.Company.Company;
import com.ticketing.ticketapp.Domain.Company.iCompanyRepository;
import org.springframework.stereotype.Repository;

@Repository
public class CompanyRepositoryImpl implements iCompanyRepository {
    private Map<String, Company> companies = new ConcurrentHashMap<String, Company>();
    public CompanyRepositoryImpl() {

    }
    @Override
    public String getCompanyDescription(String company) {
        return companies.get(company).toString();
    }

    @Override
    public void store(String company, String founderID) {
        companies.put(company, new Company(company, founderID));

    }

    @Override
    public String getCompanyFounder(String company) {
        return companies.get(company).getFounderID();
    }
    @Override
    public void save(Company companyToUpdate) {
        String name = companyToUpdate.getCompanyName();
        Company currentInDb = companies.get(name);
        if (currentInDb == null) {
            throw new RuntimeException("Company not found for update: " + name);
        }

        Company updatedCompany = new Company(companyToUpdate);

        updatedCompany.setVersion(companyToUpdate.getVersion() + 1);

        boolean success = companies.replace(name, currentInDb, updatedCompany);

        if (!success) {
            throw new RuntimeException("Optimistic Lock Failure: Company '" + name +
                    "' was updated by another thread/user.");
        }
    }

    @Override
    public void deleteAllCompany() {
        companies.clear();
    }

    @Override
    public void deleteCompany(String company) {
        companies.remove(company);
    }

    @Override
    public Company getCompany(String company) {
        return companies.get(company);
    }

    @Override
    public boolean isCompanyActive(String company) {
        return companies.get(company).getActive();
    }
}
