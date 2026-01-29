package com.example.jewell.repository;

import com.example.jewell.model.CommunityConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommunityConfigRepository extends JpaRepository<CommunityConfig, Long> {
    Optional<CommunityConfig> findByConfigKey(String configKey);
    
    List<CommunityConfig> findByConfigTypeAndIsActiveTrue(String configType);
    
    List<CommunityConfig> findByIsActiveTrueOrderByDisplayOrderAsc();
    
    List<CommunityConfig> findByIsActiveTrue();
    
    List<CommunityConfig> findAllByOrderByDisplayOrderAsc();
}

