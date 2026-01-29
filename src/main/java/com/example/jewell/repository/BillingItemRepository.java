package com.example.jewell.repository;

import com.example.jewell.model.BillingItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BillingItemRepository extends JpaRepository<BillingItem, Long> {

    @Query("SELECT bi FROM BillingItem bi JOIN FETCH bi.billing b WHERE bi.stock.id = :stockId ORDER BY b.createdAt DESC")
    List<BillingItem> findByStockIdWithBilling(@Param("stockId") Long stockId);
}
