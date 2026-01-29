package com.example.jewell.controller;

import com.example.jewell.dto.ApiResponseDTO;
import com.example.jewell.model.CurriculumLessonProgress;
import com.example.jewell.service.CurriculumProgressService;
import com.example.jewell.utils.ServiceUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/curriculum/progress")
@CrossOrigin(origins = "*", maxAge = 3600)
public class CurriculumProgressController {
    
    @Autowired
    private CurriculumProgressService progressService;
    
    @PostMapping("/update")
    public ResponseEntity<ApiResponseDTO<CurriculumLessonProgress>> updateProgress(
            @RequestBody Map<String, Object> progressData, Authentication authentication) {
        try {
            Long userId = ServiceUtils.getUserIdFromAuthentication(authentication);
            Long lessonId = Long.valueOf(progressData.get("lessonId").toString());
            Integer timeSpentSeconds = progressData.get("timeSpentSeconds") != null ? 
                    Integer.valueOf(progressData.get("timeSpentSeconds").toString()) : null;
            Integer wpm = progressData.get("wpm") != null ? 
                    Integer.valueOf(progressData.get("wpm").toString()) : null;
            Double accuracy = progressData.get("accuracy") != null ? 
                    Double.valueOf(progressData.get("accuracy").toString()) : null;
            Integer progressPercentage = progressData.get("progressPercentage") != null ? 
                    Integer.valueOf(progressData.get("progressPercentage").toString()) : null;
            Boolean completed = progressData.get("completed") != null ? 
                    Boolean.valueOf(progressData.get("completed").toString()) : null;
            
            CurriculumLessonProgress progress = progressService.updateProgress(
                    userId, lessonId, timeSpentSeconds, wpm, accuracy, progressPercentage, completed);
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "Progress updated successfully", progress));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Error updating progress: " + e.getMessage(), null));
        }
    }
    
    @GetMapping("/lesson/{lessonId}")
    public ResponseEntity<ApiResponseDTO<CurriculumLessonProgress>> getProgress(
            @PathVariable Long lessonId, Authentication authentication) {
        try {
            Long userId = ServiceUtils.getUserIdFromAuthentication(authentication);
            Optional<CurriculumLessonProgress> progress = progressService.getProgress(userId, lessonId);
            if (progress.isPresent()) {
                return ResponseEntity.ok(new ApiResponseDTO<>(true, "Progress retrieved successfully", progress.get()));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponseDTO<>(false, "Progress not found", null));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Error retrieving progress: " + e.getMessage(), null));
        }
    }
    
    @GetMapping("/unit/{unitId}")
    public ResponseEntity<ApiResponseDTO<List<CurriculumLessonProgress>>> getProgressForUnit(
            @PathVariable Long unitId, Authentication authentication) {
        try {
            Long userId = ServiceUtils.getUserIdFromAuthentication(authentication);
            List<CurriculumLessonProgress> progressList = progressService.getProgressForUnit(userId, unitId);
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "Progress retrieved successfully", progressList));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Error retrieving progress: " + e.getMessage(), null));
        }
    }
    
    @GetMapping("/stats")
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> getUserStats(Authentication authentication) {
        try {
            Long userId = ServiceUtils.getUserIdFromAuthentication(authentication);
            Map<String, Object> stats = progressService.getUserStats(userId);
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "Stats retrieved successfully", stats));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Error retrieving stats: " + e.getMessage(), null));
        }
    }
}
