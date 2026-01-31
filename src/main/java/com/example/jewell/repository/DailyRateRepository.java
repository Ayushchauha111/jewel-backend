package com.example.jewell.repository;

import com.example.jewell.model.DailyRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface DailyRateRepository extends JpaRepository<DailyRate, Long> {
    Optional<DailyRate> findByPriceDate(LocalDate priceDate);
    Optional<DailyRate> findFirstByOrderByPriceDateDesc();
}
