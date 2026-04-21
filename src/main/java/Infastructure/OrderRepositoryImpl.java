package Infastructure;

import Domain.Order.ActiveOrder;
import Domain.Order.IActiveOrderRepository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class OrderRepositoryImpl  implements IActiveOrderRepository {
    ConcurrentHashMap<String, ActiveOrder> orders = new ConcurrentHashMap<>();
    private  AtomicLong idCounter = new AtomicLong(1);

    @Override
    public String store(String company, String event, List<String> ticketsId, String buyer, Date expiration) {
        for(ActiveOrder order : orders.values()) {
            if (order.getUserId() != null && order.getUserId().equals(buyer)) {
                if (order.getExpirationTime().after(new Date())) {
                    throw new RuntimeException("you have already order");
                }
                else {
                    delete(buyer);
                    break;
                }
            }
        }
        String id=String.valueOf(idCounter.getAndIncrement());
        orders.put(id,new ActiveOrder(company,event,ticketsId,buyer,id,expiration));
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
    public List<String> getTicketsId(String userId) {
        for(ActiveOrder order : orders.values()) {
            if (order.getUserId() != null && order.getUserId().equals(userId)) {
                return order.getTicketIds();
            }
        }
        return new ArrayList<>();
    }
    @Override
    public ActiveOrder getOrder(String username) {
        for(ActiveOrder order : orders.values()) {
            if (order.getUserId() != null && order.getUserId().equals(username)) {
                return order;
            }
        }
        return null;
    }

}
