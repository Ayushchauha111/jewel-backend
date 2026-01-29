package com.example.jewell.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "batches",
       uniqueConstraints = @UniqueConstraint(columnNames = {"institution_id", "batch_code"}, name = "uk_institution_batch"))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Batch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "institution_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "batches", "labSessions", "adminUser"})
    private Institution institution;

    @Column(name = "batch_name", nullable = false)
    private String batchName;

    @Column(name = "batch_code", nullable = false, length = 100)
    private String batchCode;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "grade_level", length = 50)
    private String gradeLevel;

    @Column(name = "academic_year", length = 50)
    private String academicYear;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id")
    private User teacher;

    @Column(name = "max_students")
    private Integer maxStudents;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "batch_students",
        joinColumns = @JoinColumn(name = "batch_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> students = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
