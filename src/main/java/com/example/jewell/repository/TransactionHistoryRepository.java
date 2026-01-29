package com.example.jewell.repository;

import com.example.jewell.model.TransactionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface TransactionHistoryRepository extends JpaRepository<TransactionHistory, Long> {
    List<TransactionHistory> findByTransactionDate(LocalDate date);
    List<TransactionHistory> findByTransactionDateBetween(LocalDate startDate, LocalDate endDate);
    List<TransactionHistory> findByTransactionType(TransactionHistory.TransactionType type);
    List<TransactionHistory> findByCategory(TransactionHistory.Category category);
    List<TransactionHistory> findByTransactionDateAndTransactionType(LocalDate date, TransactionHistory.TransactionType type);
    
    @Query("SELECT SUM(t.amount) FROM TransactionHistory t WHERE t.transactionDate = :date AND t.transactionType = :type")
    BigDecimal getTotalByDateAndType(@Param("date") LocalDate date, @Param("type") TransactionHistory.TransactionType type);
    
    @Query("SELECT SUM(t.amount) FROM TransactionHistory t WHERE t.transactionDate BETWEEN :startDate AND :endDate AND t.transactionType = :type")
    BigDecimal getTotalByDateRangeAndType(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, @Param("type") TransactionHistory.TransactionType type);
    
    @Query("SELECT SUM(t.amount) FROM TransactionHistory t WHERE t.transactionDate = :date AND t.category = :category")
    BigDecimal getTotalByDateAndCategory(@Param("date") LocalDate date, @Param("category") TransactionHistory.Category category);
}
