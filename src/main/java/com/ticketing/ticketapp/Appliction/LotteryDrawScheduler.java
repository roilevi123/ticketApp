package com.ticketing.ticketapp.Appliction;

import com.ticketing.ticketapp.Domain.Lottery.ILotteryRepository;
import com.ticketing.ticketapp.Domain.Lottery.LotteryRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Periodically scans for lotteries whose registration window has closed and
 * triggers the draw for any that have not yet been processed.
 *
 * <p>Runs every 60 seconds. Spring's {@code @EnableScheduling} is already
 * active on {@code TicketappApplication}.
 */
@Component
public class LotteryDrawScheduler {

    private static final Logger logger = LoggerFactory.getLogger(LotteryDrawScheduler.class);

    private final ILotteryRepository lotteryRepository;
    private final LotteryService lotteryService;

    public LotteryDrawScheduler(ILotteryRepository lotteryRepository, LotteryService lotteryService) {
        this.lotteryRepository = lotteryRepository;
        this.lotteryService = lotteryService;
    }

    @Scheduled(fixedRate = 60_000)
    public void drawExpiredLotteries() {
        List<LotteryRegistration> expired = lotteryRepository.findClosedUndrawn();
        if (expired.isEmpty()) return;

        logger.info("LotteryDrawScheduler: {} closed lottery/lotteries ready to draw", expired.size());

        for (LotteryRegistration lr : expired) {
            try {
                logger.info("Drawing lottery for event '{}' / '{}'",
                        lr.getEventName(), lr.getCompanyName());
                lotteryService.performDraw(lr);
            } catch (Exception e) {
                logger.error("Error drawing lottery for event '{}' / '{}': {}",
                        lr.getEventName(), lr.getCompanyName(), e.getMessage());
            }
        }
    }
}
