package com.example.jewell.repository;

import com.example.jewell.model.CreditPaymentHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CreditPaymentHistoryRepository extends JpaRepository<CreditPaymentHistory, Long> {
    List<CreditPaymentHistory> findByCreditIdOrderByPaymentDateDesc(Long creditId);
}
