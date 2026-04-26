package Appliction;

import Domain.Domains.OrderDomain;
import Domain.Order.ActiveOrder;
import Domain.Ticket.Ticket;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class OrderService {
    OrderDomain OrderDomain;
    public OrderService(OrderDomain OrderDomain) {
        this.OrderDomain = OrderDomain;
    }
    public String reserveTickets(String token, String company, String event, List<int[]> requests) {
        return OrderDomain.reserveTickets(token, company, event, requests);
    }
    public String getActiveOrderTickets(String token,String orderId) {
        return OrderDomain.getActiveOrderTickets(token, orderId);
    }

}
