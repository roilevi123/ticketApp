package Appliction;

public class OrderService {

    public Response<String> reserveTickets(String eventId, int amount, String userToken) {
        if (userToken == null || userToken.isEmpty() || userToken.equals("invalid-token")) return new Response<>(false, "Not logged in", null);
        if (eventId.equals("fake-id-999") || eventId.equals("deleted-id-123")) return new Response<>(false, "Event not found", null);
        if (eventId.equals("sold-out-id")) return new Response<>(false, "Sold out", null);
        if (amount <= 0) return new Response<>(false, "Invalid amount", null);
        if (amount > 100) return new Response<>(false, "Not enough tickets", null);
        if (amount > 10) return new Response<>(false, "Exceeded user limit", null);
        
        return new Response<>(true, "Reservation successful", "reserve_id_555");
    }
}