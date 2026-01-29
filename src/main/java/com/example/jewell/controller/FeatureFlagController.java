package com.example.jewell.controller;

import com.example.jewell.dto.ApiResponseDTO;
import com.example.jewell.model.FeatureFlag;
import com.example.jewell.service.FeatureFlagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/feature-flags")
@CrossOrigin(origins = "*", maxAge = 3600)
public class FeatureFlagController {
    
    @Autowired
    private FeatureFlagService featureFlagService;
    
    // Public endpoint to check if a feature is enabled
    @GetMapping("/check/{featureKey}")
    public ResponseEntity<ApiResponseDTO<Boolean>> checkFeature(@PathVariable String featureKey) {
        try {
            boolean isEnabled = featureFlagService.isFeatureEnabled(featureKey);
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "Feature status retrieved.", isEnabled));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Error: " + e.getMessage(), null));
        }
    }
    
    // Public endpoint to get all feature flags (for frontend)
    @GetMapping("/all")
    public ResponseEntity<ApiResponseDTO<Map<String, Boolean>>> getAllFeatureFlags() {
        try {
            Map<String, Boolean> flags = featureFlagService.getAllFeatureFlags();
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "Feature flags retrieved successfully.", flags));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Error: " + e.getMessage(), null));
        }
    }
    
    // Admin endpoints
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDTO<List<FeatureFlag>>> getAllFlags() {
        try {
            List<FeatureFlag> flags = featureFlagService.getAllFlags();
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "All feature flags retrieved.", flags));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Error: " + e.getMessage(), null));
        }
    }
    
    @PostMapping("/admin/create")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDTO<FeatureFlag>> createOrUpdateFeatureFlag(
            @RequestParam String featureKey,
            @RequestParam String featureName,
            @RequestParam(required = false) Boolean isEnabled,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer displayOrder) {
        try {
            FeatureFlag flag = featureFlagService.createOrUpdateFeatureFlag(
                    featureKey, featureName, isEnabled, description, category, displayOrder);
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "Feature flag updated successfully.", flag));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Error: " + e.getMessage(), null));
        }
    }
    
    @PutMapping("/admin/{featureKey}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDTO<FeatureFlag>> toggleFeature(@PathVariable String featureKey) {
        try {
            FeatureFlag flag = featureFlagService.toggleFeature(featureKey);
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "Feature toggled successfully.", flag));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Error: " + e.getMessage(), null));
        }
    }
    
    @PutMapping("/admin/{featureKey}/enable")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDTO<FeatureFlag>> setFeatureEnabled(
            @PathVariable String featureKey,
            @RequestParam Boolean enabled) {
        try {
            FeatureFlag flag = featureFlagService.setFeatureEnabled(featureKey, enabled);
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "Feature status updated.", flag));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Error: " + e.getMessage(), null));
        }
    }
}

