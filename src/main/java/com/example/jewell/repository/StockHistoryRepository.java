package com.example.jewell.repository;

import com.example.jewell.model.StockHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockHistoryRepository extends JpaRepository<StockHistory, Long> {
    List<StockHistory> findByOrderByDeletedAtDesc();
    List<StockHistory> findByOriginalStockId(Long originalStockId);
}
