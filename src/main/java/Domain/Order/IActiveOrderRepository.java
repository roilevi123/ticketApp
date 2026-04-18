package Domain.Order;

public interface IActiveOrderRepository {
    void save(ActiveOrder order);
    ActiveOrder findById(String orderId);
    void update(ActiveOrder order);
    void delete(String orderId);
    void deleteAllActiveOrders();
}