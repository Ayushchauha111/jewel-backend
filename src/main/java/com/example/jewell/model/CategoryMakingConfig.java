package com.example.jewell.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Config: making charges per gram (₹/g) by category + material.
 * Used in price calculation: metal rate + making (from this config) + GST 3%.
 * material = e.g. Gold, Silver, Diamond; empty/null = applies to all materials for that category.
 */
@Entity
@Table(name = "category_making_config", uniqueConstraints = {
    @UniqueConstraint(columnNames = { "category", "material" })
})
public class CategoryMakingConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "category", nullable = false, length = 100)
    private String category;

    /** Metal type: Gold, Silver, Diamond, etc. Empty/null = default for category. */
    @Column(name = "material", length = 50)
    private String material;

    @NotNull
    @Column(name = "making_charges_per_gram", precision = 10, scale = 2, nullable = false)
    private BigDecimal makingChargesPerGram;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public CategoryMakingConfig() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getMaterial() { return material; }
    public void setMaterial(String material) { this.material = material; }
    public BigDecimal getMakingChargesPerGram() { return makingChargesPerGram; }
    public void setMakingChargesPerGram(BigDecimal makingChargesPerGram) { this.makingChargesPerGram = makingChargesPerGram; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
