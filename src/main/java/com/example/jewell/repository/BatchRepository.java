package com.example.jewell.repository;

import com.example.jewell.model.Batch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BatchRepository extends JpaRepository<Batch, Long> {
    List<Batch> findByInstitutionIdAndIsActiveTrue(Long institutionId);
    
    Optional<Batch> findByInstitutionIdAndBatchCode(Long institutionId, String batchCode);
    
    @Query("SELECT b FROM Batch b WHERE b.institution.id = :institutionId AND b.isActive = true")
    List<Batch> findActiveBatchesByInstitutionId(@Param("institutionId") Long institutionId);
    
    @Query("SELECT b FROM Batch b JOIN b.students s WHERE s.id = :userId AND b.isActive = true")
    List<Batch> findBatchesByStudentId(@Param("userId") Long userId);
    
    @Query("SELECT DISTINCT b FROM Batch b LEFT JOIN FETCH b.students WHERE b.institution.id = :institutionId")
    List<Batch> findByInstitutionId(@Param("institutionId") Long institutionId);
    
    @Query("SELECT DISTINCT b FROM Batch b LEFT JOIN FETCH b.students WHERE b.institution.id = :institutionId AND b.isActive = true")
    List<Batch> findActiveBatchesWithStudentsByInstitutionId(@Param("institutionId") Long institutionId);
}
