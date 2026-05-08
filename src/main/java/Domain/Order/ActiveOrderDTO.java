package Domain.Order;

import Domain.Ticket.TicketDTO;
import java.util.Date;
import java.util.List;

public record ActiveOrderDTO(
        String orderId,
        String userId,
        String eventId,
        String companyId,
        List<TicketDTO> tickets,
        Date expirationTime
) {
    public static ActiveOrderDTO create(ActiveOrder order, List<TicketDTO> ticketDTOs) {
        return new ActiveOrderDTO(
                order.getOrderId(),
                order.getUserId(),
                order.getEventId(),
                order.getCompanyId(),
                ticketDTOs,
                order.getExpirationTime()
        );
    }
}