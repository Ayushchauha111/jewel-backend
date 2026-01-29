package com.example.jewell.service;

import com.example.jewell.model.*;
import com.example.jewell.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class BillingService {
    @Autowired
    private BillingRepository billingRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private CreditRepository creditRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private WhatsAppService whatsAppService;

    @Autowired
    private IncomeExpenseService incomeExpenseService;

    public List<Billing> getAllBills() {
        return billingRepository.findAll();
    }

    public Page<Billing> getAllBillsPaginated(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return billingRepository.findAll(pageable);
    }

    public List<Billing> searchBills(String query) {
        return billingRepository.searchBills(query);
    }

    public Page<Billing> searchBillsPaginated(String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return billingRepository.searchBills(query, pageable);
    }

    public Optional<Billing> getBillById(Long id) {
        return billingRepository.findById(id);
    }

    public Optional<Billing> getBillByBillNumber(String billNumber) {
        return billingRepository.findByBillNumber(billNumber);
    }

    public Billing createBill(Billing billing) {
        // Generate bill number
        if (billing.getBillNumber() == null || billing.getBillNumber().isEmpty()) {
            billing.setBillNumber(generateBillNumber());
        }

        // Set billing reference and item name/articleCode from stock for each item
        if (billing.getItems() != null) {
            for (BillingItem item : billing.getItems()) {
                item.setBilling(billing);
                if (item.getStock() != null && item.getStock().getId() != null) {
                    Stock s = stockRepository.findById(item.getStock().getId()).orElse(null);
                    if (s != null) {
                        if (item.getItemName() == null || item.getItemName().isEmpty()) {
                            item.setItemName(s.getArticleName());
                        }
                        item.setArticleCode(s.getArticleCode());
                    }
                }
            }
        }

        // Calculate totals (filter out null prices)
        BigDecimal totalAmount = BigDecimal.ZERO;
        if (billing.getItems() != null) {
            totalAmount = billing.getItems().stream()
                    .filter(item -> item.getTotalPrice() != null)
                    .map(BillingItem::getTotalPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        billing.setTotalAmount(totalAmount);
        
        BigDecimal finalAmount = totalAmount.subtract(
            billing.getDiscountAmount() != null ? billing.getDiscountAmount() : BigDecimal.ZERO
        );
        billing.setFinalAmount(finalAmount);

        // Handle paid amount - default to final amount if not specified
        BigDecimal paidAmount = billing.getPaidAmount() != null ? billing.getPaidAmount() : BigDecimal.ZERO;
        
        // If payment method is CASH and no paid amount specified, assume full payment
        if (billing.getPaymentMethod() == Billing.PaymentMethod.CASH && paidAmount.compareTo(BigDecimal.ZERO) == 0) {
            paidAmount = finalAmount;
        }
        
        billing.setPaidAmount(paidAmount);
        
        // Set payment status based on paid amount
        if (paidAmount.compareTo(finalAmount) >= 0) {
            billing.setPaymentStatus(Billing.PaymentStatus.PAID);
        } else if (paidAmount.compareTo(BigDecimal.ZERO) > 0) {
            billing.setPaymentStatus(Billing.PaymentStatus.PARTIAL);
        } else {
            billing.setPaymentStatus(Billing.PaymentStatus.PENDING);
        }

        // Validate stock quantity before saving: total ordered must not exceed available
        if (billing.getItems() != null) {
            Map<Long, Integer> stockIdToQtyRequested = new HashMap<>();
            for (BillingItem item : billing.getItems()) {
                if (item.getStock() != null && item.getStock().getId() != null && item.getQuantity() != null && item.getQuantity() > 0) {
                    Long sid = item.getStock().getId();
                    stockIdToQtyRequested.merge(sid, item.getQuantity(), Integer::sum);
                }
            }
            for (Map.Entry<Long, Integer> e : stockIdToQtyRequested.entrySet()) {
                Stock stock = stockRepository.findById(e.getKey())
                        .orElseThrow(() -> new RuntimeException("Stock not found with id: " + e.getKey()));
                int available = stock.getQuantity() != null ? stock.getQuantity() : 0;
                if (e.getValue() > available) {
                    throw new IllegalArgumentException("Insufficient stock for " + stock.getArticleName() + " (" + stock.getArticleCode() + "): only " + available + " available, " + e.getValue() + " requested.");
                }
            }
        }

        // Save billing
        Billing savedBilling = billingRepository.save(billing);

        // Deduct sold quantity from stock; set SOLD only when quantity reaches 0
        if (savedBilling.getItems() != null) {
            Map<Long, Integer> stockIdToQtySold = new HashMap<>();
            for (BillingItem item : savedBilling.getItems()) {
                if (item.getStock() != null && item.getStock().getId() != null && item.getQuantity() != null && item.getQuantity() > 0) {
                    Long sid = item.getStock().getId();
                    stockIdToQtySold.merge(sid, item.getQuantity(), Integer::sum);
                }
            }
            for (Map.Entry<Long, Integer> e : stockIdToQtySold.entrySet()) {
                Stock stock = stockRepository.findById(e.getKey())
                        .orElseThrow(() -> new RuntimeException("Stock not found with id: " + e.getKey()));
                int currentQty = stock.getQuantity() != null ? stock.getQuantity() : 0;
                int sold = e.getValue();
                int newQty = Math.max(0, currentQty - sold);
                stock.setQuantity(newQty);
                if (newQty <= 0) {
                    stock.setStatus(Stock.StockStatus.SOLD);
                }
                stockRepository.save(stock);
            }
        }

        // Create credit entry if payment method is CREDIT or if partial payment was made
        BigDecimal remainingAmount = finalAmount.subtract(paidAmount);
        
        if (billing.getPaymentMethod() == Billing.PaymentMethod.CREDIT) {
            // Full credit for CREDIT payment method
            Credit credit = new Credit();
            credit.setCustomer(billing.getCustomer());
            credit.setBilling(savedBilling);
            credit.setCreditAmount(finalAmount);
            credit.setRemainingAmount(finalAmount);
            credit.setStatus(Credit.CreditStatus.PENDING);
            credit.setDescription("Credit for Bill: " + savedBilling.getBillNumber());
            creditRepository.save(credit);
        } else if (remainingAmount.compareTo(BigDecimal.ZERO) > 0) {
            // Partial payment - create credit for remaining amount
            Credit credit = new Credit();
            credit.setCustomer(billing.getCustomer());
            credit.setBilling(savedBilling);
            credit.setCreditAmount(remainingAmount);
            credit.setRemainingAmount(remainingAmount);
            credit.setStatus(Credit.CreditStatus.PENDING);
            credit.setDescription("Partial payment - Remaining amount for Bill: " + savedBilling.getBillNumber() + 
                                 " (Paid: ₹" + paidAmount + ", Remaining: ₹" + remainingAmount + ")");
            creditRepository.save(credit);
        }

        // Record income transaction for the paid amount
        if (paidAmount.compareTo(BigDecimal.ZERO) > 0 && savedBilling.getPaymentStatus() == Billing.PaymentStatus.PAID) {
            incomeExpenseService.recordIncomeFromBilling(savedBilling);
        } else if (paidAmount.compareTo(BigDecimal.ZERO) > 0 && savedBilling.getPaymentStatus() == Billing.PaymentStatus.PARTIAL) {
            // For partial payments, record income for the paid amount
            incomeExpenseService.recordIncomeFromBilling(savedBilling);
        }

        return savedBilling;
    }

    public void sendBillViaEmail(Long billingId) {
        Billing billing = billingRepository.findById(billingId)
                .orElseThrow(() -> new RuntimeException("Billing not found"));
        
        emailService.sendBillEmail(billing);
        billing.setEmailSent(true);
        billingRepository.save(billing);
    }

    public void sendBillViaWhatsApp(Long billingId) {
        Billing billing = billingRepository.findById(billingId)
                .orElseThrow(() -> new RuntimeException("Billing not found"));
        
        whatsAppService.sendBillWhatsApp(billing);
        billing.setWhatsappSent(true);
        billingRepository.save(billing);
    }

    private String generateBillNumber() {
        String prefix = "BILL";
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        Long count = billingRepository.count() + 1;
        return String.format("%s-%s-%05d", prefix, date, count);
    }
}
