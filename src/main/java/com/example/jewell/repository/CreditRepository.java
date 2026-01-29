package com.example.jewell.repository;

import com.example.jewell.model.Credit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface CreditRepository extends JpaRepository<Credit, Long> {
    List<Credit> findByCustomerId(Long customerId);
    List<Credit> findByStatus(Credit.CreditStatus status);
    long countByStatus(Credit.CreditStatus status);
    
    // Pagination methods
    Page<Credit> findAll(Pageable pageable);
    Page<Credit> findByCustomerId(Long customerId, Pageable pageable);
    Page<Credit> findByStatus(Credit.CreditStatus status, Pageable pageable);
    
    @Query("SELECT SUM(c.creditAmount) FROM Credit c WHERE DATE(c.createdAt) = :date")
    BigDecimal getTotalCreditGivenByDate(@Param("date") LocalDate date);
    
    @Query("SELECT SUM(c.paidAmount) FROM Credit c WHERE DATE(c.updatedAt) = :date AND c.paidAmount > 0")
    BigDecimal getTotalCreditPaidByDate(@Param("date") LocalDate date);
    
    @Query("SELECT SUM(c.remainingAmount) FROM Credit c WHERE c.status IN ('PENDING', 'PARTIAL')")
    BigDecimal getTotalOutstandingCredit();
}
