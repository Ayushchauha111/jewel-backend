package com.example.jewell.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_history", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_template_id", columnList = "template_id"),
    @Index(name = "idx_sent_at", columnList = "sent_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class EmailHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "template_id", length = 100)
    private String templateId; // e.g., "welcome", "govtCourses", "custom", etc.

    @Column(name = "subject", length = 500)
    private String subject;

    @Column(name = "email_address", length = 255, nullable = false)
    private String emailAddress; // Store email address for quick reference

    @Column(name = "sent_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime sentAt;

    @Column(name = "status", length = 50)
    private String status; // "sent", "failed"

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    // Constructor for successful sends
    public EmailHistory(User user, String templateId, String subject, String emailAddress) {
        this.user = user;
        this.templateId = templateId;
        this.subject = subject;
        this.emailAddress = emailAddress;
        this.status = "sent";
    }

    // Constructor for failed sends
    public EmailHistory(User user, String templateId, String subject, String emailAddress, String errorMessage) {
        this.user = user;
        this.templateId = templateId;
        this.subject = subject;
        this.emailAddress = emailAddress;
        this.status = "failed";
        this.errorMessage = errorMessage;
    }
}

