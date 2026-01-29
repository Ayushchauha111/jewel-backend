package com.example.jewell.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "curriculum_units")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class CurriculumUnit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", referencedColumnName = "product_id", nullable = false)
    @JsonBackReference
    private CurriculumProduct product;

    @Column(name = "unit_id", nullable = false)
    private Long unitId; // Original unit_id from source

    @Column(name = "unit_name", nullable = false)
    private String unitName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "subtext", length = 500)
    private String subtext;

    @Column(name = "header", length = 255)
    private String header; // e.g., "Learn to Type", "Beyond Typing"

    @Column(name = "grade", length = 50)
    private String grade;

    @Column(name = "standards", columnDefinition = "TEXT")
    private String standards; // e.g., "ISTE Empowered Learner"

    @Column(name = "premium_only")
    private Boolean premiumOnly = false;

    @Column(name = "badge", length = 100)
    private String badge;

    @Column(name = "unit_type", nullable = false, length = 50)
    private String unitType; // "lesson", "test", "custom"

    @Column(name = "source_id")
    private Long sourceId;

    @Column(name = "display_order")
    private Integer displayOrder = 0;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @OneToMany(mappedBy = "unit", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<CurriculumLesson> lessons = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
