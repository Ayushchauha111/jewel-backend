package com.example.jewell.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_history")
public class StockHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "original_stock_id")
    private Long originalStockId;

    @Column(name = "article_name")
    private String articleName;

    @Column(name = "article_code")
    private String articleCode;

    @Column(name = "weight_grams", precision = 10, scale = 3)
    private BigDecimal weightGrams;

    @Column(name = "carat", precision = 5, scale = 2)
    private BigDecimal carat;

    @Column(name = "purity_percentage", precision = 5, scale = 2)
    private BigDecimal purityPercentage;

    @Column(name = "purchase_price", precision = 12, scale = 2)
    private BigDecimal purchasePrice;

    @Column(name = "selling_price", precision = 12, scale = 2)
    private BigDecimal sellingPrice;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "status")
    private String status;

    @Column(name = "deletion_reason", length = 500)
    private String deletionReason;

    @Column(name = "deleted_at", nullable = false)
    private LocalDateTime deletedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public StockHistory() {
    }

    public StockHistory(Stock stock, String deletionReason) {
        this.originalStockId = stock.getId();
        this.articleName = stock.getArticleName();
        this.articleCode = stock.getArticleCode();
        this.weightGrams = stock.getWeightGrams();
        this.carat = stock.getCarat();
        this.purityPercentage = stock.getPurityPercentage();
        this.purchasePrice = stock.getPurchasePrice();
        this.sellingPrice = stock.getSellingPrice();
        this.quantity = stock.getQuantity();
        this.description = stock.getDescription();
        this.imageUrl = stock.getImageUrl();
        this.status = stock.getStatus() != null ? stock.getStatus().toString() : null;
        this.deletionReason = deletionReason;
        this.deletedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOriginalStockId() {
        return originalStockId;
    }

    public void setOriginalStockId(Long originalStockId) {
        this.originalStockId = originalStockId;
    }

    public String getArticleName() {
        return articleName;
    }

    public void setArticleName(String articleName) {
        this.articleName = articleName;
    }

    public String getArticleCode() {
        return articleCode;
    }

    public void setArticleCode(String articleCode) {
        this.articleCode = articleCode;
    }

    public BigDecimal getWeightGrams() {
        return weightGrams;
    }

    public void setWeightGrams(BigDecimal weightGrams) {
        this.weightGrams = weightGrams;
    }

    public BigDecimal getCarat() {
        return carat;
    }

    public void setCarat(BigDecimal carat) {
        this.carat = carat;
    }

    public BigDecimal getPurityPercentage() {
        return purityPercentage;
    }

    public void setPurityPercentage(BigDecimal purityPercentage) {
        this.purityPercentage = purityPercentage;
    }

    public BigDecimal getPurchasePrice() {
        return purchasePrice;
    }

    public void setPurchasePrice(BigDecimal purchasePrice) {
        this.purchasePrice = purchasePrice;
    }

    public BigDecimal getSellingPrice() {
        return sellingPrice;
    }

    public void setSellingPrice(BigDecimal sellingPrice) {
        this.sellingPrice = sellingPrice;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDeletionReason() {
        return deletionReason;
    }

    public void setDeletionReason(String deletionReason) {
        this.deletionReason = deletionReason;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
