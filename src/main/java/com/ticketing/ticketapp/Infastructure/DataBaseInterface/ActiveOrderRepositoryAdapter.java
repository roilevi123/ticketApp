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
public class ActiveOrderRepositoryAdapter implements IActiveOrderRepository {

    private final JpaOrderRepository jpaOrderRepository;

    public ActiveOrderRepositoryAdapter(JpaOrderRepository jpaOrderRepository){
        this.jpaOrderRepository= jpaOrderRepository;
    }

    @Override
    @Transactional
    public void save(ActiveOrder activeOrder){

    }

    @Override
    @Transactional
    public String store(String companyId, String eventId, List<String> ticketIds, String userId, Date expirationTime){
        return "";
    }

    @Override
    @Transactional
    public ActiveOrder findById(String orderID){
        return null;
    }

    @Override
    @Transactional
    public void update (ActiveOrder activeOrder){

    }

    @Override
    @Transactional
    public void delete(String orderID){

    }

    @Override
    @Transactional
    public void deleteAllActiveOrders(){

    }

    @Override
    @Transactional
    public List<String> getTicketsId(String userID){
        return null;
    }

    @Override
    @Transactional
    public ActiveOrder getOrder(String userID){
        return null;
    }

    @Override
    @Transactional
    public List<ActiveOrder> getAllActiveOrders(){
        return null;
    }


}
