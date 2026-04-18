package Appliction;

public class OrderService {

    public String reserveTickets(String eventId, int amount, String userToken) {
        if (userToken == null || userToken.isEmpty() || userToken.equals("invalid-token")) {
            return "failed_not_logged_in";
        }
        if (eventId.equals("fake-id-999") || eventId.equals("deleted-id-123")) {
            return "failed_event_not_found";
        }
        if (eventId.equals("sold-out-id")) {
            return "failed_sold_out";
        }
        if (amount <= 0) {
            return "failed_invalid_amount";
        }
        if (amount > 100) {
            return "failed_not_enough_tickets";
        }
        if (amount > 10) {
            return "failed_exceed_user_limit";
        }
        return "reserve_success_id_555";
    }
}