package Domain.PurchasedOrderAggregate;

import Domain.Ticket.TicketDTO;
import java.util.List;

public record PurchaseOrderDTO(
        String orderId,
        String buyer,
        String company,
        String event,
        List<TicketDTO> tickets
) {

    public static PurchaseOrderDTO create(PurchaseOrder order, List<TicketDTO> ticketDTOs) {
        return new PurchaseOrderDTO(
                order.getOrderId(),
                order.getBuyer(),
                order.getCompany(),
                order.getEvent(),
                ticketDTOs
        );
    }
}