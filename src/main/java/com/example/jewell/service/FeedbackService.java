package com.example.jewell.service;

import com.example.jewell.model.Feedback;
import com.example.jewell.model.User;
import com.example.jewell.repository.FeedbackRepository;
import com.example.jewell.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class FeedbackService {
    
    @Autowired
    private FeedbackRepository feedbackRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * Submit new feedback
     */
    public Feedback submitFeedback(Feedback feedback, Long userId) {
        // Link to user if provided
        if (userId != null) {
            Optional<User> user = userRepository.findById(userId);
            user.ifPresent(feedback::setUser);
        }
        
        // New feedbacks are not approved by default
        feedback.setIsApproved(false);
        
        return feedbackRepository.save(feedback);
    }
    
    /**
     * Get all approved feedbacks for display on homepage
     */
    public List<Feedback> getApprovedFeedbacks(int limit) {
        if (limit > 0) {
            return feedbackRepository.findTop10ByIsApprovedTrueOrderByCreatedAtDesc();
        }
        return feedbackRepository.findByIsApprovedTrueOrderByCreatedAtDesc();
    }
    
    /**
     * Get all feedbacks (admin only)
     */
    public List<Feedback> getAllFeedbacks() {
        return feedbackRepository.findAllByOrderByCreatedAtDesc();
    }
    
    /**
     * Approve a feedback (admin only)
     */
    public Feedback approveFeedback(Long id) {
        Optional<Feedback> feedbackOpt = feedbackRepository.findById(id);
        if (feedbackOpt.isPresent()) {
            Feedback feedback = feedbackOpt.get();
            feedback.setIsApproved(true);
            feedback.setApprovedAt(LocalDateTime.now());
            return feedbackRepository.save(feedback);
        }
        throw new RuntimeException("Feedback not found with id: " + id);
    }
    
    /**
     * Reject/Delete a feedback (admin only)
     */
    public void deleteFeedback(Long id) {
        feedbackRepository.deleteById(id);
    }
    
    /**
     * Get feedback by ID
     */
    public Optional<Feedback> getFeedbackById(Long id) {
        return feedbackRepository.findById(id);
    }
}


