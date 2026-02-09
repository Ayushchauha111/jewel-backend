package com.example.jewell.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "billing_returns", indexes = {
    @Index(name = "idx_billing_return_original_billing", columnList = "original_billing_id"),
    @Index(name = "idx_billing_return_created", columnList = "created_at")
})
public class BillingReturn {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "return_number", unique = true)
    private String returnNumber;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "original_billing_id", nullable = false)
    @JsonIgnoreProperties({"items", "customer"})
    private Billing originalBilling;

    @Enumerated(EnumType.STRING)
    @Column(name = "return_type", nullable = false)
    private ReturnType returnType = ReturnType.RETURN;

    @Column(name = "total_refund_amount", precision = 12, scale = 2)
    private BigDecimal totalRefundAmount = BigDecimal.ZERO;

    @Column(name = "refund_method")
    @Enumerated(EnumType.STRING)
    private Billing.PaymentMethod refundMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ReturnStatus status = ReturnStatus.COMPLETED;

    @Column(name = "notes", length = 1000)
    private String notes;

    @OneToMany(mappedBy = "billingReturn", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<BillingReturnItem> items = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum ReturnType {
        RETURN, EXCHANGE
    }

    public enum ReturnStatus {
        PENDING, COMPLETED, CANCELLED
    }

    public BillingReturn() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getReturnNumber() { return returnNumber; }
    public void setReturnNumber(String returnNumber) { this.returnNumber = returnNumber; }
    public Billing getOriginalBilling() { return originalBilling; }
    public void setOriginalBilling(Billing originalBilling) { this.originalBilling = originalBilling; }
    public ReturnType getReturnType() { return returnType; }
    public void setReturnType(ReturnType returnType) { this.returnType = returnType; }
    public BigDecimal getTotalRefundAmount() { return totalRefundAmount; }
    public void setTotalRefundAmount(BigDecimal totalRefundAmount) { this.totalRefundAmount = totalRefundAmount; }
    public Billing.PaymentMethod getRefundMethod() { return refundMethod; }
    public void setRefundMethod(Billing.PaymentMethod refundMethod) { this.refundMethod = refundMethod; }
    public ReturnStatus getStatus() { return status; }
    public void setStatus(ReturnStatus status) { this.status = status; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public List<BillingReturnItem> getItems() { return items; }
    public void setItems(List<BillingReturnItem> items) { this.items = items; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
