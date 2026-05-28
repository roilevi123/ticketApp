package com.ticketing.ticketapp.Controllers;

import com.ticketing.ticketapp.Appliction.LotteryService;
import com.ticketing.ticketapp.Appliction.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.Map;

/**
 * REST endpoints for the lottery system.
 *
 * <pre>
 * POST   /api/lottery/{company}/{event}/register – user enters the lottery
 * GET    /api/lottery/{company}/{event}/status – lottery + user status
 * POST   /api/lottery/{company}/{event}/configure – organizer sets up lottery
 * </pre>
 */
@RestController
@RequestMapping("/api/lottery")
public class LotteryController {

    private final LotteryService lotteryService;

    public LotteryController(LotteryService lotteryService) {
        this.lotteryService = lotteryService;
    }

    /**
     * Register the authenticated user for an event's lottery.
     * Requires a valid member (non-guest) token.
     */
    @PostMapping("/{companyName}/{eventName}/register")
    public ResponseEntity<?> registerForLottery(
            @RequestAttribute("cleanToken") String token,
            @PathVariable String companyName,
            @PathVariable String eventName) {

        Response<String> response = lotteryService.registerForLottery(token, companyName, eventName);
        if (response.isSuccess()) {
            return ResponseEntity.ok(Map.of("message", response.getData()));
        }
        return ResponseEntity.badRequest().body(response.getMessage());
    }

    /**
     * Returns the lottery state for an event and – if the caller is a member –
     * their personal registration / winning status.
     */
    @GetMapping("/{companyName}/{eventName}/status")
    public ResponseEntity<?> getLotteryStatus(
            @RequestAttribute(value = "cleanToken", required = false) String token,
            @PathVariable String companyName,
            @PathVariable String eventName) {

        Response<Map<String, Object>> response =
                lotteryService.getLotteryStatus(token, companyName, eventName);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response.getData());
        }
        return ResponseEntity.badRequest().body(response.getMessage());
    }

    /**
     * Organizer endpoint: configure (or update) the lottery for an event.
     * Requires an authorized token (owner / permitted manager).
     */
    @PostMapping("/{companyName}/{eventName}/configure")
    public ResponseEntity<?> configureLottery(
            @RequestAttribute("cleanToken") String token,
            @PathVariable String companyName,
            @PathVariable String eventName,
            @RequestBody LotteryConfigRequestDTO config) {

        Response<String> response = lotteryService.configureLottery(
                token, companyName, eventName, config.getStartDate(), config.getEndDate(), config.getMaxWinners());
        if (response.isSuccess()) {
            return ResponseEntity.ok(Map.of("message", response.getData()));
        }
        return ResponseEntity.badRequest().body(response.getMessage());
    }
}


class LotteryConfigRequestDTO {
    private Date startDate;
    private Date endDate;
    private int  maxWinners;
    public Date getStartDate() { return startDate; }
    public void setStartDate(Date d) { this.startDate = d; }
    public Date getEndDate() { return endDate; }
    public void setEndDate(Date d) { this.endDate = d; }
    public int  getMaxWinners() { return maxWinners; }
    public void setMaxWinners(int n) { this.maxWinners = n; }
}
