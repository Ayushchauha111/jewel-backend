package com.example.jewell.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Stock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "article_name")
    private String articleName;

    @Column(name = "article_code", unique = true)
    private String articleCode;

    @Column(name = "category", length = 100)
    private String category; // e.g., "Rings", "Necklace", "Earrings", "Bracelet", etc.

    @Column(name = "material", length = 50)
    private String material; // Gold, Silver, Diamond, Gemstone, Other

    @NotNull
    @Column(name = "weight_grams", precision = 10, scale = 3)
    private BigDecimal weightGrams;

    @NotNull
    @Column(name = "carat", precision = 5, scale = 2)
    private BigDecimal carat;

    @Column(name = "diamond_carat", precision = 5, scale = 2)
    private BigDecimal diamondCarat;

    @Column(name = "purity_percentage", precision = 5, scale = 2)
    private BigDecimal purityPercentage;

    @Column(name = "purchase_price", precision = 12, scale = 2)
    private BigDecimal purchasePrice;

    @Column(name = "selling_price", precision = 12, scale = 2)
    private BigDecimal sellingPrice;

    @Column(name = "current_gold_price_per_gram", precision = 10, scale = 2)
    private BigDecimal currentGoldPricePerGram;

    @Column(name = "quantity")
    private Integer quantity = 1;

    @Column(name = "size", length = 20)
    private String size; // e.g. ring size (shown when category is Ring/Rings)

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "qr_code", columnDefinition = "TEXT")
    private String qrCode;

    @Column(name = "image_url")
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private StockStatus status = StockStatus.AVAILABLE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum StockStatus {
        AVAILABLE, SOLD, RESERVED, RETURNED
    }

    public Stock() {
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getMaterial() {
        return material;
    }

    public void setMaterial(String material) {
        this.material = material;
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

    public BigDecimal getDiamondCarat() {
        return diamondCarat;
    }

    public void setDiamondCarat(BigDecimal diamondCarat) {
        this.diamondCarat = diamondCarat;
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

    public BigDecimal getCurrentGoldPricePerGram() {
        return currentGoldPricePerGram;
    }

    public void setCurrentGoldPricePerGram(BigDecimal currentGoldPricePerGram) {
        this.currentGoldPricePerGram = currentGoldPricePerGram;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getQrCode() {
        return qrCode;
    }

    public void setQrCode(String qrCode) {
        this.qrCode = qrCode;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public StockStatus getStatus() {
        return status;
    }

    public void setStatus(StockStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
