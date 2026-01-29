package com.example.jewell.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * StudyRoom entity for managing multiple study room instances
 * Each room has a capacity limit and tracks active participants
 */
@Entity
@Table(name = "study_rooms")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudyRoom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_name", nullable = false)
    private String roomName; // e.g., "Study Room 1", "Study Room 2"

    @Column(name = "meet_link", columnDefinition = "TEXT", nullable = false)
    private String meetLink; // Google Meet link or alternative (YouTube Live, Discord, etc.)

    @Column(name = "platform")
    private String platform; // "google_meet", "youtube_live", "discord", "zoom", "other"

    @Column(name = "max_capacity", nullable = false)
    @Builder.Default
    private Integer maxCapacity = 100; // Default Google Meet free limit

    @Column(name = "current_participants")
    @Builder.Default
    private Integer currentParticipants = 0;

    @Column(name = "room_type")
    private String roomType; // "study_room", "zen_session", "webinar"

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "region")
    private String region; // "us-east", "asia", "europe" for load balancing

    @Column(name = "priority")
    @Builder.Default
    private Integer priority = 0; // Higher priority rooms filled first

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (isActive == null) isActive = true;
        if (currentParticipants == null) currentParticipants = 0;
        if (maxCapacity == null) maxCapacity = 100;
        if (priority == null) priority = 0;
    }

    /**
     * Check if room has available space
     */
    public boolean hasSpace() {
        return currentParticipants < maxCapacity;
    }

    /**
     * Get available spots
     */
    public int getAvailableSpots() {
        return Math.max(0, maxCapacity - currentParticipants);
    }

    /**
     * Increment participant count (when user joins)
     */
    public void incrementParticipants() {
        if (hasSpace()) {
            this.currentParticipants++;
        }
    }

    /**
     * Decrement participant count (when user leaves)
     */
    public void decrementParticipants() {
        if (this.currentParticipants > 0) {
            this.currentParticipants--;
        }
    }
}

