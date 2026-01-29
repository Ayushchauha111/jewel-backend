package com.example.jewell.repository;

import com.example.jewell.model.Order;
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
public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderNumber(String orderNumber);
    List<Order> findByCustomerId(Long customerId);
    List<Order> findByPaymentStatus(Order.PaymentStatus paymentStatus);
    List<Order> findByOrderStatus(Order.OrderStatus orderStatus);
    
    // Pagination methods
    Page<Order> findAll(Pageable pageable);
    Page<Order> findByCustomerId(Long customerId, Pageable pageable);
    Page<Order> findByPaymentStatus(Order.PaymentStatus paymentStatus, Pageable pageable);
    Page<Order> findByOrderStatus(Order.OrderStatus orderStatus, Pageable pageable);
    
    @Query("SELECT SUM(o.finalAmount) FROM Order o WHERE DATE(o.createdAt) = :date AND o.paymentStatus = 'PAID'")
    BigDecimal getTotalOnlineSalesByDate(@Param("date") LocalDate date);
    
    @Query("SELECT COUNT(o) FROM Order o WHERE DATE(o.createdAt) = :date")
    Long getOrderCountByDate(@Param("date") LocalDate date);

    @Query("SELECT COALESCE(SUM(o.finalAmount), 0) FROM Order o WHERE o.paymentStatus = 'PAID'")
    BigDecimal getTotalPaidRevenue();
}
