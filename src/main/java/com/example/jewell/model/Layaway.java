package com.example.jewell.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "layaways", indexes = {
    @Index(name = "idx_layaway_customer", columnList = "customer_id"),
    @Index(name = "idx_layaway_status", columnList = "status")
})
public class Layaway {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "customer_id", nullable = false)
    @JsonIgnoreProperties({"orders", "credits"})
    private Customer customer;

    @Column(name = "total_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "paid_amount", precision = 12, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private LayawayStatus status = LayawayStatus.RESERVED;

    @Column(name = "notes", length = 500)
    private String notes;

    @OneToMany(mappedBy = "layaway", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<LayawayItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "layaway", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<LayawayPayment> payments = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum LayawayStatus {
        RESERVED, COMPLETED, CANCELLED
    }

    public Layaway() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public LayawayStatus getStatus() { return status; }
    public void setStatus(LayawayStatus status) { this.status = status; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public List<LayawayItem> getItems() { return items; }
    public void setItems(List<LayawayItem> items) { this.items = items; }
    public List<LayawayPayment> getPayments() { return payments; }
    public void setPayments(List<LayawayPayment> payments) { this.payments = payments; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
