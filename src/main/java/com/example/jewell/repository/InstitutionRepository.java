package com.example.jewell.repository;

import com.example.jewell.model.Institution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InstitutionRepository extends JpaRepository<Institution, Long> {
    Optional<Institution> findByLicenseKey(String licenseKey);
    
    Optional<Institution> findByDomain(String domain);
    
    Optional<Institution> findBySubdomain(String subdomain);
    
    @Query("SELECT i FROM Institution i WHERE i.adminUser.id = :adminUserId")
    List<Institution> findByAdminUserId(@Param("adminUserId") Long adminUserId);
    
    @Query("SELECT i FROM Institution i WHERE i.adminUser.id = :adminUserId AND i.isActive = true ORDER BY i.createdAt DESC")
    List<Institution> findActiveInstitutionsByAdminUserId(@Param("adminUserId") Long adminUserId);
}
