package com.example.jewell.repository;

import com.example.jewell.model.StudyBuddy;
import com.example.jewell.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudyBuddyRepository extends JpaRepository<StudyBuddy, Long> {
    Optional<StudyBuddy> findByUser(User user);
    
    Optional<StudyBuddy> findByUserId(Long userId);
    
    List<StudyBuddy> findByExamAndIsActiveTrue(String exam);
    
    List<StudyBuddy> findByAvailabilityAndIsActiveTrue(String availability);
    
    List<StudyBuddy> findByStatusAndIsActiveTrue(String status);
    
    @Query("SELECT sb FROM StudyBuddy sb WHERE sb.isActive = true AND sb.status = 'Available' AND sb.exam = ?1")
    List<StudyBuddy> findAvailableBuddiesByExam(String exam);
    
    @Query("SELECT sb FROM StudyBuddy sb WHERE sb.isActive = true AND sb.status = 'Available' AND (sb.exam LIKE %?1% OR sb.user.username LIKE %?1%)")
    List<StudyBuddy> searchAvailableBuddies(String searchTerm);
    
    List<StudyBuddy> findByIsActiveTrueOrderByCreatedAtDesc();
}

