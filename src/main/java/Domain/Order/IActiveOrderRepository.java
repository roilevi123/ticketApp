package Domain.Order;

import java.util.Date;
import java.util.List;

public interface IActiveOrderRepository {
    void save(ActiveOrder order);
    String store(String company, String event, List<String> tickets,String username, Date expiryDate);
    ActiveOrder findById(String orderId);
    void update(ActiveOrder order);
    void delete(String orderId);
    void deleteAllActiveOrders();
    public List<String> getTicketsId(String userId);

}