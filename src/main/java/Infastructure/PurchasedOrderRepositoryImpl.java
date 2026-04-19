package Infastructure;

import Domain.Order.IPurchasedOrderRepository;
import Domain.Order.PurchasedOrder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PurchasedOrderRepositoryImpl implements IPurchasedOrderRepository {
    
    private Map<String, PurchasedOrder> db = new HashMap<>();

    @Override
    public void save(PurchasedOrder order) {
        db.put(order.getOrderId(), order);
    }

    @Override
    public PurchasedOrder findById(String orderId) {
        return db.get(orderId);
    }

    @Override
    public List<PurchasedOrder> getOrdersByCompany(String companyId) {
        return db.values().stream()
                .filter(order -> order.getCompanyId().equals(companyId))
                .collect(Collectors.toList());
    }

    @Override
    public List<PurchasedOrder> getOrdersByUser(String userId) {
        return db.values().stream()
                .filter(order -> order.getUserId().equals(userId))
                .collect(Collectors.toList());
    }
}