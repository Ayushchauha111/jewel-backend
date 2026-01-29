package com.example.jewell.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * StudyRoomSession entity for tracking active user sessions in study rooms
 * This ensures we only count users who actually joined the meeting
 */
@Entity
@Table(name = "study_room_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudyRoomSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "room_id", nullable = false)
    private StudyRoom room;

    @Column(name = "is_confirmed")
    @Builder.Default
    private Boolean isConfirmed = false; // Only true when user confirms they joined

    @Column(name = "link_opened_at")
    private LocalDateTime linkOpenedAt; // When user clicked join and link was opened

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt; // When user confirmed they joined

    @Column(name = "expires_at")
    private LocalDateTime expiresAt; // Auto-expire unconfirmed sessions after 5 minutes

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (isConfirmed == null) isConfirmed = false;
        if (linkOpenedAt == null) linkOpenedAt = LocalDateTime.now();
        if (expiresAt == null) expiresAt = LocalDateTime.now().plusMinutes(5); // 5 min timeout
    }
}

