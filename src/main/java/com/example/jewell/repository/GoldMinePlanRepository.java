package com.example.jewell.repository;

import com.example.jewell.model.GoldMinePlan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GoldMinePlanRepository extends JpaRepository<GoldMinePlan, Long> {

    List<GoldMinePlan> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    Page<GoldMinePlan> findByCustomerId(Long customerId, Pageable pageable);

    Page<GoldMinePlan> findByStatus(GoldMinePlan.PlanStatus status, Pageable pageable);

    long countByCustomerIdAndStatus(Long customerId, GoldMinePlan.PlanStatus status);
}
