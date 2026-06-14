package com.ticketing.ticketapp.Infastructure;

import com.ticketing.ticketapp.Domain.Event.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface JpaEventRepository extends JpaRepository<Event, String> {
    Optional<Event> findByNameAndCompanyName(String name, String companyName);
    List<Event> findByCompanyName(String companyName);
    boolean existsByNameAndCompanyName(String name, String companyName);

    @Transactional
    @Modifying
    void deleteByCompanyName(String companyName);
}
