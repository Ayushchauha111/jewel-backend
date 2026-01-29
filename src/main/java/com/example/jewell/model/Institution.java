package com.example.jewell.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "institutions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Institution {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "institution_name", nullable = false)
    private String institutionName;

    @Enumerated(EnumType.STRING)
    @Column(name = "institution_type", nullable = false)
    private InstitutionType institutionType;

    @Column(name = "domain")
    private String domain;

    @Column(name = "subdomain")
    private String subdomain; // e.g., "parmar" for parmar.typogram.in

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "favicon_url", length = 500)
    private String faviconUrl;

    @Column(name = "primary_color", length = 50)
    private String primaryColor;

    @Column(name = "secondary_color", length = 50)
    private String secondaryColor;

    @Column(name = "accent_color", length = 50)
    private String accentColor;

    @Column(name = "brand_name", length = 255)
    private String brandName; // Custom brand name for white-labeling

    @Column(name = "custom_css", columnDefinition = "TEXT")
    private String customCss; // Custom CSS for complete branding control

    @Column(name = "footer_text", length = 500)
    private String footerText; // Custom footer text

    @Column(name = "contact_email", nullable = false)
    private String contactEmail;

    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "license_key", unique = true)
    private String licenseKey;

    @Column(name = "max_students")
    private Integer maxStudents;

    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_status")
    private SubscriptionStatus subscriptionStatus = SubscriptionStatus.TRIAL;

    @Column(name = "subscription_expires_at")
    private LocalDateTime subscriptionExpiresAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_user_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "roles", "password"})
    private User adminUser;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @OneToMany(mappedBy = "institution", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore // Prevent serialization of batches to reduce response size
    private List<Batch> batches = new ArrayList<>();


    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum InstitutionType {
        SCHOOL,
        COACHING_CENTER,
        COLLEGE,
        UNIVERSITY,
        OTHER
    }

    public enum SubscriptionStatus {
        TRIAL,
        ACTIVE,
        EXPIRED,
        SUSPENDED
    }
}
