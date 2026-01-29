package com.example.jewell.service;

import com.example.jewell.model.CommunityConfig;
import com.example.jewell.repository.CommunityConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class CommunityConfigService {
    
    @Autowired
    private CommunityConfigRepository communityConfigRepository;
    
    public Optional<CommunityConfig> getConfigByKey(String key) {
        return communityConfigRepository.findByConfigKey(key);
    }
    
    public String getConfigValue(String key) {
        return getConfigByKey(key)
                .map(CommunityConfig::getConfigValue)
                .orElse(null);
    }
    
    public Map<String, String> getAllActiveConfigs() {
        List<CommunityConfig> configs = communityConfigRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
        Map<String, String> configMap = new HashMap<>();
        for (CommunityConfig config : configs) {
            configMap.put(config.getConfigKey(), config.getConfigValue());
        }
        return configMap;
    }
    
    public Map<String, String> getConfigsByType(String type) {
        List<CommunityConfig> configs = communityConfigRepository.findByConfigTypeAndIsActiveTrue(type);
        Map<String, String> configMap = new HashMap<>();
        for (CommunityConfig config : configs) {
            configMap.put(config.getConfigKey(), config.getConfigValue());
        }
        return configMap;
    }
    
    public List<CommunityConfig> getAllConfigs() {
        return communityConfigRepository.findAllByOrderByDisplayOrderAsc();
    }
    
    public CommunityConfig createOrUpdateConfig(String key, String value, String type, String description, Integer displayOrder) {
        return createOrUpdateConfig(key, value, type, description, displayOrder, null);
    }
    
    public CommunityConfig createOrUpdateConfig(String key, String value, String type, String description, Integer displayOrder, Long existingId) {
        // Validate required fields
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("Config key is required");
        }
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Config value is required");
        }
        
        // Handle empty strings as null
        String processedType = (type != null && type.trim().isEmpty()) ? null : type;
        String processedDescription = (description != null && description.trim().isEmpty()) ? null : description;
        key = key.trim();
        
        // If updating an existing config (existingId provided)
        if (existingId != null) {
            Optional<CommunityConfig> existingById = communityConfigRepository.findById(existingId);
            if (existingById.isPresent()) {
                CommunityConfig config = existingById.get();
                String oldKey = config.getConfigKey();
                
                // If key is changing, check if new key already exists
                if (!oldKey.equals(key)) {
                    Optional<CommunityConfig> existingByNewKey = communityConfigRepository.findByConfigKey(key);
                    if (existingByNewKey.isPresent() && !existingByNewKey.get().getId().equals(existingId)) {
                        throw new IllegalArgumentException("Config key already exists: " + key);
                    }
                    // Update the key
                    config.setConfigKey(key);
                }
                
                config.setConfigValue(value.trim());
                if (processedType != null) {
                    config.setConfigType(processedType);
                }
                if (processedDescription != null) {
                    config.setDescription(processedDescription);
                }
                if (displayOrder != null) {
                    config.setDisplayOrder(displayOrder);
                }
                return communityConfigRepository.save(config);
            }
        }
        
        // Check if config with this key already exists
        Optional<CommunityConfig> existing = communityConfigRepository.findByConfigKey(key);
        
        if (existing.isPresent()) {
            CommunityConfig config = existing.get();
            config.setConfigValue(value.trim());
            if (processedType != null) {
                config.setConfigType(processedType);
            }
            if (processedDescription != null) {
                config.setDescription(processedDescription);
            }
            if (displayOrder != null) {
                config.setDisplayOrder(displayOrder);
            }
            return communityConfigRepository.save(config);
        } else {
            CommunityConfig config = CommunityConfig.builder()
                    .configKey(key)
                    .configValue(value.trim())
                    .configType(processedType)
                    .description(processedDescription)
                    .displayOrder(displayOrder != null ? displayOrder : 0)
                    .isActive(true)
                    .build();
            return communityConfigRepository.save(config);
        }
    }
}

