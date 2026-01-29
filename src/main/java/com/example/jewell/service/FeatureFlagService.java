package com.example.jewell.service;

import com.example.jewell.model.FeatureFlag;
import com.example.jewell.repository.FeatureFlagRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class FeatureFlagService {
    
    @Autowired
    private FeatureFlagRepository featureFlagRepository;
    
    public FeatureFlag createOrUpdateFeatureFlag(String featureKey, String featureName, Boolean isEnabled, 
                                                String description, String category, Integer displayOrder) {
        Optional<FeatureFlag> existing = featureFlagRepository.findByFeatureKey(featureKey);
        
        if (existing.isPresent()) {
            FeatureFlag flag = existing.get();
            flag.setFeatureName(featureName);
            flag.setIsEnabled(isEnabled);
            flag.setDescription(description);
            if (category != null) flag.setCategory(category);
            if (displayOrder != null) flag.setDisplayOrder(displayOrder);
            return featureFlagRepository.save(flag);
        } else {
            FeatureFlag flag = FeatureFlag.builder()
                    .featureKey(featureKey)
                    .featureName(featureName)
                    .isEnabled(isEnabled != null ? isEnabled : true)
                    .description(description)
                    .category(category)
                    .displayOrder(displayOrder != null ? displayOrder : 0)
                    .build();
            return featureFlagRepository.save(flag);
        }
    }
    
    public Optional<FeatureFlag> getFeatureFlag(String featureKey) {
        return featureFlagRepository.findByFeatureKey(featureKey);
    }
    
    public boolean isFeatureEnabled(String featureKey) {
        return featureFlagRepository.findByFeatureKey(featureKey)
                .map(FeatureFlag::getIsEnabled)
                .orElse(true); // Default to enabled if flag doesn't exist
    }
    
    public Map<String, Boolean> getAllFeatureFlags() {
        List<FeatureFlag> flags = featureFlagRepository.findAll();
        Map<String, Boolean> flagMap = new HashMap<>();
        for (FeatureFlag flag : flags) {
            flagMap.put(flag.getFeatureKey(), flag.getIsEnabled());
        }
        return flagMap;
    }
    
    public List<FeatureFlag> getAllFlags() {
        return featureFlagRepository.findAllByOrderByDisplayOrderAsc();
    }
    
    public List<FeatureFlag> getFlagsByCategory(String category) {
        return featureFlagRepository.findByCategoryOrderByDisplayOrderAsc(category);
    }
    
    @Transactional
    public FeatureFlag toggleFeature(String featureKey) {
        FeatureFlag flag = featureFlagRepository.findByFeatureKey(featureKey)
                .orElseThrow(() -> new RuntimeException("Feature flag not found: " + featureKey));
        flag.setIsEnabled(!flag.getIsEnabled());
        return featureFlagRepository.save(flag);
    }
    
    @Transactional
    public FeatureFlag setFeatureEnabled(String featureKey, Boolean enabled) {
        FeatureFlag flag = featureFlagRepository.findByFeatureKey(featureKey)
                .orElseThrow(() -> new RuntimeException("Feature flag not found: " + featureKey));
        flag.setIsEnabled(enabled);
        return featureFlagRepository.save(flag);
    }
}

