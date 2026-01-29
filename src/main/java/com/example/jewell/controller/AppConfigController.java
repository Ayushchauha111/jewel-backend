package com.example.jewell.controller;

import com.example.jewell.dto.ApiResponseDTO;
import com.example.jewell.service.FeatureFlagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/config")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AppConfigController {
    
    @Autowired
    private FeatureFlagService featureFlagService;
    
    @Value("${jewell.ai.enabled:true}")
    private boolean aiEnabledDefault; // Fallback to properties if feature flag doesn't exist
    
    /**
     * Get application configuration flags
     * Public endpoint - no authentication required
     */
    @GetMapping("/features")
    public ResponseEntity<ApiResponseDTO<Map<String, Boolean>>> getFeatureFlags() {
        try {
            Map<String, Boolean> features = new HashMap<>();
            // Check feature flag first, fallback to property
            boolean aiEnabled = featureFlagService.getFeatureFlag("ai_teaching")
                .map(flag -> flag.getIsEnabled())
                .orElse(aiEnabledDefault);
            features.put("aiEnabled", aiEnabled);
            
            return ResponseEntity.ok(new ApiResponseDTO<>(
                true,
                "Feature flags retrieved successfully",
                features
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(new ApiResponseDTO<>(false, "Error: " + e.getMessage(), null));
        }
    }
    
    /**
     * Check if AI features are enabled
     */
    @GetMapping("/ai-enabled")
    public ResponseEntity<ApiResponseDTO<Boolean>> isAiEnabled() {
        try {
            // Check feature flag first, fallback to property
            boolean aiEnabled = featureFlagService.getFeatureFlag("ai_teaching")
                .map(flag -> flag.getIsEnabled())
                .orElse(aiEnabledDefault);
            
            return ResponseEntity.ok(new ApiResponseDTO<>(
                true,
                "AI feature status retrieved",
                aiEnabled
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(new ApiResponseDTO<>(false, "Error: " + e.getMessage(), null));
        }
    }
}

