package com.example.jewell.repository;

import com.example.jewell.model.MarksheetTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MarksheetTemplateRepository extends JpaRepository<MarksheetTemplate, Long> {
    List<MarksheetTemplate> findByInstitutionIdAndIsActiveTrue(Long institutionId);
    
    @Query("SELECT mt FROM MarksheetTemplate mt WHERE mt.institution.id = :institutionId AND mt.isDefault = true")
    Optional<MarksheetTemplate> findDefaultTemplateByInstitutionId(@Param("institutionId") Long institutionId);
}
