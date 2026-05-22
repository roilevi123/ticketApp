package com.ticketing.ticketapp.Appliction;

import com.ticketing.ticketapp.Domain.Order.ActiveOrder;
import com.ticketing.ticketapp.Domain.Order.IActiveOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReservationExpirationScheduler {

    private static final Logger logger = LoggerFactory.getLogger(ReservationExpirationScheduler.class);
    private static final long WARNING_THRESHOLD_MS = 2 * 60 * 1000; // warn 2 minutes before expiry

    private final IActiveOrderRepository activeOrderRepository;
    private final INotifier notifier;

    public ReservationExpirationScheduler(IActiveOrderRepository activeOrderRepository, INotifier notifier) {
        this.activeOrderRepository = activeOrderRepository;
        this.notifier = notifier;
    }

    @Scheduled(fixedRate = 30_000)
    public void warnExpiringReservations() {
        long now = System.currentTimeMillis();
        for (ActiveOrder order : activeOrderRepository.getAllActiveOrders()) {
            if (order.getUserId() == null || order.getExpirationTime() == null) continue;
            long msUntilExpiry = order.getExpirationTime().getTime() - now;
            if (msUntilExpiry > 0 && msUntilExpiry <= WARNING_THRESHOLD_MS) {
                long secondsLeft = msUntilExpiry / 1000;
                logger.info("Sending expiry warning to user {} for order {}", order.getUserId(), order.getOrderId());
                notifier.notifyUser(order.getUserId(), "Reservation Expiring Soon",
                        "Your ticket reservation expires in " + secondsLeft + " seconds. Complete your purchase now!");
            }
        }
    }
}
