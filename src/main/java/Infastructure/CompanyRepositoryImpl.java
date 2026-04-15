package Infastructure;



import Domain.Company.Company;
import Domain.Company.iCompanyRepository;

import java.util.HashMap;
import java.util.Map;

public class CompanyRepositoryImpl implements iCompanyRepository {
    private Map<String, Company> companies = new HashMap<>();
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


}
