package com.ticketing.ticketapp.Domain.PurchasedOrderAggregate;

import java.util.List;

public class SalesReportDTO {
    private double totalRevenue;
    private int totalTicketsSold;
    private List<PurchaseOrderDTO> orders;

    public SalesReportDTO() {
    }

    public SalesReportDTO(double totalRevenue, int totalTicketsSold, List<PurchaseOrderDTO> orders) {
        this.totalRevenue = totalRevenue;
        this.totalTicketsSold = totalTicketsSold;
        this.orders = orders;
    }

    public double getTotalRevenue() { 
        return totalRevenue; 
    }

    public int getTotalTicketsSold() { 
        return totalTicketsSold; 
    }

    public List<PurchaseOrderDTO> getOrders() { 
        return orders; 
    }

    public void setTotalRevenue(double totalRevenue) { 
        this.totalRevenue = totalRevenue; 
    }

    public void setTotalTicketsSold(int totalTicketsSold) { 
        this.totalTicketsSold = totalTicketsSold; 
    }

    public void setOrders(List<PurchaseOrderDTO> orders) { 
        this.orders = orders; 
    }
}