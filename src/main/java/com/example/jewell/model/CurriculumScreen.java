package com.example.jewell.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.Type;
import com.vladmihalcea.hibernate.type.json.JsonType;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "curriculum_screens")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class CurriculumScreen {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "screen_id", nullable = false)
    private Long screenId; // Original screen_id from source

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    @JsonBackReference
    private CurriculumLesson lesson;

    @Column(name = "screen_name")
    private String screenName;

    @Column(name = "content_type", nullable = false, length = 50)
    private String contentType; // "standard", "written-prompt", "qa", etc.

    @Column(name = "screen_index")
    private Integer screenIndex = 0;

    @Column(name = "settings", columnDefinition = "JSON")
    @Type(JsonType.class)
    private Map<String, Object> settings; // Store screen-specific settings as JSON

    @Column(name = "content", columnDefinition = "TEXT")
    private String content; // Screen content/text

    @Column(name = "is_active")
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
