package com.example.jewell.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "api_configs", uniqueConstraints = {
    @UniqueConstraint(columnNames = "endpoint")
})
public class ApiConfig {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank
    @Column(name = "endpoint", unique = true, nullable = false, length = 500)
    private String endpoint;
    
    @Column(name = "http_method", length = 10)
    private String httpMethod; // GET, POST, PUT, DELETE, or null for all methods
    
    @NotNull
    @Column(name = "is_public", nullable = false)
    private Boolean isPublic = false;
    
    @Column(name = "requires_auth", nullable = false)
    private Boolean requiresAuth = true;
    
    @Column(name = "required_role", length = 50)
    private String requiredRole; // ROLE_ADMIN, ROLE_MODERATOR, ROLE_USER, or null
    
    @Column(name = "description", length = 1000)
    private String description;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // Constructors
    public ApiConfig() {
    }
    
    public ApiConfig(String endpoint, String httpMethod, Boolean isPublic, Boolean requiresAuth, String requiredRole, String description) {
        this.endpoint = endpoint;
        this.httpMethod = httpMethod;
        this.isPublic = isPublic;
        this.requiresAuth = requiresAuth;
        this.requiredRole = requiredRole;
        this.description = description;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getEndpoint() {
        return endpoint;
    }
    
    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }
    
    public String getHttpMethod() {
        return httpMethod;
    }
    
    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }
    
    public Boolean getIsPublic() {
        return isPublic;
    }
    
    public void setIsPublic(Boolean isPublic) {
        this.isPublic = isPublic;
    }
    
    public Boolean getRequiresAuth() {
        return requiresAuth;
    }
    
    public void setRequiresAuth(Boolean requiresAuth) {
        this.requiresAuth = requiresAuth;
    }
    
    public String getRequiredRole() {
        return requiredRole;
    }
    
    public void setRequiredRole(String requiredRole) {
        this.requiredRole = requiredRole;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}


