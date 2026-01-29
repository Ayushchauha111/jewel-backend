package com.example.jewell.repository;

import com.example.jewell.model.RateLimitConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RateLimitConfigRepository extends JpaRepository<RateLimitConfig, Long> {
    Optional<RateLimitConfig> findByConfigKey(String configKey);
}


