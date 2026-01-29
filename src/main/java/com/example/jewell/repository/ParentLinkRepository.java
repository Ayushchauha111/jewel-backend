package com.example.jewell.repository;

import com.example.jewell.model.ParentLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParentLinkRepository extends JpaRepository<ParentLink, Long> {
    List<ParentLink> findByStudentId(Long studentId);
    
    List<ParentLink> findByParentEmail(String parentEmail);
    
    Optional<ParentLink> findByVerificationToken(String verificationToken);
    
    @Query("SELECT pl FROM ParentLink pl WHERE pl.student.id = :studentId AND pl.isActive = true")
    List<ParentLink> findActiveLinksByStudentId(@Param("studentId") Long studentId);
}
