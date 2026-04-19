package Appliction;

public interface ISupplyService {
    //send the ticket to the user by email
    boolean supplyToEmail(String emailAddress, String content);
    
    boolean isAvailable();
}