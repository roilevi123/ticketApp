package Infastructure;

import Domain.PurchasedOrderAggregate.PurchaseOrder;
import Domain.PurchasedOrderAggregate.iPurchasedOrderRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PurchasedOrderRepositoryImpl implements iPurchasedOrderRepository {
    private List<PurchaseOrder> purchasedOrders=new ArrayList<PurchaseOrder>();


    @Override
    public void StorePurchasedOrder(String company, String event, List<String> ticketsId, String buyer, String orderId) {
        PurchaseOrder order=new PurchaseOrder(company, event, ticketsId, buyer, orderId);
        purchasedOrders.add(order);

    }

//    @Override
//    public String GetPurchasedOrder(String orderId) {
//        PurchaseOrder order1=null;
//        for (PurchaseOrder order : purchasedOrders) {
//            if (order.getOrderId().equals(orderId)) {
//                order1=order;
//            }
//        }
//        return order1==null?"null":order1.getOrderId();
//    }

    @Override
    public List<PurchaseOrder> GetAllPurchasedOrders() {
        return purchasedOrders;
    }

    @Override
    public List<PurchaseOrder> getPurchasedOrdersForUser(String userName) {
        return purchasedOrders.stream()
                .filter(order -> order.getBuyer() != null && order.getBuyer().equals(userName))
                .collect(Collectors.toList());
    }

    @Override
    public List<PurchaseOrder> getPurchasedOrdersForCompany(String company) {
        return purchasedOrders.stream()
                .filter(order -> order.getCompany() != null && order.getCompany().equals(company))
                .collect(Collectors.toList());
    }

    @Override
    public void deleteAll() {
        purchasedOrders.clear();

    }

}
