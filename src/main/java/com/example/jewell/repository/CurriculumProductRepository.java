package com.example.jewell.repository;

import com.example.jewell.model.CurriculumProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CurriculumProductRepository extends JpaRepository<CurriculumProduct, Long> {
    Optional<CurriculumProduct> findByProductId(String productId);
    List<CurriculumProduct> findByIsActiveTrueOrderByDisplayOrderAsc();
}
