package com.ticketing.ticketapp.Infastructure.DataBaseInterface;

import com.ticketing.ticketapp.Domain.Order.ActiveOrder;
import com.ticketing.ticketapp.Domain.Order.IActiveOrderRepository;
import com.ticketing.ticketapp.Infastructure.JpaOrderRepository;
import com.ticketing.ticketapp.Infastructure.JpaTicketRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Repository
@Primary
public class ActiveOrderRepositoryAdapter {

    private final JpaOrderRepository jpaOrderRepository;

    public ActiveOrderRepositoryAdapter(JpaOrderRepository jpaOrderRepository){
        this.jpaOrderRepository= jpaOrderRepository;
    }

    @Override
    @Transactional
    public String store(String companyId, String eventId, List<String> ticketIds, String userId, Date expirationTime){
        return "";
    }

    @Override
    @Transactional
    public String findById(String orderID){
        return "";
    }

    @Override
    @Transactional
    public String findByUserId(String userID){
        return "";
    }

    @Override
    @Transactional
    public String delete(String orderID){
        return "";
    }


}
