package com.example.jewell.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_streaks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserStreak {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    // Current streak count
    private Integer currentStreak;

    // Longest streak ever achieved
    private Integer longestStreak;

    // Last practice/challenge date
    private LocalDate lastActivityDate;

    // Total days practiced
    private Integer totalDaysPracticed;

    // Total XP earned
    private Long totalXp;

    // Current level
    private Integer level;

    // Streak freeze available (skip a day without breaking streak)
    private Integer streakFreezes;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (currentStreak == null) currentStreak = 0;
        if (longestStreak == null) longestStreak = 0;
        if (totalDaysPracticed == null) totalDaysPracticed = 0;
        if (totalXp == null) totalXp = 0L;
        if (level == null) level = 1;
        if (streakFreezes == null) streakFreezes = 0;
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
        
        // Update longest streak if current is higher
        if (currentStreak > longestStreak) {
            longestStreak = currentStreak;
        }
        
        // Calculate level based on XP (every 1000 XP = 1 level)
        level = (int) (totalXp / 1000) + 1;
    }

    /**
     * Add XP and update streak
     */
    public void addXpAndUpdateStreak(int xp, LocalDate activityDate) {
        // Initialize if null
        if (this.totalXp == null) this.totalXp = 0L;
        if (this.currentStreak == null) this.currentStreak = 0;
        if (this.longestStreak == null) this.longestStreak = 0;
        if (this.totalDaysPracticed == null) this.totalDaysPracticed = 0;
        if (this.level == null) this.level = 1;
        if (this.streakFreezes == null) this.streakFreezes = 0;
        
        this.totalXp += xp;
        
        if (lastActivityDate == null) {
            // First activity ever
            currentStreak = 1;
            totalDaysPracticed = 1;
            lastActivityDate = activityDate;
        } else if (activityDate.equals(lastActivityDate)) {
            // Same day - just add XP, no streak change
            // Don't update lastActivityDate to keep it as the first activity of the day
        } else if (activityDate.equals(lastActivityDate.plusDays(1))) {
            // Consecutive day - increase streak
            currentStreak++;
            totalDaysPracticed++;
            lastActivityDate = activityDate;
        } else if (activityDate.isAfter(lastActivityDate.plusDays(1))) {
            // Missed days - check for streak freeze
            long daysMissed = activityDate.toEpochDay() - lastActivityDate.toEpochDay() - 1;
            if (streakFreezes >= daysMissed) {
                // Use streak freezes
                streakFreezes -= (int) daysMissed;
                currentStreak++;
            } else {
                // Reset streak
                currentStreak = 1;
            }
            totalDaysPracticed++;
            lastActivityDate = activityDate;
        } else {
            // Activity date is before last activity date (shouldn't happen, but handle it)
            // Just update the date if it's newer
            if (activityDate.isAfter(lastActivityDate)) {
                lastActivityDate = activityDate;
            }
        }
        
        // Update longest streak immediately
        if (currentStreak > longestStreak) {
            longestStreak = currentStreak;
        }
        
        // Update level based on XP (every 1000 XP = 1 level)
        level = (int) (totalXp / 1000) + 1;
    }
}

