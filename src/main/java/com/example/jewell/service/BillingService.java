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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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

    @Autowired
    private PromoCodeService promoCodeService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private LoyaltyTransactionRepository loyaltyTransactionRepository;

    @Autowired
    private GiftVoucherService giftVoucherService;

    /** Points earned per 100 rupees of bill (configurable). */
    private static final int LOYALTY_POINTS_PER_100 = 1;

    public List<Billing> getAllBills() {
        return billingRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    public Page<Billing> getAllBillsPaginated(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return billingRepository.findAll(pageable);
    }

    public List<Billing> searchBills(String query) {
        return billingRepository.searchBills(query).stream()
                .sorted(Comparator.comparing(Billing::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    public Page<Billing> searchBillsPaginated(String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
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
                        if (item.getWeightGrams() == null && s.getWeightGrams() != null) item.setWeightGrams(s.getWeightGrams());
                        if (item.getCarat() == null && s.getCarat() != null) item.setCarat(s.getCarat());
                        if (item.getDiamondCarat() == null && s.getDiamondCarat() != null) item.setDiamondCarat(s.getDiamondCarat());
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

        BigDecimal discount = billing.getDiscountAmount() != null ? billing.getDiscountAmount() : BigDecimal.ZERO;
        if (billing.getPromoCode() != null && !billing.getPromoCode().trim().isEmpty()) {
            BigDecimal promoDiscount = promoCodeService.validateAndGetDiscount(billing.getPromoCode().trim(), totalAmount).orElse(BigDecimal.ZERO);
            discount = discount.add(promoDiscount);
            promoCodeService.applyAndIncrementUsage(billing.getPromoCode().trim(), totalAmount);
        }
        BigDecimal makingCharges = billing.getMakingCharges() != null ? billing.getMakingCharges() : BigDecimal.ZERO;
        BigDecimal finalAmount = totalAmount.subtract(discount).add(makingCharges);

        // Gift voucher: reduce final amount by voucher value
        BigDecimal voucherDiscount = BigDecimal.ZERO;
        if (billing.getVoucherCode() != null && !billing.getVoucherCode().trim().isEmpty()) {
            voucherDiscount = giftVoucherService.validateAndGetAmount(billing.getVoucherCode().trim()).orElse(BigDecimal.ZERO);
            if (voucherDiscount.compareTo(finalAmount) > 0) voucherDiscount = finalAmount;
            if (voucherDiscount.compareTo(BigDecimal.ZERO) > 0) {
                finalAmount = finalAmount.subtract(voucherDiscount);
                discount = discount.add(voucherDiscount);
            }
        }

        // Loyalty redeem: reduce final amount by points (1 point = 1 rupee)
        int toRedeem = 0;
        if (billing.getRedeemPoints() != null && billing.getRedeemPoints() > 0 && billing.getCustomer() != null && billing.getCustomer().getId() != null) {
            Customer cust = customerRepository.findById(billing.getCustomer().getId()).orElse(null);
            if (cust != null && cust.getLoyaltyPoints() != null && cust.getLoyaltyPoints().compareTo(BigDecimal.ZERO) > 0) {
                int currentPoints = cust.getLoyaltyPoints().intValue();
                toRedeem = Math.min(billing.getRedeemPoints(), currentPoints);
                if (toRedeem > 0) {
                    BigDecimal redeemValue = BigDecimal.valueOf(toRedeem);
                    finalAmount = finalAmount.subtract(redeemValue);
                    discount = discount.add(redeemValue);
                }
            }
        }
        billing.setFinalAmount(finalAmount);
        billing.setDiscountAmount(discount);

        // Handle paid amount: support split payment (paymentBreakdown) or single method + amount
        BigDecimal paidAmount = BigDecimal.ZERO;
        String breakdown = billing.getPaymentBreakdown();
        if (breakdown != null && !breakdown.trim().isEmpty()) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                List<Map<String, Object>> payments = mapper.readValue(breakdown, new TypeReference<List<Map<String, Object>>>() {});
                for (Map<String, Object> p : payments) {
                    Object amt = p.get("amount");
                    if (amt != null) {
                        if (amt instanceof Number) paidAmount = paidAmount.add(BigDecimal.valueOf(((Number) amt).doubleValue()));
                        else paidAmount = paidAmount.add(new BigDecimal(amt.toString()));
                    }
                }
                paidAmount = paidAmount.setScale(2, java.math.RoundingMode.HALF_UP);
                billing.setPaidAmount(paidAmount);
                billing.setPaymentMethod(Billing.PaymentMethod.MIXED);
            } catch (Exception e) {
                // fallback to single payment
                paidAmount = billing.getPaidAmount() != null ? billing.getPaidAmount() : BigDecimal.ZERO;
                billing.setPaidAmount(paidAmount);
            }
        } else {
            paidAmount = billing.getPaidAmount() != null ? billing.getPaidAmount() : BigDecimal.ZERO;
            if (billing.getPaymentMethod() == Billing.PaymentMethod.CASH && paidAmount.compareTo(BigDecimal.ZERO) == 0) {
                paidAmount = finalAmount;
            }
            billing.setPaidAmount(paidAmount);
        }
        
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

        // Loyalty: earn points (1 per 100 rupees) and apply redeem
        if (savedBilling.getCustomer() != null && savedBilling.getCustomer().getId() != null) {
            Customer cust = customerRepository.findById(savedBilling.getCustomer().getId()).orElse(null);
            if (cust != null) {
                BigDecimal finalAmt = savedBilling.getFinalAmount();
                if (finalAmt != null && finalAmt.compareTo(BigDecimal.ZERO) > 0) {
                    int pointsEarned = finalAmt.divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN).intValue() * LOYALTY_POINTS_PER_100;
                    if (pointsEarned > 0) {
                        BigDecimal current = cust.getLoyaltyPoints() != null ? cust.getLoyaltyPoints() : BigDecimal.ZERO;
                        cust.setLoyaltyPoints(current.add(BigDecimal.valueOf(pointsEarned)));
                        customerRepository.save(cust);
                        LoyaltyTransaction earn = new LoyaltyTransaction();
                        earn.setCustomer(cust);
                        earn.setType(LoyaltyTransaction.TransactionType.EARN);
                        earn.setPoints(BigDecimal.valueOf(pointsEarned));
                        earn.setBilling(savedBilling);
                        earn.setDescription("Earned on Bill " + savedBilling.getBillNumber());
                        loyaltyTransactionRepository.save(earn);
                    }
                }
                if (toRedeem > 0) {
                    BigDecimal current = cust.getLoyaltyPoints() != null ? cust.getLoyaltyPoints() : BigDecimal.ZERO;
                    cust.setLoyaltyPoints(current.subtract(BigDecimal.valueOf(toRedeem)));
                    customerRepository.save(cust);
                    LoyaltyTransaction redeem = new LoyaltyTransaction();
                    redeem.setCustomer(cust);
                    redeem.setType(LoyaltyTransaction.TransactionType.REDEEM);
                    redeem.setPoints(BigDecimal.valueOf(toRedeem));
                    redeem.setBilling(savedBilling);
                    redeem.setDescription("Redeemed on Bill " + savedBilling.getBillNumber());
                    loyaltyTransactionRepository.save(redeem);
                }
            }
        }

        // Mark gift voucher as redeemed if used
        if (billing.getVoucherCode() != null && !billing.getVoucherCode().trim().isEmpty() && voucherDiscount.compareTo(BigDecimal.ZERO) > 0) {
            giftVoucherService.redeemVoucher(billing.getVoucherCode().trim(), savedBilling);
        }

        return savedBilling;
    }

    public void sendBillViaEmail(Long billingId, String receiptType) {
        Billing billing = billingRepository.findById(billingId)
                .orElseThrow(() -> new RuntimeException("Billing not found"));
        
        emailService.sendBillEmail(billing, receiptType != null ? receiptType.toUpperCase() : "NORMAL");
        billing.setEmailSent(true);
        billingRepository.save(billing);
    }

    public void sendBillViaWhatsApp(Long billingId, String receiptType) {
        Billing billing = billingRepository.findById(billingId)
                .orElseThrow(() -> new RuntimeException("Billing not found"));
        
        whatsAppService.sendBillWhatsApp(billing, receiptType != null ? receiptType.toUpperCase() : "NORMAL");
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
