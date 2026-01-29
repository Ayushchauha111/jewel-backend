package com.example.jewell.controller;

import com.example.jewell.model.RateLimitConfig;
import com.example.jewell.service.RateLimitConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rate-limit")
@CrossOrigin(origins = "*", maxAge = 3600)
public class RateLimitConfigController {
    private static final Logger log = LoggerFactory.getLogger(RateLimitConfigController.class);
    
    @Autowired
    private RateLimitConfigService rateLimitConfigService;
    
    /**
     * Get all rate limit configurations - ADMIN ONLY
     */
    @GetMapping("/config")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getAllConfigs() {
        try {
            List<RateLimitConfig> configs = rateLimitConfigService.getAllConfigs();
            Map<String, Integer> configMap = rateLimitConfigService.getAllConfigsAsMap();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("configs", configs);
            response.put("configMap", configMap);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching rate limit configs", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error fetching configurations: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Update rate limit configurations - ADMIN ONLY
     */
    @PutMapping("/config")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> updateConfigs(@RequestBody Map<String, Integer> configUpdates) {
        try {
            Map<String, RateLimitConfig> updatedConfigs = rateLimitConfigService.updateConfigs(configUpdates);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Rate limit configurations updated successfully");
            response.put("configs", updatedConfigs);
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            log.error("Error updating rate limit configs", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error updating configurations: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Reset all configurations to defaults - ADMIN ONLY
     */
    @PostMapping("/config/reset")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> resetConfigs() {
        try {
            rateLimitConfigService.resetToDefaults();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Rate limit configurations reset to defaults");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error resetting rate limit configs", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error resetting configurations: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}

