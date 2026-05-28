package com.ticketing.ticketapp.Appliction;
import com.ticketing.ticketapp.Domain.Event.Event;
import com.ticketing.ticketapp.Domain.Event.iEventRepository;
import com.ticketing.ticketapp.Domain.Lottery.ILotteryCodeRepository;
import com.ticketing.ticketapp.Domain.Lottery.ILotteryRepository;
import com.ticketing.ticketapp.Domain.Lottery.LotteryCode;
import com.ticketing.ticketapp.Domain.Lottery.LotteryRegistration;
import com.ticketing.ticketapp.Domain.User.IUserRepository;
import com.ticketing.ticketapp.Infastructure.TokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LotteryService {

    private static final Logger logger = LoggerFactory.getLogger(LotteryService.class);

    /** Winners have 48 hours to use their purchase code. */
    private static final long CODE_VALIDITY_MS = 48L * 60 * 60 * 1000;

    private final ILotteryRepository lotteryRepository;
    private final ILotteryCodeRepository lotteryCodeRepository;
    private final iEventRepository eventRepository;
    private final TokenService tokenService;
    private final INotifier notifier;
    private final IUserRepository userRepository;

    public LotteryService(ILotteryRepository lotteryRepository,ILotteryCodeRepository lotteryCodeRepository,iEventRepository eventRepository,TokenService tokenService,INotifier notifier, IUserRepository userRepository) {
        this.lotteryRepository = lotteryRepository;
        this.lotteryCodeRepository = lotteryCodeRepository;
        this.eventRepository = eventRepository;
        this.tokenService = tokenService;
        this.notifier = notifier;
        this.userRepository=userRepository;
    }

    /**
     * Configures (or updates) the lottery for an event.
     * Only event organizers with a valid token may call this.
     */
    public Response<String> configureLottery(String token, String companyName, String eventName, Date startDate, Date endDate, int maxWinners) {
        try {
            if (!tokenService.validateToken(token)) {
                return Response.error("Invalid token");
            }
            String userID = tokenService.extractUserId(token);
            if(userRepository.isUserSuspendedNow(userID))
                throw new RuntimeException("User is suspended");
            Event event = eventRepository.getEvent(eventName, companyName);
            if (event == null) {
                return Response.error("Event not found: " + eventName);
            }
            if (maxWinners <= 0) {
                return Response.error("maxWinners must be a positive number");
            }
            if (endDate == null || endDate.before(new Date())) {
                return Response.error("Lottery end date must be in the future");
            }

            // Persist lottery configuration
            lotteryRepository.configure(eventName, companyName, startDate, endDate, maxWinners);

            // Mark the event as high-demand so the frontend and EventDTO reflect this
            event.setHighDemand(true);
            event.setLotteryEndDate(endDate);
            event.setLotteryMaxWinners(maxWinners);
            eventRepository.save(event);

            logger.info("Lottery configured for event '{}' / company '{}' - endDate={}, maxWinners={}",
                    eventName, companyName, endDate, maxWinners);
            return Response.success("Lottery configured successfully");
        } catch (Exception e) {
            logger.error("Failed to configure lottery for event '{}': {}", eventName, e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    /**
     * Registers the authenticated user for the lottery of the given event.
     * Guests (non-members) are rejected.
     */
    public Response<String> registerForLottery(String token, String companyName, String eventName) {
        try {
            if (!tokenService.validateToken(token)) {
                return Response.error("Invalid token - please log in");
            }
            String userId = tokenService.extractUserId(token);
            if(userRepository.isUserSuspendedNow(userId))
                throw new RuntimeException("User is suspended");
            if (userId == null) {
                return Response.error("Could not resolve user from token");
            }

            if (!lotteryRepository.exists(eventName, companyName)) {
                return Response.error("No lottery is configured for this event");
            }

            boolean added = lotteryRepository.register(eventName, companyName, userId);
            if (!added) {
                return Response.error("You are already registered for this lottery");
            }

            logger.info("User '{}' registered for the lottery of event '{}' / '{}'",
                    userId, eventName, companyName);
            return Response.success("Successfully registered for the lottery! " +
                    "Winners will be notified after the draw.");
        } catch (Exception e) {
            logger.error("Failed to register for lottery '{}': {}", eventName, e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    /**
     * Returns the current lottery status for an event, including whether the
     * authenticated user has registered or won.
     */
    public Response<Map<String, Object>> getLotteryStatus(String token, String companyName, String eventName) {
        try {
            LotteryRegistration lr = lotteryRepository.find(eventName, companyName);
            if (lr == null) {
                return Response.success(Map.of("hasLottery", false));
            }

            Map<String, Object> status = new HashMap<>();
            status.put("hasLottery", true);
            status.put("startDate", lr.getStartDate());
            status.put("endDate", lr.getEndDate());
            status.put("maxWinners", lr.getMaxWinners());
            status.put("drawn", lr.isDrawn());
            status.put("open", lr.isOpen());
            status.put("registered", false);
            status.put("hasWon", false);

            // Enrich with per-user information if we have a valid member token
            if (token != null && tokenService.validateToken(token)) {
                String userId = tokenService.extractUserId(token);
                if (userId != null) {
                    boolean registered = lr.isRegistered(userId);
                    status.put("registered", registered);

                    // Check for an active (unused, non-expired) winning code
                    List<LotteryCode> userCodes = lotteryCodeRepository.findByUser(userId);
                    for (LotteryCode lc : userCodes) {
                        if (lc.getEventName().equals(eventName)
                                && lc.getCompanyName().equals(companyName)
                                && !lc.isUsed()
                                && (lc.getExpiryDate() == null || lc.getExpiryDate().after(new Date()))) {
                            status.put("hasWon", true);
                            status.put("winningCode", lc.getCode());
                            status.put("codeExpiry", lc.getExpiryDate());
                            break;
                        }
                    }
                }
            }

            return Response.success(status);
        } catch (Exception e) {
            logger.error("Failed to get lottery status for '{}': {}", eventName, e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    /**
     * Validates a lottery purchase code for a specific user/event/company triple.
     */
    public boolean validateLotteryCode(String code, String userId, String eventName, String companyName) {
        return lotteryCodeRepository.validate(code, userId, eventName, companyName);
    }

    /**
     * Consumes (marks as used) a lottery code.
     * Called by OrderService after a successful ticket reservation.
     */
    public void consumeLotteryCode(String code) {
        lotteryCodeRepository.markUsed(code);
        logger.info("Lottery code consumed: {}", code);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Draw logic (called by LotteryDrawScheduler)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Performs the lottery draw for a closed, undrawn registration:
     * selects up to {@code maxWinners} random participants, generates purchase
     * codes, notifies winners, and marks the lottery as drawn.
     */
    public void performDraw(LotteryRegistration lr) {
        List<String> participants = lr.getRegisteredUserIds();
        int numWinners = Math.min(lr.getMaxWinners(), participants.size());

        List<String> shuffled = new ArrayList<>(participants);
        Collections.shuffle(shuffled);
        List<String> winners = shuffled.subList(0, numWinners);

        Date codeExpiry = new Date(System.currentTimeMillis() + CODE_VALIDITY_MS);

        logger.info("=== LOTTERY DRAW: event='{}', company='{}', participants={}, winners={} ===",
                lr.getEventName(), lr.getCompanyName(), participants.size(), numWinners);

        for (String winnerId : winners) {
            LotteryCode code = lotteryCodeRepository.generate(
                    winnerId, lr.getEventName(), lr.getCompanyName(), codeExpiry);

            logger.info("  WINNER userId={} → code={}", winnerId, code.getCode());

            notifier.notifyUser(winnerId,
                    "You Won the Lottery!",
                    "Congratulations! You have been selected in the lottery for '" +
                            lr.getEventName() + "' by " + lr.getCompanyName() + ". " +
                            "Your one-time purchase code is: " + code.getCode() +
                            ". It expires in 48 hours. Use it on the event page to buy your ticket.");
        }

        if (participants.isEmpty()) {
            logger.info("  No participants registered - lottery closed with 0 winners.");
        }

        lotteryRepository.markDrawn(lr.getEventName(), lr.getCompanyName());
        logger.info("=== Lottery draw completed for event '{}' ===", lr.getEventName());

        // TODO: Integrate email/SMS notification service here once implemented by the team.
    }

}
