package com.ticketing.ticketapp.Infastructure;

import com.ticketing.ticketapp.Domain.User.Suspension;
import com.ticketing.ticketapp.Domain.User.SuspensionKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JpaSuspensionRepository extends JpaRepository<Suspension, SuspensionKey> {
    Optional<Suspension> findByUserIDAndIsActiveTrue(String userID);
}
