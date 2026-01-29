package com.example.jewell.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "study_buddies",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class StudyBuddy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "exam", nullable = false)
    private String exam; // e.g., "SSC CGL", "IBPS Clerk", "RRB NTPC"

    @Column(name = "availability")
    private String availability; // "Morning", "Evening", "Night", "Flexible"

    @Column(name = "status")
    @Builder.Default
    private String status = "Available"; // "Available", "Busy", "Inactive"

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    @Column(name = "preferred_study_method")
    private String preferredStudyMethod; // "Online", "Offline", "Both"

    @Column(name = "timezone")
    private String timezone;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (status == null) status = "Available";
        if (isActive == null) isActive = true;
    }
}

