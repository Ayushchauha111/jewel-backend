package com.example.jewell.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "silver_prices", uniqueConstraints = {
    @UniqueConstraint(columnNames = "price_date")
})
public class SilverPrice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "price_date", unique = true, nullable = false)
    private LocalDate priceDate;

    @Column(name = "price_per_gram", precision = 10, scale = 2, nullable = false)
    private BigDecimal pricePerGram;

    @Column(name = "price_per_kg", precision = 10, scale = 2)
    private BigDecimal pricePerKg;

    @Column(name = "notes", length = 500)
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public SilverPrice() {
    }

    public SilverPrice(LocalDate priceDate, BigDecimal pricePerGram) {
        this.priceDate = priceDate;
        this.pricePerGram = pricePerGram;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getPriceDate() {
        return priceDate;
    }

    public void setPriceDate(LocalDate priceDate) {
        this.priceDate = priceDate;
    }

    public BigDecimal getPricePerGram() {
        return pricePerGram;
    }

    public void setPricePerGram(BigDecimal pricePerGram) {
        this.pricePerGram = pricePerGram;
    }

    public BigDecimal getPricePerKg() {
        return pricePerKg;
    }

    public void setPricePerKg(BigDecimal pricePerKg) {
        this.pricePerKg = pricePerKg;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
