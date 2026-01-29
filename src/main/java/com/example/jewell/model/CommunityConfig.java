package com.example.jewell.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "community_configs",
       uniqueConstraints = @UniqueConstraint(columnNames = "config_key"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommunityConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "config_key", unique = true, nullable = false)
    private String configKey; // e.g., "mental_health_whatsapp", "study_room_meet", "ssc_cgl_telegram"

    @Column(name = "config_value", columnDefinition = "TEXT", nullable = false)
    private String configValue; // The actual link or value

    @Column(name = "config_type")
    private String configType; // "whatsapp", "telegram", "google_meet", "other"

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "display_order")
    private Integer displayOrder; // For ordering in UI

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (isActive == null) isActive = true;
    }
}

