package com.ticketing.ticketapp.Infastructure;

import com.ticketing.ticketapp.Domain.Lottery.ILotteryCodeRepository;
import com.ticketing.ticketapp.Domain.Lottery.LotteryCode;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
@Repository
@ConditionalOnProperty(name = "repository.type", havingValue = "MEMORY")public class LotteryCodeRepositoryImpl implements ILotteryCodeRepository {

    /** Key: lottery code UUID string */
    private final Map<String, LotteryCode> codes = new ConcurrentHashMap<>();

    @Override
    public LotteryCode generate(String userId, String eventName, String companyName, Date expiryDate) {
        LotteryCode lc = new LotteryCode(userId, eventName, companyName, expiryDate);
        codes.put(lc.getCode(), lc);
        return lc;
    }

    @Override
    public LotteryCode findByCode(String code) {
        return codes.get(code);
    }

    @Override
    public boolean validate(String code, String userId, String eventName, String companyName) {
        LotteryCode lc = codes.get(code);
        return lc != null && lc.isValid(userId, eventName, companyName);
    }

    @Override
    public void markUsed(String code) {
        LotteryCode lc = codes.get(code);
        if (lc != null) {
            lc.setUsed(true);
        }
    }

    @Override
    public List<LotteryCode> findByUser(String userId) {
        return codes.values().stream()
                .filter(lc -> lc.getUserId().equals(userId))
                .collect(Collectors.toList());
    }

    @Override
    public void deleteAll() {
        codes.clear();
    }
}
