package com.ticketing.ticketapp.Infastructure.DataBaseInterface;

import com.ticketing.ticketapp.Domain.Lottery.ILotteryCodeRepository;
import com.ticketing.ticketapp.Domain.Lottery.LotteryCode;
import com.ticketing.ticketapp.Infastructure.JpaLotteryCodeRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
@Repository
@ConditionalOnProperty(name = "repository.type", havingValue = "DB")
public class LotteryCodeRepositoryAdapter implements ILotteryCodeRepository {

    private final JpaLotteryCodeRepository jpaLotteryCodeRepository;

    public LotteryCodeRepositoryAdapter(JpaLotteryCodeRepository jpaLotteryCodeRepository) {
        this.jpaLotteryCodeRepository = jpaLotteryCodeRepository;
    }

    @Override
    @Transactional
    public LotteryCode generate(String userId, String eventName, String companyName, Date expiryDate) {
        LotteryCode lc = new LotteryCode(userId, eventName, companyName, expiryDate);
        return jpaLotteryCodeRepository.saveAndFlush(lc);
    }

    @Override
    public LotteryCode findByCode(String code) {
        return jpaLotteryCodeRepository.findById(code).orElse(null);
    }

    @Override
    public boolean validate(String code, String userId, String eventName, String companyName) {
        LotteryCode lc = jpaLotteryCodeRepository.findById(code).orElse(null);
        return lc != null && lc.isValid(userId, eventName, companyName);
    }

    @Override
    @Transactional
    public void markUsed(String code) {
        jpaLotteryCodeRepository.findById(code).ifPresent(lc -> {
            lc.setUsed(true);
            jpaLotteryCodeRepository.save(lc);
        });
    }

    @Override
    public List<LotteryCode> findByUser(String userId) {
        return jpaLotteryCodeRepository.findByUserId(userId);
    }

    @Override
    @Transactional
    public void deleteAll() {
        jpaLotteryCodeRepository.deleteAll();
    }
}
