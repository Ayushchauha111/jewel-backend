package com.example.jewell.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Unified daily rates: one row per date for Gold (all carats), Silver, and Diamond.
 * Used for bill creation (gold buy-back, stock price calculation) and display.
 */
@Entity
@Table(name = "daily_rates", uniqueConstraints = {
    @UniqueConstraint(columnNames = "price_date")
})
public class DailyRate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "price_date", unique = true, nullable = false)
    private LocalDate priceDate;

    // Gold: price per gram for each carat (optional; missing derived from gold24K * carat/24)
    @Column(name = "gold_10k", precision = 10, scale = 2)
    private BigDecimal gold10K;
    @Column(name = "gold_12k", precision = 10, scale = 2)
    private BigDecimal gold12K;
    @Column(name = "gold_14k", precision = 10, scale = 2)
    private BigDecimal gold14K;
    @Column(name = "gold_18k", precision = 10, scale = 2)
    private BigDecimal gold18K;
    @Column(name = "gold_20k", precision = 10, scale = 2)
    private BigDecimal gold20K;
    @Column(name = "gold_21k", precision = 10, scale = 2)
    private BigDecimal gold21K;
    @Column(name = "gold_22k", precision = 10, scale = 2)
    private BigDecimal gold22K;
    @Column(name = "gold_24k", precision = 10, scale = 2)
    private BigDecimal gold24K;

    @Column(name = "silver_per_gram", precision = 10, scale = 2)
    private BigDecimal silverPerGram;
    /** Silver purity/accuracy e.g. 92.5 for 92.5%, or 999 for 999 fineness. */
    @Column(name = "silver_purity_percentage", precision = 5, scale = 2)
    private BigDecimal silverPurityPercentage;

    @Column(name = "diamond_per_carat", precision = 12, scale = 2)
    private BigDecimal diamondPerCarat;

    /** Making charges per gram (₹/g) – default for billing when no item-level rate. */
    @Column(name = "making_charges_per_gram", precision = 10, scale = 2)
    private BigDecimal makingChargesPerGram;

    @Column(name = "notes", length = 500)
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public DailyRate() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDate getPriceDate() { return priceDate; }
    public void setPriceDate(LocalDate priceDate) { this.priceDate = priceDate; }
    public BigDecimal getGold10K() { return gold10K; }
    public void setGold10K(BigDecimal gold10K) { this.gold10K = gold10K; }
    public BigDecimal getGold12K() { return gold12K; }
    public void setGold12K(BigDecimal gold12K) { this.gold12K = gold12K; }
    public BigDecimal getGold14K() { return gold14K; }
    public void setGold14K(BigDecimal gold14K) { this.gold14K = gold14K; }
    public BigDecimal getGold18K() { return gold18K; }
    public void setGold18K(BigDecimal gold18K) { this.gold18K = gold18K; }
    public BigDecimal getGold20K() { return gold20K; }
    public void setGold20K(BigDecimal gold20K) { this.gold20K = gold20K; }
    public BigDecimal getGold21K() { return gold21K; }
    public void setGold21K(BigDecimal gold21K) { this.gold21K = gold21K; }
    public BigDecimal getGold22K() { return gold22K; }
    public void setGold22K(BigDecimal gold22K) { this.gold22K = gold22K; }
    public BigDecimal getGold24K() { return gold24K; }
    public void setGold24K(BigDecimal gold24K) { this.gold24K = gold24K; }
    public BigDecimal getSilverPerGram() { return silverPerGram; }
    public void setSilverPerGram(BigDecimal silverPerGram) { this.silverPerGram = silverPerGram; }
    public BigDecimal getSilverPurityPercentage() { return silverPurityPercentage; }
    public void setSilverPurityPercentage(BigDecimal silverPurityPercentage) { this.silverPurityPercentage = silverPurityPercentage; }
    public BigDecimal getDiamondPerCarat() { return diamondPerCarat; }
    public void setDiamondPerCarat(BigDecimal diamondPerCarat) { this.diamondPerCarat = diamondPerCarat; }
    public BigDecimal getMakingChargesPerGram() { return makingChargesPerGram; }
    public void setMakingChargesPerGram(BigDecimal makingChargesPerGram) { this.makingChargesPerGram = makingChargesPerGram; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
