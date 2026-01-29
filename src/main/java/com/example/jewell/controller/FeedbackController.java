package com.example.jewell.controller;

import com.example.jewell.model.Feedback;
import com.example.jewell.payload.response.MessageResponse;
import com.example.jewell.security.services.UserDetailsImpl;
import com.example.jewell.service.FeedbackService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/feedback")
@CrossOrigin(origins = "*", maxAge = 3600)
public class FeedbackController {

    @Autowired
    private FeedbackService feedbackService;

    /**
     * Submit feedback (public endpoint)
     */
    @PostMapping
    public ResponseEntity<?> submitFeedback(
            @Valid @RequestBody Feedback feedback,
            Authentication authentication) {
        try {
            Long userId = null;
            if (authentication != null && authentication.isAuthenticated()) {
                UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
                userId = userDetails.getId();
            }
            
            Feedback savedFeedback = feedbackService.submitFeedback(feedback, userId);
            return ResponseEntity.ok(new MessageResponse("Thank you for your feedback! It will be reviewed before being displayed."));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error submitting feedback: " + e.getMessage()));
        }
    }

    /**
     * Get approved feedbacks for homepage (public endpoint)
     */
    @GetMapping("/approved")
    public ResponseEntity<List<Feedback>> getApprovedFeedbacks(
            @RequestParam(defaultValue = "10") int limit) {
        List<Feedback> feedbacks = feedbackService.getApprovedFeedbacks(limit);
        return ResponseEntity.ok(feedbacks);
    }

    /**
     * Get all feedbacks (admin only)
     */
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Feedback>> getAllFeedbacks() {
        List<Feedback> feedbacks = feedbackService.getAllFeedbacks();
        return ResponseEntity.ok(feedbacks);
    }

    /**
     * Approve feedback (admin only)
     */
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Feedback> approveFeedback(@PathVariable Long id) {
        Feedback feedback = feedbackService.approveFeedback(id);
        return ResponseEntity.ok(feedback);
    }

    /**
     * Delete feedback (admin only)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteFeedback(@PathVariable Long id) {
        feedbackService.deleteFeedback(id);
        return ResponseEntity.ok(new MessageResponse("Feedback deleted successfully"));
    }
}

