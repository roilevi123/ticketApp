package com.ticketing.ticketapp.Domain.Ticket;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "tickets")
public class Ticket {

    @Column(name = "ticket_row")
    private int row;

    @Column(name = "ticket_col")
    private int col;

    @Column(name = "event_name", nullable = false)
    private String event;

    @Column(name = "company_name", nullable = false)
    private String company;

    @Column(name = "is_purchased")
    private boolean isPurchased;

    @Version
    @Column(name = "version")
    private int version;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "reservation_expiry")
    private Date date;

    @Id
    @Column(name = "ticket_id")
    private String id;

    @Column(name = "price")
    private double price;

    protected Ticket() {}

    public Ticket(int row, int col, String event, String company, String id, double price) {
        this.row = row;
        this.col = col;
        this.event = event;
        this.company = company;
        this.id = id;
        this.price = price;
        this.isPurchased = false;
        this.version = 1;
    }

    public Ticket(Ticket ticket) {
        this.row = ticket.getRow();
        this.col = ticket.getCol();
        this.event = ticket.getEvent();
        this.company = ticket.getCompany();
        this.isPurchased = ticket.isPurchased();
        this.version = ticket.getVersion();
        this.date = ticket.getDate();
        this.id = ticket.getId();
        this.price = ticket.getPrice();
    }

    public int getRow() {
        return row;
    }


    public int getCol() {
        return col;
    }


    public boolean isPurchased() {
        return isPurchased;
    }

    public void purchase() {
        this.isPurchased = true;
    }

    public void cancelPurchase() {
        this.isPurchased = false;
    }


    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public String getCompany() {
        return company;
    }

    public int getVersion() {
        return version;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getId() {
        return id;
    }

    public double getPrice() {
        return price;
    }


    public void setPrice(double price) {
        this.price = price;
    }
    @Override
    public String toString() {
        return "Ticket {" +
                ", event='" + event + '\'' +
                ", company='" + company + '\'' +
                ", verticalSpote=" + row +
                ", horizontalSpote=" + col +
                ", Price=" + price +
                ", Date=" + (date != null ? date : "N/A") +
                '}';
    }
}
