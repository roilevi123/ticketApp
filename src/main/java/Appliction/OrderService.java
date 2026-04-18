package Appliction;

import Domain.Event.Event;
import Domain.Event.IEventRepository;
import Domain.Order.ActiveOrder;
import Domain.Order.IActiveOrderRepository;
import java.util.UUID;

public class OrderService {
    
    private IEventRepository eventRepository;
    private IActiveOrderRepository activeOrderRepository;

    public OrderService(IEventRepository eventRepository, IActiveOrderRepository activeOrderRepository) {
        this.eventRepository = eventRepository;
        this.activeOrderRepository = activeOrderRepository;
    }

    public String reserveTickets(String userId, String eventId, int amount) {
        Event event = eventRepository.findById(eventId);
        if (event == null) {
            return "Error: Event not found";
        }
        // reserve tickets
        boolean isReserved = event.reserveTickets(amount);
        if (!isReserved) {
            return "Error: Not enough tickets available";
        }
        // update event with new available tickets
        eventRepository.update(event);

        // create active order
        String orderId = UUID.randomUUID().toString();
        ActiveOrder activeOrder = new ActiveOrder(orderId, userId, eventId, 10);
        for (int i = 0; i < amount; i++) {
            // generate unique ticket ID for each reserved ticket
            String ticketId = "TICKET-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            activeOrder.addTicket(ticketId);
        }
        activeOrderRepository.save(activeOrder);
        return orderId;
    }
}