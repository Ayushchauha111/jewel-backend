package com.example.jewell.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "sso_configs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SSOConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "institution_id", nullable = false, unique = true)
    private Institution institution;

    @Enumerated(EnumType.STRING)
    @Column(name = "sso_provider", nullable = false)
    private SSOProvider ssoProvider;

    @Column(name = "client_id", length = 500, nullable = false)
    private String clientId;

    @Column(name = "client_secret", length = 500, nullable = false)
    private String clientSecret;

    @Column(name = "redirect_uri", length = 500)
    private String redirectUri;

    @Column(name = "tenant_id")
    private String tenantId;

    @Column(name = "domain")
    private String domain;

    @Column(name = "is_enabled")
    private Boolean isEnabled = false;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum SSOProvider {
        GOOGLE_CLASSROOM,
        MICROSOFT_TEAMS,
        CLEVER,
        CUSTOM
    }
}
