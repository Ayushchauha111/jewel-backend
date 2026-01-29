package com.example.jewell.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "lessons")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Lesson {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "world_id", nullable = false)
    @JsonBackReference
    private World world;

    @Column(name = "lesson_name", nullable = false)
    private String lessonName;

    @Column(name = "lesson_number", nullable = false)
    private Integer lessonNumber;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "narrative_english", columnDefinition = "TEXT")
    private String narrativeEnglish;

    @Column(name = "narrative_hindi", columnDefinition = "TEXT")
    private String narrativeHindi;

    @Column(name = "narrative_regional", columnDefinition = "TEXT")
    private String narrativeRegional;

    @Column(name = "target_wpm")
    private Integer targetWpm;

    @Column(name = "target_accuracy")
    private Double targetAccuracy;

    @Column(name = "coins_reward")
    private Integer coinsReward = 0;

    @Column(name = "stars_reward")
    private Integer starsReward = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "badge_id")
    private Badge badge;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "display_order")
    private Integer displayOrder = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
