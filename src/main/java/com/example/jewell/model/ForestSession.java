package com.example.jewell.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "forest_sessions",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "session_date"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ForestSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @Column(name = "trees_planted")
    @Builder.Default
    private Integer treesPlanted = 0; // Number of focused sessions (trees)

    @Column(name = "total_focus_minutes")
    @Builder.Default
    private Integer totalFocusMinutes = 0; // Total minutes of focused study

    @Column(name = "longest_session_minutes")
    @Builder.Default
    private Integer longestSessionMinutes = 0; // Longest continuous session

    @Column(name = "sessions_count")
    @Builder.Default
    private Integer sessionsCount = 0; // Number of study sessions today

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (treesPlanted == null) treesPlanted = 0;
        if (totalFocusMinutes == null) totalFocusMinutes = 0;
        if (longestSessionMinutes == null) longestSessionMinutes = 0;
        if (sessionsCount == null) sessionsCount = 0;
        if (sessionDate == null) sessionDate = LocalDate.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

