package com.example.jewell.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "feature_flags", uniqueConstraints = @UniqueConstraint(columnNames = "feature_key"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeatureFlag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "feature_key", unique = true, nullable = false)
    private String featureKey; // e.g., "inner_peace_hub", "timeless_sanctuary", "study_soulmates"

    @Column(name = "feature_name", nullable = false)
    private String featureName; // Display name: "Inner Peace Hub", "24/7 Study Room", etc.

    @Column(name = "is_enabled", nullable = false)
    @Builder.Default
    private Boolean isEnabled = true;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "category")
    private String category; // e.g., "community", "social", "games"

    @Column(name = "display_order")
    private Integer displayOrder; // For ordering in admin panel

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (isEnabled == null) isEnabled = true;
    }
}

