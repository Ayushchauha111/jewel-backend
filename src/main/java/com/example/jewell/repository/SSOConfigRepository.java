package com.example.jewell.repository;

import com.example.jewell.model.SSOConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SSOConfigRepository extends JpaRepository<SSOConfig, Long> {
    Optional<SSOConfig> findByInstitutionId(Long institutionId);
    
    @Query("SELECT sso FROM SSOConfig sso WHERE sso.institution.id = :institutionId AND sso.isEnabled = true")
    Optional<SSOConfig> findActiveSSOByInstitutionId(@Param("institutionId") Long institutionId);
}
