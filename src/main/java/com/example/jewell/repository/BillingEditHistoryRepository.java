package com.example.jewell.repository;

import com.example.jewell.model.BillingEditHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BillingEditHistoryRepository extends JpaRepository<BillingEditHistory, Long> {
    List<BillingEditHistory> findByBillingIdOrderByCreatedAtDesc(Long billingId);
}
