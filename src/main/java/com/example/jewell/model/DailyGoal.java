package com.example.jewell.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "daily_goals",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "goal_date"}, name = "uk_user_goal_date"))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailyGoal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonBackReference
    private User user;

    @Column(name = "goal_minutes", nullable = false)
    private Integer goalMinutes = 15;

    @Column(name = "today_time_spent_seconds")
    private Integer todayTimeSpentSeconds = 0;

    @Column(name = "goal_date", nullable = false)
    private LocalDate goalDate;

    @Column(name = "is_completed")
    private Boolean isCompleted = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (goalDate == null) {
            goalDate = LocalDate.now();
        }
        if (goalMinutes == null) {
            goalMinutes = 15;
        }
        if (todayTimeSpentSeconds == null) {
            todayTimeSpentSeconds = 0;
        }
        if (isCompleted == null) {
            isCompleted = false;
        }
    }
}
