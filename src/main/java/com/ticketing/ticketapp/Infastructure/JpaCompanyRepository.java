package com.ticketing.ticketapp.Infastructure;

import com.ticketing.ticketapp.Domain.Company.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JpaCompanyRepository extends JpaRepository<Company, String> {
    List<Company> findByActiveTrue();
}
