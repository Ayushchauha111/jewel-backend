package com.example.jewell.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "billing_items")
public class BillingItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "billing_id", nullable = false)
    @JsonIgnoreProperties({"items", "customer"})
    private Billing billing;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "stock_id")
    private Stock stock;

    @Column(name = "item_name")
    private String itemName;

    @Column(name = "article_code", length = 100)
    private String articleCode;

    @Column(name = "weight_grams", precision = 10, scale = 3)
    private BigDecimal weightGrams;

    @Column(name = "carat", precision = 5, scale = 2)
    private BigDecimal carat;

    @Column(name = "diamond_carat", precision = 6, scale = 3)
    private BigDecimal diamondCarat;

    /** Diamond value for this line (diamond carat × qty × rate at bill time). Metal amount = totalPrice - diamondAmount. */
    @Column(name = "diamond_amount", precision = 12, scale = 2)
    private BigDecimal diamondAmount;

    @Column(name = "quantity")
    private Integer quantity = 1;

    @Column(name = "unit_price", precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "total_price", precision = 12, scale = 2)
    private BigDecimal totalPrice;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public BillingItem() {
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Billing getBilling() {
        return billing;
    }

    public void setBilling(Billing billing) {
        this.billing = billing;
    }

    public Stock getStock() {
        return stock;
    }

    public void setStock(Stock stock) {
        this.stock = stock;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
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

    public BigDecimal getDiamondCarat() {
        return diamondCarat;
    }

    public void setDiamondCarat(BigDecimal diamondCarat) {
        this.diamondCarat = diamondCarat;
    }

    public BigDecimal getDiamondAmount() {
        return diamondAmount;
    }

    public void setDiamondAmount(BigDecimal diamondAmount) {
        this.diamondAmount = diamondAmount;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
