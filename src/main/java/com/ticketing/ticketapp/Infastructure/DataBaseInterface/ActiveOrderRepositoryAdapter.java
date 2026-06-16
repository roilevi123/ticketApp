package com.ticketing.ticketapp.Infastructure.DataBaseInterface;

import com.ticketing.ticketapp.Domain.Order.ActiveOrder;
import com.ticketing.ticketapp.Domain.Order.IActiveOrderRepository;
import com.ticketing.ticketapp.Infastructure.JpaOrderRepository;
import com.ticketing.ticketapp.Infastructure.JpaTicketRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;


import java.util.*;

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
        if(activeOrder==null || activeOrder.getOrderId()==null || activeOrder.getTicketIds()==null || activeOrder.getTicketIds().isEmpty()|| activeOrder.getCompanyId()==null || activeOrder.getEventId()==null)
            throw new RuntimeException("Order cannot be saved without the relevant info");
        jpaOrderRepository.save(activeOrder);
    }

    @Override
    @Transactional
    public String store(String companyId, String eventId, List<String> ticketIds, String userId, Date expirationTime){
        if(companyId==null || eventId==null || ticketIds==null ||ticketIds.isEmpty())
            throw new RuntimeException("Order cannot be stored without the relevant info");
        String id = UUID.randomUUID().toString();
        jpaOrderRepository.save(new ActiveOrder(companyId, eventId, ticketIds, userId,id, expirationTime));
        return id;
    }

    @Override
    @Transactional(readOnly = true)
    public ActiveOrder findById(String orderID){
        return jpaOrderRepository.findById(orderID).orElse(null);
    }

    @Override
    @Transactional
    public void update (ActiveOrder activeOrder){
        if(activeOrder==null || activeOrder.getOrderId()==null)
            throw new RuntimeException("Cannot update an order without id");
        jpaOrderRepository.save(activeOrder);
    }

    @Override
    @Transactional
    public void delete(String orderID){
        jpaOrderRepository.deleteById(orderID);
        jpaOrderRepository.flush();
    }

    @Override
    @Transactional
    public void deleteAllActiveOrders(){
        jpaOrderRepository.deleteAll();
        jpaOrderRepository.flush();
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getTicketsId(String userID){
        ActiveOrder order = jpaOrderRepository.findByUserId(userID);
        if(order==null)
            return new LinkedList<>();
        return order.getTicketIds();
    }

    @Override
    @Transactional(readOnly = true)
    public ActiveOrder getOrder(String userID){
        return jpaOrderRepository.findByUserId(userID);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActiveOrder> getAllActiveOrders(){
        return jpaOrderRepository.findAll();
    }


}
