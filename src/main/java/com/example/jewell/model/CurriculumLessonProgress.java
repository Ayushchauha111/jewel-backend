package com.example.jewell.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "curriculum_lesson_progress",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "lesson_id"}, name = "uk_user_curriculum_lesson"))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CurriculumLessonProgress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonBackReference
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    @JsonBackReference
    private CurriculumLesson curriculumLesson;

    @Column(name = "time_spent_seconds")
    private Integer timeSpentSeconds = 0;

    @Column(name = "progress_percentage")
    private Integer progressPercentage = 0; // 0-100

    @Column(name = "avg_wpm")
    private Integer avgWpm = 0;

    @Column(name = "avg_accuracy")
    private Double avgAccuracy = 0.0;

    @Column(name = "best_wpm")
    private Integer bestWpm = 0;

    @Column(name = "best_accuracy")
    private Double bestAccuracy = 0.0;

    @Column(name = "attempts_count")
    private Integer attemptsCount = 0;

    @Column(name = "is_completed")
    private Boolean isCompleted = false;

    @Column(name = "last_attempted_at")
    private LocalDateTime lastAttemptedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
