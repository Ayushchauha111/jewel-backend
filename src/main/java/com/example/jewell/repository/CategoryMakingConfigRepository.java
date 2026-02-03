package com.example.jewell.repository;

import com.example.jewell.model.CategoryMakingConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryMakingConfigRepository extends JpaRepository<CategoryMakingConfig, Long> {
    Optional<CategoryMakingConfig> findByCategoryIgnoreCase(String category);
    List<CategoryMakingConfig> findAllByOrderByCategoryAsc();
}
