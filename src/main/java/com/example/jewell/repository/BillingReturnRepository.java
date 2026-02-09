package com.example.jewell.repository;

import com.example.jewell.model.BillingReturn;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BillingReturnRepository extends JpaRepository<BillingReturn, Long> {
    List<BillingReturn> findByOriginalBillingId(Long originalBillingId);
    Page<BillingReturn> findAll(Pageable pageable);
}
