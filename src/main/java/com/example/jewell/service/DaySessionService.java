package com.example.jewell.service;

import com.example.jewell.model.DaySession;
import com.example.jewell.model.TransactionHistory;
import com.example.jewell.repository.BillingRepository;
import com.example.jewell.repository.DaySessionRepository;
import com.example.jewell.repository.TransactionHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Transactional
public class DaySessionService {
    @Autowired
    private DaySessionRepository daySessionRepository;

    @Autowired
    private BillingRepository billingRepository;

    @Autowired
    private TransactionHistoryRepository transactionHistoryRepository;

    /**
     * Open day with opening cash. One session per date.
     */
    public DaySession openDay(BigDecimal openingCash) {
        LocalDate today = LocalDate.now();
        if (daySessionRepository.findBySessionDate(today).isPresent()) {
            throw new IllegalStateException("Day is already open for " + today);
        }
        DaySession session = new DaySession();
        session.setSessionDate(today);
        session.setOpeningCash(openingCash != null ? openingCash : BigDecimal.ZERO);
        session.setStatus(DaySession.SessionStatus.OPEN);
        return daySessionRepository.save(session);
    }

    /**
     * Close day with actual cash count. Optionally provide expected cash; if null, computed as opening + income - expenses.
     */
    public DaySession closeDay(BigDecimal actualCashAtClose, String mismatchReason) {
        LocalDate today = LocalDate.now();
        DaySession session = daySessionRepository.findBySessionDateAndStatus(today, DaySession.SessionStatus.OPEN)
                .orElseThrow(() -> new IllegalStateException("No open session for " + today));

        BigDecimal income = transactionHistoryRepository.getTotalByDateAndType(today, TransactionHistory.TransactionType.INCOME);
        BigDecimal expenses = transactionHistoryRepository.getTotalByDateAndType(today, TransactionHistory.TransactionType.EXPENSE);
        if (income == null) income = BigDecimal.ZERO;
        if (expenses == null) expenses = BigDecimal.ZERO;
        BigDecimal expected = session.getOpeningCash().add(income).subtract(expenses);
        session.setExpectedCash(expected);
        session.setClosingCash(expected);
        session.setActualCashAtClose(actualCashAtClose != null ? actualCashAtClose : BigDecimal.ZERO);
        session.setMismatchReason(mismatchReason);
        session.setStatus(DaySession.SessionStatus.CLOSED);
        session.setClosedAt(LocalDateTime.now());
        return daySessionRepository.save(session);
    }

    public Optional<DaySession> getCurrentSession() {
        return daySessionRepository.findBySessionDateAndStatus(LocalDate.now(), DaySession.SessionStatus.OPEN);
    }

    public Optional<DaySession> getSessionByDate(LocalDate date) {
        return daySessionRepository.findBySessionDate(date);
    }
}
