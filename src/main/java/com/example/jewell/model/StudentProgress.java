package com.example.jewell.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "student_progress",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "lesson_id"}, name = "uk_student_lesson"))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentProgress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @Column(name = "is_completed")
    private Boolean isCompleted = false;

    @Column(name = "best_wpm")
    private Integer bestWpm = 0;

    @Column(name = "best_accuracy")
    private Double bestAccuracy = 0.0;

    @Column(name = "attempts_count")
    private Integer attemptsCount = 0;

    @Column(name = "coins_earned")
    private Integer coinsEarned = 0;

    @Column(name = "stars_earned")
    private Integer starsEarned = 0;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "last_attempted_at")
    private LocalDateTime lastAttemptedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
