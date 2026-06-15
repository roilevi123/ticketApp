package com.ticketing.ticketapp.Infastructure;

import com.ticketing.ticketapp.Domain.Order.ActiveOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface JpaOrderRepository extends JpaRepository<ActiveOrder, String>{
    Optional<ActiveOrder> findByUserId(String userId);
}
