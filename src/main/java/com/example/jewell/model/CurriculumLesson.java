package com.example.jewell.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.Type;
import com.vladmihalcea.hibernate.type.json.JsonType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "curriculum_lessons")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class CurriculumLesson {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lesson_id", nullable = false)
    private Long lessonId; // Original lesson_id from source

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id", nullable = false)
    @JsonBackReference
    private CurriculumUnit unit;

    @Column(name = "answer_key_id")
    private Long answerKeyId;

    @Column(name = "content_type", length = 50)
    private String contentType; // "standard", "written-prompt", "qa", "adventure", "click-and-drag", etc.

    @Column(name = "lesson_type", nullable = false, length = 50)
    private String lessonType; // "lesson", "grouping"

    @Column(name = "min_accuracy")
    private Integer minAccuracy = 80;

    @Column(name = "badge", length = 100)
    private String badge;

    @Column(name = "lesson_index")
    private Integer lessonIndex;

    @Column(name = "total_words")
    private Integer totalWords;

    @Column(name = "time_limit")
    private Integer timeLimit; // in seconds

    @Column(name = "lesson_name", nullable = false)
    private String lessonName;

    @Column(name = "display_index")
    private Integer displayIndex;

    @Column(name = "display_order")
    private Integer displayOrder = 0;

    @Column(name = "screens")
    private Integer screens = 0;

    @Column(name = "settings", columnDefinition = "JSON")
    @Type(JsonType.class)
    private Map<String, Object> settings; // Store lesson-specific settings as JSON

    @Column(name = "default_lesson")
    private Boolean defaultLesson = false;

    @Column(name = "keyboard", length = 50)
    private String keyboard = "qwerty";

    @Column(name = "is_active")
    private Boolean isActive = true;

    @OneToMany(mappedBy = "lesson", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<CurriculumScreen> curriculumScreens = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
