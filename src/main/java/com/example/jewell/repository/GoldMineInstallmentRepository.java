package com.example.jewell.repository;

import com.example.jewell.model.GoldMineInstallment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GoldMineInstallmentRepository extends JpaRepository<GoldMineInstallment, Long> {

    List<GoldMineInstallment> findByPlanIdOrderByInstallmentNumberAsc(Long planId);

    Optional<GoldMineInstallment> findByPlanIdAndInstallmentNumber(Long planId, int installmentNumber);
}
