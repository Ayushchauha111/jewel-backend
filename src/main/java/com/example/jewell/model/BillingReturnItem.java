package com.example.jewell.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "billing_return_items")
public class BillingReturnItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "billing_return_id", nullable = false)
    @JsonIgnore
    private BillingReturn billingReturn;

    /** Original billing item that was returned (for reference). */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "billing_item_id")
    private BillingItem billingItem;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "stock_id")
    private Stock stock;

    @Column(name = "quantity_returned", nullable = false)
    private Integer quantityReturned = 1;

    @Column(name = "refund_amount", precision = 12, scale = 2)
    private BigDecimal refundAmount;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public BillingReturnItem() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public BillingReturn getBillingReturn() { return billingReturn; }
    public void setBillingReturn(BillingReturn billingReturn) { this.billingReturn = billingReturn; }
    public BillingItem getBillingItem() { return billingItem; }
    public void setBillingItem(BillingItem billingItem) { this.billingItem = billingItem; }
    public Stock getStock() { return stock; }
    public void setStock(Stock stock) { this.stock = stock; }
    public Integer getQuantityReturned() { return quantityReturned; }
    public void setQuantityReturned(Integer quantityReturned) { this.quantityReturned = quantityReturned; }
    public BigDecimal getRefundAmount() { return refundAmount; }
    public void setRefundAmount(BigDecimal refundAmount) { this.refundAmount = refundAmount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
