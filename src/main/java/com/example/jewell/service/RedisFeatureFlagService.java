package com.example.jewell.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service to check if Redis caching should be enabled based on feature flag.
 * Note: The actual Redis beans are created based on spring.redis.enabled property.
 * When disabling Redis via feature flag, also set spring.redis.enabled=false in properties.
 */
@Service
public class RedisFeatureFlagService {
    
    private final FeatureFlagService featureFlagService;
    
    @Autowired
    public RedisFeatureFlagService(FeatureFlagService featureFlagService) {
        this.featureFlagService = featureFlagService;
    }
    
    /**
     * Check if Redis caching is enabled via feature flag.
     * @return true if feature flag is enabled, false otherwise
     */
    public boolean isRedisEnabled() {
        return featureFlagService.isFeatureEnabled("redis_caching");
    }
    
    /**
     * Validate that the feature flag matches the property setting.
     * Logs a warning if there's a mismatch.
     */
    public void validateRedisConfig(boolean propertyEnabled) {
        boolean flagEnabled = isRedisEnabled();
        if (propertyEnabled != flagEnabled) {
            // Log warning - feature flag and property don't match
            System.err.println("WARNING: Redis feature flag (" + flagEnabled + 
                ") doesn't match spring.redis.enabled property (" + propertyEnabled + 
                "). Please update application.properties to match the feature flag setting.");
        }
    }
}
