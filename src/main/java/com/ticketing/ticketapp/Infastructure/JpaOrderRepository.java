package com.ticketing.ticketapp.Infastructure;

import com.ticketing.ticketapp.Domain.Order.ActiveOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JpaOrderRepository extends JpaRepository<ActiveOrder, String>{
   Optional<ActiveOrder> findFirstByUserId(String userId);
    List<ActiveOrder> findByExpirationTimeBefore(java.util.Date now);

}
