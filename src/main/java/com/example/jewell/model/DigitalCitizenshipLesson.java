package com.example.jewell.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "digital_citizenship_lessons")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DigitalCitizenshipLesson {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lesson_title", nullable = false)
    private String lessonTitle;

    @Column(name = "lesson_content", columnDefinition = "TEXT", nullable = false)
    private String lessonContent;

    @Enumerated(EnumType.STRING)
    @Column(name = "lesson_type", nullable = false)
    private LessonType lessonType;

    @Column(name = "target_age_group", length = 50)
    private String targetAgeGroup;

    @Column(name = "video_url", length = 500)
    private String videoUrl;

    @Column(name = "interactive_quiz", columnDefinition = "TEXT")
    private String interactiveQuiz;

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

    public enum LessonType {
        TYPING_SAFETY,
        NETIQUETTE,
        CODING_SYNTAX,
        ONLINE_PRIVACY,
        CYBERBULLYING_PREVENTION
    }
}
