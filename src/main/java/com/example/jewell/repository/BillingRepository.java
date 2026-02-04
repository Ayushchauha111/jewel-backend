package com.example.jewell.repository;

import com.example.jewell.model.Billing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BillingRepository extends JpaRepository<Billing, Long> {
    Optional<Billing> findByBillNumber(String billNumber);
    List<Billing> findByCustomerId(Long customerId);
    long countByCustomerId(Long customerId);
    List<Billing> findByCreatedAtBetween(java.time.LocalDateTime start, java.time.LocalDateTime end);
    
    // Search methods
    @Query("SELECT b FROM Billing b WHERE " +
           "LOWER(b.billNumber) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(b.customer.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(b.customer.phone) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Billing> searchBills(@Param("query") String query);
    
    // Pagination methods
    Page<Billing> findAll(Pageable pageable);
    Page<Billing> findByCustomerId(Long customerId, Pageable pageable);
    
    @Query("SELECT b FROM Billing b WHERE " +
           "LOWER(b.billNumber) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(b.customer.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(b.customer.phone) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Billing> searchBills(@Param("query") String query, Pageable pageable);
    
    @Query("SELECT SUM(b.finalAmount) FROM Billing b WHERE DATE(b.createdAt) = :date AND b.paymentStatus = 'PAID'")
    BigDecimal getTotalSalesByDate(@Param("date") LocalDate date);
    
    @Query("SELECT COUNT(b) FROM Billing b WHERE DATE(b.createdAt) = :date")
    Long getBillCountByDate(@Param("date") LocalDate date);

    @Query("SELECT COALESCE(SUM(b.finalAmount), 0) FROM Billing b WHERE b.paymentStatus = 'PAID'")
    BigDecimal getTotalPaidRevenue();
}
