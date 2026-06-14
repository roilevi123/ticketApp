package com.ticketing.ticketapp.Infastructure;

import com.ticketing.ticketapp.Domain.Lottery.LotteryCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JpaLotteryCodeRepository extends JpaRepository<LotteryCode, String> {

    List<LotteryCode> findByUserId(String userId);
}
