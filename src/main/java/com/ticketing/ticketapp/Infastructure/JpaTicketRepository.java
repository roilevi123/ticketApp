package com.ticketing.ticketapp.Infastructure;

import com.ticketing.ticketapp.Domain.Ticket.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface JpaTicketRepository extends JpaRepository<Ticket, String> {

    List<Ticket> findByEventAndCompany(String event, String company);

    @Query("SELECT t FROM Ticket t WHERE t.event = :event AND t.company = :company AND t.isPurchased = false")
    List<Ticket> findAvailableByEventAndCompany(@Param("event") String event, @Param("company") String company);

    List<Ticket> findByCompany(String company);

    @Transactional
    @Modifying
    void deleteByEventAndCompany(String event, String company);
}
