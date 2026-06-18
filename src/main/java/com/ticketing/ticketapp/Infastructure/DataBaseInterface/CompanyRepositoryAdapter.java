package com.ticketing.ticketapp.Infastructure.DataBaseInterface;

import com.ticketing.ticketapp.Domain.Company.Company;
import com.ticketing.ticketapp.Domain.Company.CompanyDomainException;
import com.ticketing.ticketapp.Domain.Company.iCompanyRepository;
import com.ticketing.ticketapp.Infastructure.JpaCompanyRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
@Repository
@ConditionalOnProperty(name = "repository.type", havingValue = "DB")
public class CompanyRepositoryAdapter implements iCompanyRepository {

    private final JpaCompanyRepository jpaCompanyRepository;

    public CompanyRepositoryAdapter(JpaCompanyRepository jpaCompanyRepository) {
        this.jpaCompanyRepository = jpaCompanyRepository;
    }

    @Override
    public void store(String company, String founderID) {
        if (jpaCompanyRepository.existsById(company)) {
            throw new CompanyDomainException("Company already exists: " + company);
        }
        jpaCompanyRepository.saveAndFlush(new Company(company, founderID));
    }

    public String getCompanyFounder(String company) {
        return jpaCompanyRepository.findById(company)
                .map(Company::getFounderID)
                .orElseThrow(() -> new CompanyDomainException("Company not found: " + company));
    }

    @Override
    public Company getCompany(String company) {
        return jpaCompanyRepository.findById(company).orElse(null);
    }

    @Override
    public void save(Company companyToUpdate) {
        // @Version on Company.version handles optimistic locking automatically
        jpaCompanyRepository.save(companyToUpdate);
    }

    @Override
    public void deleteAllCompany() {
        jpaCompanyRepository.deleteAll();
        jpaCompanyRepository.flush();
    }

    @Override
    public void deleteCompany(String company) {
        jpaCompanyRepository.deleteById(company);
        jpaCompanyRepository.flush();
    }

    @Override
    public boolean isCompanyActive(String company) {
        return jpaCompanyRepository.findById(company)
                .map(Company::getActive)
                .orElse(false);
    }

    @Override
    public String getCompanyDescription(String company) {
        return jpaCompanyRepository.findById(company)
                .map(Company::toString)
                .orElse("");
    }

    @Override
    public List<Company> getActiveCompanies() {
        return jpaCompanyRepository.findByActiveTrue();
    }
}
