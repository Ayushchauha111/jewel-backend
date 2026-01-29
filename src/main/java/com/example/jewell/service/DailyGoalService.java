package com.example.jewell.service;

import com.example.jewell.model.DailyGoal;
import com.example.jewell.model.User;
import com.example.jewell.repository.DailyGoalRepository;
import com.example.jewell.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class DailyGoalService {

    @Autowired
    private DailyGoalRepository dailyGoalRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public DailyGoal setDailyGoal(Long userId, Integer goalMinutes) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        LocalDate today = LocalDate.now();
        Optional<DailyGoal> existingGoal = dailyGoalRepository.findByUserIdAndGoalDate(userId, today);

        DailyGoal dailyGoal;
        if (existingGoal.isPresent()) {
            dailyGoal = existingGoal.get();
            dailyGoal.setGoalMinutes(goalMinutes);
        } else {
            dailyGoal = new DailyGoal();
            dailyGoal.setUser(user);
            dailyGoal.setGoalDate(today);
            dailyGoal.setGoalMinutes(goalMinutes);
            dailyGoal.setTodayTimeSpentSeconds(0);
            dailyGoal.setIsCompleted(false);
        }

        // Check if goal is completed
        updateGoalCompletionStatus(dailyGoal);

        return dailyGoalRepository.save(dailyGoal);
    }

    @Transactional
    public DailyGoal getDailyGoal(Long userId) {
        LocalDate today = LocalDate.now();
        Optional<DailyGoal> todayGoal = dailyGoalRepository.findByUserIdAndGoalDate(userId, today);

        if (todayGoal.isPresent()) {
            DailyGoal goal = todayGoal.get();
            // Update completion status before returning
            updateGoalCompletionStatus(goal);
            return dailyGoalRepository.save(goal);
        }

        // If no goal exists for today, return the latest goal or create a default one
        Optional<DailyGoal> latestGoal = dailyGoalRepository.findLatestGoalByUserId(userId);
        if (latestGoal.isPresent()) {
            DailyGoal goal = latestGoal.get();
            // Create a new goal for today with the same goal minutes
            DailyGoal newGoal = new DailyGoal();
            newGoal.setUser(goal.getUser());
            newGoal.setGoalDate(today);
            newGoal.setGoalMinutes(goal.getGoalMinutes());
            newGoal.setTodayTimeSpentSeconds(0);
            newGoal.setIsCompleted(false);
            return dailyGoalRepository.save(newGoal);
        }

        // Create default goal
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
        DailyGoal defaultGoal = new DailyGoal();
        defaultGoal.setUser(user);
        defaultGoal.setGoalDate(today);
        defaultGoal.setGoalMinutes(15);
        defaultGoal.setTodayTimeSpentSeconds(0);
        defaultGoal.setIsCompleted(false);
        return dailyGoalRepository.save(defaultGoal);
    }

    @Transactional
    public DailyGoal updateTimeSpent(Long userId, Integer additionalSeconds) {
        LocalDate today = LocalDate.now();
        Optional<DailyGoal> todayGoal = dailyGoalRepository.findByUserIdAndGoalDate(userId, today);

        DailyGoal dailyGoal;
        if (todayGoal.isPresent()) {
            dailyGoal = todayGoal.get();
            dailyGoal.setTodayTimeSpentSeconds(dailyGoal.getTodayTimeSpentSeconds() + additionalSeconds);
        } else {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
            dailyGoal = new DailyGoal();
            dailyGoal.setUser(user);
            dailyGoal.setGoalDate(today);
            dailyGoal.setGoalMinutes(15); // Default goal
            dailyGoal.setTodayTimeSpentSeconds(additionalSeconds);
            dailyGoal.setIsCompleted(false);
        }

        // Check if goal is completed
        updateGoalCompletionStatus(dailyGoal);

        return dailyGoalRepository.save(dailyGoal);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDailyGoalStats(Long userId) {
        DailyGoal goal = getDailyGoal(userId);

        Map<String, Object> stats = new HashMap<>();
        stats.put("goalMinutes", goal.getGoalMinutes());
        stats.put("todayTimeSpentSeconds", goal.getTodayTimeSpentSeconds());
        stats.put("isCompleted", goal.getIsCompleted());
        stats.put("goalDate", goal.getGoalDate().toString());

        // Calculate progress percentage
        int goalSeconds = goal.getGoalMinutes() * 60;
        double progressPercentage = goalSeconds > 0 
            ? Math.min((goal.getTodayTimeSpentSeconds().doubleValue() / goalSeconds) * 100, 100) 
            : 0;
        stats.put("progressPercentage", progressPercentage);

        return stats;
    }

    private void updateGoalCompletionStatus(DailyGoal goal) {
        int goalSeconds = goal.getGoalMinutes() * 60;
        boolean completed = goal.getTodayTimeSpentSeconds() >= goalSeconds;
        goal.setIsCompleted(completed);
    }
}
