package com.example.jewell.service;

import com.example.jewell.model.*;
import com.example.jewell.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class BillingReturnService {
    @Autowired
    private BillingReturnRepository billingReturnRepository;

    @Autowired
    private BillingRepository billingRepository;

    @Autowired
    private BillingItemRepository billingItemRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private IncomeExpenseService incomeExpenseService;

    public BillingReturn createReturn(BillingReturn returnRequest) {
        Billing original = billingRepository.findById(returnRequest.getOriginalBilling().getId())
                .orElseThrow(() -> new IllegalArgumentException("Original billing not found"));
        returnRequest.setOriginalBilling(original);

        if (returnRequest.getReturnNumber() == null || returnRequest.getReturnNumber().isEmpty()) {
            returnRequest.setReturnNumber(generateReturnNumber());
        }

        BigDecimal totalRefund = BigDecimal.ZERO;
        if (returnRequest.getItems() != null) {
            for (BillingReturnItem ri : returnRequest.getItems()) {
                ri.setBillingReturn(returnRequest);
                if (ri.getBillingItem() != null && ri.getBillingItem().getId() != null) {
                    BillingItem bi = billingItemRepository.findById(ri.getBillingItem().getId())
                            .orElseThrow(() -> new IllegalArgumentException("Billing item not found: " + ri.getBillingItem().getId()));
                    if (bi.getBilling().getId() != original.getId()) {
                        throw new IllegalArgumentException("Billing item does not belong to original bill");
                    }
                    ri.setStock(bi.getStock());
                    if (ri.getQuantityReturned() == null) ri.setQuantityReturned(1);
                    if (ri.getRefundAmount() == null && bi.getTotalPrice() != null) {
                        ri.setRefundAmount(bi.getTotalPrice().divide(BigDecimal.valueOf(bi.getQuantity()), 2, java.math.RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(ri.getQuantityReturned())));
                    }
                    totalRefund = totalRefund.add(ri.getRefundAmount() != null ? ri.getRefundAmount() : BigDecimal.ZERO);
                    // Restore stock
                    if (bi.getStock() != null && ri.getQuantityReturned() != null && ri.getQuantityReturned() > 0) {
                        Stock stock = stockRepository.findById(bi.getStock().getId())
                                .orElseThrow(() -> new IllegalArgumentException("Stock not found"));
                        int current = stock.getQuantity() != null ? stock.getQuantity() : 0;
                        stock.setQuantity(current + ri.getQuantityReturned());
                        stock.setStatus(Stock.StockStatus.AVAILABLE);
                        stockRepository.save(stock);
                    }
                }
            }
        }
        returnRequest.setTotalRefundAmount(totalRefund);
        BillingReturn saved = billingReturnRepository.save(returnRequest);

        // Record refund as expense (cash-out)
        if (totalRefund.compareTo(BigDecimal.ZERO) > 0 && returnRequest.getRefundMethod() != null) {
            incomeExpenseService.recordExpense(
                    java.time.LocalDate.now(),
                    totalRefund,
                    TransactionHistory.Category.OTHER_EXPENSE,
                    "Return/Exchange: " + saved.getReturnNumber() + " (Bill: " + original.getBillNumber() + ")",
                    mapPaymentMethod(returnRequest.getRefundMethod()),
                    saved.getReturnNumber());
        }

        return saved;
    }

    private TransactionHistory.PaymentMethod mapPaymentMethod(Billing.PaymentMethod m) {
        if (m == null) return TransactionHistory.PaymentMethod.CASH;
        switch (m) {
            case CASH: return TransactionHistory.PaymentMethod.CASH;
            case UPI: return TransactionHistory.PaymentMethod.UPI;
            case CARD: return TransactionHistory.PaymentMethod.CARD;
            case BANK_TRANSFER: return TransactionHistory.PaymentMethod.BANK_TRANSFER;
            default: return TransactionHistory.PaymentMethod.CASH;
        }
    }

    private String generateReturnNumber() {
        String prefix = "RET";
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = billingReturnRepository.count() + 1;
        return String.format("%s-%s-%05d", prefix, date, count);
    }

    public Optional<BillingReturn> getById(Long id) {
        return billingReturnRepository.findById(id);
    }

    public List<BillingReturn> getByOriginalBillingId(Long billingId) {
        return billingReturnRepository.findByOriginalBillingId(billingId);
    }

    public Page<BillingReturn> getAllPaginated(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return billingReturnRepository.findAll(pageable);
    }
}
