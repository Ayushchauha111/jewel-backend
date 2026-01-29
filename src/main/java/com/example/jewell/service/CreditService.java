package com.example.jewell.service;

import com.example.jewell.model.Credit;
import com.example.jewell.model.CreditPaymentHistory;
import com.example.jewell.repository.CreditRepository;
import com.example.jewell.repository.CreditPaymentHistoryRepository;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CreditService {
    @Autowired
    private CreditRepository creditRepository;

    @Autowired
    private CreditPaymentHistoryRepository creditPaymentHistoryRepository;

    @Autowired
    private IncomeExpenseService incomeExpenseService;

    @Autowired
    private EntityManager entityManager;

    public List<Credit> getAllCredits() {
        return creditRepository.findAll();
    }

    public Page<Credit> getAllCreditsPaginated(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return creditRepository.findAll(pageable);
    }

    public Optional<Credit> getCreditById(Long id) {
        return creditRepository.findById(id);
    }

    public List<Credit> getCreditsByCustomer(Long customerId) {
        return creditRepository.findByCustomerId(customerId);
    }

    public List<Credit> getCreditsByStatus(Credit.CreditStatus status) {
        return creditRepository.findByStatus(status);
    }

    public Credit createCredit(Credit credit) {
        credit.setRemainingAmount(credit.getCreditAmount().subtract(credit.getPaidAmount()));
        updateCreditStatus(credit);
        return creditRepository.save(credit);
    }

    public Credit updateCreditPayment(Long id, BigDecimal paymentAmount) {
        Credit credit = creditRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Credit not found"));
        
        BigDecimal newPaidAmount = credit.getPaidAmount().add(paymentAmount);
        credit.setPaidAmount(newPaidAmount);
        credit.setRemainingAmount(credit.getCreditAmount().subtract(newPaidAmount));
        updateCreditStatus(credit);
        
        Credit savedCredit = creditRepository.save(credit);
        
        // Record payment history
        CreditPaymentHistory paymentHistory = new CreditPaymentHistory(savedCredit, paymentAmount);
        creditPaymentHistoryRepository.save(paymentHistory);
        
        // Record income transaction for credit payment
        incomeExpenseService.recordIncomeFromCreditPayment(savedCredit, paymentAmount);
        
        return savedCredit;
    }

    public List<CreditPaymentHistory> getPaymentHistory(Long creditId) {
        return creditPaymentHistoryRepository.findByCreditIdOrderByPaymentDateDesc(creditId);
    }

    public void deleteCredit(Long id) {
        // Check if credit exists
        if (!creditRepository.existsById(id)) {
            throw new RuntimeException("Credit not found with id: " + id);
        }
        
        // First, set credit_id to NULL in transaction_history to avoid foreign key constraint violation
        entityManager.createNativeQuery("UPDATE transaction_history SET credit_id = NULL WHERE credit_id = :creditId")
                .setParameter("creditId", id)
                .executeUpdate();
        
        // Delete payment history (cascade should handle this, but being explicit)
        creditPaymentHistoryRepository.deleteAll(creditPaymentHistoryRepository.findByCreditIdOrderByPaymentDateDesc(id));
        
        // Now delete the credit
        creditRepository.deleteById(id);
    }

    private void updateCreditStatus(Credit credit) {
        if (credit.getRemainingAmount().compareTo(BigDecimal.ZERO) <= 0) {
            credit.setStatus(Credit.CreditStatus.PAID);
        } else if (credit.getPaidAmount().compareTo(BigDecimal.ZERO) > 0) {
            credit.setStatus(Credit.CreditStatus.PARTIAL);
        } else {
            credit.setStatus(Credit.CreditStatus.PENDING);
        }
    }
}
