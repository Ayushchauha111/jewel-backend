package com.example.jewell.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Tracks POS day open/close: opening cash, closing count, mismatch reason.
 */
@Entity
@Table(name = "day_sessions", indexes = {
    @Index(name = "idx_day_sessions_session_date", columnList = "session_date"),
    @Index(name = "idx_day_sessions_status", columnList = "status")
})
public class DaySession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Business date for this session (one open session per date). */
    @Column(name = "session_date", nullable = false, unique = true)
    private LocalDate sessionDate;

    @Column(name = "opening_cash", precision = 12, scale = 2, nullable = false)
    private BigDecimal openingCash = BigDecimal.ZERO;

    @Column(name = "closing_cash", precision = 12, scale = 2)
    private BigDecimal closingCash;

    /** Expected cash at close (opening + sales - expenses, or computed). */
    @Column(name = "expected_cash", precision = 12, scale = 2)
    private BigDecimal expectedCash;

    /** Actual cash counted at close. */
    @Column(name = "actual_cash_at_close", precision = 12, scale = 2)
    private BigDecimal actualCashAtClose;

    @Column(name = "mismatch_reason", length = 500)
    private String mismatchReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SessionStatus status = SessionStatus.OPEN;

    @CreationTimestamp
    @Column(name = "opened_at", nullable = false, updatable = false)
    private LocalDateTime openedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    public enum SessionStatus {
        OPEN, CLOSED
    }

    public DaySession() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDate getSessionDate() { return sessionDate; }
    public void setSessionDate(LocalDate sessionDate) { this.sessionDate = sessionDate; }
    public BigDecimal getOpeningCash() { return openingCash; }
    public void setOpeningCash(BigDecimal openingCash) { this.openingCash = openingCash; }
    public BigDecimal getClosingCash() { return closingCash; }
    public void setClosingCash(BigDecimal closingCash) { this.closingCash = closingCash; }
    public BigDecimal getExpectedCash() { return expectedCash; }
    public void setExpectedCash(BigDecimal expectedCash) { this.expectedCash = expectedCash; }
    public BigDecimal getActualCashAtClose() { return actualCashAtClose; }
    public void setActualCashAtClose(BigDecimal actualCashAtClose) { this.actualCashAtClose = actualCashAtClose; }
    public String getMismatchReason() { return mismatchReason; }
    public void setMismatchReason(String mismatchReason) { this.mismatchReason = mismatchReason; }
    public SessionStatus getStatus() { return status; }
    public void setStatus(SessionStatus status) { this.status = status; }
    public LocalDateTime getOpenedAt() { return openedAt; }
    public void setOpenedAt(LocalDateTime openedAt) { this.openedAt = openedAt; }
    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }
}
