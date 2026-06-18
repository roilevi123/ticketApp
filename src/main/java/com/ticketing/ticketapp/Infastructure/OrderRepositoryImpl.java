package com.ticketing.ticketapp.Infastructure;

import com.ticketing.ticketapp.Domain.Order.ActiveOrder;
import com.ticketing.ticketapp.Domain.Order.IActiveOrderRepository;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

//@Repository
public class OrderRepositoryImpl  implements IActiveOrderRepository {
    ConcurrentHashMap<String, ActiveOrder> orders = new ConcurrentHashMap<>();
    private  AtomicLong idCounter = new AtomicLong(1);

    private boolean isExpired(ActiveOrder order) {
        return order != null
                && order.getExpirationTime() != null
                && !order.getExpirationTime().after(new Date());
    }

    @Override
    public String store(String company, String event, List<String> ticketsId, String buyerID, Date expiration) {
        for(ActiveOrder order : orders.values()) {
            if (order.getUserId() != null && order.getUserId().equals(buyerID)) {
                if (!isExpired(order)) {
                    throw new RuntimeException("you have already order");
                }
                else {
                    delete(order.getOrderId());
                    break;
                }
            }
        }
        String id=String.valueOf(idCounter.getAndIncrement());
        orders.put(id,new ActiveOrder(company,event,ticketsId,buyerID,id,expiration));
        return id;
    }

    @Override
    public void save(ActiveOrder orderToUpdate) {
        ActiveOrder currentInDb = orders.get(orderToUpdate.getOrderId());

        if (currentInDb == null) {
            throw new RuntimeException("Order not found for update");
        }

        if (currentInDb.getVersion() != orderToUpdate.getVersion()) {
            throw new RuntimeException("Optimistic Lock Failure: Order was updated by another thread");
        }

        ActiveOrder updatedOrder = new ActiveOrder(orderToUpdate);
        updatedOrder.SetVersion(orderToUpdate.getVersion() + 1);

        orders.put(updatedOrder.getOrderId(), updatedOrder);
    }

    @Override
    public ActiveOrder findById(String orderId) {
        if(orderId == null) {
            return null;
        }
        return orders.get(orderId);
    }

    @Override
    public void update(ActiveOrder order) {

    }

    @Override
    public void delete(String orderId) {
        orders.remove(orderId);
    }

    @Override
    public void deleteAllActiveOrders() {
        orders.clear();
    }
    @Override
    public List<String> getTicketsId(String userID) {
        for(ActiveOrder order : orders.values()) {
            if (order.getUserId() != null && order.getUserId().equals(userID)) {
                return order.getTicketIds();
            }
        }
        return new ArrayList<>();
    }
    @Override
    public ActiveOrder getOrder(String userID) {
        for(ActiveOrder order : orders.values()) {
            if (order.getUserId() != null && order.getUserId().equals(userID)) {
                return order;
            }
        }
        return null;
    }

    @Override
    public List<ActiveOrder> getAllActiveOrders() {
        return new ArrayList<>(orders.values());
    }

}
