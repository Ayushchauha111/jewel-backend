package com.example.jewell.repository;

import com.example.jewell.model.GoldPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface GoldPriceRepository extends JpaRepository<GoldPrice, Long> {
    Optional<GoldPrice> findByPriceDate(LocalDate priceDate);
    Optional<GoldPrice> findFirstByOrderByPriceDateDesc();
}
