package Domain.Ticket;

import java.util.Date;

public class Ticket {
    private int row;
    private int col;
    private String event;
    private String company;
    private boolean isPurchased;
    private int version;
    private Date date;
    private String id;
    private double price; 

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

    public void setRow(int row) {
        this.row = row;
    }

    public int getCol() {
        return col;
    }

    public void setCol(int col) {
        this.col = col;
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
                ", Event='" + event + '\'' +
                ", Company='" + company + '\'' +
                ", Row=" + row +
                ", Col=" + col +
                ", Price=" + price +
                ", Date=" + (date != null ? date : "N/A") +
                '}';
    }
}
