package com.ticketing.ticketapp.Infastructure;

import com.ticketing.ticketapp.Domain.Lottery.LotteryRegistration;
import com.ticketing.ticketapp.Domain.Lottery.LotteryRegistrationKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface JpaLotteryRegistrationRepository extends JpaRepository<LotteryRegistration, LotteryRegistrationKey> {

    boolean existsByEventNameAndCompanyName(String eventName, String companyName);

    List<LotteryRegistration> findByDrawnFalseAndEndDateBefore(Date now);
}
