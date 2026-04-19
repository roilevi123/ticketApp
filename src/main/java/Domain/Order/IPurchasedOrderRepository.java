package Domain.Order;

import java.util.List; 

public interface IPurchasedOrderRepository {
    void save(PurchasedOrder order);
    PurchasedOrder findById(String orderId);
    
    List<PurchasedOrder> getOrdersByCompany(String companyId);
    List<PurchasedOrder> getOrdersByUser(String userId);
}