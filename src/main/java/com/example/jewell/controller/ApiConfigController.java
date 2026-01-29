package com.example.jewell.controller;

import com.example.jewell.model.ApiConfig;
import com.example.jewell.service.ApiConfigService;
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
@RequestMapping("/api/api-config")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ApiConfigController {
    private static final Logger log = LoggerFactory.getLogger(ApiConfigController.class);
    
    @Autowired
    private ApiConfigService apiConfigService;
    
    /**
     * Get all API configurations - ADMIN ONLY
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getAllConfigs() {
        try {
            List<ApiConfig> configs = apiConfigService.getAllConfigs();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("configs", configs);
            response.put("count", configs.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching API configs", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error fetching configurations: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Get active API configurations - ADMIN ONLY
     */
    @GetMapping("/active")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getActiveConfigs() {
        try {
            List<ApiConfig> configs = apiConfigService.getActiveConfigs();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("configs", configs);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching active API configs", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error fetching configurations: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Get configuration by ID - ADMIN ONLY
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getConfigById(@PathVariable Long id) {
        try {
            return apiConfigService.getConfigById(id)
                .map(config -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("config", config);
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "Config not found")));
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error fetching config: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Create or update API configuration - ADMIN ONLY
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> saveConfig(@RequestBody ApiConfig config) {
        try {
            ApiConfig saved = apiConfigService.saveConfig(config);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "API configuration saved successfully");
            response.put("config", saved);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error saving API config", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error saving configuration: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Update API configuration - ADMIN ONLY
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> updateConfig(@PathVariable Long id, @RequestBody ApiConfig config) {
        try {
            ApiConfig updated = apiConfigService.updateConfig(id, config);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "API configuration updated successfully");
            response.put("config", updated);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            log.error("Error updating API config", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error updating configuration: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Delete API configuration - ADMIN ONLY
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> deleteConfig(@PathVariable Long id) {
        try {
            apiConfigService.deleteConfig(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "API configuration deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error deleting API config", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error deleting configuration: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Toggle active status - ADMIN ONLY
     */
    @PostMapping("/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> toggleActive(@PathVariable Long id) {
        try {
            ApiConfig updated = apiConfigService.toggleActive(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "API configuration status toggled successfully");
            response.put("config", updated);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error updating configuration: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}


