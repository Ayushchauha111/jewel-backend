package com.example.jewell.repository;

import com.example.jewell.model.FeatureFlag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeatureFlagRepository extends JpaRepository<FeatureFlag, Long> {
    Optional<FeatureFlag> findByFeatureKey(String featureKey);
    
    List<FeatureFlag> findByCategoryOrderByDisplayOrderAsc(String category);
    
    List<FeatureFlag> findByIsEnabledTrue();
    
    List<FeatureFlag> findAllByOrderByDisplayOrderAsc();
    
    boolean existsByFeatureKey(String featureKey);
}

