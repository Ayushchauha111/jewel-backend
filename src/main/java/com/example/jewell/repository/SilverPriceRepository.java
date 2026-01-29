package com.example.jewell.repository;

import com.example.jewell.model.SilverPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface SilverPriceRepository extends JpaRepository<SilverPrice, Long> {
    Optional<SilverPrice> findByPriceDate(LocalDate priceDate);
    Optional<SilverPrice> findFirstByOrderByPriceDateDesc();
}
