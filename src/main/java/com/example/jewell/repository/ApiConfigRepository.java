package com.example.jewell.repository;

import com.example.jewell.model.ApiConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApiConfigRepository extends JpaRepository<ApiConfig, Long> {
    Optional<ApiConfig> findByEndpointAndHttpMethod(String endpoint, String httpMethod);
    
    Optional<ApiConfig> findByEndpoint(String endpoint);
    
    List<ApiConfig> findByIsActiveTrue();
    
    @Query("SELECT a FROM ApiConfig a WHERE a.isActive = true AND " +
           "(:endpoint LIKE CONCAT(a.endpoint, '%') OR a.endpoint LIKE CONCAT(:endpoint, '%')) " +
           "AND (a.httpMethod IS NULL OR a.httpMethod = :httpMethod OR :httpMethod IS NULL)")
    List<ApiConfig> findMatchingConfigs(@Param("endpoint") String endpoint, @Param("httpMethod") String httpMethod);
}


