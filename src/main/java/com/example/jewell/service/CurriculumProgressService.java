package com.example.jewell.service;

import com.example.jewell.model.CurriculumLesson;
import com.example.jewell.model.CurriculumLessonProgress;
import com.example.jewell.model.User;
import com.example.jewell.repository.CurriculumLessonProgressRepository;
import com.example.jewell.repository.CurriculumLessonRepository;
import com.example.jewell.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CurriculumProgressService {
    
    @Autowired
    private CurriculumLessonProgressRepository progressRepository;
    
    @Autowired
    private CurriculumLessonRepository lessonRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Transactional
    public CurriculumLessonProgress updateProgress(Long userId, Long lessonId, Integer timeSpentSeconds, 
                                                   Integer wpm, Double accuracy, Integer progressPercentage, Boolean completed) {
        Optional<CurriculumLessonProgress> progressOpt = progressRepository.findByUserIdAndCurriculumLessonId(userId, lessonId);
        CurriculumLessonProgress progress;
        
        if (progressOpt.isPresent()) {
            progress = progressOpt.get();
        } else {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            CurriculumLesson lesson = lessonRepository.findById(lessonId)
                    .orElseThrow(() -> new RuntimeException("Lesson not found"));
            progress = new CurriculumLessonProgress();
            progress.setUser(user);
            progress.setCurriculumLesson(lesson);
        }
        
        // Update time spent (additive)
        if (timeSpentSeconds != null && timeSpentSeconds > 0) {
            progress.setTimeSpentSeconds(progress.getTimeSpentSeconds() + timeSpentSeconds);
        }
        
        // Update progress percentage
        if (progressPercentage != null) {
            progress.setProgressPercentage(Math.max(progress.getProgressPercentage(), progressPercentage));
        }
        
        // Update attempts
        progress.setAttemptsCount(progress.getAttemptsCount() + 1);
        progress.setLastAttemptedAt(LocalDateTime.now());
        
        // Update WPM and accuracy (calculate average)
        if (wpm != null && wpm > 0) {
            if (progress.getAttemptsCount() == 1) {
                progress.setAvgWpm(wpm);
            } else {
                // Calculate running average
                int currentAvg = progress.getAvgWpm();
                int newAvg = (currentAvg * (progress.getAttemptsCount() - 1) + wpm) / progress.getAttemptsCount();
                progress.setAvgWpm(newAvg);
            }
            
            // Update best WPM
            if (progress.getBestWpm() == null || wpm > progress.getBestWpm()) {
                progress.setBestWpm(wpm);
            }
        }
        
        if (accuracy != null && accuracy > 0) {
            if (progress.getAttemptsCount() == 1) {
                progress.setAvgAccuracy(accuracy);
            } else {
                // Calculate running average
                double currentAvg = progress.getAvgAccuracy();
                double newAvg = (currentAvg * (progress.getAttemptsCount() - 1) + accuracy) / progress.getAttemptsCount();
                progress.setAvgAccuracy(newAvg);
            }
            
            // Update best accuracy
            if (progress.getBestAccuracy() == null || accuracy > progress.getBestAccuracy()) {
                progress.setBestAccuracy(accuracy);
            }
        }
        
        // Mark as completed
        if (completed != null && completed) {
            progress.setIsCompleted(true);
            progress.setCompletedAt(LocalDateTime.now());
            progress.setProgressPercentage(100);
        }
        
        return progressRepository.save(progress);
    }
    
    @Transactional(readOnly = true)
    public Optional<CurriculumLessonProgress> getProgress(Long userId, Long lessonId) {
        return progressRepository.findByUserIdAndCurriculumLessonId(userId, lessonId);
    }
    
    @Transactional(readOnly = true)
    public Map<Long, CurriculumLessonProgress> getProgressForLessons(Long userId, List<Long> lessonIds) {
        List<CurriculumLessonProgress> progressList = progressRepository.findByUserId(userId);
        return progressList.stream()
                .filter(p -> lessonIds.contains(p.getCurriculumLesson().getId()))
                .collect(Collectors.toMap(
                    p -> p.getCurriculumLesson().getId(),
                    p -> p
                ));
    }
    
    @Transactional(readOnly = true)
    public List<CurriculumLessonProgress> getProgressForUnit(Long userId, Long unitId) {
        return progressRepository.findByUserIdAndUnitId(userId, unitId);
    }
    
    @Transactional(readOnly = true)
    public Map<String, Object> getUserStats(Long userId) {
        List<CurriculumLessonProgress> allProgress = progressRepository.findByUserId(userId);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalLessonsCompleted", allProgress.stream()
                .filter(CurriculumLessonProgress::getIsCompleted)
                .count());
        stats.put("totalTimeSpent", allProgress.stream()
                .mapToInt(p -> p.getTimeSpentSeconds() != null ? p.getTimeSpentSeconds() : 0)
                .sum());
        stats.put("averageWpm", allProgress.stream()
                .filter(p -> p.getAvgWpm() != null && p.getAvgWpm() > 0)
                .mapToInt(CurriculumLessonProgress::getAvgWpm)
                .average()
                .orElse(0.0));
        stats.put("averageAccuracy", allProgress.stream()
                .filter(p -> p.getAvgAccuracy() != null && p.getAvgAccuracy() > 0)
                .mapToDouble(CurriculumLessonProgress::getAvgAccuracy)
                .average()
                .orElse(0.0));
        stats.put("totalAttempts", allProgress.stream()
                .mapToInt(p -> p.getAttemptsCount() != null ? p.getAttemptsCount() : 0)
                .sum());
        
        return stats;
    }
}
