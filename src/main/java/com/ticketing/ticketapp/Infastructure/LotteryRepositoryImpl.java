package com.ticketing.ticketapp.Infastructure;

import com.ticketing.ticketapp.Domain.Lottery.ILotteryRepository;
import com.ticketing.ticketapp.Domain.Lottery.LotteryRegistration;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class LotteryRepositoryImpl implements ILotteryRepository {

    /** Key: "eventName::companyName" */
    private final Map<String, LotteryRegistration> lotteries = new ConcurrentHashMap<>();

    private String key(String eventName, String companyName) {
        return eventName + "::" + companyName;
    }

    @Override
    public void configure(String eventName, String companyName, Date startDate, Date endDate, int maxWinners) {
        lotteries.compute(key(eventName, companyName), (k, existing) -> {
            if (existing != null) {
                existing.setStartDate(startDate);
                existing.setEndDate(endDate);
                existing.setMaxWinners(maxWinners);
                return existing;
            }
            return new LotteryRegistration(eventName, companyName, startDate, endDate, maxWinners);
        });
    }

    @Override
    public boolean register(String eventName, String companyName, String userId) {
        LotteryRegistration lr = lotteries.get(key(eventName, companyName));
        if (lr == null) {
            throw new RuntimeException("No lottery is configured for this event");
        }
        if (lr.isDrawn()) {
            throw new RuntimeException("The lottery has already been drawn");
        }
        if (!lr.isOpen()) {
            throw new RuntimeException(lr.isClosed()
                    ? "Lottery registration is closed"
                    : "Lottery registration has not opened yet");
        }
        return lr.addUser(userId);
    }

    @Override
    public boolean isRegistered(String eventName, String companyName, String userId) {
        LotteryRegistration lr = lotteries.get(key(eventName, companyName));
        return lr != null && lr.isRegistered(userId);
    }

    @Override
    public LotteryRegistration find(String eventName, String companyName) {
        return lotteries.get(key(eventName, companyName));
    }

    @Override
    public List<LotteryRegistration> findClosedUndrawn() {
        return lotteries.values().stream()
                .filter(lr -> !lr.isDrawn() && lr.isClosed())
                .collect(Collectors.toList());
    }

    @Override
    public void markDrawn(String eventName, String companyName) {
        LotteryRegistration lr = lotteries.get(key(eventName, companyName));
        if (lr != null) {
            lr.setDrawn(true);
        }
    }

    @Override
    public boolean exists(String eventName, String companyName) {
        return lotteries.containsKey(key(eventName, companyName));
    }

    @Override
    public void deleteAll() {
        lotteries.clear();
    }
}
