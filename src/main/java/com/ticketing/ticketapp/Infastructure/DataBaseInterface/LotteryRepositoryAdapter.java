package com.ticketing.ticketapp.Infastructure.DataBaseInterface;

import com.ticketing.ticketapp.Domain.Lottery.ILotteryRepository;
import com.ticketing.ticketapp.Domain.Lottery.LotteryDomainException;
import com.ticketing.ticketapp.Domain.Lottery.LotteryRegistration;
import com.ticketing.ticketapp.Domain.Lottery.LotteryRegistrationKey;
import com.ticketing.ticketapp.Infastructure.JpaLotteryRegistrationRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
@Repository
@ConditionalOnProperty(name = "repository.type", havingValue = "DB")
public class LotteryRepositoryAdapter implements ILotteryRepository {

    private final JpaLotteryRegistrationRepository jpaLotteryRepository;

    public LotteryRepositoryAdapter(JpaLotteryRegistrationRepository jpaLotteryRepository) {
        this.jpaLotteryRepository = jpaLotteryRepository;
    }

    @Override
    @Transactional
    public void configure(String eventName, String companyName, Date startDate, Date endDate, int maxWinners) {
        LotteryRegistrationKey key = new LotteryRegistrationKey(eventName, companyName);
        jpaLotteryRepository.findById(key).ifPresentOrElse(
                existing -> {
                    existing.setStartDate(startDate);
                    existing.setEndDate(endDate);
                    existing.setMaxWinners(maxWinners);
                    jpaLotteryRepository.save(existing);
                },
                () -> jpaLotteryRepository.save(
                        new LotteryRegistration(eventName, companyName, startDate, endDate, maxWinners))
        );
    }

    @Override
    @Transactional
    public boolean register(String eventName, String companyName, String userId) {
        LotteryRegistrationKey key = new LotteryRegistrationKey(eventName, companyName);
        LotteryRegistration lr = jpaLotteryRepository.findById(key)
                .orElseThrow(() -> new LotteryDomainException("No lottery is configured for this event"));
        if (lr.isDrawn()) {
            throw new LotteryDomainException("The lottery has already been drawn");
        }
        if (!lr.isOpen()) {
            throw new LotteryDomainException(lr.isClosed()
                    ? "Lottery registration is closed"
                    : "Lottery registration has not opened yet");
        }
        boolean added = lr.addUser(userId);
        jpaLotteryRepository.save(lr);
        return added;
    }

    @Override
    public boolean isRegistered(String eventName, String companyName, String userId) {
        LotteryRegistrationKey key = new LotteryRegistrationKey(eventName, companyName);
        return jpaLotteryRepository.findById(key)
                .map(lr -> lr.isRegistered(userId))
                .orElse(false);
    }

    @Override
    public LotteryRegistration find(String eventName, String companyName) {
        LotteryRegistrationKey key = new LotteryRegistrationKey(eventName, companyName);
        return jpaLotteryRepository.findById(key).orElse(null);
    }

    @Override
    public List<LotteryRegistration> findClosedUndrawn() {
        return jpaLotteryRepository.findByDrawnFalseAndEndDateBefore(new Date());
    }

    @Override
    @Transactional
    public void markDrawn(String eventName, String companyName) {
        LotteryRegistrationKey key = new LotteryRegistrationKey(eventName, companyName);
        jpaLotteryRepository.findById(key).ifPresent(lr -> {
            lr.setDrawn(true);
            jpaLotteryRepository.save(lr);
        });
    }

    @Override
    public boolean exists(String eventName, String companyName) {
        return jpaLotteryRepository.existsByEventNameAndCompanyName(eventName, companyName);
    }

    @Override
    @Transactional
    public void deleteAll() {
        jpaLotteryRepository.deleteAll();
    }
}
