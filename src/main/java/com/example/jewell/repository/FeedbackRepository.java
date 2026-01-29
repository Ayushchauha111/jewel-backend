package com.example.jewell.repository;

import com.example.jewell.model.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    
    // Find all approved feedbacks, ordered by creation date (newest first)
    List<Feedback> findByIsApprovedTrueOrderByCreatedAtDesc();
    
    // Find all approved feedbacks with limit
    List<Feedback> findTop10ByIsApprovedTrueOrderByCreatedAtDesc();
    
    // Find all feedbacks (for admin)
    List<Feedback> findAllByOrderByCreatedAtDesc();
    
    // Find feedbacks by user
    List<Feedback> findByUser_IdOrderByCreatedAtDesc(Long userId);
}


