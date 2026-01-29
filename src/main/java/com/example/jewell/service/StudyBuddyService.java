package com.example.jewell.service;

import com.example.jewell.model.StudyBuddy;
import com.example.jewell.model.User;
import com.example.jewell.repository.StudyBuddyRepository;
import com.example.jewell.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class StudyBuddyService {
    
    @Autowired
    private StudyBuddyRepository studyBuddyRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    public StudyBuddy createOrUpdateStudyBuddy(Long userId, StudyBuddy studyBuddy) {
        // Validate required fields
        if (studyBuddy.getExam() == null || studyBuddy.getExam().trim().isEmpty()) {
            throw new IllegalArgumentException("Exam is required");
        }
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        
        Optional<StudyBuddy> existing = studyBuddyRepository.findByUserId(userId);
        
        if (existing.isPresent()) {
            StudyBuddy existingBuddy = existing.get();
            existingBuddy.setExam(studyBuddy.getExam());
            existingBuddy.setAvailability(studyBuddy.getAvailability());
            if (studyBuddy.getStatus() != null) {
                existingBuddy.setStatus(studyBuddy.getStatus());
            }
            existingBuddy.setBio(studyBuddy.getBio());
            existingBuddy.setPreferredStudyMethod(studyBuddy.getPreferredStudyMethod());
            existingBuddy.setTimezone(studyBuddy.getTimezone());
            if (studyBuddy.getIsActive() != null) {
                existingBuddy.setIsActive(studyBuddy.getIsActive());
            }
            return studyBuddyRepository.save(existingBuddy);
        } else {
            // Create new - ensure user is not set from request body
            studyBuddy.setUser(user);
            studyBuddy.setId(null); // Ensure it's a new entity
            if (studyBuddy.getStatus() == null) {
                studyBuddy.setStatus("Available");
            }
            studyBuddy.setIsActive(true);
            return studyBuddyRepository.save(studyBuddy);
        }
    }
    
    public Optional<StudyBuddy> getStudyBuddyByUserId(Long userId) {
        return studyBuddyRepository.findByUserId(userId);
    }
    
    public List<StudyBuddy> findAvailableBuddies(String exam, String availability, String searchTerm) {
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            return studyBuddyRepository.searchAvailableBuddies(searchTerm);
        }
        
        if (exam != null && !exam.trim().isEmpty()) {
            if (availability != null && !availability.trim().isEmpty()) {
                List<StudyBuddy> byExam = studyBuddyRepository.findByExamAndIsActiveTrue(exam);
                return byExam.stream()
                        .filter(buddy -> availability.equals(buddy.getAvailability()))
                        .toList();
            }
            return studyBuddyRepository.findAvailableBuddiesByExam(exam);
        }
        
        if (availability != null && !availability.trim().isEmpty()) {
            return studyBuddyRepository.findByAvailabilityAndIsActiveTrue(availability);
        }
        
        return studyBuddyRepository.findByIsActiveTrueOrderByCreatedAtDesc();
    }
    
    @Transactional
    public void deactivateStudyBuddy(Long userId) {
        Optional<StudyBuddy> buddy = studyBuddyRepository.findByUserId(userId);
        if (buddy.isPresent()) {
            StudyBuddy studyBuddy = buddy.get();
            studyBuddy.setIsActive(false);
            studyBuddy.setStatus("Inactive");
            studyBuddyRepository.save(studyBuddy);
        }
    }
}

