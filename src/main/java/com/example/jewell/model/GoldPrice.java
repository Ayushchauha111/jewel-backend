package com.example.jewell.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "gold_prices", uniqueConstraints = {
    @UniqueConstraint(columnNames = "price_date")
})
public class GoldPrice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "price_date", unique = true, nullable = false)
    private LocalDate priceDate;

    @Column(name = "price_per_gram", precision = 10, scale = 2, nullable = false)
    private BigDecimal pricePerGram;

    @Column(name = "price_22_carat", precision = 10, scale = 2)
    private BigDecimal price22Carat;

    @Column(name = "price_24_carat", precision = 10, scale = 2)
    private BigDecimal price24Carat;

    @Column(name = "notes", length = 500)
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public GoldPrice() {
    }

    public GoldPrice(LocalDate priceDate, BigDecimal pricePerGram) {
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

    public BigDecimal getPrice22Carat() {
        return price22Carat;
    }

    public void setPrice22Carat(BigDecimal price22Carat) {
        this.price22Carat = price22Carat;
    }

    public BigDecimal getPrice24Carat() {
        return price24Carat;
    }

    public void setPrice24Carat(BigDecimal price24Carat) {
        this.price24Carat = price24Carat;
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
