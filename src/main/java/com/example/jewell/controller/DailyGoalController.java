package com.example.jewell.controller;

import com.example.jewell.dto.ApiResponseDTO;
import com.example.jewell.model.DailyGoal;
import com.example.jewell.service.DailyGoalService;
import com.example.jewell.utils.ServiceUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/daily-goal")
@CrossOrigin(origins = "*", maxAge = 3600)
public class DailyGoalController {

    @Autowired
    private DailyGoalService dailyGoalService;

    @PostMapping("/set")
    public ResponseEntity<ApiResponseDTO<DailyGoal>> setDailyGoal(
            @RequestBody Map<String, Object> goalData, Authentication authentication) {
        try {
            Long userId = ServiceUtils.getUserIdFromAuthentication(authentication);
            Integer goalMinutes = Integer.valueOf(goalData.get("goalMinutes").toString());

            DailyGoal dailyGoal = dailyGoalService.setDailyGoal(userId, goalMinutes);
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "Daily goal set successfully", dailyGoal));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Error setting daily goal: " + e.getMessage(), null));
        }
    }

    @GetMapping("/get")
    public ResponseEntity<ApiResponseDTO<DailyGoal>> getDailyGoal(Authentication authentication) {
        try {
            Long userId = ServiceUtils.getUserIdFromAuthentication(authentication);
            DailyGoal dailyGoal = dailyGoalService.getDailyGoal(userId);
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "Daily goal retrieved successfully", dailyGoal));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Error retrieving daily goal: " + e.getMessage(), null));
        }
    }

    @PostMapping("/update-time")
    public ResponseEntity<ApiResponseDTO<DailyGoal>> updateTimeSpent(
            @RequestBody Map<String, Object> timeData, Authentication authentication) {
        try {
            Long userId = ServiceUtils.getUserIdFromAuthentication(authentication);
            Integer additionalSeconds = Integer.valueOf(timeData.get("additionalSeconds").toString());

            DailyGoal dailyGoal = dailyGoalService.updateTimeSpent(userId, additionalSeconds);
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "Time spent updated successfully", dailyGoal));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Error updating time spent: " + e.getMessage(), null));
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> getDailyGoalStats(Authentication authentication) {
        try {
            Long userId = ServiceUtils.getUserIdFromAuthentication(authentication);
            Map<String, Object> stats = dailyGoalService.getDailyGoalStats(userId);
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "Daily goal stats retrieved successfully", stats));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "Error retrieving daily goal stats: " + e.getMessage(), null));
        }
    }
}
