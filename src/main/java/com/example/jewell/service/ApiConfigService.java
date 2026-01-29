package com.example.jewell.service;

import com.example.jewell.model.ApiConfig;
import com.example.jewell.repository.ApiConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ApiConfigService {
    
    @Autowired
    private ApiConfigRepository apiConfigRepository;
    
    /**
     * Get all API configurations
     */
    public List<ApiConfig> getAllConfigs() {
        return apiConfigRepository.findAll();
    }
    
    /**
     * Get active API configurations
     */
    public List<ApiConfig> getActiveConfigs() {
        return apiConfigRepository.findByIsActiveTrue();
    }
    
    /**
     * Get configuration by ID
     */
    public Optional<ApiConfig> getConfigById(Long id) {
        return apiConfigRepository.findById(id);
    }
    
    /**
     * Get configuration by endpoint and HTTP method
     */
    public Optional<ApiConfig> getConfigByEndpointAndMethod(String endpoint, String httpMethod) {
        if (httpMethod != null && !httpMethod.isEmpty()) {
            return apiConfigRepository.findByEndpointAndHttpMethod(endpoint, httpMethod);
        }
        return apiConfigRepository.findByEndpoint(endpoint);
    }
    
    /**
     * Find matching configurations for an endpoint
     */
    public List<ApiConfig> findMatchingConfigs(String endpoint, String httpMethod) {
        return apiConfigRepository.findMatchingConfigs(endpoint, httpMethod);
    }
    
    /**
     * Create or update API configuration
     */
    @Transactional
    public ApiConfig saveConfig(ApiConfig config) {
        // Check if config exists for this endpoint and method
        Optional<ApiConfig> existing = Optional.empty();
        if (config.getHttpMethod() != null && !config.getHttpMethod().isEmpty()) {
            existing = apiConfigRepository.findByEndpointAndHttpMethod(
                config.getEndpoint(), config.getHttpMethod());
        } else {
            existing = apiConfigRepository.findByEndpoint(config.getEndpoint());
        }
        
        if (existing.isPresent()) {
            ApiConfig existingConfig = existing.get();
            existingConfig.setIsPublic(config.getIsPublic());
            existingConfig.setRequiresAuth(config.getRequiresAuth());
            existingConfig.setRequiredRole(config.getRequiredRole());
            existingConfig.setDescription(config.getDescription());
            existingConfig.setIsActive(config.getIsActive());
            return apiConfigRepository.save(existingConfig);
        } else {
            return apiConfigRepository.save(config);
        }
    }
    
    /**
     * Update API configuration
     */
    @Transactional
    public ApiConfig updateConfig(Long id, ApiConfig config) {
        return apiConfigRepository.findById(id)
            .map(existing -> {
                existing.setEndpoint(config.getEndpoint());
                existing.setHttpMethod(config.getHttpMethod());
                existing.setIsPublic(config.getIsPublic());
                existing.setRequiresAuth(config.getRequiresAuth());
                existing.setRequiredRole(config.getRequiredRole());
                existing.setDescription(config.getDescription());
                existing.setIsActive(config.getIsActive());
                return apiConfigRepository.save(existing);
            })
            .orElseThrow(() -> new RuntimeException("API config not found with id: " + id));
    }
    
    /**
     * Delete API configuration
     */
    @Transactional
    public void deleteConfig(Long id) {
        apiConfigRepository.deleteById(id);
    }
    
    /**
     * Toggle active status
     */
    @Transactional
    public ApiConfig toggleActive(Long id) {
        ApiConfig config = apiConfigRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("API config not found with id: " + id));
        config.setIsActive(!config.getIsActive());
        return apiConfigRepository.save(config);
    }
    
    /**
     * Check if endpoint should be public
     */
    public boolean isEndpointPublic(String endpoint, String httpMethod) {
        List<ApiConfig> configs = findMatchingConfigs(endpoint, httpMethod);
        if (configs.isEmpty()) {
            return false; // Default to private if no config found
        }
        // Return the most specific match (longest endpoint path)
        return configs.stream()
            .filter(ApiConfig::getIsActive)
            .max((a, b) -> Integer.compare(a.getEndpoint().length(), b.getEndpoint().length()))
            .map(ApiConfig::getIsPublic)
            .orElse(false);
    }
    
    /**
     * Get required role for endpoint
     */
    public String getRequiredRole(String endpoint, String httpMethod) {
        List<ApiConfig> configs = findMatchingConfigs(endpoint, httpMethod);
        if (configs.isEmpty()) {
            return null;
        }
        return configs.stream()
            .filter(ApiConfig::getIsActive)
            .max((a, b) -> Integer.compare(a.getEndpoint().length(), b.getEndpoint().length()))
            .map(ApiConfig::getRequiredRole)
            .orElse(null);
    }
}


