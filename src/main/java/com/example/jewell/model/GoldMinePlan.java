package com.example.jewell.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Gold Mine 10+1 plan: customer pays 10 monthly installments and gets 11th free (100% discount).
 * Redeemable from 6th month (early redemption with reduced value).
 */
@Entity
@Table(name = "gold_mine_plans")
public class GoldMinePlan {

    public static final int TOTAL_PAID_INSTALLMENTS = 10;
    public static final int DISCOUNT_INSTALLMENT_NUMBER = 11;
    public static final BigDecimal MIN_MONTHLY_AMOUNT = new BigDecimal("500");
    public static final BigDecimal MAX_MONTHLY_AMOUNT = new BigDecimal("100000");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "customer_id", nullable = false)
    @JsonIgnoreProperties({"orders", "credits"})
    private Customer customer;

    @NotNull
    @Column(name = "monthly_amount", precision = 12, scale = 2, nullable = false)
    @DecimalMin(value = "500", message = "Monthly amount must be at least 500")
    private BigDecimal monthlyAmount;

    @NotNull
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "status", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private PlanStatus status = PlanStatus.ACTIVE;

    /** Number of installments paid (1-10). 11th is free when this reaches 10. */
    @Column(name = "paid_count", nullable = false)
    private int paidCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @OrderBy("installmentNumber ASC")
    private List<GoldMineInstallment> installments = new ArrayList<>();

    public enum PlanStatus {
        ACTIVE,    // Ongoing, not yet completed
        COMPLETED, // All 11 installments done (10 paid + 11th waived)
        CANCELLED
    }

    public GoldMinePlan() {
    }

    /** Total jewellery value after full plan = monthlyAmount * 11. */
    public BigDecimal getRedemptionValue() {
        if (monthlyAmount == null) return null;
        return monthlyAmount.multiply(BigDecimal.valueOf(11));
    }

    /** Early redemption value at 6th month (approx 87.5% of 6 installments). */
    public BigDecimal getEarlyRedemptionValue6() {
        if (monthlyAmount == null) return null;
        return monthlyAmount.multiply(BigDecimal.valueOf(6)).multiply(new BigDecimal("0.875"));
    }

    /** Early redemption value at 8th month (approx 93.75% of 8 installments). */
    public BigDecimal getEarlyRedemptionValue8() {
        if (monthlyAmount == null) return null;
        return monthlyAmount.multiply(BigDecimal.valueOf(8)).multiply(new BigDecimal("0.9375"));
    }

    // --- Getters / Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }

    public BigDecimal getMonthlyAmount() { return monthlyAmount; }
    public void setMonthlyAmount(BigDecimal monthlyAmount) { this.monthlyAmount = monthlyAmount; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public PlanStatus getStatus() { return status; }
    public void setStatus(PlanStatus status) { this.status = status; }

    public int getPaidCount() { return paidCount; }
    public void setPaidCount(int paidCount) { this.paidCount = paidCount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<GoldMineInstallment> getInstallments() { return installments; }
    public void setInstallments(List<GoldMineInstallment> installments) { this.installments = installments; }
}
