package com.example.jewell.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "gold_mine_installments")
public class GoldMineInstallment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    @JsonIgnore
    private GoldMinePlan plan;

    /** 1 to 11. 11th is the free (waived) installment. */
    @Column(name = "installment_number", nullable = false)
    private int installmentNumber;

    /** Amount due for this installment (0 for 11th). */
    @Column(name = "amount_due", precision = 12, scale = 2, nullable = false)
    private BigDecimal amountDue;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "status", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private InstallmentStatus status = InstallmentStatus.PENDING;

    @Column(name = "payment_reference", length = 255)
    private String paymentReference;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum InstallmentStatus {
        PENDING, // Not yet paid
        PAID,    // Customer paid
        WAIVED   // 11th installment - free (100% discount)
    }

    public GoldMineInstallment() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public GoldMinePlan getPlan() { return plan; }
    public void setPlan(GoldMinePlan plan) { this.plan = plan; }

    public int getInstallmentNumber() { return installmentNumber; }
    public void setInstallmentNumber(int installmentNumber) { this.installmentNumber = installmentNumber; }

    public BigDecimal getAmountDue() { return amountDue; }
    public void setAmountDue(BigDecimal amountDue) { this.amountDue = amountDue; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }

    public InstallmentStatus getStatus() { return status; }
    public void setStatus(InstallmentStatus status) { this.status = status; }

    public String getPaymentReference() { return paymentReference; }
    public void setPaymentReference(String paymentReference) { this.paymentReference = paymentReference; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
