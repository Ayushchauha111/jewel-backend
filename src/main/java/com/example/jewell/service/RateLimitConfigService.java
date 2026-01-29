package com.example.jewell.service;

import com.example.jewell.model.RateLimitConfig;
import com.example.jewell.repository.RateLimitConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class RateLimitConfigService {
    
    @Autowired
    private RateLimitConfigRepository rateLimitConfigRepository;
    
    // Default values
    private static final int DEFAULT_AUTH_REQUESTS_PER_MINUTE = 5;
    private static final int DEFAULT_GENERAL_REQUESTS_PER_MINUTE = 100;
    private static final int DEFAULT_PUBLIC_REQUESTS_PER_MINUTE = 200;
    private static final long DEFAULT_WINDOW_SIZE_MS = 60_000; // 1 minute
    
    // Config keys
    public static final String KEY_AUTH_REQUESTS_PER_MINUTE = "AUTH_REQUESTS_PER_MINUTE";
    public static final String KEY_GENERAL_REQUESTS_PER_MINUTE = "GENERAL_REQUESTS_PER_MINUTE";
    public static final String KEY_PUBLIC_REQUESTS_PER_MINUTE = "PUBLIC_REQUESTS_PER_MINUTE";
    public static final String KEY_WINDOW_SIZE_MS = "WINDOW_SIZE_MS";
    
    /**
     * Get all rate limit configurations
     */
    public List<RateLimitConfig> getAllConfigs() {
        return rateLimitConfigRepository.findAll();
    }
    
    /**
     * Get a specific config value by key, with default fallback
     */
    public Integer getConfigValue(String configKey, Integer defaultValue) {
        Optional<RateLimitConfig> config = rateLimitConfigRepository.findByConfigKey(configKey);
        return config.map(RateLimitConfig::getConfigValue).orElse(defaultValue);
    }
    
    /**
     * Get all configs as a map for easy access
     */
    public Map<String, Integer> getAllConfigsAsMap() {
        List<RateLimitConfig> configs = getAllConfigs();
        Map<String, Integer> configMap = new HashMap<>();
        
        // Initialize with defaults
        configMap.put(KEY_AUTH_REQUESTS_PER_MINUTE, DEFAULT_AUTH_REQUESTS_PER_MINUTE);
        configMap.put(KEY_GENERAL_REQUESTS_PER_MINUTE, DEFAULT_GENERAL_REQUESTS_PER_MINUTE);
        configMap.put(KEY_PUBLIC_REQUESTS_PER_MINUTE, DEFAULT_PUBLIC_REQUESTS_PER_MINUTE);
        configMap.put(KEY_WINDOW_SIZE_MS, (int) (DEFAULT_WINDOW_SIZE_MS / 1000)); // Convert to seconds for display
        
        // Override with database values
        for (RateLimitConfig config : configs) {
            configMap.put(config.getConfigKey(), config.getConfigValue());
        }
        
        return configMap;
    }
    
    /**
     * Update a config value
     */
    @Transactional
    public RateLimitConfig updateConfig(String configKey, Integer configValue, String description) {
        Optional<RateLimitConfig> existingConfig = rateLimitConfigRepository.findByConfigKey(configKey);
        
        if (existingConfig.isPresent()) {
            RateLimitConfig config = existingConfig.get();
            config.setConfigValue(configValue);
            if (description != null) {
                config.setDescription(description);
            }
            return rateLimitConfigRepository.save(config);
        } else {
            RateLimitConfig newConfig = new RateLimitConfig(configKey, configValue, description);
            return rateLimitConfigRepository.save(newConfig);
        }
    }
    
    /**
     * Update multiple configs at once
     */
    @Transactional
    public Map<String, RateLimitConfig> updateConfigs(Map<String, Integer> configUpdates) {
        Map<String, RateLimitConfig> updatedConfigs = new HashMap<>();
        
        for (Map.Entry<String, Integer> entry : configUpdates.entrySet()) {
            String key = entry.getKey();
            Integer value = entry.getValue();
            
            // Validate value
            if (value == null || value < 1) {
                throw new IllegalArgumentException("Config value for " + key + " must be at least 1");
            }
            
            // Get description based on key
            String description = getDescriptionForKey(key);
            
            RateLimitConfig updated = updateConfig(key, value, description);
            updatedConfigs.put(key, updated);
        }
        
        return updatedConfigs;
    }
    
    /**
     * Reset all configs to defaults
     */
    @Transactional
    public void resetToDefaults() {
        updateConfig(KEY_AUTH_REQUESTS_PER_MINUTE, DEFAULT_AUTH_REQUESTS_PER_MINUTE, 
            "Maximum requests per minute for authentication endpoints");
        updateConfig(KEY_GENERAL_REQUESTS_PER_MINUTE, DEFAULT_GENERAL_REQUESTS_PER_MINUTE, 
            "Maximum requests per minute for general API endpoints");
        updateConfig(KEY_PUBLIC_REQUESTS_PER_MINUTE, DEFAULT_PUBLIC_REQUESTS_PER_MINUTE, 
            "Maximum requests per minute for public read-only endpoints");
        updateConfig(KEY_WINDOW_SIZE_MS, (int) (DEFAULT_WINDOW_SIZE_MS / 1000), 
            "Rate limit window size in seconds");
    }
    
    /**
     * Initialize default configs if they don't exist
     */
    @Transactional
    public void initializeDefaults() {
        if (rateLimitConfigRepository.findByConfigKey(KEY_AUTH_REQUESTS_PER_MINUTE).isEmpty()) {
            updateConfig(KEY_AUTH_REQUESTS_PER_MINUTE, DEFAULT_AUTH_REQUESTS_PER_MINUTE, 
                "Maximum requests per minute for authentication endpoints");
        }
        if (rateLimitConfigRepository.findByConfigKey(KEY_GENERAL_REQUESTS_PER_MINUTE).isEmpty()) {
            updateConfig(KEY_GENERAL_REQUESTS_PER_MINUTE, DEFAULT_GENERAL_REQUESTS_PER_MINUTE, 
                "Maximum requests per minute for general API endpoints");
        }
        if (rateLimitConfigRepository.findByConfigKey(KEY_PUBLIC_REQUESTS_PER_MINUTE).isEmpty()) {
            updateConfig(KEY_PUBLIC_REQUESTS_PER_MINUTE, DEFAULT_PUBLIC_REQUESTS_PER_MINUTE, 
                "Maximum requests per minute for public read-only endpoints");
        }
        if (rateLimitConfigRepository.findByConfigKey(KEY_WINDOW_SIZE_MS).isEmpty()) {
            updateConfig(KEY_WINDOW_SIZE_MS, (int) (DEFAULT_WINDOW_SIZE_MS / 1000), 
                "Rate limit window size in seconds");
        }
    }
    
    private String getDescriptionForKey(String key) {
        switch (key) {
            case KEY_AUTH_REQUESTS_PER_MINUTE:
                return "Maximum requests per minute for authentication endpoints";
            case KEY_GENERAL_REQUESTS_PER_MINUTE:
                return "Maximum requests per minute for general API endpoints";
            case KEY_PUBLIC_REQUESTS_PER_MINUTE:
                return "Maximum requests per minute for public read-only endpoints";
            case KEY_WINDOW_SIZE_MS:
                return "Rate limit window size in seconds";
            default:
                return "Rate limit configuration";
        }
    }
}


