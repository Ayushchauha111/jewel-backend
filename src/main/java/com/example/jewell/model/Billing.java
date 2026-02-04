package com.example.jewell.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "billing")
public class Billing {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "bill_number", unique = true)
    private String billNumber;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "customer_id", nullable = false)
    @JsonIgnoreProperties({"orders", "credits"})
    private Customer customer;

    @Column(name = "total_amount", precision = 12, scale = 2)
    private BigDecimal totalAmount;

    /** Total diamond value (diamond carat × rate) for display as separate row on receipts. */
    @Column(name = "total_diamond_amount", precision = 12, scale = 2)
    private BigDecimal totalDiamondAmount;

    @Column(name = "discount_amount", precision = 12, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    /** Making charges (added to total after discount). */
    @Column(name = "making_charges", precision = 12, scale = 2)
    private BigDecimal makingCharges = BigDecimal.ZERO;

    @Column(name = "final_amount", precision = 12, scale = 2)
    private BigDecimal finalAmount;

    @Column(name = "paid_amount", precision = 12, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(name = "payment_method")
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    /** JSON array of { "method": "CASH"|"UPI"|..., "amount": number } for split payment (e.g. cash + UPI). */
    @Column(name = "payment_breakdown", length = 2000)
    private String paymentBreakdown;

    @Column(name = "payment_status")
    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "email_sent")
    private Boolean emailSent = false;

    @Column(name = "whatsapp_sent")
    private Boolean whatsappSent = false;

    @OneToMany(mappedBy = "billing", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<BillingItem> items = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum PaymentMethod {
        CASH, CARD, UPI, BANK_TRANSFER, CREDIT, MIXED
    }

    public enum PaymentStatus {
        PENDING, PAID, PARTIAL, CANCELLED
    }

    public Billing() {
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBillNumber() {
        return billNumber;
    }

    public void setBillNumber(String billNumber) {
        this.billNumber = billNumber;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getTotalDiamondAmount() {
        return totalDiamondAmount;
    }

    public void setTotalDiamondAmount(BigDecimal totalDiamondAmount) {
        this.totalDiamondAmount = totalDiamondAmount;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public BigDecimal getMakingCharges() {
        return makingCharges;
    }

    public void setMakingCharges(BigDecimal makingCharges) {
        this.makingCharges = makingCharges;
    }

    public BigDecimal getFinalAmount() {
        return finalAmount;
    }

    public void setFinalAmount(BigDecimal finalAmount) {
        this.finalAmount = finalAmount;
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(BigDecimal paidAmount) {
        this.paidAmount = paidAmount;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getPaymentBreakdown() {
        return paymentBreakdown;
    }

    public void setPaymentBreakdown(String paymentBreakdown) {
        this.paymentBreakdown = paymentBreakdown;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Boolean getEmailSent() {
        return emailSent;
    }

    public void setEmailSent(Boolean emailSent) {
        this.emailSent = emailSent;
    }

    public Boolean getWhatsappSent() {
        return whatsappSent;
    }

    public void setWhatsappSent(Boolean whatsappSent) {
        this.whatsappSent = whatsappSent;
    }

    public List<BillingItem> getItems() {
        return items;
    }

    public void setItems(List<BillingItem> items) {
        this.items = items;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
