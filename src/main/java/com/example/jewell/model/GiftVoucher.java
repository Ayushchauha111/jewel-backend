package com.example.jewell.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "gift_vouchers", indexes = {
    @Index(name = "idx_gift_voucher_code", columnList = "code"),
    @Index(name = "idx_gift_voucher_status", columnList = "status")
})
public class GiftVoucher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", unique = true, nullable = false, length = 50)
    private String code;

    @Column(name = "amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private VoucherStatus status = VoucherStatus.ISSUED;

    @Column(name = "sold_at")
    private LocalDateTime soldAt;

    @Column(name = "redeemed_at")
    private LocalDateTime redeemedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "billing_id")
    @JsonIgnore
    private Billing redeemedAgainstBilling;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum VoucherStatus {
        ISSUED, REDEEMED, EXPIRED
    }

    public GiftVoucher() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public VoucherStatus getStatus() { return status; }
    public void setStatus(VoucherStatus status) { this.status = status; }
    public LocalDateTime getSoldAt() { return soldAt; }
    public void setSoldAt(LocalDateTime soldAt) { this.soldAt = soldAt; }
    public LocalDateTime getRedeemedAt() { return redeemedAt; }
    public void setRedeemedAt(LocalDateTime redeemedAt) { this.redeemedAt = redeemedAt; }
    public Billing getRedeemedAgainstBilling() { return redeemedAgainstBilling; }
    public void setRedeemedAgainstBilling(Billing billing) { this.redeemedAgainstBilling = billing; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
