package com.example.jewell.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "parent_links")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParentLink {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(name = "parent_email", nullable = false)
    private String parentEmail;

    @Column(name = "parent_phone", length = 20)
    private String parentPhone;

    @Column(name = "parent_name")
    private String parentName;

    @Column(name = "relationship", length = 50)
    private String relationship;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_preference")
    private NotificationPreference notificationPreference = NotificationPreference.EMAIL;

    @Column(name = "is_verified")
    private Boolean isVerified = false;

    @Column(name = "verification_token")
    private String verificationToken;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum NotificationPreference {
        EMAIL,
        WHATSAPP,
        BOTH
    }
}
