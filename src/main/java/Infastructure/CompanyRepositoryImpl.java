package Infastructure;



import Domain.Company.Company;
import Domain.Company.iCompanyRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CompanyRepositoryImpl implements iCompanyRepository {
    private Map<String, Company> companies = new ConcurrentHashMap<String, Company>();
    public CompanyRepositoryImpl() {

    }


    @Override
    public void store(String company, String founder) {
        companies.put(company, new Company(company, founder));

    }

    @Override
    public String getCompanyFounder(String company) {
        return companies.get(company).getFounder();
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
    public Company getCompany(String company) {
        return companies.get(company);
    }


}
